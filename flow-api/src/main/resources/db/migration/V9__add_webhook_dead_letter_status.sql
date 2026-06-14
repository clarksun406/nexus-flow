-- Track webhook dead-letter operator workflow state

ALTER TABLE webhook_dead_letters
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP WITH TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_webhook_dead_letters_status_created_at
    ON webhook_dead_letters(status, created_at DESC);
