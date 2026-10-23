package com.civictrack.repository;

import com.civictrack.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.*;
import java.util.*;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    /** SKIP LOCKED: two dispatchers (two containers) never pick up the same row. */
    @Query(value = "SELECT * FROM outbox_events WHERE status = 'PENDING' ORDER BY created_at LIMIT :limit FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> lockPending(@Param("limit") int limit);

    /** Recovery: DISPATCHED for too long means the job vanished (deleted in the dashboard, DB restore...). */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = :to WHERE e.status = :from AND e.dispatchedAt < :cutoff")
    int requeueStuck(@Param("from") OutboxStatus from, @Param("to") OutboxStatus to, @Param("cutoff") Instant cutoff);
}