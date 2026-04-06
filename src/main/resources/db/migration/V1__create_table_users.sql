CREATE TABLE users
(
    id         UUID         NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,


    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    created_by VARCHAR(255),
    version    BIGINT,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE TABLE user_roles
(
    user_id UUID NOT NULL,
    roles   VARCHAR(50),

    CONSTRAINT fk_user_roles_on_user FOREIGN KEY (user_id) REFERENCES users (id)
);