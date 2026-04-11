package com.scarlxrd.identity_service.service;

import com.scarlxrd.identity_service.config.redis.RedisService;
import com.scarlxrd.identity_service.config.security.TokenService;
import com.scarlxrd.identity_service.dto.AuthenticationDTO;
import com.scarlxrd.identity_service.dto.RegisterDTO;
import com.scarlxrd.identity_service.dto.TokenResponseDTO;
import com.scarlxrd.identity_service.entity.Role;
import com.scarlxrd.identity_service.entity.User;
import com.scarlxrd.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    public TokenResponseDTO login(AuthenticationDTO data) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(data.email(), data.password())
        );

        User user = (User) auth.getPrincipal();
        return generateAndStoreTokens(user);
    }

    public void register(RegisterDTO data) {
        if (repository.findByEmail(data.email()) != null) {
            throw new RuntimeException("User already exists");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = new User(data.email(), encryptedPassword, Set.of(Role.USER));
        repository.save(newUser);
    }

    public TokenResponseDTO refreshToken(String refreshToken) {
        var decoded = tokenService.decode(refreshToken);
        String refreshJti = decoded.getId();

        if (!redisService.isRefreshTokenValid(refreshJti)) {
            throw new RuntimeException("Invalid Refresh Token");
        }

        User user = (User) repository.findByEmail(decoded.getSubject());


        redisService.deleteRefreshToken(refreshJti);


        return generateAndStoreTokens(user);
    }

    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String jti = tokenService.getJti(token);
            long ttl = tokenService.getExpiration(token).getEpochSecond() - Instant.now().getEpochSecond();
            redisService.blackListToken(jti, ttl);
        }
    }


    private TokenResponseDTO generateAndStoreTokens(User user) {
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);

        var jti = tokenService.getJti(refreshToken);
        long ttl = tokenService.getExpiration(refreshToken).getEpochSecond() - Instant.now().getEpochSecond();
        redisService.saveRefreshToken(jti, ttl);

        return new TokenResponseDTO(accessToken, refreshToken);
    }
}