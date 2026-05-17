package com.scarlxrd.identity_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para renovação de token")
public record RefreshRequestDTO(
        @Schema(description = "Refresh token válido", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}