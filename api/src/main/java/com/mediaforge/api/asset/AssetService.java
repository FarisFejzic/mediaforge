package com.mediaforge.api.asset;

import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AssetService {

    private static final int DOWNLOAD_EXPIRY_SECONDS = 300;

    private final AssetRepository assetRepository;
    private final UploadRepository uploadRepository;
    private final StorageService storageService;

    public AssetService(AssetRepository assetRepository,
                        UploadRepository uploadRepository,
                        StorageService storageService) {
        this.assetRepository = assetRepository;
        this.uploadRepository = uploadRepository;
        this.storageService = storageService;
    }

    public DownloadResponse getDownloadUrl(UUID assetId, UUID userId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        Upload upload = uploadRepository.findById(asset.getUploadId())
                .orElseThrow(() -> new AssetNotFoundException(assetId));

        if (!upload.getUserId().equals(userId)) {
            throw new AssetNotFoundException(assetId);
        }

        String url = storageService.presignedGetUrl(
                asset.getStorageKey(), DOWNLOAD_EXPIRY_SECONDS, true);

        return new DownloadResponse(url, DOWNLOAD_EXPIRY_SECONDS);
    }
}