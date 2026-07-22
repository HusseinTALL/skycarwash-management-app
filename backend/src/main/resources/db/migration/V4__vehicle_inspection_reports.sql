-- ============================================================
-- V4 – Vehicle inspection reports (état des lieux véhicule)
--
-- Protège client & entreprise : photos avant/après, dommages
-- pré-existants, objets retrouvés, et un espace client consultable
-- sans compte (téléphone + code d'accès).
-- ============================================================

-- ── Rapport d'état d'un véhicule, rattaché à une commande ──────────────
CREATE TABLE inspection_report (
    id                 BIGSERIAL PRIMARY KEY,
    -- La commande (transaction) associée. SET NULL si la transaction est purgée.
    transaction_id     BIGINT       UNIQUE REFERENCES transaction(id) ON DELETE SET NULL,
    -- Client enregistré éventuel (abonné). NULL pour un client de passage.
    client_id          BIGINT       REFERENCES client(id) ON DELETE SET NULL,
    -- Coordonnées dénormalisées : indispensables pour l'accès au portail
    -- même quand le client n'est pas enregistré (paiement cash).
    customer_name      VARCHAR(100),
    customer_phone     VARCHAR(20),
    -- Véhicule inspecté
    vehicle_type       VARCHAR(20)  CHECK (vehicle_type IN ('MOTO','VOITURE','SUV','PICKUP')),
    plate              VARCHAR(20),
    vehicle_label      VARCHAR(100),
    remarks            TEXT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'VALIDATED'
                                    CHECK (status IN ('DRAFT','VALIDATED','COMPLETED')),
    created_by_user_id BIGINT       REFERENCES "user"(id) ON DELETE SET NULL,
    created_at         TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMP
);

CREATE INDEX idx_inspection_report_transaction ON inspection_report(transaction_id);
CREATE INDEX idx_inspection_report_phone       ON inspection_report(customer_phone);
CREATE INDEX idx_inspection_report_created_at  ON inspection_report(created_at);

-- ── Photos horodatées (avant / après lavage) ──────────────────────────
-- Les octets sont stockés en base (bytea) : zéro dépendance externe,
-- migrable vers un stockage objet plus tard.
CREATE TABLE inspection_photo (
    id            BIGSERIAL PRIMARY KEY,
    report_id     BIGINT       NOT NULL REFERENCES inspection_report(id) ON DELETE CASCADE,
    phase         VARCHAR(10)  NOT NULL CHECK (phase IN ('BEFORE','AFTER')),
    zone          VARCHAR(20)  NOT NULL CHECK (zone IN
                     ('AVANT','ARRIERE','GAUCHE','DROIT','INTERIEUR','COFFRE','DOMMAGE','AUTRE')),
    content_type  VARCHAR(50)  NOT NULL,
    image_data    BYTEA        NOT NULL,
    caption       VARCHAR(255),
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_photo_report ON inspection_photo(report_id);

-- ── Dommages déjà présents avant le lavage ────────────────────────────
CREATE TABLE inspection_damage (
    id            BIGSERIAL PRIMARY KEY,
    report_id     BIGINT       NOT NULL REFERENCES inspection_report(id) ON DELETE CASCADE,
    zone_label    VARCHAR(100),
    description   TEXT         NOT NULL,
    photo_id      BIGINT       REFERENCES inspection_photo(id) ON DELETE SET NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_damage_report ON inspection_damage(report_id);

-- ── Objets retrouvés dans le véhicule ─────────────────────────────────
CREATE TABLE inspection_found_item (
    id            BIGSERIAL PRIMARY KEY,
    report_id     BIGINT       NOT NULL REFERENCES inspection_report(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    description   TEXT,
    quantity      INTEGER      NOT NULL DEFAULT 1,
    remark        TEXT,
    photo_id      BIGINT       REFERENCES inspection_photo(id) ON DELETE SET NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inspection_found_item_report ON inspection_found_item(report_id);

-- ── Accès client au portail « Rapports de lavage » ────────────────────
-- Connexion sans compte : téléphone + code (par défaut les 4 derniers
-- chiffres du numéro). Le client peut changer son code à la 1re connexion.
CREATE TABLE portal_customer (
    id                BIGSERIAL PRIMARY KEY,
    phone             VARCHAR(20)  NOT NULL UNIQUE,
    access_code_hash  VARCHAR(255) NOT NULL,
    must_change_code  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP
);

CREATE INDEX idx_portal_customer_phone ON portal_customer(phone);
