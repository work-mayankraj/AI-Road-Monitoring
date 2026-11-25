package com.roadguard.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public final class UploadDto {
    private UploadDto() { }

    public record Point(@NotNull Double lat, @NotNull Double lng) { }

    /** img is a JPEG data URL (data:image/jpeg;base64,...) made by the browser. */
    public record FrameIn(@NotBlank String img, Double t) { }

    public record CreateRequest(
            @NotBlank @Size(max = 150) String name,
            @NotBlank @Pattern(regexp = "video|photo", message = "must be video or photo") String mediaType,
            @Size(max = 255) String fileName,
            @Size(max = 255) String gpsFileName,
            Double lat,
            Double lng,
            @Size(max = 5000) List<@Valid Point> track,
            @Size(max = 12) List<@Valid FrameIn> frames) { }

    public record FrameOut(String img, Double t) { }

    public record Out(String id, String name, String mediaType, String fileName, String gps,
                      List<FrameOut> frames, String date, String owner) { }

    public record Result(Out upload, List<DetectionDto.Out> detections) { }
}
