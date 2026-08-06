CREATE TABLE facebook_page (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), meta_page_id VARCHAR(80) NOT NULL UNIQUE, page_name VARCHAR(200) NOT NULL,
 normalized_page_name VARCHAR(200) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE, verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED',
 last_verified_at TIMESTAMPTZ, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE instagram_account (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), meta_instagram_account_id VARCHAR(80) NOT NULL UNIQUE, username VARCHAR(100), display_name VARCHAR(200),
 active BOOLEAN NOT NULL DEFAULT TRUE, verification_status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED', last_verified_at TIMESTAMPTZ,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0);
CREATE TABLE meta_business_portfolio (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), meta_business_id VARCHAR(80) NOT NULL UNIQUE, name VARCHAR(200) NOT NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP);
CREATE TABLE meta_ad_account (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), meta_ad_account_id VARCHAR(80) NOT NULL UNIQUE, account_name VARCHAR(200) NOT NULL,
 account_currency CHAR(3) NOT NULL CHECK(account_currency ~ '^[A-Z]{3}$'), timezone_name VARCHAR(80) NOT NULL,
 exclusive_client_account BOOLEAN NOT NULL DEFAULT FALSE, active BOOLEAN NOT NULL DEFAULT TRUE,
 business_portfolio_id UUID REFERENCES meta_business_portfolio(id) ON DELETE RESTRICT,
 created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP, version BIGINT NOT NULL DEFAULT 0,
 CONSTRAINT ck_ad_account_id CHECK(meta_ad_account_id !~ '^act_'));

CREATE TABLE client_facebook_page (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
 facebook_page_id UUID NOT NULL REFERENCES facebook_page(id) ON DELETE RESTRICT, primary_page BOOLEAN NOT NULL DEFAULT FALSE,
 active_from DATE NOT NULL DEFAULT CURRENT_DATE, active_until DATE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_by UUID REFERENCES app_user(id) ON DELETE SET NULL, CHECK(active_until IS NULL OR active_until >= active_from));
CREATE UNIQUE INDEX uq_current_facebook_page ON client_facebook_page(facebook_page_id) WHERE active_until IS NULL;
CREATE UNIQUE INDEX uq_current_client_facebook_page ON client_facebook_page(client_id,facebook_page_id) WHERE active_until IS NULL;

CREATE TABLE client_instagram_account (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
 instagram_account_id UUID NOT NULL REFERENCES instagram_account(id) ON DELETE RESTRICT, primary_account BOOLEAN NOT NULL DEFAULT FALSE,
 active_from DATE NOT NULL DEFAULT CURRENT_DATE, active_until DATE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_by UUID REFERENCES app_user(id) ON DELETE SET NULL, CHECK(active_until IS NULL OR active_until >= active_from));
CREATE UNIQUE INDEX uq_current_instagram_account ON client_instagram_account(instagram_account_id) WHERE active_until IS NULL;

CREATE TABLE client_ad_account (
 id UUID PRIMARY KEY DEFAULT gen_random_uuid(), client_id UUID NOT NULL REFERENCES client(id) ON DELETE RESTRICT,
 ad_account_id UUID NOT NULL REFERENCES meta_ad_account(id) ON DELETE RESTRICT, exclusive BOOLEAN NOT NULL DEFAULT FALSE,
 active_from DATE NOT NULL DEFAULT CURRENT_DATE, active_until DATE, created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
 created_by UUID REFERENCES app_user(id) ON DELETE SET NULL, CHECK(active_until IS NULL OR active_until >= active_from));
CREATE UNIQUE INDEX uq_current_client_ad_account ON client_ad_account(client_id,ad_account_id) WHERE active_until IS NULL;
CREATE INDEX idx_current_ad_account ON client_ad_account(ad_account_id) WHERE active_until IS NULL;
