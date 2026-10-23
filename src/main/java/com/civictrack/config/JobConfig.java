package com.civictrack.config;

import com.civictrack.job.*;
import org.jobrunr.scheduling.*;
import org.springframework.boot.*;
import org.springframework.context.annotation.*;

@Configuration
public class JobConfig {
    @Bean
    CommandLineRunner slaSchedule(JobScheduler scheduler, SlaJob job) {
        return a -> scheduler.scheduleRecurrently("sla-check", "0 */15 * * * *", job::check);
    }
}
