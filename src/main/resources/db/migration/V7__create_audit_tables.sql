CREATE TABLE audit_log (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), actor_user_id UUID REFERENCES app_user(id) ON DELETE SET NULL,
 action VARCHAR(80) NOT NULL, entity_type VARCHAR(100) NOT NULL, entity_id VARCHAR(80), description VARCHAR(500) NOT NULL,
 previous_data JSONB, new_data JSONB, ip_address VARCHAR(45), occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE INDEX idx_audit_occurred_at ON audit_log(occurred_at DESC);
CREATE INDEX idx_audit_entity ON audit_log(entity_type,entity_id);
