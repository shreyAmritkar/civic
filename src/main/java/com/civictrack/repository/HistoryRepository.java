package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface HistoryRepository extends JpaRepository<StatusHistory, UUID> {
    List<StatusHistory> findByComplaintIdOrderByCreatedAtAsc(UUID id);
}
