package com.scarlxrd.identity_service.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import static net.logstash.logback.argument.StructuredArguments.kv;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public void saveRefreshToken(String jti, long ttlSeconds){

        ttlSeconds = Math.max(ttlSeconds, 0);
        redisTemplate.opsForValue().set("refresh:" + jti, "valid", ttlSeconds, TimeUnit.SECONDS);

        log.info("Refresh token stored in Redis",
                kv("jti", jti),
                kv("ttl_seconds", ttlSeconds),
                kv("action", "save_refresh_token"));
    }

    public boolean isRefreshTokenValid(String jti){
        boolean exists = redisTemplate.hasKey("refresh:" + jti);
        if (!exists) {
            log.warn("Invalid or expired refresh token attempt",
                    kv("jti", jti),
                    kv("action", "validate_refresh_token"));
        }
        return exists;
    }

    public void deleteRefreshToken(String jti){
        redisTemplate.delete("refresh:" + jti);
    }


    public void blackListToken(String jti, long ttlSeconds){
        ttlSeconds = Math.max(ttlSeconds, 0);
        redisTemplate.opsForValue().set("blacklist:" + jti, "revoked", ttlSeconds, TimeUnit.SECONDS);
    }
    public boolean isBlackListed(String jti){
        boolean blocked = redisTemplate.hasKey("blacklist:" + jti);
        if (blocked) {
            log.warn("Revoked token access attempt",
                    kv("jti", jti),
                    kv("security_status", "blocked"));
        }
        return blocked;
    }

    public boolean isAllowed(String key, int limit, long seconds) {
        Long current = redisTemplate.opsForValue().increment("rl:" + key);

        if (current != null && current == 1) {

            redisTemplate.expire("rl:" + key, seconds, TimeUnit.SECONDS);
        }

        boolean allowed = current != null && current <= limit;

        if (!allowed) {
            log.warn("Rate limit exceeded",
                    kv("key", key),
                    kv("action", "rate_limit_blocked"));
        }
        return allowed;
    }

    public Long getExpire(String key) {
        return redisTemplate.getExpire("rl:" + key, TimeUnit.SECONDS);
    }
}