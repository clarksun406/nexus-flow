-- Failed outbound merchant and execution webhook deliveries

CREATE TABLE IF NOT EXISTS webhook_dead_letters (
    id             VARCHAR(64) PRIMARY KEY,
    delivery_type  VARCHAR(40) NOT NULL,
    target_url     VARCHAR(2048) NOT NULL,
    payload        TEXT NOT NULL,
    event_id       VARCHAR(128),
    event_type     VARCHAR(120),
    payment_id     VARCHAR(64),
    order_id       VARCHAR(128),
    failure_reason VARCHAR(1024) NOT NULL,
    attempts       INT NOT NULL DEFAULT 0,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_dead_letters_created_at ON webhook_dead_letters(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_webhook_dead_letters_event_type ON webhook_dead_letters(event_type);
CREATE INDEX IF NOT EXISTS idx_webhook_dead_letters_payment_id ON webhook_dead_letters(payment_id);
