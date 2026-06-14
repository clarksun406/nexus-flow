-- Request fingerprint for full createPayment request/response idempotency

ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_idempotency_request_hash ON idempotency_keys(request_hash);
