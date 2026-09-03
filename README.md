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
- Redis and the worker boundary are provisioned for the next analytics/caching slice but are not yet on the redirect path.

Errors use RFC 9457 problem details. Redirects return `302 Found` with `Cache-Control: no-store` so future disablement and expiration changes take effect without stale client caching.

## Validation

`mvn clean verify` runs domain tests, application smoke tests, and API integration tests against a PostgreSQL 17 Testcontainer. The integration suite covers generated links, custom alias conflicts, invalid and expired requests, redirect resolution, and missing codes.

## Next slice

Publish redirect click events, consume them in the worker, aggregate analytics, and add Redis read-through caching with explicit invalidation rules.
