package com.roadguard.entity;

import jakarta.persistence.*;

/** One pothole found in one frame of an upload. */
@Entity
@Table(name = "detections", indexes = @Index(name = "idx_detections_upload", columnList = "upload_id"))
public class Detection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "upload_id", nullable = false)
    private Upload upload;

    /** 1-based number inside the upload ("Pothole #n"). */
    @Column(nullable = false)
    private int seq;

    /** Index into the upload's frames, or -1 when no picture is stored. */
    @Column(name = "frame_index", nullable = false)
    private int frameIndex;

    @Column(name = "frame_number", nullable = false)
    private int frameNumber;

    /** mm:ss position in the video; null for photos. */
    @Column(name = "time_label", length = 10)
    private String timeLabel;

    /** AI confidence in percent (0-100). */
    @Column(nullable = false)
    private int confidence;

    private Double latitude;
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DetectionStatus status = DetectionStatus.PENDING;

    /** Bounding box in percent of the picture size. */
    @Column(name = "box_x", nullable = false) private int boxX;
    @Column(name = "box_y", nullable = false) private int boxY;
    @Column(name = "box_w", nullable = false) private int boxW;
    @Column(name = "box_h", nullable = false) private int boxH;

    public Long getId() { return id; }
    public Upload getUpload() { return upload; }
    public void setUpload(Upload upload) { this.upload = upload; }
    public int getSeq() { return seq; }
    public void setSeq(int seq) { this.seq = seq; }
    public int getFrameIndex() { return frameIndex; }
    public void setFrameIndex(int frameIndex) { this.frameIndex = frameIndex; }
    public int getFrameNumber() { return frameNumber; }
    public void setFrameNumber(int frameNumber) { this.frameNumber = frameNumber; }
    public String getTimeLabel() { return timeLabel; }
    public void setTimeLabel(String timeLabel) { this.timeLabel = timeLabel; }
    public int getConfidence() { return confidence; }
    public void setConfidence(int confidence) { this.confidence = confidence; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public DetectionStatus getStatus() { return status; }
    public void setStatus(DetectionStatus status) { this.status = status; }
    public int getBoxX() { return boxX; }
    public void setBoxX(int boxX) { this.boxX = boxX; }
    public int getBoxY() { return boxY; }
    public void setBoxY(int boxY) { this.boxY = boxY; }
    public int getBoxW() { return boxW; }
    public void setBoxW(int boxW) { this.boxW = boxW; }
    public int getBoxH() { return boxH; }
    public void setBoxH(int boxH) { this.boxH = boxH; }
}
