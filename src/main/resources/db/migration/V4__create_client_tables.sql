CREATE TABLE client (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), code VARCHAR(30) NOT NULL UNIQUE,
    commercial_name VARCHAR(160) NOT NULL, legal_name VARCHAR(200), description VARCHAR(1000),
    status VARCHAR(20) NOT NULL, primary_currency VARCHAR(3) NOT NULL, timezone VARCHAR(80) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    updated_by UUID REFERENCES app_user(id) ON DELETE SET NULL, version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_client_code CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]{1,29}$'),
    CONSTRAINT ck_client_status CHECK (status IN ('ACTIVE','INACTIVE','PILOT','ARCHIVED')),
    CONSTRAINT ck_client_currency CHECK (primary_currency ~ '^[A-Z]{3}$')
);
CREATE INDEX idx_client_commercial_name ON client(lower(commercial_name));
CREATE INDEX idx_client_active ON client(active);

CREATE TABLE client_branding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(), client_id UUID NOT NULL UNIQUE REFERENCES client(id) ON DELETE CASCADE,
    primary_color CHAR(7), secondary_color CHAR(7), accent_color CHAR(7), logo_path VARCHAR(500),
    report_header_text VARCHAR(500), report_footer_text VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_brand_primary CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_brand_secondary CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT ck_brand_accent CHECK (accent_color IS NULL OR accent_color ~ '^#[0-9A-Fa-f]{6}$')
);
