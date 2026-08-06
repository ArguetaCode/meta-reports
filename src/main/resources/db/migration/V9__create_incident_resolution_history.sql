ALTER TABLE ad_classification DROP CONSTRAINT ad_classification_method_check;
ALTER TABLE ad_classification ADD CONSTRAINT ad_classification_method_check
    CHECK (method IN ('FACEBOOK_PAGE', 'INSTAGRAM_ACCOUNT', 'EXCLUSIVE_AD_ACCOUNT', 'MANUAL', 'UNCLASSIFIED'));

CREATE TABLE manual_ad_assignment (
    ad_id UUID PRIMARY KEY REFERENCES meta_ad(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
    note VARCHAR(1000) NOT NULL,
    assigned_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_manual_ad_assignment_client ON manual_ad_assignment(client_id);

CREATE TABLE incident_resolution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES classification_incident(id) ON DELETE CASCADE,
    action VARCHAR(20) NOT NULL CHECK (action IN ('RESOLVE', 'IGNORE', 'REPROCESS')),
    previous_status VARCHAR(20) NOT NULL,
    resulting_status VARCHAR(20) NOT NULL,
    selected_client_id UUID REFERENCES client(id) ON DELETE RESTRICT,
    note VARCHAR(1000),
    performed_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    performed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_incident_resolution_history_incident
    ON incident_resolution_history(incident_id, performed_at DESC);
