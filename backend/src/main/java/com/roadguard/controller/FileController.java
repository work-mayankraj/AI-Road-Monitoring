package com.roadguard.controller;

import com.roadguard.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

/** Serves stored frame pictures. Names are random UUIDs, so only someone who has the link can open one. */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileStorageService storage;

    public FileController(FileStorageService storage) {
        this.storage = storage;
    }

    @GetMapping("/frames/{name:.+}")
    public ResponseEntity<Resource> frame(@PathVariable String name,
                                          @RequestParam(defaultValue = "false") boolean download) {
        Resource file = storage.load(name);
        ResponseEntity.BodyBuilder res = ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable());
        if (download) {
            res.header(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.attachment().filename("roadguard-" + name).build().toString());
        }
        return res.body(file);
    }
}
