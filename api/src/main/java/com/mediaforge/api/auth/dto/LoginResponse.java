package com.mediaforge.api.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        Long expiresIn
) {
}
