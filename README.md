# URL Shortener

Maven-based Spring Boot URL shortener. The first vertical slice creates persistent short links and resolves them with HTTP redirects. The same artifact supports an HTTP API runtime and a background worker runtime through Spring profiles.

## Prerequisites

- Java 17
- Maven 3.6.3 or newer, or the Maven Wrapper
- Docker with Docker Compose for PostgreSQL and Redis

## Build and test

```powershell
.\mvnw.cmd clean verify
```

## Run the application

```bash
docker compose up --build
```

Useful initial endpoints:

- `POST /api/v1/links` creates a short link
- `GET /api/v1/links/{code}` returns owner-visible link state
- `PATCH /api/v1/links/{code}` updates destination, status, or expiration
- `GET /{code}` returns a temporary redirect to the destination
- `GET /api/v1/links/{code}/analytics` returns the persisted click total
- `GET /openapi.yaml` returns the OpenAPI 3.1 contract
- `GET /api/v1/system/info`
- `GET /actuator/health`
- `GET /actuator/health/liveness`
- `GET /actuator/health/readiness`

Create a generated short link:

```powershell
$body = @{ destinationUrl = 'https://example.com/docs' } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/v1/links' -ContentType 'application/json' -Body $body
```

The creation response contains an `ownerToken` exactly once. Store it as a secret; only its SHA-256
digest is persisted. Management and analytics calls require `X-Link-Owner-Token`:

```powershell
$headers = @{ 'X-Link-Owner-Token' = $created.ownerToken }
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/links/$($created.code)" -Headers $headers
$update = @{ expectedVersion = 0; destinationUrl = 'https://example.com/new'; status = 'DISABLED' } | ConvertTo-Json
Invoke-RestMethod -Method Patch -Uri "http://localhost:8080/api/v1/links/$($created.code)" -Headers $headers -ContentType 'application/json' -Body $update
```

Updates use the response `version` for optimistic concurrency; stale writers receive `409 Conflict`.
Creation is limited per direct client address using an atomic Redis fixed window (20 requests per 60
seconds by default). Configure `LINK_CREATION_RATE_LIMIT` and `LINK_CREATION_RATE_WINDOW`. The limiter
fails open when Redis is unavailable, so production deployments should also enforce an edge limit.

An optional custom alias and expiration can be supplied:

```json
{
  "destinationUrl": "https://example.com/docs",
  "customAlias": "docs-2026",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Only absolute public-network `http` and `https` destination URLs are accepted. Credentials, localhost,
private/link-local IP literals, metadata hosts, and internal host suffixes are rejected. Aliases are
case-sensitive, must contain 4-32 letters, digits, underscores, or hyphens, and cannot use reserved routes.

## Current architecture

- Controllers expose versioned link management and root-level redirects.
- Application services own creation and resolution use cases.
- Domain policies validate URLs, aliases, status, and expiration.
- A JDBC repository persists links in PostgreSQL; Flyway owns schema evolution.
- Redis provides expiration-aware positive caching and short-lived negative caching on the redirect path.
- Successful redirects publish privacy-filtered click events to a Redis Stream without making analytics availability a redirect dependency.
- The worker consumes events with at-least-once delivery, idempotently persists them in PostgreSQL, retries pending failures, and dead-letters poison messages.
- Owner-token-protected analytics returns the database-counted click total.

Errors use RFC 9457 problem details. Redirects return `302 Found` with `Cache-Control: no-store` so future disablement and expiration changes take effect without stale client caching.

## Validation

`mvn clean verify` runs domain tests, failure-path tests, application smoke tests, and API integration tests against PostgreSQL 17 and Redis Testcontainers. The suite covers link creation/resolution, authorization, optimistic concurrency, cache and Redis failures, rate limiting, event publishing, analytics persistence/restarts, input validation, and privacy filtering. JaCoCo generates `target/site/jacoco/index.html` and fails verification when aggregate line coverage is 90% or lower.

## Analytics worker configuration

Each worker replica needs a unique, stable `ANALYTICS_CONSUMER_NAME`; the default Compose worker uses `worker-1`. `ANALYTICS_MAX_ATTEMPTS` defaults to `3`. A failed record remains pending for retry and is acknowledged only after successful persistence or after it is copied to the `clicks:dead-letter` stream. For production, alert on that stream and define an operator replay procedure.

Operational inspection and replay procedures are in [the analytics worker runbook](docs/analytics-worker-runbook.md).
The worker exposes Prometheus metrics at `http://localhost:8081/actuator/prometheus`, including stream
length and consumer pending-message gauges. Retry counts live in Redis so restarts preserve poison-message
budgets. API metrics remain available at port 8080.

## Load smoke test

With the Compose stack running and a valid short code, install k6 and run:

```powershell
$env:CODE = 'your-code'
k6 run tests/load/url-shortener-smoke.js
```

Defaults are 50 redirect requests/second for 30 seconds with thresholds of less than 1% failures and
under 200 ms p95 latency. Override `BASE_URL`, `REQUESTS_PER_SECOND`, and `DURATION` as needed.

## CI/CD

GitHub Actions provides two guarded pipelines:

- `CI` runs Maven verification with a health-checked Redis service, builds the container, and blocks on fixable high or critical vulnerabilities for pull requests, non-`main` branch pushes, and manual runs.
- `Delivery` repeats verification for trusted `main`, semantic version tag (`v*.*.*`), or manual runs; it scans before publishing the image to `ghcr.io/<owner>/<repository>` with provenance and an SBOM.

Published tags include `latest` on the default branch, the branch name, `sha-<commit>`, and semantic-version tags when a release tag is pushed. The workflow uses the scoped `GITHUB_TOKEN`; no registry password is required. Repository settings must allow Actions to write packages.

Dependabot checks GitHub Actions, Maven, and Docker dependencies weekly. Configure `Maven verification` and `Container build and vulnerability scan` as required checks on `main` to prevent unverified merges.

This pipeline performs continuous delivery to GHCR, not deployment to a runtime environment. Add a deployment job with a protected GitHub Environment after selecting the hosting platform and defining rollback and health-check behavior.

Required-check configuration, release handling, scan triage, and rollback constraints are documented in [the CI/CD runbook](docs/ci-cd-runbook.md).

## Next slice

Replace capability tokens with organizational identity when a user model is selected, add explicit token
rotation/recovery, and choose a production deployment platform with protected environments and alerting.
