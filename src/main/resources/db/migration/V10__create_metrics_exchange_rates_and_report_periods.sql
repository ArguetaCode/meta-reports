CREATE TABLE daily_ad_insight (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ad_id UUID NOT NULL REFERENCES meta_ad(id) ON DELETE CASCADE,
    insight_date DATE NOT NULL,
    source VARCHAR(20) NOT NULL CHECK (source='META_FIXTURE'),
    attribution_window VARCHAR(40) NOT NULL,
    currency CHAR(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    spend NUMERIC(19,6) NOT NULL CHECK (spend >= 0),
    impressions BIGINT NOT NULL CHECK (impressions >= 0),
    reach BIGINT NOT NULL CHECK (reach >= 0),
    clicks BIGINT NOT NULL CHECK (clicks >= 0),
    source_payload JSONB NOT NULL,
    imported_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(ad_id,insight_date,source,attribution_window)
);
CREATE INDEX idx_daily_ad_insight_date ON daily_ad_insight(insight_date,ad_id);

CREATE TABLE insight_action (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insight_id UUID NOT NULL REFERENCES daily_ad_insight(id) ON DELETE CASCADE,
    action_type VARCHAR(120) NOT NULL,
    value NUMERIC(19,6) NOT NULL CHECK (value >= 0),
    UNIQUE(insight_id,action_type)
);

CREATE TABLE exchange_rate (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rate_month DATE NOT NULL CHECK (rate_month=date_trunc('month',rate_month)::date),
    source_currency CHAR(3) NOT NULL CHECK (source_currency ~ '^[A-Z]{3}$'),
    target_currency CHAR(3) NOT NULL CHECK (target_currency ~ '^[A-Z]{3}$'),
    rate NUMERIC(19,8) NOT NULL CHECK (rate > 0),
    rate_source VARCHAR(120) NOT NULL,
    created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(rate_month,source_currency,target_currency,rate_source),
    CHECK (source_currency <> target_currency)
);

CREATE TABLE report_period (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT','READY')),
    report_currency CHAR(3) NOT NULL CHECK (report_currency ~ '^[A-Z]{3}$'),
    exchange_rate_id UUID REFERENCES exchange_rate(id) ON DELETE RESTRICT,
    exchange_rate_snapshot NUMERIC(19,8),
    created_by UUID REFERENCES app_user(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(client_id,period_start,period_end),
    CHECK (period_start <= period_end),
    CHECK (date_trunc('month',period_start)=date_trunc('month',period_end))
);
CREATE INDEX idx_report_period_client_date ON report_period(client_id,period_start DESC);
