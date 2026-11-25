package com.roadguard.controller;

import com.roadguard.dto.AuthDto;
import com.roadguard.dto.ComplaintDto;
import com.roadguard.dto.DetectionDto;
import com.roadguard.dto.UploadDto;
import com.roadguard.service.AdminService;
import com.roadguard.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Everything under /api/admin/** requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService admin;
    private final ComplaintService complaints;

    public AdminController(AdminService admin, ComplaintService complaints) {
        this.admin = admin;
        this.complaints = complaints;
    }

    @GetMapping("/complaints")
    public List<ComplaintDto.Out> complaints(@RequestParam(required = false) String status) {
        return complaints.all(status);
    }

    @PatchMapping("/complaints/{id}/status")
    public ComplaintDto.Out setStatus(@PathVariable String id, @Valid @RequestBody ComplaintDto.StatusRequest request) {
        return complaints.setStatus(id, request.status());
    }

    @GetMapping("/detections")
    public List<DetectionDto.Out> detections() {
        return admin.detections();
    }

    @GetMapping("/uploads")
    public List<UploadDto.Out> uploads() {
        return admin.uploads();
    }

    @GetMapping("/users")
    public List<AuthDto.UserInfo> users() {
        return admin.users();
    }
}
