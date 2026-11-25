package com.roadguard.service;

import com.roadguard.dto.UploadDto;
import com.roadguard.entity.Detection;
import com.roadguard.entity.Upload;

import java.util.List;

/**
 * Finds potholes in an upload. The real YOLO/OpenCV service (ml-service/) will implement this later;
 * for now {@link MockDetectionEngine} produces sample detections so the whole flow can be used.
 * Returned detections are NOT saved yet.
 */
public interface DetectionEngine {
    List<Detection> detect(Upload upload, List<UploadDto.Point> track);
}
