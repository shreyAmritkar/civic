package com.civictrack.dto;

import com.civictrack.entity.*;
import jakarta.validation.constraints.*;
import java.util.*;

public class ComplaintDtos {
    public record Create(@NotBlank @Size(max = 160) String title, @NotBlank String description,
            @NotBlank String category, @NotBlank String location, String ward, Priority priority) {
    }

    public record Transition(@NotNull ComplaintStatus status, String note) {
    }

    public record Assign(@NotNull UUID departmentId, UUID officialId) {
    }

    public record DepartmentInput(@NotBlank String name, @NotBlank String categories) {
    }

    public record AssignmentInput(@NotNull UUID officialId, @NotNull UUID departmentId, String ward) {
    }
}
