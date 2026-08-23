package com.mediaforge.worker;

import com.mediaforge.common.domain.Asset;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.AssetKind;
import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.repository.AssetRepository;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import com.mediaforge.common.storage.StorageException;
import com.mediaforge.common.storage.StorageService;
import com.mediaforge.worker.ffmpeg.FfmpegException;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
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

    private static final int IMMEDIATE_RETRY_LIMIT = 2;

    private static final Logger log = LoggerFactory.getLogger(JobListener.class);

    private final JobRepository jobRepository;
    private final ThumbnailProcessor thumbnailProcessor;
    private final PosterProcessor posterProcessor;
    private final MetadataProcessor metadataProcessor;
    private final VideoTranscodeProcessor videoTranscodeProcessor;
    private final  PreviewProcessor previewProcessor;
    private final WaveformProcessor waveformProcessor;
    private final AudioTranscodeProcessor audioTranscodeProcessor;
    private final JobStatusPublisher jobStatusPublisher;
    private final RetryPublisher retryPublisher;

    public JobListener(JobRepository jobRepository,
                       ThumbnailProcessor thumbnailProcessor,
                       PosterProcessor posterProcessor,
                       MetadataProcessor metadataProcessor,
                       VideoTranscodeProcessor videoTranscodeProcessor,
                       PreviewProcessor previewProcessor,
                       WaveformProcessor waveformProcessor,
                       AudioTranscodeProcessor audioTranscodeProcessor,
                       JobStatusPublisher jobStatusPublisher,
                       RetryPublisher retryPublisher) {
        this.jobRepository = jobRepository;
        this.thumbnailProcessor = thumbnailProcessor;
        this.posterProcessor = posterProcessor;
        this.metadataProcessor = metadataProcessor;
        this.videoTranscodeProcessor = videoTranscodeProcessor;
        this.previewProcessor = previewProcessor;
        this.waveformProcessor = waveformProcessor;
        this.audioTranscodeProcessor = audioTranscodeProcessor;
        this.jobStatusPublisher = jobStatusPublisher;
        this.retryPublisher = retryPublisher;
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
        jobStatusPublisher.publish(job);

        try {
            processor.accept(job);
            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);
            jobStatusPublisher.publish(job);
            log.info("Completed job: {}", jobId);
        } catch (Exception e) {
            if (isPermanent(e)) {
                log.error("Job failed (permanent): {}", jobId, e);
                markFailed(job, e);
                throw new AmqpRejectAndDontRequeueException("Permanent failure: " + jobId, e);
            }

            // transient — tiered retry driven by attempts count
            int attempts = job.getAttempts();
            if (attempts >= job.getMaxAttempts()) {
                log.error("Job failed (transient, exhausted after {} attempts): {}", attempts, jobId, e);
                markFailed(job, e);
                throw new AmqpRejectAndDontRequeueException("Retries exhausted: " + jobId, e);
            } else if (attempts <= IMMEDIATE_RETRY_LIMIT) {
                log.warn("Job failed (transient), immediate retry (attempt {}): {}", attempts, jobId, e);
                retryPublisher.requeueImmediate(jobId, job.getType());
            } else {
                log.warn("Job failed (transient), delayed retry (attempt {}): {}", attempts, jobId, e);
                retryPublisher.requeueDelayed(jobId, job.getType());
            }
        }
    }

    private void markFailed(Job job, Throwable e) {
        job.setStatus(JobStatus.FAILED);
        job.setError(e.getMessage());
        job.setFinishedAt(OffsetDateTime.now());
        jobRepository.save(job);
        jobStatusPublisher.publish(job);
    }

    private boolean isPermanent(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof FfmpegException) return true;
            if (cause instanceof StorageException) return false;
            cause = cause.getCause();
        }
        return true; // unknown → treat as permanent (don't retry-loop)
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.transcode-queue}")
    public void handleTranscode(JobMessage message) {
        runJob(message.jobId(), job ->
                videoTranscodeProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.preview-queue}")
    public void handlePreview(JobMessage message) {
        runJob(message.jobId(), job ->
                previewProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.waveform-queue}")
    public void handleWaveform(JobMessage message) {
        runJob(message.jobId(), job ->
                waveformProcessor.process(job.getId(), job.getUploadId()));
    }

    @RabbitListener(queues = "${mediaforge.rabbitmq.audio-transcode-queue}")
    public void handleAudioTranscode(JobMessage message) {
        runJob(message.jobId(), job ->
                audioTranscodeProcessor.process(job.getId(), job.getUploadId()));
    }
}