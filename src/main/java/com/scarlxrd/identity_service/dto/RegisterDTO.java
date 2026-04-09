package com.scarlxrd.identity_service.dto;

import com.scarlxrd.identity_service.entity.Role;
import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record RegisterDTO(

        @NotBlank
        String email,
        @NotBlank
        String password,
        @NotBlank
        Set<Role> roles
) {
}
