package com.roadguard.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** A road video or photo uploaded by a user (called a "session" in the frontend). */
@Entity
@Table(name = "uploads")
public class Upload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_kind", nullable = false, length = 10)
    private MediaKind mediaKind;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "gps_file_name", length = 255)
    private String gpsFileName;

    /** Photo location (videos use a GPS track instead). */
    private Double latitude;
    private Double longitude;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @ElementCollection
    @CollectionTable(name = "upload_frames", joinColumns = @JoinColumn(name = "upload_id"))
    @OrderColumn(name = "position")
    private List<UploadFrame> frames = new ArrayList<>();

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public MediaKind getMediaKind() { return mediaKind; }
    public void setMediaKind(MediaKind mediaKind) { this.mediaKind = mediaKind; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getGpsFileName() { return gpsFileName; }
    public void setGpsFileName(String gpsFileName) { this.gpsFileName = gpsFileName; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public Instant getCreatedAt() { return createdAt; }
    public List<UploadFrame> getFrames() { return frames; }
}
