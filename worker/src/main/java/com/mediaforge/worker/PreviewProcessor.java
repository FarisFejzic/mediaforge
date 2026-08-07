package com.mediaforge.worker;

import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.AssetKind;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageService;
import com.mediaforge.worker.ffmpeg.CommandResult;
import com.mediaforge.worker.ffmpeg.FfmpegException;
import com.mediaforge.worker.ffmpeg.FfmpegRunner;
import com.mediaforge.worker.ffmpeg.TempFiles;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class PreviewProcessor {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;
    private final FfmpegRunner ffmpegRunner;

    public PreviewProcessor(StorageService storageService,
                            UploadRepository uploadRepository,
                            AssetRepository assetRepository,
                            FfmpegRunner ffmpegRunner) {
        this.storageService = storageService;
        this.uploadRepository = uploadRepository;
        this.assetRepository = assetRepository;
        this.ffmpegRunner = ffmpegRunner;
    }

    public void process(UUID jobId, UUID uploadId) {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("Upload not found: " + uploadId));

        Path input = null;
        Path output = null;
        try {
            try (InputStream original = storageService.retrieve(upload.getStorageKey())) {
                input = TempFiles.write(original, ".mp4");
            }
            output = TempFiles.create(".mp4");

            List<String> command = List.of(
                    "ffmpeg", "-y",
                    "-i", input.toString(),
                    "-t", "5",
                    "-vf", "scale=480:-2",
                    "-c:v", "libx264",
                    "-preset", "fast",
                    "-crf", "28",
                    "-an",
                    "-movflags", "+faststart",
                    output.toString()
            );
            CommandResult result = ffmpegRunner.run(command);
            if (!result.isSuccess()) {
                throw new FfmpegException("ffmpeg preview failed (exit "
                        + result.exitCode() + "): " + result.stderr());
            }

            byte[] previewBytes = Files.readAllBytes(output);
            String key = "previews/" + uploadId + "/" + jobId + ".mp4";
            storageService.store(key, new ByteArrayInputStream(previewBytes),
                    previewBytes.length, "video/mp4");

            Asset asset = Asset.create(jobId, uploadId, AssetKind.PREVIEW,
                    key, (long) previewBytes.length, null);
            assetRepository.save(asset);

        } catch (Exception e) {
            throw new FfmpegException("Preview failed for job " + jobId, e);
        } finally {
            TempFiles.deleteQuietly(input);
            TempFiles.deleteQuietly(output);
        }
    }
}