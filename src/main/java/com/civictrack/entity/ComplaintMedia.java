package com.civictrack.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "complaint_media")
public class ComplaintMedia {
    @Id
    public UUID id = UUID.randomUUID();
    @ManyToOne(optional = false)
    @JoinColumn(name = "complaint_id")
    public Complaint complaint;
    @ManyToOne(optional = false)
    @JoinColumn(name = "uploaded_by")
    public User uploadedBy;
    @Column(name = "object_key", nullable = false, unique = true)
    public String objectKey;
    @Column(name = "original_filename", nullable = false)
    public String originalFilename;
    @Column(name = "content_type", nullable = false)
    public String contentType;
    @Column(name = "size_bytes", nullable = false)
    public long sizeBytes;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public MediaStatus status = MediaStatus.PENDING;
    @Column(name = "created_at")
    public Instant createdAt = Instant.now();
    @Column(name = "uploaded_at")
    public Instant uploadedAt;
}