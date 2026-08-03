package com.mediaforge.api.auth.dto;

import com.mediaforge.common.domain.enums.Role;
import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(

        UUID id,
        String email,
        Role role,
        OffsetDateTime createdAt
) {
}
