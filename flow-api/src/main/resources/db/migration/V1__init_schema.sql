-- NexusFlow: Initial schema
-- Phase 1 MVP tables

-- Crypto payments
CREATE TABLE IF NOT EXISTS crypto_payments (
    id              VARCHAR(64) PRIMARY KEY,
    order_id        VARCHAR(128) NOT NULL UNIQUE,
    currency        VARCHAR(32) NOT NULL,
    expected_amount DECIMAL(36, 18) NOT NULL,
    received_amount DECIMAL(36, 18),
    status          VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    receiving_address VARCHAR(128) NOT NULL,
    tx_hash         VARCHAR(128),
    confirmations   INT DEFAULT 0,
    required_confirmations INT DEFAULT 12,
    callback_url    VARCHAR(512),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    detected_at     TIMESTAMP WITH TIME ZONE,
    confirmed_at    TIMESTAMP WITH TIME ZONE,
    expired_at      TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payments_order_id ON crypto_payments(order_id);
CREATE INDEX idx_payments_status ON crypto_payments(status);
CREATE INDEX idx_payments_tx_hash ON crypto_payments(tx_hash);
CREATE INDEX idx_payments_address ON crypto_payments(receiving_address);

-- Wallets
CREATE TABLE IF NOT EXISTS wallets (
    id                   VARCHAR(64) PRIMARY KEY,
    name                 VARCHAR(128) NOT NULL,
    chain                VARCHAR(20) NOT NULL,
    type                 VARCHAR(10) NOT NULL DEFAULT 'HOT',
    address              VARCHAR(256) NOT NULL UNIQUE,
    encrypted_private_key TEXT NOT NULL,
    kms_key_id           VARCHAR(256),
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallets_chain_active ON wallets(chain, active);
CREATE INDEX idx_wallets_address ON wallets(address);

-- Idempotency keys (for deduplication)
CREATE TABLE IF NOT EXISTS idempotency_keys (
    key         VARCHAR(256) PRIMARY KEY,
    response    JSONB,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_idempotency_expires ON idempotency_keys(expires_at);