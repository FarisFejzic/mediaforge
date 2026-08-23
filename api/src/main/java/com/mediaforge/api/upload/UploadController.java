package com.mediaforge.api.upload;

import com.mediaforge.api.upload.dto.UploadDetailResponse;
import com.mediaforge.api.upload.dto.UploadResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("api/uploads")
@SecurityRequirement(name = "bearerAuth")
public class UploadController {

    private  final UploadService uploadService;

    public UploadController(UploadService uploadService){
        this.uploadService = uploadService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        UploadResponse response = uploadService.upload(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public Page<UploadResponse> list(
            Pageable pageable,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return uploadService.list(userId, pageable);
    }

    @GetMapping("/{id}")
    public UploadDetailResponse getDetail(
            @PathVariable UUID id,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return uploadService.getDetail(id, userId);
    }

}
