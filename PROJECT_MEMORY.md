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
- Baseline commit: `dc4c900 initial commit`. The working tree currently contains uncommitted CI/CD work and a separate in-progress analytics slice; check `git status` and preserve both sets of changes.
- Existing files belong to the user; do not discard or overwrite unrelated changes.

## Technology baseline

- Java 17
- Maven Wrapper
- Spring Boot 3.5.16
- Spring MVC, Bean Validation, Actuator
- Spring JDBC with PostgreSQL 17
- Flyway schema migrations
- Spring Data Redis provisioned but not used by business logic yet
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
- `worker`: worker runtime boundary; analytics consumption is not implemented.

One deployable artifact supports two runtime roles through Spring profiles:

- `api`: serves HTTP APIs and redirects.
- `worker`: currently starts and remains alive; intended for asynchronous click-event processing.

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

Environment overrides include `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_POOL_SIZE`, `REDIS_HOST`, `REDIS_PORT`, and `PUBLIC_BASE_URL`.

Docker Compose starts PostgreSQL, Redis, the API, and the worker. Persistent database and Redis volumes are intentionally retained by a normal `docker compose down`.

## Verification evidence

Last successful verification:

```powershell
cd E:\Projects\url-shortener
.\mvnw.cmd -B clean verify
```

Result: 10 tests passed, 0 failures, 0 errors, and the executable JAR was produced at `target\url-shortener-0.0.1-SNAPSHOT.jar`.

After the analytics slice was added concurrently, Maven compilation succeeds but the full test suite is currently red: API integration tests require Redis, and the `test` profile still instantiates `JdbcAnalyticsRepository` after JDBC auto-configuration is excluded. The CI workflows now provide Redis; the analytics slice must conditionally disable/mock its JDBC beans in the smoke-test profile before `clean verify` will pass again.

Coverage currently includes:

- Destination URL policy unit tests.
- Secure short-code shape unit test.
- Application context and API-profile smoke tests.
- PostgreSQL 17 Testcontainer integration tests for generated creation and redirect, custom-alias conflict, unsafe/expired input, and missing code.

The Docker Compose stack was also built and smoke-tested successfully: health became `UP`, alias `smoke-2026` was created, and resolution returned `302` to `https://example.com/docs` with `Cache-Control: no-store`. The containers were stopped afterward; volumes were retained.

## Decisions and rationale

- Use PostgreSQL uniqueness rather than a pre-insert existence check to prevent alias races.
- Use a cryptographically secure random Base62 identifier rather than sequential IDs to reduce enumeration and avoid a central sequence encoder.
- Return `302`, not `301`, because destinations, link state, and expiration may change.
- Keep redirects uncached initially for correctness. Redis read-through caching requires explicit negative-cache, TTL, expiration, and invalidation rules.
- Inject `Clock` so time-dependent expiration behavior can be tested deterministically.
- Keep API and worker runtime boundaries in one artifact for prototype speed while retaining a path to independent scaling.
- Use direct JDBC for a small, explicit persistence surface rather than introducing ORM lifecycle complexity.

## Known limitations and risks

- No authentication, ownership, quotas, or rate limiting.
- No SSRF network-range policy beyond URL syntax/scheme/credential validation. Decide whether private, loopback, link-local, and metadata-service destinations must be rejected.
- No Unicode/IDN normalization policy or canonical URL normalization.
- No link update, disable, delete, or lookup management APIs.
- No analytics, event delivery, retry/dead-letter handling, or reporting API.
- Redis is provisioned but unused.
- No cache invalidation strategy.
- No observability beyond baseline Actuator endpoints.
- No load, latency, resilience, or security testing.
- No OpenAPI document.
- No runtime deployment target, deployment manifests, authentication secrets management, or production TLS/proxy configuration.
- Generated-code collision retry behavior lacks a focused service-level unit test.
- CI/CD and the concurrent analytics slice are currently uncommitted; keep their changes separated when reviewing or committing.

## CI/CD baseline

- `.github/workflows/ci.yml` runs Maven verification, a Docker build, and a blocking Trivy high/critical vulnerability scan for pull requests, non-`main` branch pushes, and manual runs.
- `.github/workflows/delivery.yml` verifies trusted `main`/version-tag/manual runs, builds and scans the image before publication, then publishes to `ghcr.io/<owner>/<repository>` with OCI metadata, provenance, and an SBOM.
- Delivery uses only the scoped `GITHUB_TOKEN`; the publish job alone receives `packages: write`, `attestations: write`, and `id-token: write`.
- `latest`, branch, commit SHA, and semantic-version image tags are generated as applicable. Repository names are normalized to lowercase for OCI compatibility.
- `.github/dependabot.yml` checks GitHub Actions, Maven, and Docker dependencies weekly.
- The delivery security gate required patched Netty `4.1.136.Final`, PostgreSQL JDBC `42.7.12`, and current Alpine runtime packages; these are explicit build inputs in `pom.xml` and `Dockerfile`.
- Actual deployment is intentionally not implemented because no hosting platform, environment credentials, health gate, or rollback contract has been selected.

## Recommended next slice: asynchronous click analytics

Do not implement every reliability feature at once. Complete one reviewable vertical slice:

1. Define a versioned click-event contract containing event ID, link ID/code, occurred-at timestamp, and privacy-conscious request metadata.
2. Decide and document privacy behavior before storing IP address or user-agent data. Prefer derived/coarsened values and avoid raw IP persistence unless explicitly required.
3. Publish an event from successful redirect resolution without making analytics availability a redirect dependency.
4. Use Redis Streams for the prototype queue and a consumer group in the worker profile.
5. Add an idempotent PostgreSQL analytics schema keyed by event ID; consider raw events plus a daily aggregate table only if required by the API.
6. Add worker retry and pending-message recovery behavior, with a bounded poison-message/dead-letter policy.
7. Add an analytics read API such as `GET /api/v1/links/{code}/analytics`, after deciding whether access control is in scope.
8. Test successful publishing/consumption, duplicate delivery, Redis unavailability, poison events, and the guarantee that redirects still work when analytics fails.
9. Update this memory, README, architecture decisions, and validation evidence.

Redis read-through redirect caching should follow analytics as a separate slice. Its design must specify positive/negative TTLs, expiration-aware TTL capping, and invalidation for future update/disable operations.

## Human decisions still required

Clarify these before their answers materially affect implementation:

- Are analytics public, authenticated, or protected by an owner token?
- Which click dimensions are required, and what privacy/retention policy applies?
- Is at-least-once analytics acceptable? It is the recommended prototype contract.
- Should redirect destinations be immutable after creation?
- Must private-network/localhost destinations be blocked?
- Expected traffic, latency SLO, availability target, and retention period.

## Completion discipline for future agents

- Inspect `git status` first and preserve user changes.
- Keep schema changes additive through new Flyway migrations; never edit an applied migration.
- Add tests with each behavior change and use PostgreSQL/Redis integration tests where semantics matter.
- Run the full Maven verification command before handoff.
- For runtime changes, build and smoke-test Docker Compose.
- Record generated, edited, or rejected AI suggestions and rationale if formal AI traceability is added to the deliverables.
- Update this file so its completed work, decisions, limitations, and next slice remain accurate.
