package com.roadguard.controller;

import com.roadguard.dto.UploadDto;
import com.roadguard.security.AuthUser;
import com.roadguard.service.UploadService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploads;

    public UploadController(UploadService uploads) {
        this.uploads = uploads;
    }

    /** Creates the upload, stores its frames and runs detection. Returns the upload and its detections. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UploadDto.Result create(@AuthenticationPrincipal AuthUser me,
                                   @Valid @RequestBody UploadDto.CreateRequest request) {
        return uploads.create(me, request);
    }

    @GetMapping
    public List<UploadDto.Out> mine(@AuthenticationPrincipal AuthUser me) {
        return uploads.mine(me);
    }

    @GetMapping("/{id}")
    public UploadDto.Out get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return uploads.get(me, id);
    }
}
