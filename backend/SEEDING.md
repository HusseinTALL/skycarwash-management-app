# Seeding demo data

`MockDataSeeder` fills the database with a large, realistic demo dataset:
staff accounts, a service catalogue, products, ~200 clients (with tags, notes,
vehicles) and several **months of transactions and expenses** — enough to make
the dashboards and the CRM (Client 360, history, segmentation) look alive.

It **only runs when the `seed` profile is active**, so a normal or production
startup never touches it. It is also guarded: if the database already holds
more than 20 clients it skips, unless you pass `seed.force=true`.

## Run it

### Local, against the docker-compose Postgres

```bash
# 1. start just the database
docker compose up -d postgres

# 2. run the backend once with the seed profile
cd backend
SPRING_PROFILES_ACTIVE=seed mvn spring-boot:run
```

The seeder logs `[seed] Done. users=… clients=… transactions=…` and the app
keeps running. Stop it (Ctrl-C) once seeding is done, then start normally.

### Against a hosted database (Supabase / Railway / Render)

Point the datasource at your database and add `seed` to the active profiles for
a single run:

```bash
cd backend
DATABASE_URL="jdbc:postgresql://<host>:5432/<db>" \
DATABASE_USERNAME="<user>" \
DATABASE_PASSWORD="<pass>" \
SPRING_PROFILES_ACTIVE=prod,seed \
mvn spring-boot:run
```

Then redeploy / restart with your usual profile (without `seed`).

## Tuning the volume

Override any of these (Maven `-Dseed.clients=…` or env `SEED_CLIENTS=…`):

| Property            | Default | Meaning                              |
|---------------------|---------|--------------------------------------|
| `seed.clients`      | 200     | number of clients                    |
| `seed.transactions` | 5000    | number of wash transactions          |
| `seed.expenses`     | 150     | number of expenses                   |
| `seed.months`       | 6       | history spread (months back)         |
| `seed.force`        | false   | seed even if data already exists      |

Example — a bigger dataset:

```bash
SPRING_PROFILES_ACTIVE=seed mvn spring-boot:run \
  -Dseed.clients=500 -Dseed.transactions=20000 -Dseed.months=12
```

## Demo logins

All seeded staff share the password **`Demo1234!`**:

| Role      | Phone           |
|-----------|-----------------|
| Partner   | +22670000001    |
| Manager   | +22670000002    |
| Employee  | +22670000003    |

(The original Flyway-seeded manager `+22600000000 / Admin1234!` still works too.)
