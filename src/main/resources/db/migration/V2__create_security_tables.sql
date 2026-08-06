CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), username VARCHAR(80) NOT NULL,
    email VARCHAR(254) NOT NULL, password_hash VARCHAR(100) NOT NULL,
    first_name VARCHAR(100) NOT NULL, last_name VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE, account_locked BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_attempts >= 0),
    locked_until TIMESTAMPTZ, last_login_at TIMESTAMPTZ, password_changed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_user_username UNIQUE (username), CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT ck_user_username_lower CHECK (username = lower(username)),
    CONSTRAINT ck_user_email_lower CHECK (email = lower(email))
);
CREATE INDEX idx_user_enabled ON app_user(enabled);

CREATE TABLE role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL, description VARCHAR(500), system_role BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL, description VARCHAR(500), created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    PRIMARY KEY (user_id, role_id)
);
CREATE TABLE role_permission (
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permission(id) ON DELETE RESTRICT,
    PRIMARY KEY (role_id, permission_id)
);
