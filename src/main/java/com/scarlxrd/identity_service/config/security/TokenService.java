package com.scarlxrd.identity_service.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.scarlxrd.identity_service.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class TokenService {


    @Value("${api.security.token.secret}")
    private String secret;
    @Value("${api.security.token.access-expiration-hours}")
    private long accessExpirationHours;
    @Value("${api.security.token.refresh-expiration-days}")
    private long refreshExpirationDays;

    private Algorithm getAlgorithm(){
        return Algorithm.HMAC256(secret);
    }

    public String generateAccessToken(User user) {
        return JWT.create()
                .withIssuer("book-commerce")
                .withSubject(user.getEmail())
                .withExpiresAt(LocalDateTime.now().plusHours(accessExpirationHours).toInstant(ZoneOffset.of("-03:00")))
                .withJWTId(UUID.randomUUID().toString())
                .sign(getAlgorithm());
    }

    public String generateRefreshToken(User user){
        return JWT.create()
                .withIssuer("book-commerce")
                .withSubject(user.getEmail())
                .withExpiresAt(LocalDateTime.now().plusDays(refreshExpirationDays).toInstant(ZoneOffset.of("-03:00")))
                .withJWTId(UUID.randomUUID().toString())
                .sign(getAlgorithm());
    }
    public DecodedJWT decode (String token){
        return JWT.require(getAlgorithm())
                .withIssuer("book-commerce")
                .build()
                .verify(token);
    }

    public String getSubject(String token){
        return decode(token).getSubject();
    }

    public String getJti(String token) {
        return decode(token).getId();
    }

    public Instant getExpiration(String token) {
        return decode(token).getExpiresAt().toInstant();
    }
}