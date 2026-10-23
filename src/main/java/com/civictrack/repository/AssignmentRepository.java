package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface AssignmentRepository extends JpaRepository<OfficialAssignment, UUID> {
    List<OfficialAssignment> findByDepartmentIdAndActiveTrue(UUID id);

    List<OfficialAssignment> findByOfficialIdAndActiveTrue(UUID id);
}
