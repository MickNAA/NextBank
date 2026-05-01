CREATE TABLE transfers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_account_id       UUID NOT NULL REFERENCES accounts(id),
    destination_account_id  UUID NOT NULL REFERENCES accounts(id),
    amount                  DECIMAL(19, 4) NOT NULL,
    currency                VARCHAR(3) NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    description             VARCHAR(500),
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transfers_source_account_id ON transfers(source_account_id);
CREATE INDEX idx_transfers_destination_account_id ON transfers(destination_account_id);
CREATE INDEX idx_transfers_status ON transfers(status);
CREATE INDEX idx_transfers_created_at ON transfers(created_at);
