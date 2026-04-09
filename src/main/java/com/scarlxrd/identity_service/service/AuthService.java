package com.scarlxrd.identity_service.service;

import com.scarlxrd.identity_service.config.redis.RedisService;
import com.scarlxrd.identity_service.config.security.TokenService;
import com.scarlxrd.identity_service.dto.AuthenticationDTO;
import com.scarlxrd.identity_service.dto.RefreshRequestDTO;
import com.scarlxrd.identity_service.dto.RegisterDTO;
import com.scarlxrd.identity_service.dto.TokenResponseDTO;
import com.scarlxrd.identity_service.entity.Role;
import com.scarlxrd.identity_service.entity.User;
import com.scarlxrd.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository repository;
    private final TokenService tokenService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;


    public ResponseEntity<TokenResponseDTO> login (AuthenticationDTO data){
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(data.email(), data.password()));
        User user = (User) auth.getPrincipal();
        String accessToken = tokenService.generateAccessToken(user);
        String refreshToken = tokenService.generateRefreshToken(user);


        var jti = tokenService.getJti(refreshToken);
        long ttl = tokenService.getExpiration(refreshToken).getEpochSecond() - Instant.now().getEpochSecond();
        redisService.saveRefreshToken(jti,ttl);
        return ResponseEntity.ok(new TokenResponseDTO(accessToken,refreshToken));
    }

    public  ResponseEntity<?> register(RegisterDTO data) {
        if (repository.findByEmail(data.email()) != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User already exists");
        }

        String encryptedPassword = passwordEncoder.encode(data.password());
        User newUser = new User(data.email(), encryptedPassword, Set.of(Role.USER));
        this.repository.save(newUser);
        return ResponseEntity.ok().build();
    }

    public ResponseEntity<TokenResponseDTO> refresh( RefreshRequestDTO requestDto){
        var decoded = tokenService.decode(requestDto.refreshToken());
        String refreshJti = decoded.getId();
        if(!redisService.isRefreshTokenValid(refreshJti)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = (User) repository.findByEmail(decoded.getSubject());
        String newAccessToken = tokenService.generateAccessToken(user);
        String newRefreshToken = tokenService.generateRefreshToken(user);

        redisService.deleteRefreshToken(refreshJti);

        String newJti = tokenService.getJti(newRefreshToken);
        long ttl = tokenService.getExpiration(newRefreshToken).getEpochSecond() - Instant.now().getEpochSecond();
        redisService.saveRefreshToken(newJti,ttl);

        return ResponseEntity.ok(new TokenResponseDTO(newAccessToken,newRefreshToken));
    }

    public ResponseEntity<Void> logout(String authHeader ){
        if(authHeader!= null && authHeader.startsWith("Bearer ")){
            String token = authHeader.substring(7);
            String jti = tokenService.getJti(token);
            long ttl = tokenService.getExpiration(token).getEpochSecond() - Instant.now().getEpochSecond();
            redisService.blackListToken(jti,ttl);
        }
        return ResponseEntity.ok().build();
    }
}
