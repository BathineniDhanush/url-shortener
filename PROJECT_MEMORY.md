# URL Shortener Project Memory

This document is the durable handoff context for continuing this project in another Codex task, IDE session, or with another engineer. Treat the repository as the source of truth when this document and the code disagree, and update this file after each substantial slice.

## Resume instruction

Use this prompt when opening a new task:

> Read `E:\Projects\url-shortener\PROJECT_MEMORY.md` and `README.md`, inspect the current working tree, and continue from the next uncompleted slice. Preserve existing work, verify assumptions against the code, and run `mvnw.cmd -B clean verify` before declaring the slice complete.

## Product objective

Build a reviewable, production-minded URL shortener prototype in two to three days using engineer-led, AI-assisted execution. The broader assignment expects:

- Requirement interpretation and ambiguity management.
- Explicit task decomposition, dependencies, and sequencing.
- A working end-to-end prototype with core APIs, analytics, and reliability features.
- Greenfield, brownfield, and ambiguous-requirement scenarios showing decomposition, execution, and validation.
- Production-quality code, API/schema definitions, tests, documentation, risk analysis, and defensible trade-offs.
- AI traceability and human ownership of correctness, security, maintainability, and production readiness.

Spring Boot and Maven are mandatory for API development.

## Repository and working-tree state

- Canonical repository: `E:\Projects\url-shortener`
- Previous location: `C:\Users\dhanu\OneDrive\Documents\ChatGPT\url-shortner` (do not use)
- Package root: `com.example.shortener`
- Baseline commit: `dc4c900 initial commit`; analytics/caching and CI/CD baseline commit: `3ea422d`. The working tree contains follow-up reliability fixes and tests; check `git status` and preserve all user changes.
- Existing files belong to the user; do not discard or overwrite unrelated changes.

## Technology baseline

- Java 17
- Maven Wrapper
- Spring Boot 3.5.16
- Spring MVC, Bean Validation, Actuator
- Spring JDBC with PostgreSQL 17
- Flyway schema migrations
- Spring Data Redis for redirect caching and click-event streaming
- JUnit 5, Spring Boot Test, and Testcontainers
- Docker multi-stage image and Docker Compose

## Implemented architecture

The code uses a lightweight layered/package-by-feature structure:

```text
HTTP controller
    -> application service/use case
        -> domain model and policy
            -> repository port
                -> JDBC adapter -> PostgreSQL
```

Important areas:

- `link/api`: create-link HTTP contract.
- `link/application`: link creation orchestration.
- `link/domain`: `Link`, status, repository interface, URL policy, and short-code generator.
- `link/infrastructure`: PostgreSQL JDBC repository.
- `redirect/api` and `redirect/application`: redirect resolution.
- `shared/api`: centralized RFC 9457 problem-detail responses.
- `configuration`: clock and API/worker runtime configuration.
- `worker`: Redis Stream consumption, bounded in-process retries, idempotent persistence, dead-lettering, and graceful shutdown.
- `observability`: centralized low-cardinality cache, publication, processing, retry, and dead-letter counters.

One deployable artifact supports two runtime roles through Spring profiles:

- `api`: serves HTTP APIs and redirects.
- `worker`: consumes Redis Stream click events and idempotently stores them in PostgreSQL.

## Completed vertical slice: create and resolve links

### Create API

`POST /api/v1/links`

Request:

```json
{
  "destinationUrl": "https://example.com/docs",
  "customAlias": "docs-2026",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

- `destinationUrl` is required and limited to 2048 characters.
- Only absolute `http` and `https` URLs with a host are accepted.
- URL credentials/user-info are rejected.
- `customAlias` is optional, case-sensitive, and must match `[A-Za-z0-9_-]{4,32}`.
- `api`, `actuator`, and `health` are reserved aliases, compared case-insensitively.
- `expiresAt` is optional and must be in the future.
- Without a custom alias, a cryptographically secure 10-character Base62 code is generated.
- Generated-code collisions are retried up to five times; database uniqueness is the concurrency authority.
- Success returns `201 Created`, a response body containing the link, and a `Location` header derived from `app.links.public-base-url`/`PUBLIC_BASE_URL`.

### Redirect API

`GET /{code}`

- Accepts codes matching `[A-Za-z0-9_-]{4,32}`.
- Returns `302 Found` with the destination in `Location`.
- Adds `Cache-Control: no-store` so expiration/disablement changes are not hidden by browser caching.
- Adds `Referrer-Policy: no-referrer`.
- Missing links return `404`; expired or disabled links return `410`.

### Error contract

Errors are returned as `application/problem+json` using Spring `ProblemDetail`:

- `400`: malformed fields, invalid destination, or invalid expiration.
- `404`: unknown short code.
- `409`: custom alias conflict or reserved alias.
- `410`: expired or disabled link.
- `503`: generated code could not be allocated after collision retries.

The current problem type base (`https://url-shortener.example/problems/...`) is a placeholder and must be replaced before a real production release.

## Persistence

Flyway migration `V1__create_links.sql` creates `links` with:

- UUID primary key.
- Unique short code.
- Destination URL.
- `ACTIVE`/`DISABLED` status constraint.
- Optional `TIMESTAMPTZ` expiration.
- Created and updated timestamps.
- Partial index for active expiring links.

At the JDBC boundary, Java `Instant` values are converted to UTC `OffsetDateTime` because the PostgreSQL driver expects `OffsetDateTime` for `TIMESTAMP_WITH_TIMEZONE`. Preserve this conversion when extending the repository.

## Runtime configuration

Defaults are defined in `src/main/resources/application.yml`:

- PostgreSQL: `localhost:5432/url_shortener`
- Database credentials: `url_shortener` / `url_shortener`
- Redis: `localhost:6379`
- Public short-link base URL: `http://localhost:8080`
- Graceful shutdown enabled.
- Actuator exposes health, info, and metrics; liveness/readiness probes are enabled.
- The API serves the OpenAPI 3.1 contract at `/openapi.yaml`.

Environment overrides include `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE`, `REDIS_HOST`, `REDIS_PORT`, and `PUBLIC_BASE_URL`.

Docker Compose starts PostgreSQL, Redis, the API, and the worker. Persistent database and Redis volumes are intentionally retained by a normal `docker compose down`.

## Verification evidence

Last successful verification:

```powershell
cd E:\Projects\url-shortener
.\mvnw.cmd -B clean verify
```

Result on 2026-09-03: 46 tests passed, 0 failures, 0 errors, and the executable JAR was produced at `target\url-shortener-0.0.1-SNAPSHOT.jar`. The JaCoCo 90.1% aggregate line-coverage gate passed.

The Azure Functions profile also packages successfully with `mvnw.cmd -Pazure-functions -DskipTests package`.
An end-to-end Core Tools smoke test against an isolated PostgreSQL 17 instance returned `200` health,
`201` creation, `302` redirect with `Cache-Control: no-store`, and an owner-authorized analytics count of one.

The Azure Functions runtime was stress-tested locally with k6 using a ramp from 10 to 300 requested
redirects/second over 105 seconds. It completed 12,012 redirects with zero HTTP failures and 100% checks;
latency was 12.34 ms average, 23.92 ms p95, and 126.15 ms maximum. PostgreSQL contained exactly 12,012
analytics rows afterward and health remained `UP`. This is a local regression baseline, not a cloud
capacity claim.

A subsequent high-volume run ramped to 3,000 requested redirects/second and held that target for 20
seconds using a 20-connection pool. It completed 120,475 requests at 1,095 requests/second overall,
dropped 51,024 scheduled iterations, produced six database-connection failures, and reached 386.43 ms
average / 1.04 s p95 / 2.6 s maximum latency. The 1,000-VU ceiling was reached near 2,300 requested
requests/second, and one successful redirect lost its best-effort analytics insert. Health recovered to
`UP`. This establishes that the current synchronous PostgreSQL redirect path does not satisfy a 3,000
requests/second capacity target on one local Function worker.

Security validation on 2026-09-03 rebuilt the runtime image with Tomcat `10.1.59` and scanned it using a freshly downloaded Trivy vulnerability and Java advisory database. Both the Alpine layer and `app/app.jar` reported zero `HIGH` or `CRITICAL` vulnerabilities.

The earlier intermittent integration failure was caused by Redis state leaking between tests. The suite now flushes its isolated Redis Testcontainer before each test and deletes analytics rows before parent links. Verification also covers PostgreSQL `TIMESTAMPTZ` binding, idempotent event writes, and database-side click counting.

Coverage currently includes:

- Destination URL policy unit tests.
- Privacy tests for IPv4 `/24` and IPv6 `/64` anonymization, malformed literals, missing values, and bounded header-safe User-Agent storage.
- Secure short-code shape unit test.
- Redirect cache tests for positive/negative hits, database misses, stale cached link rejection, and expiration-capped TTLs.
- Redis integration tests for worker persistence acknowledgement, pending-message retries, poison-message dead-lettering, and acknowledgement after dead-lettering.
- Metric-contract tests for stable low-cardinality names/tags and Actuator exposure.
- An application smoke test proving `/openapi.yaml` is served with the core link and analytics paths.
- Application context and API-profile smoke tests.
- PostgreSQL 17 Testcontainer integration tests for generated creation and redirect, custom-alias conflict, unsafe/expired input, and missing code.

The Docker Compose stack was also built and smoke-tested successfully: health became `UP`, alias `smoke-2026` was created, and resolution returned `302` to `https://example.com/docs` with `Cache-Control: no-store`. The containers were stopped afterward; volumes were retained.

## Decisions and rationale

- Use PostgreSQL uniqueness rather than a pre-insert existence check to prevent alias races.
- Use a cryptographically secure random Base62 identifier rather than sequential IDs to reduce enumeration and avoid a central sequence encoder.
- Return `302`, not `301`, because destinations, link state, and expiration may change.
- Use Redis read-through caching with distinct positive, negative, miss, and error states. Revalidate cached status/expiration and cap positive TTL at the link's remaining lifetime.
- Inject `Clock` so time-dependent expiration behavior can be tested deterministically.
- Keep API and worker runtime boundaries in one artifact for prototype speed while retaining a path to independent scaling.
- Use direct JDBC for a small, explicit persistence surface rather than introducing ORM lifecycle complexity.

## Known limitations and risks

- Capability-token ownership is implemented, but there is no user identity, token rotation/recovery, or organization model. Legacy links created before `V3` have no owner token and are intentionally unmanageable.
- Creation has a Redis fixed-window limit that fails open; production still requires a trusted-proxy and edge-rate-limit policy.
- Local/private IP literals and internal/metadata hostnames are blocked. Hostname DNS is deliberately not resolved during creation, so production egress/DNS-rebinding controls remain necessary if server-side URL fetching is ever added.
- No Unicode/IDN normalization policy or canonical URL normalization.
- Link lookup, optimistic updates, and owner-authorized hard deletion exist; owner-token rotation does not.
- Analytics reporting requires the owner token but still has no retention policy or aggregate tables.
- Retry attempt counts persist in Redis across worker restarts.
- Dead-letter records require monitoring and an operator replay procedure.
- Cache invalidation for future update/disable operations is not implemented because those mutation APIs do not yet exist.
- API and worker expose Prometheus metrics; deployment-level alert rules are not yet defined.
- Stream length and pending-record gauges exist; a true consumer-group lag gauge still needs production validation.
- Security, concurrency, Redis-failure, and configurable k6 load-smoke coverage exist; sustained capacity and chaos testing remain outstanding.
- Local Functions stress coverage now includes a 3,000 requests/second target and exposes saturation near 2,200-2,300 requested requests/second; sustained cloud capacity, cold-start concurrency, and chaos testing remain outstanding.
- The deployed HTTP API currently runs as the `url-api` Azure Container App, with `url-worker` as a separate revision-scaled Container App. The optional Azure Functions packaging remains available but is not the active production target.
- Sustained production capacity and multi-region consistency have not been validated.
- CI/CD and the concurrent analytics slice are currently uncommitted; keep their changes separated when reviewing or committing.

## CI/CD baseline

- `.github/workflows/ci.yml` runs Maven verification, a Docker build, and a blocking Trivy high/critical vulnerability scan for pull requests, non-`main` branch pushes, and manual runs.
- `.github/workflows/delivery.yml` verifies trusted `main`/version-tag/manual runs, builds and scans the image before publication, then publishes to `ghcr.io/<owner>/<repository>` with OCI metadata, provenance, and an SBOM.
- Delivery uses only the scoped `GITHUB_TOKEN`; the publish job alone receives `packages: write`, `attestations: write`, and `id-token: write`.
- `latest`, branch, commit SHA, and semantic-version image tags are generated as applicable. Repository names are normalized to lowercase for OCI compatibility.
- `.github/dependabot.yml` checks GitHub Actions, Maven, and Docker dependencies weekly.
- `docs/ci-cd-runbook.md` documents exact required checks, release tags, failure triage, GHCR permissions, and rollback constraints.
- `docs/analytics-worker-runbook.md` documents pending/dead-letter inspection, idempotent replay, retired-consumer recovery, shutdown, and current operational gaps.
- The delivery security gate requires patched Netty `4.1.136.Final`, PostgreSQL JDBC `42.7.12`, Tomcat `10.1.59`, and current Alpine runtime packages; these are explicit build inputs in `pom.xml` and `Dockerfile`. Tomcat `10.1.59` supersedes the unreleased `10.1.58` candidate and fixes the vulnerabilities affecting versions through `10.1.57`.
- Production deployment currently uses the existing `url-api` and `url-worker` Azure Container Apps plus Azure Static Web Apps; CI delivery automation still needs to codify this manually validated rollout and its rollback/health gates.

## Completed slice: ownership, management, and operational hardening

1. Creation returns a one-time 256-bit capability token and stores only its SHA-256 digest.
2. Owner-protected lookup, analytics, and PATCH management use optimistic concurrency.
3. Successful mutations evict redirect cache entries after the database commit.
4. Creation rate limiting, private-destination rejection, structured audit logs, and security/concurrency tests are present.
5. Worker retries persist in Redis and Prometheus exposes stream-length and pending-record gauges on port 8081.

## Completed slice: Azure Functions API refactor

1. The `azure-functions` Maven profile packages a Java 17 HTTP Function for Flex Consumption without changing the normal Spring Boot executable JAR.
2. A catch-all explicit Function router preserves create, management, redirect, analytics, OpenAPI, system-info, and health routes and their status/header contracts.
3. The Function starts the existing Spring application as a non-web context and reuses domain services, JDBC repositories, validation, and Flyway.
4. Redis is optional in the Function runtime. Cache operations become no-ops, rate limiting fails open, and click analytics write synchronously to PostgreSQL; container API/worker behavior remains unchanged.
5. Function context initialization is synchronized, lazy, and retryable after transient database startup failures.
6. CI and Delivery package the Function; Delivery retains the deployable artifact for 14 days.
7. `docs/azure-functions-deployment.md` records configuration, validation, trade-offs, and rollback steps.

## Completed slice: deletion and frontend SPA

1. `DELETE /api/v1/links/{code}?expectedVersion={version}` requires the owner capability token, checks
   optimistic concurrency, hard-deletes the link, evicts its cache entry, and returns `204`.
2. Flyway migration `V4` changes analytics ownership to `ON DELETE CASCADE`; delayed analytics events
   for deleted links are ignored so queue/stream consumers can acknowledge them without dead-letter noise.
3. The Azure Functions router and OpenAPI contract expose the same delete behavior.
4. The React/Vite SPA in `frontend` centers the real create → open redirect → refresh asynchronous
   analytics story. A separate interactive Architecture route traces the synchronous cache path and
   asynchronous Redis Stream worker path. Owner tokens are not written to URLs or browser storage.
5. Spring MVC CORS is disabled by default and accepts only explicit comma-separated `FRONTEND_ORIGIN`
   values when configured for the separately deployed SPA.
6. Verification on 2026-09-04 passed 48 backend tests, the JaCoCo 90.1% gate, Azure Functions packaging,
   frontend lint, TypeScript compilation, the Vite production build, and rendered UI inspection.

## Completed slice: Azure deployment (2026-09-04)

1. GitHub Actions Delivery run `33859800395` verified, scanned, and published commit `70e8eec` as
   `ghcr.io/bathinenidhanush/url-shortener:sha-70e8eec` (OCI index digest
   `sha256:01da90425ba056891ac8d0f7467a14b501f0ba767bc463e50df23c4eec89c328`).
2. That GitHub-built image is deployed to the existing `url-api` and `url-worker` Container Apps in
   resource group `url-shortener`. API revision `url-api--0000003` and worker revision
   `url-worker--0000002` are healthy. The API is
   `https://url-api.redriver-672175da.eastus.azurecontainerapps.io`.
3. `FRONTEND_ORIGIN` is the exact Static Web App origin. The SPA was built with the API URL and deployed
   to `https://polite-ocean-0b239190f.6.azurestaticapps.net`; the Azure navigation fallback is included
   in the Vite output so `/architecture` returns the SPA.
4. Production smoke validation passed: API system-info `200`, link creation, redirect `302`, asynchronous
   analytics count `1`, exact-origin CORS, owner-authorized deletion `204`, and post-delete resolution `404`.
5. One disposable `smoke...` link targeting `https://example.com/docs` may remain from an earlier smoke
   script that stopped after treating the expected redirect as an error; its one-time owner token is not
   recoverable by design.
6. The SPA availability probe uses the MVC-managed `/api/v1/system/info` endpoint. Do not probe
   `/actuator/health` directly from the browser: Actuator uses a separate handler mapping and does not
   inherit the application's MVC CORS configuration. The deployed browser reports `API healthy` after
   this correction.

## Human decisions still required

Clarify these before their answers materially affect implementation:

- Are analytics public, authenticated, or protected by an owner token?
- Which click dimensions are required, and what privacy/retention policy applies?
- Is at-least-once analytics acceptable? It is the recommended prototype contract.
- Should redirect destinations be immutable after creation?
- Must private-network/localhost destinations be blocked?
- Expected traffic, latency SLO, availability target, and retention period.
- Approval to provision the billable Azure Database for PostgreSQL Flexible Server test SKU and its desired lifecycle/cleanup policy.

## Completion discipline for future agents

- Inspect `git status` first and preserve user changes.
- Keep schema changes additive through new Flyway migrations; never edit an applied migration.
- Add tests with each behavior change and use PostgreSQL/Redis integration tests where semantics matter.
- Run the full Maven verification command before handoff.
- For runtime changes, build and smoke-test Docker Compose.
- Record generated, edited, or rejected AI suggestions and rationale if formal AI traceability is added to the deliverables.
- Update this file so its completed work, decisions, limitations, and next slice remain accurate.
