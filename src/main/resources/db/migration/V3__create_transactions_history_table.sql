CREATE TABLE transaction_history (
                                     id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     account_id      UUID NOT NULL REFERENCES accounts(id),
                                     transfer_id     UUID REFERENCES transfers(id),
                                     type            VARCHAR(20) NOT NULL,
                                     amount          DECIMAL(19, 4) NOT NULL,
                                     balance_before  DECIMAL(19, 4) NOT NULL,
                                     balance_after   DECIMAL(19, 4) NOT NULL,
                                     description     VARCHAR(500),
                                     created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_txn_history_account ON transaction_history(account_id);
CREATE INDEX idx_txn_history_created ON transaction_history(account_id, created_at DESC);