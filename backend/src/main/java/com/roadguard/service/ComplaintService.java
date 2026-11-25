package com.roadguard.service;

import com.roadguard.dto.ComplaintDto;
import com.roadguard.dto.Mappers;
import com.roadguard.entity.Complaint;
import com.roadguard.entity.ComplaintStatus;
import com.roadguard.entity.Detection;
import com.roadguard.entity.DetectionStatus;
import com.roadguard.entity.Severity;
import com.roadguard.exception.ApiException;
import com.roadguard.repository.ComplaintRepository;
import com.roadguard.repository.DetectionRepository;
import com.roadguard.repository.UserRepository;
import com.roadguard.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final DetectionRepository detections;
    private final UserRepository users;

    public ComplaintService(ComplaintRepository complaints, DetectionRepository detections, UserRepository users) {
        this.complaints = complaints;
        this.detections = detections;
        this.users = users;
    }

    /** One complaint can include several confirmed frames of the caller's own uploads. */
    @Transactional
    public ComplaintDto.Out create(AuthUser me, ComplaintDto.CreateRequest r) {
        Set<Long> ids = new LinkedHashSet<>(r.detectionIds());
        Map<Long, Detection> found = detections.findAllById(ids).stream()
                .collect(Collectors.toMap(Detection::getId, Function.identity()));

        Complaint c = new Complaint();
        for (Long id : ids) {
            Detection d = found.get(id);
            if (d == null || !d.getUpload().getUser().getId().equals(me.id())) {
                throw new ApiException(HttpStatus.NOT_FOUND, "Detection " + id + " not found.");
            }
            if (d.getStatus() != DetectionStatus.CONFIRMED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only confirmed detections can be reported.");
            }
            if (complaints.existsByDetections_Id(id)) {
                throw new ApiException(HttpStatus.CONFLICT, "Detection " + id + " already has a complaint.");
            }
            c.getDetections().add(d);
            if (c.getLatitude() == null && d.getLatitude() != null && d.getLongitude() != null) {
                c.setLatitude(d.getLatitude());
                c.setLongitude(d.getLongitude());
            }
        }
        c.setUser(users.getReferenceById(me.id()));
        c.setDescription(r.description().trim());
        c.setSeverity(Severity.valueOf(r.severity().toUpperCase()));
        complaints.saveAndFlush(c);
        return Mappers.complaint(c);
    }

    @Transactional(readOnly = true)
    public List<ComplaintDto.Out> mine(AuthUser me) {
        return complaints.findByUserIdOrderByIdDesc(me.id()).stream().map(Mappers::complaint).toList();
    }

    @Transactional(readOnly = true)
    public ComplaintDto.Out get(AuthUser me, String ref) {
        return Mappers.complaint(owned(me, ref));
    }

    /** Users can reword their complaint only while it is still pending. */
    @Transactional
    public ComplaintDto.Out updateDescription(AuthUser me, String ref, String description) {
        Complaint c = owned(me, ref);
        if (c.getStatus() != ComplaintStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Only pending complaints can be edited.");
        }
        c.setDescription(description.trim());
        return Mappers.complaint(c);
    }

    // ---- administrator ----

    @Transactional(readOnly = true)
    public List<ComplaintDto.Out> all(String status) {
        List<Complaint> list = (status == null || status.isBlank())
                ? complaints.findAllByOrderByIdDesc()
                : complaints.findByStatusOrderByIdDesc(parseStatus(status));
        return list.stream().map(Mappers::complaint).toList();
    }

    @Transactional
    public ComplaintDto.Out setStatus(String ref, String status) {
        Complaint c = complaints.findById(Mappers.parseComplaintRef(ref))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found."));
        ComplaintStatus next = parseStatus(status);
        c.setStatus(next);
        c.setResolvedAt(next == ComplaintStatus.RESOLVED ? Instant.now() : null);
        return Mappers.complaint(c);
    }

    private ComplaintStatus parseStatus(String s) {
        try {
            return ComplaintStatus.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Status must be pending, in_progress or resolved.");
        }
    }

    private Complaint owned(AuthUser me, String ref) {
        Complaint c = complaints.findById(Mappers.parseComplaintRef(ref))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found."));
        if (!c.getUser().getId().equals(me.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Complaint not found.");
        }
        return c;
    }
}
