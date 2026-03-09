# SkyCarWash — Backlog V2

> Features deferred from MVP scope. Prioritize for the next development cycle.

---

## High Priority

### User Management UI
- Create / edit / deactivate user accounts from SettingsView
- Role assignment (EMPLOYEE / MANAGER / PARTNER)
- Password reset flow (SMS OTP or email link)

### Refresh Token Strategy
- Issue short-lived access tokens (15 min) + long-lived refresh tokens
- Silent token refresh via interceptor — avoid forced logouts
- Revoke refresh tokens on logout

### Advanced Cancellation
- MANAGER can cancel any transaction (not just within 2-minute window)
- Cancellation reason dropdown with freetext fallback
- Audit log viewable from dashboard

---

## Medium Priority

### Subscription Renewal Automation
- Auto-renew BOUCLIER/VIP subscriptions on expiry if card on file
- Prorated renewal billing

### Expense Tracking
- Record purchases / operational expenses
- Factor expenses into estimated profit calculation on dashboard

### Multi-Location Support
- Add `location` field to Transaction / User
- Dashboard filtered per location or aggregated across all

### Daily Cash Closing Report
- End-of-day summary: total cash, Orange Money, Moov totals
- Printable / shareable PDF receipt

### Client Loyalty Program
- Accumulate points per passage (CARTE clients)
- Redeem points for free washes

---

## Low Priority / Nice to Have

### Dark / Light Theme Toggle
- System preference detection
- Manual override stored in localStorage

### Service Bundles / Packages
- Group multiple services at a discounted price
- Track bundle usage per client

### Employee Performance Dashboard
- Transactions per employee per day/week
- Leaderboard (opt-in)

### SMS Notifications
- Alert clients when subscription is about to expire (via Orange/Moov API)
- Confirmation SMS after subscription recharge

### Barcode / QR Client Cards
- Generate QR code for each client subscription card
- Scan at caisse instead of manual search

### Offline Dashboard (cached)
- Service worker caches last-known dashboard data
- Visible indicator when viewing stale data

---

## Technical Debt

- Replace `@JdbcTypeCode(SqlTypes.JSON)` with a proper embedded JSON column type for portability
- Add Flyway repeatable migrations for seed data management
- Move Firebase config to Spring `@ConfigurationProperties` bean
- Add structured logging (JSON) for production observability
- API rate limiting on auth endpoint (prevent brute-force)

---

*Backlog V2 — March 2026 · Feature freeze for MVP maintained*
