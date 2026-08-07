package com.mediaforge.api.job;

import com.mediaforge.api.upload.dto.JobResponse;
import com.mediaforge.common.domain.Job;
import com.mediaforge.common.domain.Upload;
import com.mediaforge.common.repository.JobRepository;
import com.mediaforge.common.repository.UploadRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UploadRepository uploadRepository;

    public JobService(JobRepository jobRepository, UploadRepository uploadRepository) {
        this.jobRepository = jobRepository;
        this.uploadRepository = uploadRepository;
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
}