# 🔐 Identity Service

> Microsserviço responsável pela autenticação e autorização dos usuários do **BookCommerce**, gerenciando tokens JWT com suporte a refresh token, blacklist via Redis e controle de acesso por roles.

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-FF4438?style=for-the-badge&logo=redis&logoColor=white)
![Flyway](https://img.shields.io/badge/Flyway-CC0200?style=for-the-badge&logo=flyway&logoColor=white)

---

## 📋 Sumário

- [Sobre](#-sobre)
- [Responsabilidades](#-responsabilidades)
- [Arquitetura Interna](#-arquitetura-interna)
- [Modelo de Dados](#-modelo-de-dados)
- [Endpoints](#-endpoints)
- [Fluxo de Autenticação](#-fluxo-de-autenticação)
- [Gerenciamento de Tokens](#-gerenciamento-de-tokens)
- [Segurança](#-segurança)
- [Rate Limiting de Login](#-rate-limiting-de-login)
- [Admin Initializer](#-admin-initializer)
- [Tecnologias](#-tecnologias)
- [Como Rodar](#-como-rodar)

---

## 📖 Sobre

O **Identity Service** é o guardião de identidade do BookCommerce. Ele é responsável por registrar usuários, autenticá-los via JWT, gerenciar o ciclo de vida dos tokens (emissão, renovação e revogação) e fornecer informações de autorização para o `gateway-service`.

- **Porta:** `8084`
- **Banco de dados:** PostgreSQL (`auth_db`)
- **Cache:** Redis (blacklist de tokens + refresh tokens válidos)

---

## 🧠 Responsabilidades

- ✅ Registrar novos usuários com senha criptografada (BCrypt)
- ✅ Autenticar usuários e emitir par de tokens (access + refresh)
- ✅ Renovar access token via refresh token
- ✅ Revogar tokens no logout (blacklist via Redis)
- ✅ Validar tokens em cada requisição via `SecurityFilter`
- ✅ Inicializar usuário administrador automaticamente na subida do serviço

---

## 🏗️ Arquitetura Interna

### 📁 Estrutura de Pacotes

```
src/main/java/com/scarlxrd/identity_service/
│
├── config/
│   ├── JpaConfig.java                  # Configurações JPA/Auditing
│   ├── redis/
│   │   ├── RedisConfig.java            # Configuração da conexão Redis
│   │   └── RedisService.java           # Operações de blacklist e refresh token
│   └── security/
│       ├── SecurityConfig.java         # Configuração Spring Security (CORS, filtros, rotas)
│       ├── SecurityFilter.java         # Filtro JWT (OncePerRequestFilter)
│       ├── TokenService.java           # Geração, decodificação e validação de JWT
│       ├── AuthorizationService.java   # UserDetailsService para Spring Security
│       └── AdminInitializer.java       # Cria admin padrão na inicialização
│
├── controller/
│   └── AuthenticationController.java   # Endpoints REST de autenticação
│
├── dto/
│   ├── AuthenticationDTO.java          # Payload de login (email + password)
│   ├── RegisterDTO.java                # Payload de registro
│   ├── RefreshRequestDTO.java          # Payload de renovação de token
│   └── TokenResponseDTO.java          # Resposta com accessToken + refreshToken
│
├── entity/
│   ├── User.java                       # Entidade de usuário
│   ├── Role.java                       # Enum de roles (USER, ADMIN)
│   └── Auditable.java                  # Base com campos de auditoria
│
├── repository/
│   └── UserRepository.java
│
└── service/
    └── AuthService.java                # Lógica de autenticação, registro e tokens
```

---

## 🗃️ Modelo de Dados

### Entidade `User`

| Campo       | Tipo             | Descrição                              |
|-------------|------------------|----------------------------------------|
| `id`        | `UUID`           | PK, gerado automaticamente             |
| `email`     | `String`         | Único, usado como `username`           |
| `password`  | `String`         | Hash BCrypt                            |
| `roles`     | `Set<Role>`      | Conjunto de roles do usuário           |
| `createdAt` | `LocalDateTime`  | Herdado de `Auditable`                 |
| `updatedAt` | `LocalDateTime`  | Herdado de `Auditable`                 |

### Enum `Role`

| Valor   | Descrição              |
|---------|------------------------|
| `USER`  | Usuário padrão         |
| `ADMIN` | Administrador do sistema |

---

## 🚀 Endpoints

Base URL: `/auth`

### Login

**`POST`** `/auth/login` — 🔓 Público

```json
// Request Body
{
  "email": "user@email.com",
  "password": "senha123"
}

// Response 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

### Registro

**`POST`** `/auth/register` — 🔓 Público

```json
// Request Body
{
  "email": "user@email.com",
  "password": "senha123"
}

// Response 201 Created (sem body)
```

> ⚠️ Retorna erro se o e-mail já estiver cadastrado.

---

### Refresh Token

**`POST`** `/auth/refresh` — 🔒 Autenticado

```json
// Request Body
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}

// Response 200 OK
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

> ⚠️ O refresh token anterior é invalidado após o uso (rotação de tokens).

---

### Logout

**`POST`** `/auth/logout` — 🔒 Autenticado

```
Authorization: Bearer <accessToken>
```

```
// Response 200 OK (sem body)
```

> O access token é adicionado à blacklist no Redis com TTL igual ao tempo restante até sua expiração.

---

## 🔄 Fluxo de Autenticação

### Login
1. Usuário envia `email` + `password`
2. Spring Security autentica via `AuthenticationManager`
3. `TokenService` gera `accessToken` (JWT, 1h) e `refreshToken` (JWT, 7 dias)
4. JTI do refresh token é salvo no Redis com TTL de 7 dias
5. Ambos os tokens são retornados ao cliente

### Requisições autenticadas
1. Cliente envia `Authorization: Bearer <accessToken>`
2. `SecurityFilter` intercepta a requisição
3. Verifica se o JTI do token está na **blacklist** do Redis
4. Valida e decodifica o JWT via `TokenService`
5. Autentica o usuário no `SecurityContextHolder`

### Refresh
1. Cliente envia o `refreshToken`
2. `AuthService` decodifica e valida o JTI no Redis
3. Refresh token atual é deletado do Redis (invalidado)
4. Novo par de tokens é gerado e armazenado

### Logout
1. Cliente envia o `accessToken` no header `Authorization`
2. JTI do token é extraído e adicionado à **blacklist** no Redis
3. TTL da blacklist é igual ao tempo restante de validade do token

---

## 🎟️ Gerenciamento de Tokens

| Token          | Duração | Armazenamento Redis              |
|----------------|---------|----------------------------------|
| `accessToken`  | 1 hora  | JTI na blacklist após logout     |
| `refreshToken` | 7 dias  | JTI salvo como válido; deletado após uso ou logout |

### Configuração via `application.yaml`

```yaml
api:
  security:
    token:
      secret: ${JWT_SECRET}
      access-expiration-hours: 1
      refresh-expiration-days: 7
```

---

## 🔐 Segurança

### Rotas

| Rota               | Acesso        |
|--------------------|---------------|
| `POST /auth/login`    | 🔓 Público   |
| `POST /auth/register` | 🔓 Público   |
| `POST /auth/logout`   | 🔒 Autenticado |
| `POST /auth/refresh`  | 🔒 Autenticado |
| Qualquer outra rota   | 🔒 Autenticado |

### CORS

Configurado via `CorsConfigurationSource`:
- Origens: `*` *(aberto temporariamente)*
- Métodos permitidos: `GET`, `POST`, `DELETE`, `OPTIONS`
- Credenciais: habilitadas

> ⚠️ Recomenda-se restringir as origens permitidas antes de ir para produção.

### Sessão

Stateless — nenhuma sessão HTTP é criada (`SessionCreationPolicy.STATELESS`).

### Senhas

Criptografadas com **BCrypt** via `PasswordEncoder`.

---

## ⚡ Rate Limiting de Login

Configurado via `application.yaml`:

```yaml
api:
  security:
    rate-limit:
      login-attempts: 5
      login-timeout-seconds: 60
```

- Máximo de **5 tentativas** de login
- Bloqueio de **60 segundos** após exceder o limite

---

## 👤 Admin Initializer

O `AdminInitializer` cria automaticamente um usuário administrador na primeira inicialização do serviço, caso não exista:

```yaml
app:
  admin:
    email: ${ADMIN_EMAIL:SCARLXRD@teste.com}
    password: ${ADMIN_PASSWORD:1234569999}
```

> 💡 Recomenda-se sobrescrever essas credenciais via variáveis de ambiente em produção.

---

## ⚙️ Tecnologias

| Tecnologia        | Finalidade                              |
|-------------------|-----------------------------------------|
| Java 21           | Linguagem principal                     |
| Spring Boot 3.x   | Framework base                          |
| Spring Security 6 | Autenticação, autorização e filtros JWT |
| Spring Data JPA   | Persistência                            |
| Spring Validation | Validação de DTOs                       |
| PostgreSQL        | Banco de dados relacional               |
| Flyway            | Migrations do banco                     |
| Redis             | Blacklist de tokens e refresh tokens    |
| JWT               | Geração e validação de tokens           |
| BCrypt            | Hash de senhas                          |
| Lombok            | Redução de boilerplate                  |
| Virtual Threads   | Concorrência leve (Java 21)             |
| Logstash Logback  | Logs estruturados (JSON)                |

---

## 🐳 Como Rodar

### Pré-requisitos

- Java 21+
- Maven
- PostgreSQL na porta `5436` com banco `auth_db`
- Redis na porta `6379`

### Executar o serviço

```bash
./mvnw spring-boot:run
```

### Variáveis de ambiente recomendadas para produção

| Variável         | Descrição                        |
|------------------|----------------------------------|
| `JWT_SECRET`     | Chave secreta para assinar JWTs  |
| `ADMIN_EMAIL`    | E-mail do administrador padrão   |
| `ADMIN_PASSWORD` | Senha do administrador padrão    |

> 💡 Nunca exponha `JWT_SECRET` ou credenciais do admin no `application.yaml` em produção. Use variáveis de ambiente ou um cofre de segredos (ex: AWS Secrets Manager, Vault).

### Configurações (`application.yaml`)

| Propriedade                  | Valor padrão                                   |
|------------------------------|------------------------------------------------|
| `server.port`                | `8084`                                         |
| `spring.datasource.url`      | `jdbc:postgresql://localhost:5436/auth_db`     |
| `spring.redis.host`          | `localhost`                                    |
| `spring.redis.port`          | `6379`                                         |
| `access-expiration-hours`    | `1`                                            |
| `refresh-expiration-days`    | `7`                                            |

---

> Parte da arquitetura de microserviços do **BookCommerce** — fornece autenticação para o `gateway-service` e demais microsserviços internos.