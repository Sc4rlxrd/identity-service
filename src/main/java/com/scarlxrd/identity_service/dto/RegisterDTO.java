package com.scarlxrd.identity_service.dto;

import com.scarlxrd.identity_service.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

@Schema(description = "Dados para registro de novo usuário")
public record RegisterDTO(
        @Schema(description = "Email do usuário", example = "novo@usuario.com")
        @NotBlank
        String email,

        @Schema(description = "Senha do usuário", example = "123456")
        @NotBlank
        String password,

        @Schema(description = "Roles do usuário", example = "[\"USER\"]")
        Set<Role> roles
) {}
