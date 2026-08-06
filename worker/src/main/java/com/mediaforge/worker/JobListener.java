package com.mediaforge.worker;

import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.AssetKind;
import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageService;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import com.mediaforge.common.messaging.JobMessage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class JobListener {

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);
    private final JobRepository jobRepository;
    private final UploadRepository uploadRepository;
    private final AssetRepository assetRepository;
    private final StorageService storageService;

    public JobListener(JobRepository jobRepository,
                       UploadRepository uploadRepository,
                       AssetRepository assetRepository,
                       StorageService storageService) {
        this.jobRepository = jobRepository;
        this.uploadRepository = uploadRepository;
        this.assetRepository = assetRepository;
        this.storageService = storageService;
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.thumbnail-queue}")
    public void handleThumbnailJob(JobMessage message) {
        UUID jobId = message.jobId();
        log.info("Processing thumbnail job: {}", jobId);

        Job job = jobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("Job not found, discarding: {}", jobId);
            return;
        }

        if (job.getStatus() == JobStatus.COMPLETED) {
            log.info("Job already completed, skipping: {}", jobId);
            return;
        }

        job.setStatus(JobStatus.PROCESSING);
        job.setStartedAt(OffsetDateTime.now());
        job.setAttempts(job.getAttempts() + 1);
        jobRepository.save(job);

        try {
            Upload upload = uploadRepository.findById(job.getUploadId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Upload not found: " + job.getUploadId()));

            byte[] thumbnailBytes;
            try (InputStream original = storageService.retrieve(upload.getStorageKey())) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                Thumbnails.of(original)
                        .size(200, 200)
                        .outputFormat("jpg")
                        .toOutputStream(out);
                thumbnailBytes = out.toByteArray();
            }

            String thumbnailKey = "thumbnails/" + upload.getId() + "/" + jobId + ".jpg";

            storageService.store(
                    thumbnailKey,
                    new ByteArrayInputStream(thumbnailBytes),
                    thumbnailBytes.length,
                    "image/jpeg"
            );

            Asset asset = Asset.create(
                    jobId,
                    upload.getId(),
                    AssetKind.THUMBNAIL,
                    thumbnailKey,
                    (long) thumbnailBytes.length,
                    null
            );
            assetRepository.save(asset);

            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);

            log.info("Completed thumbnail job: {}", jobId);

        } catch (Exception e) {
            log.error("Thumbnail job failed: {}", jobId, e);
            job.setStatus(JobStatus.FAILED);
            job.setError(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }
    }
}