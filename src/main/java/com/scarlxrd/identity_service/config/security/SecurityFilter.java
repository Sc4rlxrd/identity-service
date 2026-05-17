package com.scarlxrd.identity_service.config.security;

import com.scarlxrd.identity_service.config.redis.RedisService;
import com.scarlxrd.identity_service.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import static net.logstash.logback.argument.StructuredArguments.kv;
import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RedisService redisService;


    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        if (path.contains("/auth/login") || path.contains("/auth/register")) {
            filterChain.doFilter(request, response);
            return;
        }

        var token = this.recoverToken(request);
        if (token != null) {
            try {
                var decoded = tokenService.decode(token);
                String jti = decoded.getId();


                if (redisService.isBlackListed(jti)){
                    log.warn("Token revoked detected",
                            kv("jti", jti),
                            kv("status", "denied"),
                            kv("remote_ip", request.getRemoteAddr()));
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }

                String login = decoded.getSubject();
                UserDetails user = userRepository.findByEmail(login);
                var authentication = new UsernamePasswordAuthenticationToken(user,null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.info("Authenticated user",
                        kv("user_email", login),
                        kv("auth_method", "jwt"));


            } catch (Exception e) {
                log.error("Token authentication failed.",
                        kv("error", e.getMessage()),
                        kv("request_uri", request.getRequestURI()));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request,response);
    }
    private String recoverToken(HttpServletRequest request){
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) {
            return null;
        }
        String normalizedHeader = authHeader.trim().toUpperCase();
        if (!normalizedHeader.startsWith("BEARER ")) {
            return null;
        }
        return authHeader.substring(authHeader.indexOf("Bearer ") + 7).trim();
    }

    }

