CREATE TABLE accounts (
                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          account_number  VARCHAR(20) UNIQUE NOT NULL,
                          owner_name      VARCHAR(255) NOT NULL,
                          email           VARCHAR(255) UNIQUE NOT NULL,
                          balance         DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
                          currency        VARCHAR(3) NOT NULL DEFAULT 'THB',
                          status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                          created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
                          version         BIGINT NOT NULL DEFAULT 0
);

-- Index for frequent queries
CREATE INDEX idx_accounts_status ON accounts(status);
CREATE INDEX idx_accounts_email ON accounts(email);