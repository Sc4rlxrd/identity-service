package com.scarlxrd.identity_service.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scarlxrd.identity_service.config.security.SecurityFilter;
import com.scarlxrd.identity_service.dto.AuthenticationDTO;
import com.scarlxrd.identity_service.dto.RefreshRequestDTO;
import com.scarlxrd.identity_service.dto.RegisterDTO;
import com.scarlxrd.identity_service.dto.TokenResponseDTO;
import com.scarlxrd.identity_service.entity.Role;
import com.scarlxrd.identity_service.exception.InvalidTokenException;
import com.scarlxrd.identity_service.exception.UserAlreadyExistsException;
import com.scarlxrd.identity_service.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthenticationController.class,
        excludeAutoConfiguration = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class
        }
)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("application-test.properties")
@Import(AuthService.class)
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private SecurityFilter securityFilter;


    @Test
    @DisplayName("Deve autenticar e retornar tokens")
    void login_Success() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("user@user.com", "123");

        when(authService.login(any(AuthenticationDTO.class)))
                .thenReturn(new TokenResponseDTO("access", "refresh"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access"))
                .andExpect(jsonPath("$.refreshToken").value("refresh"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando credenciais forem inválidas")
    void login_Invalid() throws Exception {
        AuthenticationDTO dto = new AuthenticationDTO("user@user.com", "wrong");

        when(authService.login(any(AuthenticationDTO.class)))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("Deve registrar usuário com sucesso")
    void register_Success() throws Exception {
        RegisterDTO dto = new RegisterDTO("new@user.com", "123", Set.of(Role.USER));

        doNothing().when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        verify(authService).register(any(RegisterDTO.class));
    }

    @Test
    @DisplayName("Deve retornar 400 ao tentar registrar email existente")
    void register_ExistingEmail() throws Exception {
        RegisterDTO dto = new RegisterDTO("existing@user.com", "123", Set.of(Role.USER));

        doThrow(new UserAlreadyExistsException("User already exists"))
                .when(authService).register(any(RegisterDTO.class));

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Deve retornar 200 e novos tokens ao fazer refresh com sucesso")
    void refresh_Success() throws Exception {
        RefreshRequestDTO dto = new RefreshRequestDTO("valid.refresh.token");

        when(authService.refreshToken("valid.refresh.token"))
                .thenReturn(new TokenResponseDTO("new.access", "new.refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access"))
                .andExpect(jsonPath("$.refreshToken").value("new.refresh"));
    }

    @Test
    @DisplayName("Deve retornar 401 quando refresh token for inválido")
    void refresh_InvalidToken() throws Exception {
        RefreshRequestDTO dto = new RefreshRequestDTO("invalid.refresh.token");

        doThrow(new InvalidTokenException("Invalid Refresh Token"))
                .when(authService).refreshToken("invalid.refresh.token");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    @DisplayName("Deve fazer logout com sucesso")
    void logout_Success() throws Exception {
        doNothing().when(authService).logout("Bearer valid.token");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk());

        verify(authService).logout("Bearer valid.token");
    }

    @Test
    @DisplayName("Não deve fazer blacklist se header for inválido")
    void logout_InvalidHeader() throws Exception {
        doNothing().when(authService).logout("");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", ""))
                .andExpect(status().isOk());

        verify(authService).logout("");
    }
}