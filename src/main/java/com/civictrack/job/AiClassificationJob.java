package com.civictrack.job;

import com.civictrack.repository.*;
import org.jobrunr.jobs.annotations.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.*;
import org.springframework.stereotype.*;
import org.springframework.web.client.*;
import java.util.*;

@Component
public class AiClassificationJob {
    private final ComplaintRepository complaints;
    private final String url;
    private final String model;
    private final String key;
    private final RestTemplate http = new RestTemplate();

    public AiClassificationJob(ComplaintRepository c, @Value("${app.llm.url}") String u,
            @Value("${app.llm.model}") String m, @Value("${app.llm.api-key}") String k) {
        complaints = c;
        url = u;
        model = m;
        key = k;
    }

    @Job(name = "Classify complaint")
    public void classify(UUID id) {
        var complaint = complaints.findById(id).orElseThrow();
        if (url == null || url.isBlank() || model == null || model.isBlank()) {
            complaint.aiStatus = "SKIPPED_NO_PROVIDER";
            complaints.save(complaint);
            return;
        }
        try {
            var headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (key != null && !key.isBlank())
                headers.setBearerAuth(key);
            String prompt = "Classify this civic complaint. Reply ONLY as compact JSON with keys category, priority, summary. Suggested category must be one of ROADS, WASTE, WATER, STREETLIGHTS, DRAINAGE, OTHER. Do not invent facts.\\nTitle: "
                    + complaint.title + "\\nDescription: " + complaint.description;
            var request = Map.of("model", model, "prompt", prompt, "stream", false, "format", "json");
            var result = http.postForObject(url, new HttpEntity<>(request, headers), Map.class);
            Object response = result == null ? null : result.get("response");
            if (response == null || response.toString().isBlank())
                throw new IllegalStateException("Ollama returned no response");
            complaint.aiStatus = "COMPLETED";
            complaint.aiSummary = response.toString();
            complaints.save(complaint);
        } catch (Exception e) {
            complaint.aiStatus = "FAILED";
            complaints.save(complaint);
        }
    }
}
