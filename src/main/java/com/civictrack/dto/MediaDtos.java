package com.civictrack.dto;

import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public class MediaDtos {
    public record PresignRequest(@NotBlank @Size(max = 255) String fileName, @NotBlank String contentType,
            @Positive long sizeBytes) {
    }

    /** What the browser needs to PUT the file straight to S3. */
    public record PresignResponse(UUID mediaId, String uploadUrl, Map<String, String> headers,
            long expiresInSeconds) {
    }

    /** url is a short-lived presigned GET link (null until the upload is confirmed). */
    public record MediaResponse(UUID id, String fileName, String contentType, long sizeBytes, String url,
            Instant createdAt) {
    }
}