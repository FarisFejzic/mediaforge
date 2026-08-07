package com.mediaforge.common.repository;

import com.mediaforge.common.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findByUploadId(UUID uploadId);
}
