-- Merchant portal login users (M5)
CREATE TABLE merchant_users (
    user_id       VARCHAR(36) PRIMARY KEY,
    email         VARCHAR(256) UNIQUE NOT NULL,
    password_hash VARCHAR(256),
    display_name  VARCHAR(128) NOT NULL,
    status        VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMPTZ,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- User-merchant membership with role (M5)
CREATE TABLE merchant_user_memberships (
    id          BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchant_profiles(merchant_id),
    user_id     VARCHAR(36) NOT NULL REFERENCES merchant_users(user_id),
    role_code   VARCHAR(64) NOT NULL,
    status      VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (merchant_id, user_id)
);

CREATE INDEX idx_merchant_user_memberships_user ON merchant_user_memberships(user_id);
CREATE INDEX idx_merchant_user_memberships_merchant ON merchant_user_memberships(merchant_id);
