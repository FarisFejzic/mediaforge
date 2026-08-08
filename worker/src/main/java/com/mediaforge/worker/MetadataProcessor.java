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
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

@Component
public class MetadataProcessor {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;
    private final FfmpegRunner ffmpegRunner;

    public MetadataProcessor(StorageService storageService,
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
        try {
            // original from MinIO → temp input file
            try (InputStream original = storageService.retrieve(upload.getStorageKey())) {
                input = TempFiles.write(original, ".audio");
            }

            // ffprobe: emit format + stream info as JSON on stdout
            List<String> command = List.of(
                    "ffprobe",
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    input.toString()
            );
            CommandResult result = ffmpegRunner.run(command);
            if (!result.isSuccess()) {
                throw new FfmpegException("ffprobe failed (exit "
                        + result.exitCode() + "): " + result.stderrTail());
            }

            String metadataJson = result.stdout();

            // no binary output object for metadata — the JSON *is* the asset's payload.
            // storageKey references the source; the data lives in metadata_json.
            Asset asset = Asset.create(jobId, uploadId, AssetKind.METADATA,
                    upload.getStorageKey(), 0L, metadataJson);
            assetRepository.save(asset);

        } catch (Exception e) {
            throw new FfmpegException("Metadata processing failed for job " + jobId, e);
        } finally {
            TempFiles.deleteQuietly(input);
        }
    }
}