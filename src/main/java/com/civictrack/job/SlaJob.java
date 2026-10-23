package com.civictrack.job;

import com.civictrack.service.*;
import org.jobrunr.jobs.annotations.*;
import org.springframework.stereotype.*;

@Component
public class SlaJob {
    private final ComplaintService service;

    public SlaJob(ComplaintService s) {
        service = s;
    }

    @Job(name = "Check overdue complaints")
    public void check() {
        service.markOverdue();
    }
}
