CREATE TABLE campaign_prefix (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
 prefix VARCHAR(30) NOT NULL, normalized_prefix VARCHAR(30) NOT NULL, description VARCHAR(300), primary_prefix BOOLEAN NOT NULL DEFAULT FALSE,
 active BOOLEAN NOT NULL DEFAULT TRUE, valid_from DATE NOT NULL DEFAULT CURRENT_DATE, valid_until DATE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 CHECK(normalized_prefix ~ '^[A-Z0-9][A-Z0-9_-]{1,29}$'), CHECK(valid_until IS NULL OR valid_until >= valid_from));
CREATE UNIQUE INDEX uq_active_campaign_prefix ON campaign_prefix(normalized_prefix) WHERE active AND valid_until IS NULL;
CREATE INDEX idx_campaign_prefix_client ON campaign_prefix(client_id);
