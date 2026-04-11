package com.scarlxrd.identity_service.config.security;

import com.scarlxrd.identity_service.entity.Role;
import com.scarlxrd.identity_service.entity.User;
import com.scarlxrd.identity_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) throws Exception {

        if (userRepository.findByEmail(adminEmail) == null) {
            User admin = new User(
                    adminEmail,
                    passwordEncoder.encode(adminPassword),
                    Set.of(Role.ADMIN, Role.USER)
            );
            userRepository.save(admin);
           log.info(" Usuário ADMIN criado com sucesso: {}",adminEmail);
        } else {
            log.info(" Usuário ADMIN já existe, não será recriado.");
        }

    }
}
