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

An optional custom alias and expiration can be supplied:

```json
{
  "destinationUrl": "https://example.com/docs",
  "customAlias": "docs-2026",
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

Only absolute `http` and `https` destination URLs are accepted. Aliases are case-sensitive, must contain 4-32 letters, digits, underscores, or hyphens, and cannot use reserved application routes.

## Current architecture

- Controllers expose versioned link management and root-level redirects.
- Application services own creation and resolution use cases.
- Domain policies validate URLs, aliases, status, and expiration.
- A JDBC repository persists links in PostgreSQL; Flyway owns schema evolution.
- Redis provides expiration-aware positive caching and short-lived negative caching on the redirect path.
- Successful redirects publish privacy-filtered click events to a Redis Stream without making analytics availability a redirect dependency.
- The worker consumes events with at-least-once delivery, idempotently persists them in PostgreSQL, retries pending failures, and dead-letters poison messages.
- `GET /api/v1/links/{code}/analytics` returns the database-counted click total.

Errors use RFC 9457 problem details. Redirects return `302 Found` with `Cache-Control: no-store` so future disablement and expiration changes take effect without stale client caching.

## Validation

`mvn clean verify` runs domain tests, application smoke tests, and API integration tests against PostgreSQL 17 and Redis Testcontainers. The suite covers link creation/resolution, cache/event publishing, analytics timestamp persistence and idempotency, database-side counting, input validation, and privacy filtering.

## Analytics worker configuration

Each worker replica needs a unique, stable `ANALYTICS_CONSUMER_NAME`; the default Compose worker uses `worker-1`. `ANALYTICS_MAX_ATTEMPTS` defaults to `3`. A failed record remains pending for retry and is acknowledged only after successful persistence or after it is copied to the `clicks:dead-letter` stream. For production, alert on that stream and define an operator replay procedure.

Operational inspection and replay procedures are in [the analytics worker runbook](docs/analytics-worker-runbook.md). The API exposes cache and analytics-publication counters through Actuator using bounded outcome tags. Worker processing counters require an external exporter because the worker profile intentionally has no HTTP server.

## CI/CD

GitHub Actions provides two guarded pipelines:

- `CI` runs Maven verification with a health-checked Redis service, builds the container, and blocks on fixable high or critical vulnerabilities for pull requests, non-`main` branch pushes, and manual runs.
- `Delivery` repeats verification for trusted `main`, semantic version tag (`v*.*.*`), or manual runs; it scans before publishing the image to `ghcr.io/<owner>/<repository>` with provenance and an SBOM.

Published tags include `latest` on the default branch, the branch name, `sha-<commit>`, and semantic-version tags when a release tag is pushed. The workflow uses the scoped `GITHUB_TOKEN`; no registry password is required. Repository settings must allow Actions to write packages.

Dependabot checks GitHub Actions, Maven, and Docker dependencies weekly. Configure `Maven verification` and `Container build and vulnerability scan` as required checks on `main` to prevent unverified merges.

This pipeline performs continuous delivery to GHCR, not deployment to a runtime environment. Add a deployment job with a protected GitHub Environment after selecting the hosting platform and defining rollback and health-check behavior.

Required-check configuration, release handling, scan triage, and rollback constraints are documented in [the CI/CD runbook](docs/ci-cd-runbook.md).

## Next slice

Add authenticated link ownership and management operations (lookup, disable, and update), then connect those mutations to explicit cache invalidation. Add metrics and alerts for stream lag, pending records, retries, and dead-letter volume.
