package com.scarlxrd.identity_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI(@Value("${app.gateway-url:http://localhost:8080}") String gatewayUrl) {
        return new OpenAPI()
                .servers(buildServers(gatewayUrl))
                .info(buildInfo())
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(buildComponents())
                .externalDocs(new ExternalDocumentation()
                        .description("Repositório do projeto")
                        .url("https://github.com/Sc4rlxrd/identity-service"));
    }

    private List<Server> buildServers(String gatewayUrl) {
        return List.of(
                new Server()
                        .url(gatewayUrl)
                        .description("Gateway")
        );
    }

    private Info buildInfo() {
        return new Info()
                .title("Identity-Service API")
                .description("""
                        Serviço de autenticação e autorização do BookCommerce.
                        
                        **Endpoints disponíveis:**
                        - Login com email e senha
                        - Registro de novo usuário
                        - Renovação de tokens (refresh)
                        - Logout com revogação de token
                        
                        **Segurança:**
                        - Access Token JWT com validade de **1 hora**
                        - Refresh Token JWT com validade de **7 dias** com rotação a cada uso
                        - Blacklist de tokens revogados via **Redis**
                        - Rate limiting no endpoint de login
                        
                        **Como usar:**
                        1. Faça login em `POST /auth/login`
                        2. Copie o `accessToken` retornado
                        3. Clique em **Authorize** no topo da página
                        4. Cole o token no campo `bearerAuth`
                        """)
                .version("v1")
                .contact(new Contact()
                        .name("Scarlxrd")
                        .url("https://github.com/Sc4rlxrd")
                        .email("contato@exemplo.com"));
    }

    private Components buildComponents() {
        return new Components()
                .addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT"));
    }
}