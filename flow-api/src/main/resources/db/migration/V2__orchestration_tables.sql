-- NexusFlow: Orchestration layer tables

CREATE TABLE IF NOT EXISTS payment_orders (
    payment_id         VARCHAR(64) PRIMARY KEY,
    merchant_id        VARCHAR(64) NOT NULL,
    merchant_order_no  VARCHAR(128) NOT NULL,
    amount_fiat        DECIMAL(36, 18) NOT NULL,
    currency_fiat      VARCHAR(8) NOT NULL,
    amount_crypto      DECIMAL(36, 18),
    currency_crypto    VARCHAR(16),
    network            VARCHAR(16),
    exchange_rate      DECIMAL(36, 18),
    channel_id         VARCHAR(32),
    channel_user_id    VARCHAR(128),
    channel_order_id   VARCHAR(128),
    status             VARCHAR(32) NOT NULL DEFAULT 'WAITING_PAYMENT',
    pay_address        VARCHAR(256),
    memo               VARCHAR(256),
    paid_amount_crypto DECIMAL(36, 18) DEFAULT 0,
    paid_amount_fiat   DECIMAL(36, 18) DEFAULT 0,
    tx_hash            VARCHAR(128),
    notify_url         VARCHAR(512),
    return_url         VARCHAR(512),
    extend_data        TEXT,
    expire_time        TIMESTAMP WITH TIME ZONE,
    pay_time           TIMESTAMP WITH TIME ZONE,
    confirm_time       TIMESTAMP WITH TIME ZONE,
    create_time        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_order_merchant_no ON payment_orders(merchant_id, merchant_order_no);
CREATE INDEX idx_order_channel ON payment_orders(channel_id, channel_order_id);
CREATE INDEX idx_order_status ON payment_orders(status);

CREATE TABLE IF NOT EXISTS payment_flows (
    flow_no          VARCHAR(64) PRIMARY KEY,
    payment_id       VARCHAR(64) NOT NULL,
    channel_id       VARCHAR(32),
    token            VARCHAR(16),
    network          VARCHAR(16),
    crypto_amount    DECIMAL(36, 18),
    fiat_amount      DECIMAL(36, 18),
    fiat_currency    VARCHAR(8),
    exchange_rate    DECIMAL(36, 18),
    pay_address      VARCHAR(256),
    memo             VARCHAR(256),
    payer_address    VARCHAR(256),
    status           VARCHAR(32) NOT NULL DEFAULT 'INIT',
    tx_hash          VARCHAR(128),
    paid_amount      DECIMAL(36, 18),
    create_time      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_flow_payment_id ON payment_flows(payment_id);
CREATE INDEX idx_flow_status ON payment_flows(payment_id, status);

CREATE TABLE IF NOT EXISTS refund_orders (
    refund_order_no     VARCHAR(64) PRIMARY KEY,
    payment_id          VARCHAR(64) NOT NULL,
    channel_refund_id   VARCHAR(128),
    refund_amount_fiat  DECIMAL(36, 18) NOT NULL,
    refund_amount_crypto DECIMAL(36, 18) NOT NULL,
    exchange_rate       DECIMAL(36, 18),
    token               VARCHAR(16),
    network             VARCHAR(16),
    to_address          VARCHAR(256),
    tx_hash             VARCHAR(128),
    notify_url          VARCHAR(512),
    status              VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    create_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    confirm_time        TIMESTAMP WITH TIME ZONE,
    update_time         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refund_payment_id ON refund_orders(payment_id);
CREATE INDEX idx_refund_channel ON refund_orders(channel_refund_id);