package com.roadguard.service;

import com.roadguard.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Pattern;

/** Stores the frame pictures on disk under random UUID names. */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final String PREFIX = "data:image/jpeg;base64,";
    private static final int MAX_BYTES = 3 * 1024 * 1024;
    private static final Pattern NAME = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$");
    public static final String URL_PREFIX = "/api/files/frames/";

    private final Path root;

    public FileStorageService(@Value("${roadguard.upload-dir}") String dir) throws IOException {
        this.root = Paths.get(dir).toAbsolutePath().normalize().resolve("frames");
        Files.createDirectories(root);
    }

    /** Saves a JPEG data URL and returns its URL path (e.g. /api/files/frames/<uuid>.jpg). */
    public String saveJpegDataUrl(String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith(PREFIX)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Frames must be JPEG pictures.");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A frame picture is not valid.");
        }
        if (bytes.length > MAX_BYTES) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "A frame picture is too large (max 3 MB).");
        }
        // JPEG files start with FF D8 FF
        if (bytes.length < 4 || (bytes[0] & 0xFF) != 0xFF || (bytes[1] & 0xFF) != 0xD8 || (bytes[2] & 0xFF) != 0xFF) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A frame picture is not a JPEG.");
        }
        String name = UUID.randomUUID() + ".jpg";
        try {
            Files.write(root.resolve(name), bytes);
        } catch (IOException e) {
            log.error("Could not store frame", e);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not store the uploaded picture.");
        }
        return URL_PREFIX + name;
    }

    public Resource load(String name) {
        if (name == null || !NAME.matcher(name).matches()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found.");
        }
        Path p = root.resolve(name).normalize();
        if (!p.startsWith(root) || !Files.isRegularFile(p)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found.");
        }
        return new FileSystemResource(p);
    }

    /** Best-effort cleanup, used when an upload fails half way. */
    public void deleteQuietly(String urlPath) {
        try {
            if (urlPath != null && urlPath.startsWith(URL_PREFIX)) {
                String name = urlPath.substring(URL_PREFIX.length());
                if (NAME.matcher(name).matches()) Files.deleteIfExists(root.resolve(name));
            }
        } catch (IOException ignored) { }
    }
}
