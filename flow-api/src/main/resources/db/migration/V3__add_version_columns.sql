-- Add optimistic locking version columns for concurrent callback safety
ALTER TABLE payment_orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE payment_flows ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE refund_orders ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
