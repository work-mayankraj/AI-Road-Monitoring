package com.roadguard.dto;

import com.roadguard.entity.Complaint;
import com.roadguard.entity.Detection;
import com.roadguard.entity.Upload;
import com.roadguard.entity.User;
import com.roadguard.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/** Entity -> JSON shape. Call these inside a transaction (they read lazy relations). */
public final class Mappers {
    private Mappers() { }

    private static String lower(Enum<?> e) { return e.name().toLowerCase(); }

    private static String date(Instant i) {
        return i == null ? null : i.atZone(ZoneId.systemDefault()).toLocalDate().toString();
    }

    public static AuthDto.UserInfo user(User u) {
        return new AuthDto.UserInfo(u.getName(), u.getEmail(), lower(u.getRole()));
    }

    public static UploadDto.Out upload(Upload u) {
        List<UploadDto.FrameOut> frames = u.getFrames().stream()
                .map(f -> new UploadDto.FrameOut(f.getImageUrl(), f.getTimeSec())).toList();
        return new UploadDto.Out(String.valueOf(u.getId()), u.getName(), lower(u.getMediaKind()), u.getFileName(),
                u.getGpsFileName(), frames, date(u.getCreatedAt()), u.getUser().getEmail());
    }

    public static DetectionDto.Out detection(Detection d) {
        Upload up = d.getUpload();
        return new DetectionDto.Out(d.getId(), d.getSeq(), String.valueOf(up.getId()), up.getUser().getEmail(),
                lower(up.getMediaKind()), d.getFrameIndex(), d.getFrameNumber(), d.getTimeLabel(),
                d.getConfidence(), d.getLatitude(), d.getLongitude(), lower(d.getStatus()),
                new DetectionDto.Box(d.getBoxX(), d.getBoxY(), d.getBoxW(), d.getBoxH()));
    }

    public static String complaintRef(Long id) { return "C-" + (1000 + id); }

    public static Long parseComplaintRef(String ref) {
        try {
            if (ref != null && ref.startsWith("C-")) {
                long id = Long.parseLong(ref.substring(2)) - 1000;
                if (id > 0) return id;
            }
        } catch (NumberFormatException ignored) { }
        throw new ApiException(HttpStatus.NOT_FOUND, "Complaint not found.");
    }

    public static ComplaintDto.Out complaint(Complaint c) {
        List<Long> ids = c.getDetections().stream().map(Detection::getId).sorted(Comparator.naturalOrder()).toList();
        return new ComplaintDto.Out(complaintRef(c.getId()), lower(c.getStatus()), date(c.getCreatedAt()),
                date(c.getResolvedAt()), c.getUser().getName(), c.getUser().getEmail(), ids, c.getDescription(),
                lower(c.getSeverity()), c.getLatitude(), c.getLongitude(), ids.size());
    }
}
