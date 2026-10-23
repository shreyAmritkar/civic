package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import java.time.*;
import java.util.*;

public interface ComplaintMediaRepository extends JpaRepository<ComplaintMedia, UUID> {
    List<ComplaintMedia> findByComplaintIdAndStatusOrderByCreatedAtAsc(UUID complaintId, MediaStatus status);

    List<ComplaintMedia> findByComplaintIdAndStatusAndCreatedAtBefore(UUID complaintId, MediaStatus status,
            Instant before);

    Optional<ComplaintMedia> findByIdAndComplaintId(UUID id, UUID complaintId);

    long countByComplaintId(UUID complaintId);
}