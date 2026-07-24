-- ============================================================
-- V7 – Advanced CRM: interaction journal & loyalty program
-- ============================================================

-- ── Interaction journal ──────────────────────────────────────
-- Every contact with a client (call, SMS, WhatsApp, visit,
-- complaint, feedback) with an optional follow-up date so the
-- team can plan "relances".
CREATE TABLE client_interaction (
    id             BIGSERIAL PRIMARY KEY,
    client_id      BIGINT       NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    user_id        BIGINT       REFERENCES "user"(id) ON DELETE SET NULL,
    type           VARCHAR(20)  NOT NULL CHECK (type IN
                       ('CALL','SMS','WHATSAPP','VISIT','COMPLAINT','FEEDBACK','OTHER')),
    notes          TEXT         NOT NULL,
    follow_up_at   DATE,
    follow_up_done BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interaction_client    ON client_interaction(client_id);
CREATE INDEX idx_interaction_follow_up ON client_interaction(follow_up_at)
    WHERE follow_up_at IS NOT NULL AND follow_up_done = FALSE;

-- ── Loyalty program ──────────────────────────────────────────
-- Running balance on the client + an auditable ledger of every
-- point movement (earn per wash, redeem, manual adjustment).
ALTER TABLE client ADD COLUMN loyalty_points INT NOT NULL DEFAULT 0;

CREATE TABLE loyalty_movement (
    id             BIGSERIAL PRIMARY KEY,
    client_id      BIGINT      NOT NULL REFERENCES client(id) ON DELETE CASCADE,
    transaction_id BIGINT      REFERENCES transaction(id) ON DELETE SET NULL,
    points         INT         NOT NULL,               -- signed delta
    type           VARCHAR(10) NOT NULL CHECK (type IN ('EARN','REDEEM','ADJUST')),
    note           VARCHAR(255),
    created_at     TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_loyalty_client ON loyalty_movement(client_id);

-- Backfill: one point per historical non-cancelled wash, so the
-- balance matches the visit count previously displayed as points.
UPDATE client c
SET loyalty_points = COALESCE(sub.cnt, 0)
FROM (
    SELECT t.client_id, COUNT(*) AS cnt
    FROM transaction t
    WHERE t.client_id IS NOT NULL AND t.cancelled_at IS NULL
    GROUP BY t.client_id
) sub
WHERE sub.client_id = c.id;

INSERT INTO loyalty_movement (client_id, points, type, note)
SELECT id, loyalty_points, 'ADJUST', 'Reprise historique (1 point par lavage)'
FROM client
WHERE loyalty_points > 0;
