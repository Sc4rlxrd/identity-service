package com.scarlxrd.identity_service.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final AuthorizationService authorizationService;

    @Autowired
    @Qualifier("inMemoryUserDetailsManager")
    private UserDetailsService monitoringUserDetailsService;

    @Bean
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize->authorize.requestMatchers("/actuator/prometheus").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .userDetailsService(monitoringUserDetailsService)
                .httpBasic(withDefaults())
                .build();
    }


    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .userDetailsService(authorizationService)
                .authorizeHttpRequests(authorize ->authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").authenticated()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/error").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration configuration = new CorsConfiguration();

        // aberto temporariamente para qualquer origem
        configuration.addAllowedOriginPattern("*");
        // métodos permitidos
        configuration.setAllowedMethods(List.of("GET","POST","DELETE","OPTIONS"));
        // headers permitidos
        configuration.setAllowedHeaders(List.of("*"));
        // permitir (cookies, Authorization header)
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return  source;
    }


    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public static PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(authorizationService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
}