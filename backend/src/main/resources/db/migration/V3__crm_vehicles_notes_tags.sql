-- ============================================================
-- V3 – CRM enhancements: vehicles, client notes & tags
-- ============================================================

-- Free-text CRM notes and comma-separated tags on the client profile
ALTER TABLE client ADD COLUMN notes TEXT;
ALTER TABLE client ADD COLUMN tags  VARCHAR(255);

-- A client may own several vehicles (moto, voiture, 4x4/SUV, pick-up)
CREATE TABLE vehicle (
    id          BIGSERIAL PRIMARY KEY,
    client_id   BIGINT       NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    plate       VARCHAR(20),
    label       VARCHAR(100),
    type        VARCHAR(20)  NOT NULL CHECK (type IN ('MOTO','VOITURE','SUV','PICKUP')),
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_vehicle_client   ON vehicle(client_id);
CREATE INDEX idx_transaction_client ON transaction(client_id);
