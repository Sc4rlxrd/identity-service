package com.scarlxrd.identity_service.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens gerados após autenticação")
public record TokenResponseDTO(
        @Schema(description = "Access token JWT com validade de 1 hora", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Refresh token JWT com validade de 7 dias", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String refreshToken
) {}