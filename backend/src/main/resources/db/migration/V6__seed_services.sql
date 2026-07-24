-- ============================================================
-- V6 – Seed service catalogue
-- Source: Sky CarWash — Grille tarifaire, Ouagadougou 2026 (FCFA)
--
-- One row per (prestation x type de véhicule). The vehicle type is stored
-- in `category` so it shows as the badge on each caisse service card, and is
-- also included in the name so it appears clearly on receipts.
--
-- Seeds only when the service table is empty, so it never conflicts with
-- services added manually via the app.
-- ============================================================

INSERT INTO service (name, price, category)
SELECT s.name, s.price, s.category
FROM (VALUES
    -- ── Voiture ─────────────────────────────────────────────
    ('Lavage complet - Voiture',        2000,  'Voiture'),
    ('Lavage extérieur - Voiture',      1000,  'Voiture'),
    ('Lavage intérieur - Voiture',      1000,  'Voiture'),
    ('Aspiration - Voiture',            1000,  'Voiture'),
    ('Polissage / Brillance - Voiture', 10000, 'Voiture'),
    ('Protection / Cire - Voiture',     7000,  'Voiture'),

    -- ── 4x4 / SUV ───────────────────────────────────────────
    ('Lavage complet - 4x4/SUV',        3000,  '4x4 / SUV'),
    ('Lavage extérieur - 4x4/SUV',      1500,  '4x4 / SUV'),
    ('Lavage intérieur - 4x4/SUV',      1500,  '4x4 / SUV'),
    ('Aspiration - 4x4/SUV',            1000,  '4x4 / SUV'),
    ('Polissage / Brillance - 4x4/SUV', 13000, '4x4 / SUV'),
    ('Protection / Cire - 4x4/SUV',     9000,  '4x4 / SUV'),

    -- ── Pick-up ─────────────────────────────────────────────
    ('Lavage complet - Pick-up',        4000,  'Pick-up'),
    ('Lavage extérieur - Pick-up',      2000,  'Pick-up'),
    ('Lavage intérieur - Pick-up',      2000,  'Pick-up'),
    ('Aspiration - Pick-up',            1500,  'Pick-up'),
    ('Polissage / Brillance - Pick-up', 16000, 'Pick-up'),
    ('Protection / Cire - Pick-up',     11000, 'Pick-up'),

    -- ── Moto ────────────────────────────────────────────────
    ('Lavage complet - Moto',           500,   'Moto'),
    ('Polissage / Brillance - Moto',    2000,  'Moto'),
    ('Protection / Cire - Moto',        1500,  'Moto')
) AS s(name, price, category)
WHERE NOT EXISTS (SELECT 1 FROM service);
