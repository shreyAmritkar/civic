package com.civictrack.controller;

import com.civictrack.dto.MediaDtos.*;
import com.civictrack.entity.*;
import com.civictrack.service.*;
import jakarta.validation.*;
import org.springframework.security.core.annotation.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/complaints/{complaintId}/media")
public class MediaController {
    private final MediaService service;

    public MediaController(MediaService s) {
        service = s;
    }

    /** Step 1: ask for a presigned upload URL. */
    @PostMapping("/presign")
    public PresignResponse presign(@PathVariable UUID complaintId, @AuthenticationPrincipal User u,
            @Valid @RequestBody PresignRequest r) {
        return service.presign(complaintId, r, u);
    }

    /** Step 3: tell the server the browser finished uploading to S3. */
    @PostMapping("/{mediaId}/confirm")
    public MediaResponse confirm(@PathVariable UUID complaintId, @PathVariable UUID mediaId,
            @AuthenticationPrincipal User u) {
        return service.confirm(complaintId, mediaId, u);
    }

    @GetMapping
    public List<MediaResponse> list(@PathVariable UUID complaintId, @AuthenticationPrincipal User u) {
        return service.list(complaintId, u);
    }
}