package com.roadguard.controller;

import com.roadguard.dto.ComplaintDto;
import com.roadguard.security.AuthUser;
import com.roadguard.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService complaints;

    public ComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintDto.Out create(@AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody ComplaintDto.CreateRequest request) {
        return complaints.create(me, request);
    }

    @GetMapping
    public List<ComplaintDto.Out> mine(@AuthenticationPrincipal AuthUser me) {
        return complaints.mine(me);
    }

    @GetMapping("/{id}")
    public ComplaintDto.Out get(@AuthenticationPrincipal AuthUser me, @PathVariable String id) {
        return complaints.get(me, id);
    }

    @PatchMapping("/{id}")
    public ComplaintDto.Out update(@AuthenticationPrincipal AuthUser me, @PathVariable String id,
                                   @Valid @RequestBody ComplaintDto.UpdateRequest request) {
        return complaints.updateDescription(me, id, request.description());
    }
}
