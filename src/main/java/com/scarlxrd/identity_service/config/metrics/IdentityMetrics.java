package com.scarlxrd.identity_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityMetrics {

    private final MeterRegistry meterRegistry;

    public void loginSuccess() {
        Counter.builder("auth_login_success_total")
                .description("Total de logins realizados com sucesso")
                .tag("service", "identity-service")
                .register(meterRegistry)
                .increment();
    }

    public void loginFailed(String reason) {
        Counter.builder("auth_login_failed_total")
                .description("Total de falhas de login")
                .tag("service", "identity-service")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void registerSuccess() {
        Counter.builder("auth_register_success_total")
                .description("Total de usuários registrados com sucesso")
                .tag("service", "identity-service")
                .register(meterRegistry)
                .increment();
    }

    public void registerFailed(String reason) {
        Counter.builder("auth_register_failed_total")
                .description("Total de falhas ao registrar usuários")
                .tag("service", "identity-service")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void tokenGenerated(String type) {
        Counter.builder("jwt_tokens_generated_total")
                .description("Total de tokens JWT gerados")
                .tag("service", "identity-service")
                .tag("type", type)
                .register(meterRegistry)
                .increment();
    }

    public void refreshTokenSuccess() {
        Counter.builder("auth_refresh_token_success_total")
                .description("Total de refresh tokens renovados com sucesso")
                .tag("service", "identity-service")
                .register(meterRegistry)
                .increment();
    }

    public void refreshTokenFailed(String reason) {
        Counter.builder("auth_refresh_token_failed_total")
                .description("Total de falhas ao renovar refresh token")
                .tag("service", "identity-service")
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }
}