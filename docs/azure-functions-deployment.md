# Azure Functions API deployment

## Runtime design

The `azure-functions` Maven profile adds one anonymous HTTP-triggered Java Function. Its explicit
router preserves the Spring MVC API paths and delegates to the existing application services,
repositories, validation, and Flyway migrations. Spring starts as a non-web application during a
Function cold start.

Azure Functions Flex Consumption is used so the API can scale to zero. PostgreSQL remains required.
Redis is optional for this runtime: cache operations become no-ops, creation rate limiting fails open,
and redirect analytics are written directly to PostgreSQL. The existing container API and worker retain
Redis caching and asynchronous Redis Stream analytics.

This is an intentional test-environment trade-off. Before high-volume production traffic, place an
edge rate limiter in front of the Function and assess whether synchronous analytics latency is acceptable.

## Build

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd -B -Pazure-functions -DskipTests package
```

The second command creates `target/azure-functions/url-shortener-api-test` with a thin application JAR,
dependency libraries, `host.json`, and generated trigger metadata.

## Required Function App settings

| Setting | Purpose |
| --- | --- |
| `DB_HOST` | Azure Database for PostgreSQL hostname |
| `DB_PORT` | PostgreSQL port, normally `5432` |
| `DB_NAME` | Application database |
| `DB_USER` | Database login |
| `DB_PASSWORD` | Database password; store as a secret setting |
| `DB_SSL_MODE` | Set to `require` for Azure PostgreSQL |
| `DB_POOL_SIZE` | Keep small for serverless concurrency, initially `5` |
| `PUBLIC_BASE_URL` | Public HTTPS Function URL used in create responses |

`FUNCTIONS_EXTENSION_VERSION=~4` and `FUNCTIONS_WORKER_RUNTIME=java` are set by the Maven deployment
configuration. Never commit database credentials or `local.settings.json` values.

## Deploy and validate

After `az login` and after the PostgreSQL server and Function App settings exist:

```powershell
.\mvnw.cmd -Pazure-functions azure-functions:deploy
```

Validate in this order:

1. `GET /actuator/health` returns `200` and `{"status":"UP"}`.
2. `POST /api/v1/links` returns `201`, a `Location`, and a one-time owner token.
3. `GET /{code}` returns `302` with the original destination.
4. `GET /api/v1/links/{code}/analytics` with the owner token reports the click.
5. Invalid input uses `application/problem+json`; unknown links return `404`.

## Rollback

Retain the previous deployment package in CI. Redeploy that package if the health or API smoke checks
fail. Database migrations must remain backward compatible because Flyway migrations are applied during
cold start and are not automatically rolled back.
