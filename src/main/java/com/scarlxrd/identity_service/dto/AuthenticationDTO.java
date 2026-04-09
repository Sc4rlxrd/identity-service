package com.scarlxrd.identity_service.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthenticationDTO(

        @NotBlank
        String email,
        @NotBlank
        String password
) {
}
