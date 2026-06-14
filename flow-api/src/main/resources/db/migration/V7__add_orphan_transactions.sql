-- Orphan chain transactions detected on managed addresses without a matching pending payment

CREATE TABLE IF NOT EXISTS orphan_transactions (
    id                  VARCHAR(64) PRIMARY KEY,
    chain               VARCHAR(20) NOT NULL,
    tx_hash             VARCHAR(128) NOT NULL,
    to_address          VARCHAR(256) NOT NULL,
    amount              VARCHAR(80) NOT NULL,
    currency            VARCHAR(32) NOT NULL,
    block_number        BIGINT,
    status              VARCHAR(20) NOT NULL DEFAULT 'UNMATCHED',
    first_seen_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_seen_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    seen_count          INT NOT NULL DEFAULT 1,
    resolved_payment_id VARCHAR(64),
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_orphan_transaction_chain_tx UNIQUE (chain, tx_hash)
);

CREATE INDEX IF NOT EXISTS idx_orphan_transactions_status ON orphan_transactions(status);
CREATE INDEX IF NOT EXISTS idx_orphan_transactions_address ON orphan_transactions(to_address);
