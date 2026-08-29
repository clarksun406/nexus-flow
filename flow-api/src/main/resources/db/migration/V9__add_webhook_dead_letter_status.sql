-- Track webhook dead-letter operator workflow state
-- Note: two separate ALTER statements instead of one comma-separated multi-column
-- ALTER, so the script also applies on H2 (PostgreSQL compatibility mode) used
-- by the local 'h2' profile. Guarded by H2MigrationCompatibilityTest.

ALTER TABLE webhook_dead_letters
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE webhook_dead_letters
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_webhook_dead_letters_status_created_at
    ON webhook_dead_letters(status, created_at DESC);
