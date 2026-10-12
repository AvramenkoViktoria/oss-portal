# OSS Portal

Portal of the student self-government bodies of NaUKMA: the Student Board (SK) and the Student Congregation (KS).
Members, meetings, petitions, protocols and per-member votings, with announcements sent to the body's messenger.

Spring Boot 4 · Spring Modulith · JTE (server-side rendering) · PostgreSQL · Flyway.

## Run

```bash
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

Open http://localhost:8080. The `demo` profile loads sample data; accounts (password `demo12345`):

| Account | Role |
| --- | --- |
| `admin@ukma.edu.ua` | administrator |
| `sk.1@ukma.edu.ua` | SK editor |
| `ks.1@ukma.edu.ua` | KS editor |
| `student1@ukma.edu.ua` | student |

Without the profile, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` to create the first administrator.

## Test

```bash
mvn verify
```

Integration tests need Docker (PostgreSQL via Testcontainers).

## Architecture

One Maven module, split into Spring Modulith application modules under `ua.edu.ukma.oss`:

| Module | Responsibility |
| --- | --- |
| `organization` | bodies, membership, terms of office |
| `auth` | accounts, roles, JWT, access policy |
| `meeting` | meetings and agenda |
| `petition` | petitions bound to meetings |
| `protocol` | protocol lifecycle (draft, approved, published), documents |
| `voting` | per-member votes, counting, chart views |
| `notification` | outbox, per-body messenger routing, stub WhatsApp/Telegram |
| `web` | server-rendered pages and cabinets |

Modules talk through their root-package API and domain events; `internal` packages are private.
`ModularityTests` verifies the boundaries and writes module diagrams to `target/spring-modulith-docs`.

Notes:

- WhatsApp and Telegram are stubs that record messages in memory; an administrator can simulate an outage.
- Protocol documents are stored on the local filesystem behind the `FileStorage` interface (`STORAGE_PATH`).

## CI/CD

- `.github/workflows/ci.yml` — on pull requests and pushes to `main`: `mvn verify` (unit, module and integration tests), then a Docker image build check.
- `.github/workflows/cd.yml` — after CI succeeds on `main` (or manually): builds the image and pushes it to `ghcr.io/<owner>/oss-portal` as `latest` and `sha-<short>`.
- `deploy/compose.yaml` — runs the published image with PostgreSQL; required variables are `IMAGE` and `JWT_SECRET`.
