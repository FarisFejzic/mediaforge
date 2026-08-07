package com.mediaforge.common.repository;

import com.mediaforge.common.domain.Upload;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {
    Page<Upload> findByUserId(UUID userId, Pageable pageable);
}
