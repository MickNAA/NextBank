CREATE TABLE transactions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id      UUID NOT NULL REFERENCES accounts(id),
    type            VARCHAR(20) NOT NULL,
    amount          DECIMAL(19, 4) NOT NULL,
    balance_before  DECIMAL(19, 4) NOT NULL,
    balance_after   DECIMAL(19, 4) NOT NULL,
    currency        VARCHAR(3) NOT NULL,
    reference_id    UUID,
    description     VARCHAR(500),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_type ON transactions(type);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
CREATE INDEX idx_transactions_reference_id ON transactions(reference_id);
