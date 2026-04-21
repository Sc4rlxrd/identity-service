package com.scarlxrd.identity_service.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.scarlxrd.identity_service.config.redis.RedisService;
import com.scarlxrd.identity_service.config.security.TokenService;
import com.scarlxrd.identity_service.dto.AuthenticationDTO;
import com.scarlxrd.identity_service.dto.RegisterDTO;
import com.scarlxrd.identity_service.dto.TokenResponseDTO;
import com.scarlxrd.identity_service.entity.Role;
import com.scarlxrd.identity_service.entity.User;
import com.scarlxrd.identity_service.repository.UserRepository;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository repository;

    @Mock
    private TokenService tokenService;

    @Mock
    private RedisService redisService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setup() {
        user = new User("test@email.com", "123", Set.of(Role.USER));
    }

    @Test
    void shouldLoginSuccessfully() {
        Authentication auth = mock(Authentication.class);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(auth.getPrincipal()).thenReturn(user);
        when(tokenService.generateAccessToken(user)).thenReturn("access");
        when(tokenService.generateRefreshToken(user)).thenReturn("refresh");
        when(tokenService.getJti("refresh")).thenReturn("jti123");
        when(tokenService.getExpiration("refresh")).thenReturn(Instant.now().plusSeconds(3600));

        TokenResponseDTO response = authService.login(new AuthenticationDTO("test@email.com", "123")
        );

        assertThat(response.accessToken()).isEqualTo("access");
        assertThat(response.refreshToken()).isEqualTo("refresh");

        verify(redisService).saveRefreshToken(eq("jti123"), anyLong());
    }


    @Test
    void shouldRegisterUserSuccessfully() {
        when(repository.findByEmail("test@email.com")).thenReturn(null);
        when(passwordEncoder.encode("123")).thenReturn("encoded");

        authService.register(new RegisterDTO("test@email.com", "123", Set.of(Role.USER)));

        verify(repository).save(any(User.class));
    }

    @Test
    void shouldThrowWhenUserAlreadyExists() {
        when(repository.findByEmail("test@email.com")).thenReturn(user);

        assertThatThrownBy(() -> authService.register(new RegisterDTO("test@email.com", "123", Set.of(Role.USER))))
                .isInstanceOf(RuntimeException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldRefreshTokenSuccessfully() {
        DecodedJWT decoded = mock(DecodedJWT.class);

        when(tokenService.decode("refresh")).thenReturn(decoded);
        when(decoded.getId()).thenReturn("jti123");
        when(decoded.getSubject()).thenReturn("test@email.com");

        when(redisService.isRefreshTokenValid("jti123")).thenReturn(true);
        when(repository.findByEmail("test@email.com")).thenReturn(user);

        when(tokenService.generateAccessToken(user)).thenReturn("newAccess");
        when(tokenService.generateRefreshToken(user)).thenReturn("newRefresh");
        when(tokenService.getJti("newRefresh")).thenReturn("newJti");
        when(tokenService.getExpiration("newRefresh")).thenReturn(Instant.now().plusSeconds(3600));

        TokenResponseDTO response = authService.refreshToken("refresh");

        assertThat(response.accessToken()).isEqualTo("newAccess");

        verify(redisService).deleteRefreshToken("jti123");
        verify(redisService).saveRefreshToken(eq("newJti"), anyLong());
    }

    @Test
    void shouldFailWhenRefreshTokenInvalid() {
        DecodedJWT decoded = mock(DecodedJWT.class);

        when(tokenService.decode("refresh")).thenReturn(decoded);
        when(decoded.getId()).thenReturn("jti123");

        when(redisService.isRefreshTokenValid("jti123")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("refresh"))
                .isInstanceOf(RuntimeException.class);
    }


    @Test
    void shouldLogoutSuccessfully() {
        String token = "Bearer abc";

        when(tokenService.getJti("abc")).thenReturn("jti123");
        when(tokenService.getExpiration("abc")).thenReturn(Instant.now().plusSeconds(3600));

        authService.logout(token);

        verify(redisService).blackListToken(eq("jti123"), anyLong());
    }
}