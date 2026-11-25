package com.roadguard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class DetectionDto {
    private DetectionDto() { }

    public record Box(int x, int y, int w, int h) { }

    /** Field names match what the frontend (app.js) already uses. */
    public record Out(Long id, int n, String sessionId, String owner, String kind, int fi, int frame,
                      String time, int conf, Double lat, Double lng, String status, Box bx) { }

    public record StatusRequest(
            @NotBlank @Pattern(regexp = "pending|confirmed|rejected",
                    message = "must be pending, confirmed or rejected") String status) { }
}
