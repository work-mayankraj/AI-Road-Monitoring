package com.roadguard.service;

import com.roadguard.dto.AuthDto;
import com.roadguard.dto.DetectionDto;
import com.roadguard.dto.Mappers;
import com.roadguard.dto.UploadDto;
import com.roadguard.repository.DetectionRepository;
import com.roadguard.repository.UploadRepository;
import com.roadguard.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only views across all users. Only reachable under /api/admin/** (ADMIN role). */
@Service
public class AdminService {

    private final UserRepository users;
    private final UploadRepository uploads;
    private final DetectionRepository detections;

    public AdminService(UserRepository users, UploadRepository uploads, DetectionRepository detections) {
        this.users = users;
        this.uploads = uploads;
        this.detections = detections;
    }

    @Transactional(readOnly = true)
    public List<AuthDto.UserInfo> users() {
        return users.findAll(Sort.by("id")).stream().map(Mappers::user).toList();
    }

    @Transactional(readOnly = true)
    public List<UploadDto.Out> uploads() {
        return uploads.findAllByOrderByIdDesc().stream().map(Mappers::upload).toList();
    }

    @Transactional(readOnly = true)
    public List<DetectionDto.Out> detections() {
        return detections.findAllByOrderByIdAsc().stream().map(Mappers::detection).toList();
    }
}
