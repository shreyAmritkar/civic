package com.civictrack.job;

import com.civictrack.service.OutboxService;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import java.util.UUID;

/** Sends one outbox event as an e-mail. A thrown exception makes JobRunr retry with back-off. */
@Component
public class NotificationWorker {
    private final OutboxService outbox;
    private final JavaMailSender mail;
    private final String from;

    public NotificationWorker(OutboxService o, JavaMailSender m,
            @Value("${app.mail.from:noreply@civictrack.local}") String f) {
        outbox = o;
        mail = m;
        from = f;
    }

    @Job(name = "Send notification e-mail", retries = 10)
    public void send(UUID eventId) {
        var prepared = outbox.prepare(eventId);
        if (prepared.isEmpty())
            return; // already sent, given up on, or deleted
        OutboxService.Mail m = prepared.get();
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(m.to());
            msg.setSubject(m.subject());
            msg.setText(m.body());
            mail.send(msg);
        } catch (Exception e) {
            boolean dead = outbox.recordFailure(eventId, e.getClass().getSimpleName() + ": " + e.getMessage());
            if (dead)
                return; // attempts used up: stop here, the row stays DEAD for someone to look at
            throw new IllegalStateException("E-mail failed, JobRunr will retry", e);
        }
        outbox.markSent(eventId);
    }
}