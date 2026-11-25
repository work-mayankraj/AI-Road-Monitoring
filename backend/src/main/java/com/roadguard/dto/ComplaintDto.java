package com.roadguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class ComplaintDto {
    private ComplaintDto() { }

    public record CreateRequest(
            @NotEmpty List<@NotNull Long> detectionIds,
            @NotBlank @Size(max = 2000) String description,
            @NotBlank @Pattern(regexp = "low|medium|high", message = "must be low, medium or high") String severity) { }

    public record UpdateRequest(@NotBlank @Size(max = 2000) String description) { }

    public record StatusRequest(
            @NotBlank @Pattern(regexp = "pending|in_progress|resolved",
                    message = "must be pending, in_progress or resolved") String status) { }

    /** id is the public reference such as "C-1001". */
    public record Out(String id, String status, String createdAt, String resolvedAt, String userName,
                      String email, List<Long> detectionIds, String description, String severity,
                      Double lat, Double lng, int evidence) { }
}
