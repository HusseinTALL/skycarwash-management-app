-- ============================================================
-- V1 – Initial Schema for SkyCarWash MVP
-- ============================================================

CREATE TABLE "user" (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    role        VARCHAR(20)  NOT NULL CHECK (role IN ('EMPLOYEE','MANAGER','PARTNER')),
    password_hash VARCHAR(255) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE client (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('CARTE','BOUCLIER','VIP')),
    balance     INTEGER      NOT NULL DEFAULT 0,   -- passage count for CARTE, custom for VIP
    expires_at  DATE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE service (
    id                    BIGSERIAL PRIMARY KEY,
    name                  VARCHAR(100) NOT NULL,
    price                 INTEGER      NOT NULL,   -- FCFA
    category              VARCHAR(50),
    product_consumption   JSONB,                   -- e.g. {"shampoo_l": 0.05, "wax_g": 10}
    active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE transaction (
    id              BIGSERIAL PRIMARY KEY,
    service_id      BIGINT       NOT NULL REFERENCES service(id),
    client_id       BIGINT       REFERENCES client(id),   -- nullable for non-subscription
    user_id         BIGINT       NOT NULL REFERENCES "user"(id),
    amount          INTEGER      NOT NULL,
    payment_method  VARCHAR(20)  NOT NULL CHECK (payment_method IN ('CASH','ORANGE','MOOV','ABONNEMENT')),
    cancelled_at    TIMESTAMP,
    cancel_reason   TEXT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE product (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(100) NOT NULL UNIQUE,
    stock            NUMERIC(10,3) NOT NULL DEFAULT 0,
    alert_threshold  NUMERIC(10,3) NOT NULL DEFAULT 0,
    unit             VARCHAR(20)  NOT NULL,   -- L, kg, unit…
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE stock_movement (
    id              BIGSERIAL PRIMARY KEY,
    product_id      BIGINT       NOT NULL REFERENCES product(id),
    quantity        NUMERIC(10,3) NOT NULL,
    type            VARCHAR(10)  NOT NULL CHECK (type IN ('IN','OUT')),
    transaction_id  BIGINT       REFERENCES transaction(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Indexes for common queries
CREATE INDEX idx_transaction_created_at  ON transaction(created_at);
CREATE INDEX idx_transaction_user_id     ON transaction(user_id);
CREATE INDEX idx_transaction_service_id  ON transaction(service_id);
CREATE INDEX idx_client_phone            ON client(phone);
CREATE INDEX idx_stock_movement_product  ON stock_movement(product_id);

-- Seed: default manager account (password: Admin1234!)
-- BCrypt hash generated for 'Admin1234!'
INSERT INTO "user" (name, phone, role, password_hash)
VALUES ('Manager', '+22600000000', 'MANAGER',
        '$2b$12$FTbJf16S1Jsq3413sBXCaeC.7fVaUL8//vHhcLFLaoYjTzBh/y.ry');
