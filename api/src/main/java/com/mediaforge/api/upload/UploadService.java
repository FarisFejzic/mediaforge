package com.mediaforge.api.upload;

import com.mediaforge.api.messaging.JobPublisher;
import com.mediaforge.api.upload.dto.AssetResponse;
import com.mediaforge.api.upload.dto.JobResponse;
import com.mediaforge.api.upload.dto.UploadDetailResponse;
import com.mediaforge.api.upload.dto.UploadResponse;
import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.JobType;
import com.mediaforge.common.domain.enums.MediaType;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageException;
import com.mediaforge.common.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
public class UploadService {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final JobRepository jobRepository;
    private final JobPublisher jobPublisher;
    private final AssetRepository assetRepository;

    public UploadService(StorageService storageService,
                         UploadRepository uploadRepository,
                         JobRepository jobRepository,
                         JobPublisher jobPublisher,
                         AssetRepository assetRepository){
        this.storageService = storageService;
        this.uploadRepository = uploadRepository;
        this.jobRepository = jobRepository;
        this.jobPublisher = jobPublisher;
        this.assetRepository = assetRepository;


    }

    public UploadResponse upload(MultipartFile file, UUID userId) {
        MediaType mediaType = detectMediaType(file.getContentType());
        String originalName = file.getOriginalFilename();

        Upload upload = Upload.create(
                userId,
                originalName,
                mediaType,
                file.getSize(),
                null // storageKey set below, once we have the id
        );

        String storageKey = "uploads/" + upload.getId() + "/" + originalName;
        upload.setStorageKey(storageKey);

        try {
            storageService.store(
                    storageKey,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file", e);
        }

        Upload saved = uploadRepository.save(upload);

        switch (mediaType) {
            case IMAGE -> {
                Job job = Job.create(saved.getId(), JobType.THUMBNAIL);
                jobRepository.save(job);
                jobPublisher.publishThumbnailJob(job.getId());
            }
            case VIDEO -> {
                Job job = Job.create(saved.getId(), JobType.POSTER);
                jobRepository.save(job);
                jobPublisher.publishPosterJob(job.getId());
            }
            case AUDIO -> {
                Job job = Job.create(saved.getId(), JobType.METADATA);
                jobRepository.save(job);
                jobPublisher.publishMetadataJob(job.getId());
            }
        }

        return new UploadResponse(
                saved.getId(),
                saved.getOriginalName(),
                saved.getMediaType(),
                saved.getSizeBytes(),
                saved.getStatus(),
                saved.getCreatedAt()
        );
    }

    private MediaType detectMediaType(String contentType) {
        if (contentType == null) {
            throw new UnsupportedMediaTypeException("null");
        }
        if (contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        if (contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (contentType.startsWith("audio/")) {
            return MediaType.AUDIO;
        }
        throw new UnsupportedMediaTypeException(contentType);
    }

    private UploadResponse toResponse(Upload upload) {
        return new UploadResponse(
                upload.getId(),
                upload.getOriginalName(),
                upload.getMediaType(),
                upload.getSizeBytes(),
                upload.getStatus(),
                upload.getCreatedAt()
        );
    }

    public Page<UploadResponse> list(UUID userId, Pageable pageable) {
        return uploadRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    private JobResponse toJobResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getUploadId(),
                job.getType(),
                job.getStatus(),
                job.getAttempts(),
                job.getError(),
                job.getStartedAt(),
                job.getFinishedAt()
        );
    }

    private AssetResponse toAssetResponse(Asset asset) {
        return new AssetResponse(
                asset.getId(),
                asset.getJobId(),
                asset.getKind(),
                asset.getSizeBytes(),
                asset.getCreatedAt()
        );
    }

    public UploadDetailResponse getDetail(UUID uploadId, UUID userId) {
        Upload upload = uploadRepository.findById(uploadId)
                .filter(u -> u.getUserId().equals(userId))
                .orElseThrow(() -> new UploadNotFoundException(uploadId));

        List<JobResponse> jobs = jobRepository.findByUploadId(uploadId).stream()
                .map(this::toJobResponse)
                .toList();

        List<AssetResponse> assets = assetRepository.findByUploadId(uploadId).stream()
                .map(this::toAssetResponse)
                .toList();

        return new UploadDetailResponse(
                upload.getId(),
                upload.getOriginalName(),
                upload.getMediaType(),
                upload.getSizeBytes(),
                upload.getStatus(),
                upload.getCreatedAt(),
                jobs,
                assets
        );
    }
}
