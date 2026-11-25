package com.roadguard.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One still picture taken from an upload (a video has several, a photo has one). */
@Embeddable
public class UploadFrame {

    @Column(name = "image_url", nullable = false, length = 300)
    private String imageUrl;

    /** Position in the video in seconds; null for photos. */
    @Column(name = "time_sec")
    private Double timeSec;

    protected UploadFrame() { }

    public UploadFrame(String imageUrl, Double timeSec) {
        this.imageUrl = imageUrl;
        this.timeSec = timeSec;
    }

    public String getImageUrl() { return imageUrl; }
    public Double getTimeSec() { return timeSec; }
}
