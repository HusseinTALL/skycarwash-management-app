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

// ── WS dashboard events ────────────────────────────────────────────── //
export const WS_EVENTS = {
  TRANSACTION_CREATED:   'transaction.created',
  TRANSACTION_CANCELLED: 'transaction.cancelled'
}
