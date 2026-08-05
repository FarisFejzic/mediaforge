package com.mediaforge.api.upload;

import com.mediaforge.api.upload.dto.UploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("api/uploads")
public class UploadController {

    private  final UploadService uploadService;

    public UploadController(UploadService uploadService){
        this.uploadService = uploadService;
    }

    @PostMapping
    public ResponseEntity<UploadResponse> upload (
            @RequestParam("file")MultipartFile file,
            Authentication authentication){
        UUID userId = UUID.fromString(authentication.getName());
        UploadResponse response = uploadService.upload(file,userId);
        return  ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

}
