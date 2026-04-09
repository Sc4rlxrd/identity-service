package com.scarlxrd.identity_service.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken
) {
}
