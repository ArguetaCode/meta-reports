CREATE TABLE sync_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source VARCHAR(20) NOT NULL CHECK (source IN ('FIXTURE')),
    fixture_name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED')),
    correlation_id UUID NOT NULL UNIQUE,
    current_cursor VARCHAR(200),
    pages_processed INTEGER NOT NULL DEFAULT 0 CHECK (pages_processed >= 0),
    records_received INTEGER NOT NULL DEFAULT 0 CHECK (records_received >= 0),
    records_upserted INTEGER NOT NULL DEFAULT 0 CHECK (records_upserted >= 0),
    incidents_created INTEGER NOT NULL DEFAULT 0 CHECK (incidents_created >= 0),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    error_message VARCHAR(1000),
    requested_by UUID REFERENCES app_user(id) ON DELETE SET NULL
);
CREATE INDEX idx_sync_execution_status_started ON sync_execution(status, started_at DESC);

CREATE TABLE meta_campaign (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_campaign_id VARCHAR(80) NOT NULL UNIQUE,
    ad_account_id UUID NOT NULL REFERENCES meta_ad_account(id) ON DELETE RESTRICT,
    campaign_name VARCHAR(300) NOT NULL,
    effective_status VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_meta_campaign_account ON meta_campaign(ad_account_id);

CREATE TABLE meta_ad_set (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_ad_set_id VARCHAR(80) NOT NULL UNIQUE,
    campaign_id UUID NOT NULL REFERENCES meta_campaign(id) ON DELETE RESTRICT,
    ad_set_name VARCHAR(300) NOT NULL,
    effective_status VARCHAR(40),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_meta_ad_set_campaign ON meta_ad_set(campaign_id);

CREATE TABLE meta_ad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    meta_ad_id VARCHAR(80) NOT NULL UNIQUE,
    ad_set_id UUID NOT NULL REFERENCES meta_ad_set(id) ON DELETE RESTRICT,
    ad_name VARCHAR(300) NOT NULL,
    effective_status VARCHAR(40),
    facebook_page_meta_id VARCHAR(80),
    instagram_account_meta_id VARCHAR(80),
    source_payload JSONB NOT NULL,
    last_sync_execution_id UUID NOT NULL REFERENCES sync_execution(id) ON DELETE RESTRICT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_meta_ad_ad_set ON meta_ad(ad_set_id);
CREATE INDEX idx_meta_ad_page_signal ON meta_ad(facebook_page_meta_id) WHERE facebook_page_meta_id IS NOT NULL;
CREATE INDEX idx_meta_ad_instagram_signal ON meta_ad(instagram_account_meta_id) WHERE instagram_account_meta_id IS NOT NULL;

CREATE TABLE ad_classification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id UUID NOT NULL UNIQUE REFERENCES meta_ad(id) ON DELETE CASCADE,
    client_id UUID REFERENCES client(id) ON DELETE RESTRICT,
    method VARCHAR(30) NOT NULL CHECK (method IN ('FACEBOOK_PAGE', 'INSTAGRAM_ACCOUNT', 'EXCLUSIVE_AD_ACCOUNT', 'UNCLASSIFIED')),
    confidence VARCHAR(20) NOT NULL CHECK (confidence IN ('HIGH', 'MEDIUM', 'NONE')),
    signals JSONB NOT NULL,
    classified_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sync_execution_id UUID NOT NULL REFERENCES sync_execution(id) ON DELETE RESTRICT
);
CREATE INDEX idx_ad_classification_client ON ad_classification(client_id) WHERE client_id IS NOT NULL;

CREATE TABLE classification_incident (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id UUID NOT NULL REFERENCES meta_ad(id) ON DELETE CASCADE,
    sync_execution_id UUID NOT NULL REFERENCES sync_execution(id) ON DELETE RESTRICT,
    incident_type VARCHAR(40) NOT NULL CHECK (incident_type IN ('UNCLASSIFIED', 'CONFLICTING_SIGNALS', 'PREFIX_ONLY', 'UNKNOWN_PREFIX')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'RESOLVED', 'IGNORED')),
    description VARCHAR(500) NOT NULL,
    signals JSONB NOT NULL,
    resolved_client_id UUID REFERENCES client(id) ON DELETE RESTRICT,
    resolution_note VARCHAR(1000),
    resolved_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    resolved_at TIMESTAMPTZ,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_incident_per_execution UNIQUE(ad_id, sync_execution_id, incident_type)
);
CREATE INDEX idx_classification_incident_status ON classification_incident(status, occurred_at DESC);
CREATE INDEX idx_classification_incident_ad ON classification_incident(ad_id);
