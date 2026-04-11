package com.scarlxrd.identity_service.dto;

import com.scarlxrd.identity_service.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record RegisterDTO(

        @NotBlank
        String email,
        @NotBlank
        String password,
        Set<Role> roles
) {
}
