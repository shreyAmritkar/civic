package com.civictrack.config;

import com.civictrack.job.*;
import org.jobrunr.scheduling.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;
import java.time.Duration;

@Configuration
public class JobConfig {
    @Bean
    CommandLineRunner slaSchedule(JobScheduler scheduler, SlaJob job, OutboxDispatcherJob dispatcher) {
        return a -> {
            scheduler.scheduleRecurrently("sla-check", "0 */15 * * * *", job::check);
            scheduler.scheduleRecurrently("outbox-dispatcher", Duration.ofSeconds(15), dispatcher::run);
        };
    }
}