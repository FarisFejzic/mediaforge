package com.mediaforge.api.job;

import com.mediaforge.api.messaging.JobPublisher;
import com.mediaforge.api.upload.dto.JobResponse;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.domain.enums.JobStatus;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UploadRepository uploadRepository;
    private final JobPublisher jobPublisher;

    public JobService(JobRepository jobRepository, UploadRepository uploadRepository, JobPublisher jobPublisher) {
        this.jobRepository = jobRepository;
        this.uploadRepository = uploadRepository;
        this.jobPublisher = jobPublisher;
    }

    public JobResponse getJob(UUID jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Upload upload = uploadRepository.findById(job.getUploadId())
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!upload.getUserId().equals(userId)) {
            throw new JobNotFoundException(jobId);
        }

        return toJobResponse(job);
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

    public void retryJob(UUID jobId, UUID userId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new JobNotFoundException(jobId));

        Upload upload = uploadRepository.findById(job.getUploadId())
                .orElseThrow(() -> new JobNotFoundException(jobId));

        if (!upload.getUserId().equals(userId)) {
            throw new JobNotFoundException(jobId);
        }

        if (job.getStatus() != JobStatus.FAILED) {
            throw new JobNotRetryableException(jobId, job.getStatus());
        }

        // reset job to a fresh queued state
        job.setStatus(JobStatus.QUEUED);
        job.setError(null);
        job.setAttempts(0);
        job.setStartedAt(null);
        job.setFinishedAt(null);
        jobRepository.save(job);

        // re-publish to the correct queue for its type
        publishForType(job);
    }

    private void publishForType(Job job) {
        switch (job.getType()) {
            case THUMBNAIL -> jobPublisher.publishThumbnailJob(job.getId());
            case POSTER -> jobPublisher.publishPosterJob(job.getId());
            case TRANSCODE -> jobPublisher.publishTranscodeJob(job.getId());
            case PREVIEW -> jobPublisher.publishPreviewJob(job.getId());
            case METADATA -> jobPublisher.publishMetadataJob(job.getId());
            case WAVEFORM -> jobPublisher.publishWaveformJob(job.getId());
            case AUDIO_TRANSCODE -> jobPublisher.publishAudioTranscodeJob(job.getId());
        }
    }
}