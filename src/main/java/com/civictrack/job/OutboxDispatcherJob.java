package com.civictrack.job;

import com.civictrack.service.OutboxService;
import org.jobrunr.jobs.annotations.Job;
import org.springframework.stereotype.Component;

@Component
public class OutboxDispatcherJob {
    private final OutboxService outbox;

    public OutboxDispatcherJob(OutboxService o) {
        outbox = o;
    }

    @Job(name = "Dispatch outbox events")
    public void run() {
        outbox.dispatch();
    }
}