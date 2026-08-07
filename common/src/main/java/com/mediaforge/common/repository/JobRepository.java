package com.mediaforge.common.repository;

import com.mediaforge.common.domain.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByUploadId(UUID uploadId);
}
