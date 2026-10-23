package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import java.time.*;
import java.util.*;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
    List<Complaint> findByCitizenIdOrderByCreatedAtDesc(UUID id);

    List<Complaint> findByDepartmentIdOrderByCreatedAtDesc(UUID id);

    List<Complaint> findBySlaDeadlineBeforeAndStatusNotIn(Instant at, Collection<ComplaintStatus> statuses);
}
