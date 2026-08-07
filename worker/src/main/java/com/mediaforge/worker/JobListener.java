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
import java.util.function.Consumer;

@Component
public class JobListener {

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    private final JobRepository jobRepository;
    private final ThumbnailProcessor thumbnailProcessor;
    private final PosterProcessor posterProcessor;
    private final MetadataProcessor metadataProcessor;

    public JobListener(JobRepository jobRepository,
                       ThumbnailProcessor thumbnailProcessor,
                       PosterProcessor posterProcessor,
                       MetadataProcessor metadataProcessor) {
        this.jobRepository = jobRepository;
        this.thumbnailProcessor = thumbnailProcessor;
        this.posterProcessor = posterProcessor;
        this.metadataProcessor = metadataProcessor;
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.thumbnail-queue}")
    public void handleThumbnail(JobMessage message) {
        runJob(message.jobId(), job -> {
            try { thumbnailProcessor.process(job.getId(), job.getUploadId()); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.poster-queue}")
    public void handlePoster(JobMessage message) {
        runJob(message.jobId(), job ->
                posterProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.metadata-queue}")
    public void handleMetadata(JobMessage message) {
        runJob(message.jobId(), job ->
                metadataProcessor.process(job.getId(), job.getUploadId()));
    }

    /** Shared status lifecycle around any processor. */
    private void runJob(UUID jobId, Consumer<Job> processor) {
        log.info("Processing job: {}", jobId);

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
            processor.accept(job);
            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);
            log.info("Completed job: {}", jobId);
        } catch (Exception e) {
            log.error("Job failed: {}", jobId, e);
            job.setStatus(JobStatus.FAILED);
            job.setError(e.getMessage());
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);
        }
    }
}