-- P1: HD wallet backups, address pool, scanner cursors, and retry/reorg metadata

CREATE TABLE IF NOT EXISTS mnemonic_backups (
    id                  VARCHAR(64) PRIMARY KEY,
    wallet_id           VARCHAR(64) NOT NULL UNIQUE,
    chain               VARCHAR(20) NOT NULL,
    encrypted_mnemonic  TEXT NOT NULL,
    derivation_path     VARCHAR(64) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version             BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_mnemonic_wallet ON mnemonic_backups(wallet_id);

CREATE TABLE IF NOT EXISTS address_pool (
    id                    VARCHAR(64) PRIMARY KEY,
    chain                 VARCHAR(20) NOT NULL,
    address               VARCHAR(256) NOT NULL UNIQUE,
    encrypted_private_key TEXT NOT NULL,
    derivation_path       VARCHAR(64),
    derivation_index      INT,
    status                VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    assigned_payment_id   VARCHAR(64),
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    assigned_at           TIMESTAMP WITH TIME ZONE,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version               BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_address_pool_chain_status ON address_pool(chain, status);
CREATE INDEX IF NOT EXISTS idx_address_pool_payment ON address_pool(assigned_payment_id);

CREATE TABLE IF NOT EXISTS chain_scan_cursors (
    chain                   VARCHAR(20) PRIMARY KEY,
    last_scanned_block      BIGINT NOT NULL DEFAULT 0,
    last_scanned_block_hash VARCHAR(128),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version                 BIGINT NOT NULL DEFAULT 0
);

ALTER TABLE crypto_payments ADD COLUMN IF NOT EXISTS detected_block_number BIGINT;
ALTER TABLE crypto_payments ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE crypto_payments ADD COLUMN IF NOT EXISTS next_retry_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE crypto_payments ADD COLUMN IF NOT EXISTS last_failure_reason VARCHAR(512);

CREATE INDEX IF NOT EXISTS idx_payments_detected_block ON crypto_payments(detected_block_number);
CREATE INDEX IF NOT EXISTS idx_payments_next_retry ON crypto_payments(next_retry_at);
