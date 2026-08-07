package com.mediaforge.worker.ffmpeg;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class TempFiles {

    private TempFiles() {}

    /** Write a stream to a new temp file with the given suffix (e.g. ".mp4"). */
    public static Path write(InputStream data, String suffix) {
        try {
            Path temp = Files.createTempFile("mediaforge-", suffix);
            Files.copy(data, temp, StandardCopyOption.REPLACE_EXISTING);
            return temp;
        } catch (IOException e) {
            throw new FfmpegException("Failed to write temp file", e);
        }
    }

    /** Create an empty temp file path for a command to write into. */
    public static Path create(String suffix) {
        try {
            return Files.createTempFile("mediaforge-", suffix);
        } catch (IOException e) {
            throw new FfmpegException("Failed to create temp file", e);
        }
    }

    /** Delete quietly; never throws. */
    public static void deleteQuietly(Path path) {
        if (path == null) return;
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best-effort cleanup
        }
    }
}