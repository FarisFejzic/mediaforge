package com.mediaforge.api.upload;

import com.mediaforge.api.messaging.JobPublisher;
import com.mediaforge.api.upload.dto.UploadResponse;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.JobType;
import com.mediaforge.common.domain.enums.MediaType;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageException;
import com.mediaforge.common.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class UploadService {

    private final StorageService storageService;
    private final UploadRepository uploadRepository;
    private final JobRepository jobRepository;
    private final JobPublisher jobPublisher;

    public UploadService(StorageService storageService,
                         UploadRepository uploadRepository,
                         JobRepository jobRepository,
                         JobPublisher jobPublisher){
        this.storageService = storageService;
        this.uploadRepository = uploadRepository;
        this.jobRepository = jobRepository;
        this.jobPublisher = jobPublisher;


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

        Job job = Job.create(saved.getId(), JobType.THUMBNAIL);
        Job savedJob = jobRepository.save(job);
        jobPublisher.publishThumbnailJob(savedJob.getId());

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
}
