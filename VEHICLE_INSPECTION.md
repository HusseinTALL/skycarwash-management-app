# 🚗 Rapport d'état du véhicule

Contrôle visuel du véhicule **avant** (et **après**) le lavage, pour protéger le
client et l'entreprise contre les litiges : dommages pré-existants, objets
oubliés, argent retrouvé, comparaison avant / après.

---

## Vue d'ensemble

| Acteur | Où | Ce qu'il fait |
|---|---|---|
| **Laveur / Caissier** | Tablette (staff) | Crée le rapport, prend les photos, note les dommages et objets retrouvés |
| **Client** | Son téléphone | Consulte ses rapports via le portail « Rapports de lavage » (téléphone + code) |

Le rapport est rattaché à une **commande** (`transaction`), mais peut aussi
être créé de façon autonome.

---

## Parcours staff

1. À la caisse, après avoir validé une transaction, le bouton
   **« 📋 Rapport d'état du véhicule »** ouvre le formulaire (pré-rempli avec la
   commande et le client éventuel).
2. L'employé renseigne : type de véhicule, immatriculation, nom/téléphone du
   client, **photos par zone** (avant, arrière, côtés, intérieur, coffre),
   **dommages existants** (avec photo), **objets retrouvés**, remarques, et fait
   **signer le client** (canvas tactile) pour valider l'état initial.
3. À l'enregistrement, l'état initial est stocké (statut `VALIDATED`) et un
   **accès portail** est créé pour le client (code = 4 derniers chiffres du tél.).
4. Après le lavage, depuis le détail du rapport, l'employé ajoute les
   **photos « après »** (statut → `COMPLETED`). La **signature** peut aussi être
   recueillie depuis le détail si elle n'a pas été prise à la caisse.
5. Le détail affiche un **QR code** vers le portail à montrer au client.

L'onglet **« Rapports »** liste les contrôles récents.

> Les photos sont **compressées côté client** (max 1280 px, JPEG) avant envoi —
> important sur connexion instable — puis stockées en base (`bytea`).

---

## Portail client — « Rapports de lavage »

Accessible sans compte à **`/rapports`** :

- **Connexion** : numéro de téléphone + code d'accès.
- **Code par défaut** : les 4 derniers chiffres du numéro
  (ex. `70 12 34 56` → `3456`).
- À la première connexion, le client est invité à **changer son code**.
- Il consulte l'historique de ses lavages, chaque rapport détaillé, et le
  **comparateur avant / après** (slider).

L'accès est **cloisonné par numéro** : un client ne voit que ses propres rapports
et photos (jeton JWT `type=PORTAL`, filtre `PortalAuthFilter`).

---

## API

### Staff (`ROLE_EMPLOYEE` / `MANAGER` / `PARTNER`)

| Méthode | Endpoint | Rôle |
|---|---|---|
| `POST` | `/api/inspections` | Créer un rapport (JSON) |
| `POST` | `/api/inspections/{id}/photos` | Ajouter une photo (multipart : `file`, `phase`, `zone`, `caption?`, `damageId?`, `foundItemId?`) |
| `POST` | `/api/inspections/{id}/signature` | Signature du client (multipart : `file`, `signerName?`) |
| `GET` | `/api/inspections` | Liste récente (ou `?transactionId=` pour le rapport d'une commande) |
| `GET` | `/api/inspections/{id}` | Détail complet (inclut `signed`, `signerName`, `signedAt`) |
| `GET` | `/api/inspections/photos/{photoId}` | Octets d'une photo |
| `GET` | `/api/inspections/{id}/signature` | Octets de la signature |

`POST` réservé à `EMPLOYEE`/`MANAGER` ; `GET` ouvert aussi à `PARTNER`.

### Portail client (jeton `PORTAL`)

| Méthode | Endpoint | Auth |
|---|---|---|
| `POST` | `/api/portal/login` | public — `{ phone, code }` → `{ token, phone, mustChangeCode }` |
| `POST` | `/api/portal/change-code` | portal — `{ newCode }` (4–6 chiffres) |
| `GET` | `/api/portal/reports` | portal — historique du numéro |
| `GET` | `/api/portal/reports/{id}` | portal — détail (cloisonné) |
| `GET` | `/api/portal/photos/{photoId}` | portal — octets (cloisonné) |
| `GET` | `/api/portal/reports/{id}/signature` | portal — signature (cloisonné) |

---

## Modèle de données (migration `V4`)

- `inspection_report` — véhicule, client/commande, statut, remarques
- `inspection_photo` — octets `bytea`, `phase` (BEFORE/AFTER), `zone`
- `inspection_damage` — dommages pré-existants (+ photo optionnelle)
- `inspection_found_item` — objets retrouvés (nom, quantité, description, remarque, photo)
- `inspection_signature` — signature du client (`bytea`), signataire, horodatage (migration `V5`)
- `portal_customer` — accès portail (téléphone + code hashé BCrypt)

---

## Configuration

| Variable | Défaut | Rôle |
|---|---|---|
| `AGENCY_NAME` | `SkyCarWash` | Nom de l'agence affiché sur les rapports |
| `JWT_EXPIRATION_PORTAL` | `2592000` (30 j) | Durée du jeton portail |
| `spring.servlet.multipart.max-file-size` | `8MB` | Taille max d'une photo |

---

## Non couvert (V2)

- Migration du stockage des photos/signatures vers un stockage objet (Supabase Storage / S3).
- Capture des photos en mode **hors-ligne** (le rapport nécessite la connexion).
