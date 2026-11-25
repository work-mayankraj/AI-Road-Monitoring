package com.roadguard.service;

import com.roadguard.dto.DetectionDto;
import com.roadguard.dto.Mappers;
import com.roadguard.dto.UploadDto;
import com.roadguard.entity.Detection;
import com.roadguard.entity.MediaKind;
import com.roadguard.entity.Upload;
import com.roadguard.entity.UploadFrame;
import com.roadguard.exception.ApiException;
import com.roadguard.repository.DetectionRepository;
import com.roadguard.repository.UploadRepository;
import com.roadguard.repository.UserRepository;
import com.roadguard.security.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UploadService {

    private final UploadRepository uploads;
    private final DetectionRepository detections;
    private final UserRepository users;
    private final FileStorageService storage;
    private final DetectionEngine engine;

    public UploadService(UploadRepository uploads, DetectionRepository detections, UserRepository users,
                         FileStorageService storage, DetectionEngine engine) {
        this.uploads = uploads;
        this.detections = detections;
        this.users = users;
        this.storage = storage;
        this.engine = engine;
    }

    @Transactional
    public UploadDto.Result create(AuthUser me, UploadDto.CreateRequest r) {
        boolean photo = "photo".equals(r.mediaType());
        List<UploadDto.FrameIn> frames = r.frames() == null ? List.of() : r.frames();
        List<UploadDto.Point> track = r.track() == null ? List.of() : r.track();

        if (photo && frames.size() > 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A photo upload can have only one picture.");
        }
        if ((r.lat() == null) != (r.lng() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Enter both latitude and longitude.");
        }
        if (r.lat() != null && (Math.abs(r.lat()) > 90 || Math.abs(r.lng()) > 180)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Latitude or longitude is out of range.");
        }
        for (UploadDto.Point p : track) {
            if (Math.abs(p.lat()) > 90 || Math.abs(p.lng()) > 180) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A GPS point is out of range.");
            }
        }

        Upload up = new Upload();
        up.setUser(users.getReferenceById(me.id()));
        up.setName(r.name().trim());
        up.setMediaKind(photo ? MediaKind.PHOTO : MediaKind.VIDEO);
        up.setFileName(r.fileName());
        up.setGpsFileName(photo ? null : r.gpsFileName());
        if (photo) {
            up.setLatitude(r.lat());
            up.setLongitude(r.lng());
        }

        List<String> saved = new ArrayList<>();
        try {
            for (UploadDto.FrameIn f : frames) {
                String url = storage.saveJpegDataUrl(f.img());
                saved.add(url);
                up.getFrames().add(new UploadFrame(url, photo ? null : f.t()));
            }
            uploads.saveAndFlush(up);                         // gives the upload its id
            List<Detection> found = detections.saveAll(engine.detect(up, track));

            List<DetectionDto.Out> out = found.stream().map(Mappers::detection).toList();
            return new UploadDto.Result(Mappers.upload(up), out);
        } catch (RuntimeException e) {
            saved.forEach(storage::deleteQuietly);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<UploadDto.Out> mine(AuthUser me) {
        return uploads.findByUserIdOrderByIdDesc(me.id()).stream().map(Mappers::upload).toList();
    }

    @Transactional(readOnly = true)
    public UploadDto.Out get(AuthUser me, Long id) {
        Upload u = uploads.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Upload not found."));
        if (!u.getUser().getId().equals(me.id())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Upload not found.");
        }
        return Mappers.upload(u);
    }
}
