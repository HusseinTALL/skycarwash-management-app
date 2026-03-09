# CarWash Manager — Development Plan

> Based on: `CarWash_MVP_Spec.md`
> Start date: March 2026 | Target: ~10 weeks

---

## Project Structure

```
skycarwash/
├── backend/          # Spring Boot 3 (Java 17)
└── frontend/         # Vue 3 + Vite + TailwindCSS (PWA)
```

---

## Stack Summary

| Layer | Tech |
|---|---|
| Frontend | Vue 3, Vite, Pinia, Vue Router, TailwindCSS, PWA (Workbox) |
| Backend | Spring Boot 3, Java 17, Spring Security + JWT, Spring Data JPA, Spring WebSocket |
| Database | PostgreSQL (Supabase) |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Hosting | Backend → Railway/Render · Frontend → Vercel · DB → Supabase |

---

## Database Schema (6 tables)

```sql
User          (id, name, phone, role ENUM[EMPLOYEE,MANAGER,PARTNER], passwordHash, createdAt)
Client        (id, name, phone, type ENUM[CARTE,BOUCLIER,VIP], balance, expiresAt, createdAt)
Service       (id, name, price, category, productConsumption JSON, active)
Transaction   (id, serviceId, clientId, userId, amount, paymentMethod ENUM[CASH,ORANGE,MOOV,ABONNEMENT], cancelledAt, cancelReason, createdAt)
Product       (id, name, stock, alertThreshold, unit)
StockMovement (id, productId, quantity, type ENUM[IN,OUT], transactionId, createdAt)
```

---

## Sprint Plan

### S1–S2 — Foundation (Weeks 1–2)

**Goal:** Runnable skeleton, auth working, database seeded.

#### Backend
- [ ] Initialize Spring Boot project (Spring Initializr)
  - Dependencies: Web, Security, JPA, WebSocket, PostgreSQL Driver, Lombok, Validation
- [ ] Connect to Supabase PostgreSQL (application.properties)
- [ ] Define all 6 JPA entities with relationships
- [ ] Liquibase/Flyway migrations (schema + seed data)
- [ ] Spring Security + JWT
  - POST `/api/auth/login` → returns access token
  - Role-based access: `EMPLOYEE`, `MANAGER`, `PARTNER`
- [ ] CRUD endpoints for `Service` (manager only)
- [ ] CRUD endpoints for `Product` (manager only)
- [ ] Global exception handler (`@ControllerAdvice`)
- [ ] CORS config for local dev

#### Frontend
- [ ] Initialize Vue 3 + Vite project
- [ ] Install: Pinia, Vue Router, TailwindCSS, Axios
- [ ] Configure PWA plugin (vite-plugin-pwa + Workbox)
- [ ] Auth store (Pinia) — login, logout, JWT persistence (localStorage)
- [ ] Login page (phone + password)
- [ ] Route guards (redirect by role after login)
- [ ] Base layout components: `AppLayout`, `NavBar`, `BottomNav`

**Deliverable:** Login works, roles redirect correctly, services/products manageable via API.

---

### S3–S4 — Module Caisse (Weeks 3–4)

**Goal:** Employee can record a transaction in < 10 seconds.

#### Backend
- [ ] POST `/api/transactions` — create transaction
  - Validate service exists, payment method valid
  - If `ABONNEMENT` → check client balance, decrement
  - Trigger stock decrement (StockMovement)
  - Return receipt data
- [ ] POST `/api/transactions/{id}/cancel` — cancel within 2 min, reason required
- [ ] GET `/api/transactions/today` — list today's transactions (manager)
- [ ] WebSocket event on new transaction / cancellation

#### Frontend (Employee view — tablet optimized)
- [ ] `CaisseView` — full screen, large touch targets
  - Grid of service cards (icon + name + price)
  - Payment method selector (Cash / Orange Money / Moov / Abonnement)
  - If Abonnement → inline client search (name or phone)
  - Confirm button → POST transaction → show receipt overlay
- [ ] Receipt overlay (transaction number, service, amount, time)
- [ ] Cancel button (2-min window) → modal with reason input
- [ ] Offline queue: store transactions in IndexedDB when offline, sync on reconnect
- [ ] Service Worker — cache app shell for offline use

**Deliverable:** Employee records cash and abonnement transactions offline-capable.

---

### S5–S6 — Module Abonnements & Clients (Weeks 5–6)

**Goal:** Client lifecycle managed end-to-end.

#### Backend
- [ ] POST `/api/clients` — create client (name + phone, 30-sec rule)
- [ ] GET `/api/clients?q=search` — search by name or phone
- [ ] GET `/api/clients/{id}` — detail + transaction history
- [ ] PUT `/api/clients/{id}` — update (recharge balance, renew subscription)
- [ ] Scheduled job (`@Scheduled`) — run daily:
  - Alert if subscription expires in ≤ 5 days
  - Alert if carte balance = 1 passage
  - Send FCM push to MANAGER role users

#### Frontend (Manager view)
- [ ] `ClientsView` — list with search bar, status badges
- [ ] `ClientDetailView` — balance, expiry, last 10 transactions
- [ ] `ClientFormView` — create / edit client (name, phone, type, balance, expiry)
- [ ] Alert banner for expiring/low-balance clients
- [ ] Client picker component (reused in Caisse for abonnement payment)

**Deliverable:** Full client management, alerts working, caisse uses live client search.

---

### S7–S8 — Module Dashboard Manager (Weeks 7–8)

**Goal:** Manager sees live numbers from phone, anywhere.

#### Backend
- [ ] GET `/api/dashboard/daily?date=` — vehicles, CA by payment method, breakdown by service, vs last week
- [ ] GET `/api/dashboard/monthly?month=` — 30-day curve, top 3 services, active subscriptions, estimated profit
- [ ] WebSocket topic `/topic/dashboard` — push updates on each new transaction
- [ ] GET `/api/dashboard/alerts` — pending alerts list

#### Frontend (Manager — mobile optimized)
- [ ] `DashboardView`
  - Daily summary card (vehicles, CA total, payment split)
  - Service breakdown list
  - "vs last week" indicator (+ / - %)
  - Monthly revenue chart (Chart.js or lightweight alternative)
  - Top 3 services widget
  - Active vs expired subscriptions count
- [ ] Real-time update via WebSocket (Pinia store subscribes to socket)
- [ ] Alert feed (cancelled transactions, daily goal reached, low stock)
- [ ] FCM integration (request permission, register token, handle foreground messages)

**Deliverable:** Live dashboard, push notifications working on manager's phone.

---

### S9 — Module Stock (Week 9)

**Goal:** Never run out of product without warning.

#### Backend
- [ ] Stock auto-decrement already wired in S3–S4 (StockMovement on transaction)
- [ ] GET `/api/products` — list with current stock levels
- [ ] PUT `/api/products/{id}/restock` — add quantity (delivery received)
- [ ] GET `/api/products/alerts` — products below threshold
- [ ] FCM push when product crosses alert threshold

#### Frontend (Manager)
- [ ] `StockView` — product list, current level vs threshold, visual progress bar
- [ ] Restock button → modal (quantity input) → PUT restock
- [ ] Alert badge in nav when any product is low

**Deliverable:** Stock tracked automatically, manager restocks in 2 clicks.

---

### S10 — Polish, Tests & Deployment (Week 10)

#### Testing
- [ ] Backend: unit tests for transaction logic (balance decrement, cancellation window, stock decrement)
- [ ] Backend: integration tests for auth endpoints
- [ ] Frontend: manual test session with non-tech user (caisse flow validation)
- [ ] Offline sync test: create transactions offline, go online, verify sync

#### Production Deployment
- [ ] Backend → Railway (set env vars: DB_URL, JWT_SECRET, FCM_KEY)
- [ ] Frontend → Vercel (set VITE_API_URL)
- [ ] Database → Supabase (run migrations on prod)
- [ ] Configure PWA manifest (name, icons, theme_color)
- [ ] Test install on Android tablet (employee) and iPhone (manager)

#### Documentation
- [ ] `BACKLOG_V2.md` — capture all deferred features
- [ ] Employee quick-start guide (1 page, screenshots)

**Deliverable:** App live, employee trained, manager using dashboard.

---

## API Routes Summary

| Method | Route | Role | Description |
|---|---|---|---|
| POST | `/api/auth/login` | ALL | Login |
| GET | `/api/services` | ALL | List active services |
| POST | `/api/services` | MANAGER | Create service |
| PUT | `/api/services/{id}` | MANAGER | Update service |
| POST | `/api/transactions` | EMPLOYEE | Record transaction |
| POST | `/api/transactions/{id}/cancel` | EMPLOYEE | Cancel transaction |
| GET | `/api/transactions/today` | MANAGER | Today's transactions |
| GET | `/api/clients` | ALL | Search clients |
| POST | `/api/clients` | ALL | Create client |
| GET | `/api/clients/{id}` | ALL | Client detail |
| PUT | `/api/clients/{id}` | MANAGER | Update client |
| GET | `/api/dashboard/daily` | MANAGER/PARTNER | Daily stats |
| GET | `/api/dashboard/monthly` | MANAGER/PARTNER | Monthly stats |
| GET | `/api/products` | MANAGER | List products |
| PUT | `/api/products/{id}/restock` | MANAGER | Add stock |

---

## Frontend Routes

| Path | View | Role |
|---|---|---|
| `/login` | `LoginView` | ALL |
| `/caisse` | `CaisseView` | EMPLOYEE |
| `/dashboard` | `DashboardView` | MANAGER / PARTNER |
| `/clients` | `ClientsView` | MANAGER |
| `/clients/:id` | `ClientDetailView` | MANAGER |
| `/clients/new` | `ClientFormView` | MANAGER |
| `/stock` | `StockView` | MANAGER |
| `/settings` | `SettingsView` | MANAGER |

---

## Key Implementation Rules

1. **Offline-first caisse** — IndexedDB queue, sync on reconnect. Service Worker set up in S1, not as afterthought.
2. **< 10 seconds per transaction** — no more than 3 taps to validate.
3. **Mobile-first dashboard** — test on 390px width, large tap targets everywhere.
4. **JWT stored in localStorage** — refresh token strategy (MANAGER sessions last 7 days, EMPLOYEE 12 hours).
5. **Feature freeze** — anything not in spec goes to `BACKLOG_V2.md`.

---

*Plan version 1.0 — March 2026*
