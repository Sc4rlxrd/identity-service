package com.scarlxrd.identity_service.config.actuator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class MonitoringUserConfig {


    @Value("${monitoring.prometheus.username}")
    private String username;

    @Value("${monitoring.prometheus.password}")
    private String password;

    @Bean
    public InMemoryUserDetailsManager inMemoryUserDetailsManager(PasswordEncoder passwordEncoder) {

        UserDetails prometheusUser = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .roles("PROMETHEUS")
                .build();

        return new InMemoryUserDetailsManager(prometheusUser);
    }
}
