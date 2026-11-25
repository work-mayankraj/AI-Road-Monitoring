package com.roadguard.controller;

import com.roadguard.dto.DetectionDto;
import com.roadguard.security.AuthUser;
import com.roadguard.service.DetectionService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/detections")
public class DetectionController {

    private final DetectionService detections;

    public DetectionController(DetectionService detections) {
        this.detections = detections;
    }

    @GetMapping
    public List<DetectionDto.Out> mine(@AuthenticationPrincipal AuthUser me) {
        return detections.mine(me);
    }

    @GetMapping("/{id}")
    public DetectionDto.Out get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return detections.get(me, id);
    }

    @PatchMapping("/{id}/status")
    public DetectionDto.Out setStatus(@AuthenticationPrincipal AuthUser me, @PathVariable Long id,
                                      @Valid @RequestBody DetectionDto.StatusRequest request) {
        return detections.setStatus(me, id, request.status());
    }
}
