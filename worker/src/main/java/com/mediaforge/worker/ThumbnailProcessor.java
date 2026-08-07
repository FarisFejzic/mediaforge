package com.mediaforge.worker;

import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.AssetKind;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.UUID;

@Component
public class ThumbnailProcessor {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;

    public ThumbnailProcessor(StorageService storageService,
                              UploadRepository uploadRepository,
                              AssetRepository assetRepository) {
        this.storageService = storageService;
        this.uploadRepository = uploadRepository;
        this.assetRepository = assetRepository;
    }

    public void process(UUID jobId, UUID uploadId) throws Exception {
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalStateException("Upload not found: " + uploadId));

        byte[] thumbnailBytes;
        try (InputStream original = storageService.retrieve(upload.getStorageKey())) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(original).size(200, 200).outputFormat("jpg").toOutputStream(out);
            thumbnailBytes = out.toByteArray();
        }

        String thumbnailKey = "thumbnails/" + uploadId + "/" + jobId + ".jpg";
        storageService.store(thumbnailKey, new ByteArrayInputStream(thumbnailBytes),
                thumbnailBytes.length, "image/jpeg");

        Asset asset = Asset.create(jobId, uploadId, AssetKind.THUMBNAIL,
                thumbnailKey, (long) thumbnailBytes.length, null);
        assetRepository.save(asset);
    }
}