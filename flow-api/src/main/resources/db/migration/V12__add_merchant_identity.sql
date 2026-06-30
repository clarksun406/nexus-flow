CREATE TABLE IF NOT EXISTS merchant_profiles (
    merchant_id    VARCHAR(64) PRIMARY KEY,
    merchant_code  VARCHAR(128) NOT NULL UNIQUE,
    display_name   VARCHAR(128) NOT NULL,
    status         VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    create_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS merchant_credentials (
    credential_id  VARCHAR(64) PRIMARY KEY,
    merchant_id    VARCHAR(64) NOT NULL REFERENCES merchant_profiles(merchant_id),
    key_hash       VARCHAR(128) NOT NULL UNIQUE,
    key_prefix     VARCHAR(32) NOT NULL,
    active         BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at     TIMESTAMP WITH TIME ZONE,
    create_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_merchant_credentials_merchant
    ON merchant_credentials(merchant_id);
CREATE INDEX IF NOT EXISTS idx_merchant_credentials_active
    ON merchant_credentials(active, expires_at);

CREATE TABLE IF NOT EXISTS merchant_webhook_configs (
    config_id    VARCHAR(64) PRIMARY KEY,
    merchant_id  VARCHAR(64) NOT NULL REFERENCES merchant_profiles(merchant_id),
    url          VARCHAR(512) NOT NULL,
    secret_hash  VARCHAR(128),
    active       BOOLEAN NOT NULL DEFAULT TRUE,
    create_time  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_merchant_webhook_configs_merchant
    ON merchant_webhook_configs(merchant_id, active);
