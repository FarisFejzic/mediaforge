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
public class WaveformProcessor {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;
    private final FfmpegRunner ffmpegRunner;

    public WaveformProcessor(StorageService storageService,
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
                input = TempFiles.write(original, ".audio");
            }
            output = TempFiles.create(".png");

            List<String> command = List.of(
                    "ffmpeg", "-y",
                    "-i", input.toString(),
                    "-filter_complex", "showwavespic=s=1000x200:colors=#3b82f6",
                    "-frames:v", "1",
                    output.toString()
            );
            CommandResult result = ffmpegRunner.run(command);
            if (!result.isSuccess()) {
                throw new FfmpegException("ffmpeg waveform failed (exit "
                        + result.exitCode() + "): " + result.stderrTail());
            }

            byte[] waveformBytes = Files.readAllBytes(output);
            String key = "waveforms/" + uploadId + "/" + jobId + ".png";
            storageService.store(key, new ByteArrayInputStream(waveformBytes),
                    waveformBytes.length, "image/png");

            Asset asset = Asset.create(jobId, uploadId, AssetKind.WAVEFORM,
                    key, (long) waveformBytes.length, null);
            assetRepository.save(asset);

        } catch (Exception e) {
            throw new FfmpegException("Waveform failed for job " + jobId, e);
        } finally {
            TempFiles.deleteQuietly(input);
            TempFiles.deleteQuietly(output);
        }
    }
}