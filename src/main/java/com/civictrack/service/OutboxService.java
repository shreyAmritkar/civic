package com.civictrack.service;

import com.civictrack.entity.*;
import com.civictrack.repository.*;
import com.civictrack.job.NotificationWorker;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;
import java.util.*;

/**
 * Transactional outbox for e-mail notifications.
 * publish()  - called inside the business transaction, so the event exists if and only if the change committed
 * dispatch() - run by the dispatcher job: turns PENDING events into JobRunr jobs (and recovers lost ones)
 * prepare / markSent / recordFailure - used by NotificationWorker, each in its own short transaction
 */
@Service
public class OutboxService {
    public static final int MAX_ATTEMPTS = 5;
    private static final int BATCH_SIZE = 50;
    /** JobRunr's own retries (about an hour in total) finish well before this. */
    private static final Duration STUCK_AFTER = Duration.ofHours(2);

    public record Mail(String to, String subject, String body) {
    }

    private final OutboxRepository outbox;
    private final JobScheduler scheduler;

    public OutboxService(OutboxRepository o, JobScheduler s) {
        outbox = o;
        scheduler = s;
    }

    /** MANDATORY: refuses to run outside a transaction, which would defeat the whole pattern. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(String type, User recipient, Complaint complaint, String subject, String body) {
        OutboxEvent e = new OutboxEvent();
        e.type = type;
        e.recipient = recipient;
        e.complaint = complaint;
        e.subject = subject;
        e.body = body;
        outbox.save(e);
    }

    /** Returns how many events were handed to JobRunr. */
    @Transactional
    public int dispatch() {
        outbox.requeueStuck(OutboxStatus.DISPATCHED, OutboxStatus.PENDING, Instant.now().minus(STUCK_AFTER));
        List<OutboxEvent> batch = outbox.lockPending(BATCH_SIZE);
        for (OutboxEvent e : batch) {
            UUID id = e.id;
            // Enqueue first, mark afterwards: a crash in between means a harmless duplicate job
            // (the worker skips finished events), never a lost notification.
            scheduler.<NotificationWorker>enqueue(w -> w.send(id));
            e.status = OutboxStatus.DISPATCHED;
            e.dispatchedAt = Instant.now();
        }
        return batch.size();
    }

    /** Empty when there is nothing (left) to send for this event. */
    @Transactional
    public Optional<Mail> prepare(UUID id) {
        OutboxEvent e = outbox.findById(id).orElse(null);
        if (e == null || e.status == OutboxStatus.SENT || e.status == OutboxStatus.DEAD)
            return Optional.empty();
        if (e.recipient.email == null || e.recipient.email.isBlank()) {
            e.status = OutboxStatus.DEAD;
            e.lastError = "Recipient has no e-mail address";
            return Optional.empty();
        }
        return Optional.of(new Mail(e.recipient.email, e.subject, e.body));
    }

    @Transactional
    public void markSent(UUID id) {
        outbox.findById(id).ifPresent(e -> {
            e.status = OutboxStatus.SENT;
            e.sentAt = Instant.now();
            e.lastError = null;
        });
    }

    /** Returns true when the event has used up its attempts and is now DEAD (no more retries). */
    @Transactional
    public boolean recordFailure(UUID id, String error) {
        OutboxEvent e = outbox.findById(id).orElse(null);
        if (e == null)
            return true;
        e.attempts++;
        e.lastError = error == null ? "unknown error" : error.substring(0, Math.min(error.length(), 1000));
        if (e.attempts >= MAX_ATTEMPTS)
            e.status = OutboxStatus.DEAD;
        return e.status == OutboxStatus.DEAD;
    }
}