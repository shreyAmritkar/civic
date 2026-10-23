package com.civictrack.service;

import com.civictrack.exception.StorageException;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import java.net.URI;
import java.time.Duration;
import java.util.*;

/**
 * The only class that talks to S3. The browser uploads straight to S3 with a presigned PUT URL;
 * this server only signs URLs and checks (HEAD) that the object really arrived.
 * If S3_BUCKET is not set the app still starts and evidence upload is simply switched off.
 */
@Service
public class S3StorageService {
    public record UploadTicket(String url, Map<String, String> headers) {
    }

    public record ObjectInfo(long size, String contentType) {
    }

    private static final Duration UPLOAD_TTL = Duration.ofMinutes(10);
    private static final Duration DOWNLOAD_TTL = Duration.ofMinutes(10);

    private final String bucket;
    private final S3Presigner presigner;
    private final S3Client client;

    public S3StorageService(@Value("${app.s3.bucket:}") String bucket, @Value("${app.s3.region:ap-south-1}") String region,
            @Value("${app.s3.access-key:}") String accessKey, @Value("${app.s3.secret-key:}") String secretKey,
            @Value("${app.s3.endpoint:}") String endpoint) {
        this.bucket = bucket == null ? "" : bucket.trim();
        if (this.bucket.isEmpty()) {
            presigner = null;
            client = null;
            return;
        }
        // Keys from configuration if given, otherwise the standard AWS chain (env vars, ~/.aws profile, role)
        AwsCredentialsProvider credentials = accessKey.isBlank() || secretKey.isBlank()
                ? DefaultCredentialsProvider.create()
                : StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
        Region awsRegion = Region.of(region.trim());
        S3Presigner.Builder presignerBuilder = S3Presigner.builder().region(awsRegion).credentialsProvider(credentials);
        S3ClientBuilder clientBuilder = S3Client.builder().region(awsRegion).credentialsProvider(credentials);
        if (!endpoint.isBlank()) { // optional: MinIO / LocalStack
            URI uri = URI.create(endpoint.trim());
            S3Configuration pathStyle = S3Configuration.builder().pathStyleAccessEnabled(true).build();
            presignerBuilder.endpointOverride(uri).serviceConfiguration(pathStyle);
            clientBuilder.endpointOverride(uri).serviceConfiguration(pathStyle);
        }
        presigner = presignerBuilder.build();
        client = clientBuilder.build();
    }

    public boolean isEnabled() {
        return presigner != null;
    }

    public void requireEnabled() {
        if (!isEnabled())
            throw new StorageException("Evidence upload is not configured on this server (set S3_BUCKET).");
    }

    public long uploadTtlSeconds() {
        return UPLOAD_TTL.toSeconds();
    }

    /** Signs a PUT for exactly this key, content type and size. S3 rejects anything else. */
    public UploadTicket presignUpload(String key, String contentType, long sizeBytes) {
        requireEnabled();
        try {
            PutObjectRequest put = PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                    .contentLength(sizeBytes).build();
            PresignedPutObjectRequest signed = presigner.presignPutObject(
                    PutObjectPresignRequest.builder().signatureDuration(UPLOAD_TTL).putObjectRequest(put).build());
            // Headers the browser must send unchanged (the browser adds Host and Content-Length itself)
            Map<String, String> headers = new LinkedHashMap<>();
            signed.signedHeaders().forEach((name, values) -> {
                String lower = name.toLowerCase(Locale.ROOT);
                if (!lower.equals("host") && !lower.equals("content-length"))
                    headers.put(name, String.join(",", values));
            });
            return new UploadTicket(signed.url().toString(), headers);
        } catch (SdkException e) {
            throw new StorageException("Could not create an upload link: " + e.getMessage());
        }
    }

    public String presignDownload(String key) {
        requireEnabled();
        try {
            GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
            return presigner.presignGetObject(
                    GetObjectPresignRequest.builder().signatureDuration(DOWNLOAD_TTL).getObjectRequest(get).build())
                    .url().toString();
        } catch (SdkException e) {
            throw new StorageException("Could not create a download link: " + e.getMessage());
        }
    }

    /** Empty if the object does not exist (yet). */
    public Optional<ObjectInfo> head(String key) {
        requireEnabled();
        try {
            HeadObjectResponse r = client.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return Optional.of(new ObjectInfo(r.contentLength(), r.contentType()));
        } catch (S3Exception e) {
            if (e.statusCode() == 404)
                return Optional.empty();
            throw new StorageException("Could not verify the upload (storage answered HTTP " + e.statusCode()
                    + "). Check the IAM permissions: s3:GetObject and s3:ListBucket are needed.");
        } catch (SdkException e) {
            throw new StorageException("Could not reach storage: " + e.getMessage());
        }
    }

    public void deleteQuietly(String key) {
        if (!isEnabled())
            return;
        try {
            client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (RuntimeException ignored) {
        }
    }

    @PreDestroy
    void close() {
        if (presigner != null)
            presigner.close();
        if (client != null)
            client.close();
    }
}