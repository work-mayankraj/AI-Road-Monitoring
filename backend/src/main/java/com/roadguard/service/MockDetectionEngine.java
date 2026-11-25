package com.roadguard.service;

import com.roadguard.dto.UploadDto;
import com.roadguard.entity.Detection;
import com.roadguard.entity.MediaKind;
import com.roadguard.entity.Upload;
import com.roadguard.entity.UploadFrame;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Sample detections (one per frame, fake confidence and box). Replace with the ML service later. */
@Component
public class MockDetectionEngine implements DetectionEngine {

    @Override
    public List<Detection> detect(Upload upload, List<UploadDto.Point> track) {
        boolean photo = upload.getMediaKind() == MediaKind.PHOTO;
        List<UploadFrame> frames = upload.getFrames();
        int count = photo ? 1 : (frames.isEmpty() ? 4 : frames.size());
        List<Detection> out = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            UploadFrame frame = i < frames.size() ? frames.get(i) : null;
            Double t = frame != null ? frame.getTimeSec() : null;

            Double lat = null, lng = null;
            if (photo) {
                lat = upload.getLatitude();
                lng = upload.getLongitude();
            } else if (!track.isEmpty()) {
                int idx = Math.min(track.size() - 1, (int) Math.floor((i + 0.5) / count * track.size()));
                lat = track.get(idx).lat();
                lng = track.get(idx).lng();
            }

            Detection d = new Detection();
            d.setUpload(upload);
            d.setSeq(i + 1);
            d.setFrameIndex(frame != null ? i : -1);
            d.setFrameNumber((int) Math.round((t != null ? t : i * 5.0) * 30));
            d.setTimeLabel(t != null ? mmss(t) : null);
            d.setConfidence(confidence(upload.getId(), i));
            d.setLatitude(lat);
            d.setLongitude(lng);
            if (photo) {
                d.setBoxX(18); d.setBoxY(20); d.setBoxW(64); d.setBoxH(58);
            } else {
                d.setBoxX(28 + (i * 7) % 14); d.setBoxY(48 + (i * 5) % 10); d.setBoxW(34); d.setBoxH(28);
            }
            out.add(d);
        }
        return out;
    }

    private static String mmss(double seconds) {
        long s = Math.round(seconds);
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    /** Stable pseudo-random confidence between 52 and 96. */
    private static int confidence(Long uploadId, int i) {
        long h = 7;
        for (char ch : (uploadId + ":" + i).toCharArray()) {
            h = (h * 31 + ch) & 0xFFFFFFFFL;
        }
        return 52 + (int) (h % 45);
    }
}
