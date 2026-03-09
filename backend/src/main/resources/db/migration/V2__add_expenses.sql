-- ============================================================
-- V2 – Expense tracking table
-- ============================================================

CREATE TABLE expense (
    id           BIGSERIAL PRIMARY KEY,
    category     VARCHAR(20)  NOT NULL CHECK (category IN ('PRODUIT', 'SALAIRE', 'AUTRE')),
    amount       INTEGER      NOT NULL CHECK (amount > 0),
    description  TEXT,
    expense_date DATE         NOT NULL DEFAULT CURRENT_DATE,
    created_by   BIGINT       NOT NULL REFERENCES "user"(id),
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_expense_date ON expense(expense_date);
