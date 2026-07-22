-- ============================================================
-- V5 – Signature électronique du client
--
-- Le client signe sur tablette/smartphone pour valider l'état
-- initial du véhicule avant le lavage. Une signature par rapport.
-- ============================================================

CREATE TABLE inspection_signature (
    id            BIGSERIAL PRIMARY KEY,
    report_id     BIGINT       NOT NULL UNIQUE REFERENCES inspection_report(id) ON DELETE CASCADE,
    signer_name   VARCHAR(100),
    content_type  VARCHAR(50)  NOT NULL,
    image_data    BYTEA        NOT NULL,
    signed_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_signature_report ON inspection_signature(report_id);
