CREATE TABLE transfers (
                           id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           reference_id        VARCHAR(64) UNIQUE NOT NULL,
                           source_account_id   UUID NOT NULL REFERENCES accounts(id),
                           target_account_id   UUID NOT NULL REFERENCES accounts(id),
                           amount              DECIMAL(19, 4) NOT NULL,
                           currency            VARCHAR(3) NOT NULL,
                           status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                           description         VARCHAR(500),
                           created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                           completed_at        TIMESTAMP WITH TIME ZONE,

                           CONSTRAINT chk_different_accounts CHECK (source_account_id != target_account_id),
    CONSTRAINT chk_positive_amount CHECK (amount > 0)
);

CREATE INDEX idx_transfers_source ON transfers(source_account_id);
CREATE INDEX idx_transfers_target ON transfers(target_account_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_reference ON transfers(reference_id);