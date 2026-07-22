// ── Payment methods ───────────────────────────────────────────────── //
export const PAYMENT_METHODS = {
  CASH:       'CASH',
  ORANGE:     'ORANGE',
  MOOV:       'MOOV',
  ABONNEMENT: 'ABONNEMENT'
}

export const PAYMENT_LABELS = {
  CASH:       'Espèces',
  ORANGE:     'Orange Money',
  MOOV:       'Moov Money',
  ABONNEMENT: 'Abonnement'
}

// ── Client subscription types ──────────────────────────────────────── //
export const CLIENT_TYPES = {
  CARTE:    'CARTE',
  BOUCLIER: 'BOUCLIER',
  VIP:      'VIP'
}

export const CLIENT_TYPE_LABELS = {
  CARTE:    'Carte passages',
  BOUCLIER: 'Bouclier',
  VIP:      'VIP'
}

export const CLIENT_TYPE_HINTS = {
  CARTE:    'Prépayé — décrémenté à chaque lavage',
  BOUCLIER: 'Mensuel — 2 lavages + protections inclus',
  VIP:      'Tarif négocié — flotte ou entreprise'
}

// ── User roles ─────────────────────────────────────────────────────── //
export const ROLES = {
  EMPLOYEE: 'EMPLOYEE',
  PARTNER:  'PARTNER',
  MANAGER:  'MANAGER'
}

export const ROLE_LABELS = {
  EMPLOYEE: 'Employé',
  PARTNER:  'Partenaire',
  MANAGER:  'Manager'
}

// ── Vehicle types (match caisse pricing grid) ──────────────────────── //
export const VEHICLE_TYPES = {
  MOTO:    'MOTO',
  VOITURE: 'VOITURE',
  SUV:     'SUV',
  PICKUP:  'PICKUP'
}

export const VEHICLE_TYPE_LABELS = {
  MOTO:    'Moto',
  VOITURE: 'Voiture',
  SUV:     '4x4 / SUV',
  PICKUP:  'Pick-up'
}

export const VEHICLE_TYPE_ICONS = {
  MOTO:    '🏍️',
  VOITURE: '🚗',
  SUV:     '🚙',
  PICKUP:  '🛻'
}

// ── Stock units ────────────────────────────────────────────────────── //
export const STOCK_UNITS = ['L', 'kg', 'g', 'unité']

// ── Validation ────────────────────────────────────────────────────── //
/** Accepts optional leading + then at least 8 digits (no spaces/dashes) */
export const PHONE_REGEX = /^\+?[0-9]{8,}$/

// ── Vehicle inspection reports ─────────────────────────────────────── //
export const PHOTO_ZONES = {
  AVANT:     'AVANT',
  ARRIERE:   'ARRIERE',
  GAUCHE:    'GAUCHE',
  DROIT:     'DROIT',
  INTERIEUR: 'INTERIEUR',
  COFFRE:    'COFFRE',
  DOMMAGE:   'DOMMAGE',
  AUTRE:     'AUTRE'
}

export const PHOTO_ZONE_LABELS = {
  AVANT:     'Avant',
  ARRIERE:   'Arrière',
  GAUCHE:    'Côté gauche',
  DROIT:     'Côté droit',
  INTERIEUR: 'Intérieur',
  COFFRE:    'Coffre',
  DOMMAGE:   'Dommage',
  AUTRE:     'Autre'
}

/** Ordered zones proposed by default in the capture UI. */
export const DEFAULT_PHOTO_ZONES = ['AVANT', 'ARRIERE', 'GAUCHE', 'DROIT', 'INTERIEUR', 'COFFRE']

export const INSPECTION_STATUS_LABELS = {
  DRAFT:     'Brouillon',
  VALIDATED: 'État initial enregistré',
  COMPLETED: 'Lavage terminé'
}

/** Common found-item presets for quick selection. */
export const FOUND_ITEM_PRESETS = [
  'Argent liquide', 'Téléphone', 'Sac', 'Documents', 'Clés',
  'Lunettes', 'Chargeur', 'Ordinateur', 'Bijoux', 'Autres'
]

// ── WS dashboard events ────────────────────────────────────────────── //
export const WS_EVENTS = {
  TRANSACTION_CREATED:   'transaction.created',
  TRANSACTION_CANCELLED: 'transaction.cancelled'
}
