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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class PosterProcessor {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;
    private final FfmpegRunner ffmpegRunner;

    public PosterProcessor(StorageService storageService,
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
            // 1. original from MinIO → temp input file
            try (InputStream original = storageService.retrieve(upload.getStorageKey())) {
                input = TempFiles.write(original, ".mp4");
            }
            // 2. temp output file for the frame
            output = TempFiles.create(".jpg");

            // 3. run ffmpeg: grab one frame at ~1 second in
            List<String> command = List.of(
                    "ffmpeg", "-y",
                    "-ss", "00:00:01",
                    "-i", input.toString(),
                    "-frames:v", "1",
                    "-q:v", "2",
                    output.toString()
            );
            CommandResult result = ffmpegRunner.run(command);
            if (!result.isSuccess()) {
                throw new FfmpegException("ffmpeg poster failed (exit "
                        + result.exitCode() + "): " + result.stderrTail());
            }

            // 4. read the frame back, store it
            byte[] posterBytes = Files.readAllBytes(output);
            String posterKey = "posters/" + uploadId + "/" + jobId + ".jpg";
            storageService.store(posterKey, new java.io.ByteArrayInputStream(posterBytes),
                    posterBytes.length, "image/jpeg");

            // 5. record the asset
            Asset asset = Asset.create(jobId, uploadId, AssetKind.POSTER,
                    posterKey, (long) posterBytes.length, null);
            assetRepository.save(asset);

        } catch (Exception e) {
            throw new FfmpegException("Poster processing failed for job " + jobId, e);
        } finally {
            TempFiles.deleteQuietly(input);
            TempFiles.deleteQuietly(output);
        }
    }
}