package com.roadguard.service;

import com.roadguard.dto.DetectionDto;
import com.roadguard.dto.Mappers;
import com.roadguard.entity.Detection;
import com.roadguard.entity.DetectionStatus;
import com.roadguard.exception.ApiException;
import com.roadguard.repository.ComplaintRepository;
import com.roadguard.repository.DetectionRepository;
import com.roadguard.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DetectionService {

    private final DetectionRepository detections;
    private final ComplaintRepository complaints;

    public DetectionService(DetectionRepository detections, ComplaintRepository complaints) {
        this.detections = detections;
        this.complaints = complaints;
    }

    @Transactional(readOnly = true)
    public List<DetectionDto.Out> mine(AuthUser me) {
        return detections.findByOwner(me.id()).stream().map(Mappers::detection).toList();
    }

    @Transactional(readOnly = true)
    public DetectionDto.Out get(AuthUser me, Long id) {
        return Mappers.detection(owned(me, id));
    }

    /** The reporter confirms or rejects what the AI found. Locked once a complaint exists. */
    @Transactional
    public DetectionDto.Out setStatus(AuthUser me, Long id, String status) {
        Detection d = owned(me, id);
        if (complaints.existsByDetections_Id(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This detection is already part of a complaint.");
        }
        d.setStatus(DetectionStatus.valueOf(status.toUpperCase()));
        return Mappers.detection(d);
    }

    /** Someone else's detection looks exactly like a missing one (404), so ids cannot be probed. */
    private Detection owned(AuthUser me, Long id) {
        Detection d = detections.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Detection not found."));
        if (!d.getUpload().getUser().getId().equals(me.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Detection not found.");
        }
        return d;
    }
}
