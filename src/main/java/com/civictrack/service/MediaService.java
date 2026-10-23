package com.civictrack.service;

import com.civictrack.dto.MediaDtos.*;
import com.civictrack.entity.*;
import com.civictrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Evidence upload flow (presigned URL architecture):
 * 1. presign: validate, store a PENDING row (object key + metadata), return a signed PUT URL
 * 2. the browser uploads the file directly to S3
 * 3. confirm: HEAD the object, check size and type, mark the row UPLOADED
 * 4. list: return UPLOADED files with short-lived signed GET URLs
 */
@Service
public class MediaService {
    private static final int MAX_FILES_PER_COMPLAINT = 5;
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 50L * 1024 * 1024;
    /** Allowed content types and the extension used in the S3 object key. */
    private static final Map<String, String> ALLOWED = Map.of("image/jpeg", "jpg", "image/png", "png", "image/webp",
            "webp", "video/mp4", "mp4", "video/webm", "webm", "video/quicktime", "mov");

    private final ComplaintMediaRepository media;
    private final ComplaintRepository complaints;
    private final AssignmentRepository assignments;
    private final S3StorageService storage;

    public MediaService(ComplaintMediaRepository m, ComplaintRepository c, AssignmentRepository a,
            S3StorageService s) {
        media = m;
        complaints = c;
        assignments = a;
        storage = s;
    }

    @Transactional
    public PresignResponse presign(UUID complaintId, PresignRequest in, User actor) {
        storage.requireEnabled();
        Complaint c = ownedComplaint(complaintId, actor);
        String type = in.contentType().trim().toLowerCase(Locale.ROOT);
        String extension = ALLOWED.get(type);
        if (extension == null)
            throw new IllegalArgumentException("Unsupported file type. Allowed: JPEG, PNG, WebP, MP4, WebM, MOV.");
        long limit = type.startsWith("video/") ? MAX_VIDEO_BYTES : MAX_IMAGE_BYTES;
        if (in.sizeBytes() > limit)
            throw new IllegalArgumentException("File is too large (limit " + limit / 1024 / 1024 + " MB for this type).");
        purgeStale(c.id);
        if (media.countByComplaintId(c.id) >= MAX_FILES_PER_COMPLAINT)
            throw new IllegalArgumentException("A complaint can have at most " + MAX_FILES_PER_COMPLAINT + " files.");

        ComplaintMedia m = new ComplaintMedia();
        m.complaint = c;
        m.uploadedBy = actor;
        // The key never contains user-supplied text; the original filename lives only in the database
        m.objectKey = "complaints/" + c.id + "/" + m.id + "." + extension;
        m.originalFilename = cleanName(in.fileName());
        m.contentType = type;
        m.sizeBytes = in.sizeBytes();
        media.save(m);

        S3StorageService.UploadTicket ticket = storage.presignUpload(m.objectKey, type, m.sizeBytes);
        return new PresignResponse(m.id, ticket.url(), ticket.headers(), storage.uploadTtlSeconds());
    }

    @Transactional
    public MediaResponse confirm(UUID complaintId, UUID mediaId, User actor) {
        storage.requireEnabled();
        Complaint c = ownedComplaint(complaintId, actor);
        ComplaintMedia m = media.findByIdAndComplaintId(mediaId, c.id)
                .orElseThrow(() -> new NoSuchElementException("Upload not found"));
        if (m.status == MediaStatus.UPLOADED)
            return toResponse(m);
        S3StorageService.ObjectInfo info = storage.head(m.objectKey).orElseThrow(
                () -> new IllegalArgumentException("The file has not reached storage. Please upload it again."));
        if (info.size() != m.sizeBytes || !m.contentType.equalsIgnoreCase(info.contentType())) {
            storage.deleteQuietly(m.objectKey);
            throw new IllegalArgumentException("The stored file does not match the request and was discarded.");
        }
        m.status = MediaStatus.UPLOADED;
        m.uploadedAt = Instant.now();
        return toResponse(media.save(m));
    }

    @Transactional(readOnly = true)
    public List<MediaResponse> list(UUID complaintId, User actor) {
        Complaint c = visibleComplaint(complaintId, actor);
        if (!storage.isEnabled())
            return List.of();
        return media.findByComplaintIdAndStatusOrderByCreatedAtAsc(c.id, MediaStatus.UPLOADED).stream()
                .map(this::toResponse).toList();
    }

    private MediaResponse toResponse(ComplaintMedia m) {
        String url = m.status == MediaStatus.UPLOADED ? storage.presignDownload(m.objectKey) : null;
        return new MediaResponse(m.id, m.originalFilename, m.contentType, m.sizeBytes, url, m.createdAt);
    }

    /** Uploads that were never confirmed within an hour no longer count against the file limit. */
    private void purgeStale(UUID complaintId) {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
        for (ComplaintMedia m : media.findByComplaintIdAndStatusAndCreatedAtBefore(complaintId, MediaStatus.PENDING,
                cutoff)) {
            storage.deleteQuietly(m.objectKey);
            media.delete(m);
        }
    }

    /** Same visibility rules as viewing the complaint itself. */
    private Complaint visibleComplaint(UUID id, User u) {
        Complaint c = complaints.findById(id).orElseThrow(() -> new NoSuchElementException("Complaint not found"));
        if (u.role == Role.CITIZEN && !c.citizen.id.equals(u.id))
            throw new SecurityException("Not your complaint");
        if (u.role == Role.OFFICIAL && (c.department == null || assignments.findByOfficialIdAndActiveTrue(u.id).stream()
                .noneMatch(a -> a.department.id.equals(c.department.id))))
            throw new SecurityException("Not assigned to this department");
        return c;
    }

    /** Only the citizen who filed the complaint may attach evidence. */
    private Complaint ownedComplaint(UUID id, User u) {
        Complaint c = visibleComplaint(id, u);
        if (u.role != Role.CITIZEN)
            throw new SecurityException("Only the citizen who filed the complaint can add evidence");
        return c;
    }

    private static String cleanName(String name) {
        String n = name == null ? "" : name.replaceAll("[\\\\/\\r\\n\\t]", "_").trim();
        if (n.isEmpty())
            n = "file";
        return n.length() > 150 ? n.substring(0, 150) : n;
    }
}