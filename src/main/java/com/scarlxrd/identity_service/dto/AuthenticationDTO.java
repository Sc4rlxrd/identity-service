package com.scarlxrd.identity_service.dto;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para autenticação")
public record AuthenticationDTO(
        @Schema(description = "Email do usuário", example = "SCARLXRD@teste.com")
        @NotBlank
        String email,

        @Schema(description = "Senha do usuário", example = "123456")
        @NotBlank
        String password
) {}