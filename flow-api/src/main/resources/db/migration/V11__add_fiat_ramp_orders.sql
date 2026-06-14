CREATE TABLE IF NOT EXISTS fiat_ramp_orders (
    ramp_order_id      VARCHAR(64) PRIMARY KEY,
    merchant_id        VARCHAR(128) NOT NULL,
    merchant_order_no  VARCHAR(128) NOT NULL,
    payment_id         VARCHAR(64),
    direction          VARCHAR(20) NOT NULL,
    provider_id        VARCHAR(64) NOT NULL,
    provider_order_id  VARCHAR(128),
    quote_id           VARCHAR(128),
    fiat_amount        DECIMAL(36, 18) NOT NULL,
    fiat_currency      VARCHAR(16) NOT NULL,
    crypto_amount      DECIMAL(36, 18) NOT NULL,
    token              VARCHAR(32) NOT NULL,
    network            VARCHAR(32) NOT NULL,
    exchange_rate      DECIMAL(36, 18) NOT NULL,
    fee_amount_fiat    DECIMAL(36, 18),
    wallet_address     VARCHAR(256),
    checkout_url       VARCHAR(1024),
    fiat_transfer_id   VARCHAR(128),
    crypto_tx_hash     VARCHAR(128),
    notify_url         VARCHAR(512),
    return_url         VARCHAR(512),
    failure_reason     VARCHAR(512),
    status             VARCHAR(32) NOT NULL,
    expire_time        TIMESTAMP WITH TIME ZONE,
    complete_time      TIMESTAMP WITH TIME ZONE,
    create_time        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    version            BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_fiat_ramp_merchant_order
    ON fiat_ramp_orders(merchant_id, merchant_order_no);
CREATE UNIQUE INDEX IF NOT EXISTS idx_fiat_ramp_provider_order
    ON fiat_ramp_orders(provider_id, provider_order_id)
    WHERE provider_order_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fiat_ramp_payment ON fiat_ramp_orders(payment_id);
CREATE INDEX IF NOT EXISTS idx_fiat_ramp_status ON fiat_ramp_orders(status);
