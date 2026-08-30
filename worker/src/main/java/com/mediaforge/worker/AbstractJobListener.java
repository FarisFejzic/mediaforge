package com.mediaforge.worker;

import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.storage.StorageException;
import com.mediaforge.worker.ffmpeg.FfmpegException;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class AbstractJobListener {

    protected static final int IMMEDIATE_RETRY_LIMIT = 2;
    private static final Logger log = LoggerFactory.getLogger(AbstractJobListener.class);

    protected final JobRepository jobRepository;
    protected final JobStatusPublisher jobStatusPublisher;
    protected final RetryPublisher retryPublisher;
    protected final MeterRegistry meterRegistry;

    protected AbstractJobListener(JobRepository jobRepository,
                                  JobStatusPublisher jobStatusPublisher,
                                  RetryPublisher retryPublisher,
                                  MeterRegistry meterRegistry) {
        this.jobRepository = jobRepository;
        this.jobStatusPublisher = jobStatusPublisher;
        this.retryPublisher = retryPublisher;
        this.meterRegistry = meterRegistry;
    }

    protected void runJob(UUID jobId, Consumer<Job> processor) {
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
            meterRegistry.timer("mediaforge.job.duration", "type", job.getType().name())
                    .record(() -> processor.accept(job));
            job.setStatus(JobStatus.COMPLETED);
            job.setFinishedAt(OffsetDateTime.now());
            jobRepository.save(job);
            jobStatusPublisher.publish(job);
            countJob(job, "completed");
            log.info("Completed job: {}", jobId);
        } catch (Exception e) {
            if (isPermanent(e)) {
                log.error("Job failed (permanent): {}", jobId, e);
                markFailed(job, e);
                countJob(job, "failed");
                throw new AmqpRejectAndDontRequeueException("Permanent failure: " + jobId, e);
            }
            int attempts = job.getAttempts();
            if (attempts >= job.getMaxAttempts()) {
                log.error("Job failed (transient, exhausted after {} attempts): {}", attempts, jobId, e);
                markFailed(job, e);
                countJob(job, "exhausted");
                throw new AmqpRejectAndDontRequeueException("Retries exhausted: " + jobId, e);
            } else if (attempts <= IMMEDIATE_RETRY_LIMIT) {
                log.warn("Job failed (transient), immediate retry (attempt {}): {}", attempts, jobId, e);
                retryPublisher.requeueImmediate(jobId, job.getType());
                countJob(job, "retried");
            } else {
                log.warn("Job failed (transient), delayed retry (attempt {}): {}", attempts, jobId, e);
                retryPublisher.requeueDelayed(jobId, job.getType());
                countJob(job, "retried");
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

    private void countJob(Job job, String outcome) {
        meterRegistry.counter("mediaforge.jobs",
                "type", job.getType().name(),
                "outcome", outcome).increment();
    }
}