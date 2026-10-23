package com.civictrack.dto;

import jakarta.validation.constraints.*;

public class AuthDtos {
    public record Register(@NotBlank String name, @Email String email, @Size(min = 6) String password) {
    }

    public record Login(@Email String email, @NotBlank String password) {
    }

    public record Token(String token, String name, String role) {
    }
}
