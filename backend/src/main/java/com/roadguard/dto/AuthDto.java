package com.roadguard.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDto {
    private AuthDto() { }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 6, max = 100, message = "must be 6-100 characters") String password) { }

    /** role is optional: "user" or "admin" (the tab the person used on the login page). */
    public record LoginRequest(
            @NotBlank String email,
            @NotBlank String password,
            String role) { }

    public record UserInfo(String name, String email, String role) { }

    public record AuthResponse(String token, UserInfo user) { }
}
