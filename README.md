# URL Shortener

Maven-based Spring Boot URL shortener. The first vertical slice creates persistent short links and resolves them with HTTP redirects. The same artifact supports an HTTP API runtime and a background worker runtime through Spring profiles.

The HTTP API can also be packaged as an Azure Functions Java application on Flex Consumption. This
runtime reuses the same Spring application/domain services and PostgreSQL schema without starting an
embedded servlet server.

## Engineering review against the assignment

### 1. Objective and normalized engineering problem

The objective was to turn a high-level URL-shortener requirement into a reviewable, runnable engineering
outcome through **engineer-led, AI-assisted execution**. The normalized problem was:

> Build a secure URL-shortening system with owner-controlled link lifecycle, low-latency redirects,
> durable eventually consistent click analytics, explicit failure behavior, measurable quality gates,
> reproducible setup, deployable artifacts, and honest capacity evidence.

AI was used as an accelerator within bounded tasks. The engineer retained control of architecture,
acceptance criteria, code review, credentials, schema changes, external pushes, cloud mutations,
validation, and release sign-off. AI output was treated as a candidate—not as evidence of correctness.

### 2. Scenario and execution approach

The service was created from scratch and improved in dependency-ordered vertical slices suitable for a
2–3 day prototype exercise:

1. Maven/Spring Boot skeleton and runtime configuration.
2. PostgreSQL schema, link creation, and redirects.
3. Validation, RFC 9457 errors, and integration tests.
4. Redis read-through caching and Redis Streams publication.
5. Independently operated analytics worker, retries, idempotency, and dead letters.
6. Owner capability tokens, optimistic management, deletion, and Flyway V4.
7. Observability, security hardening, coverage, Trivy, k6, and CI/CD.
8. Azure deployment, reviewer-focused SPA, architecture evidence, and executable API documentation.

This sequencing kept every slice runnable and reviewable while allowing test evidence to influence later
architecture decisions.

### 3. Scope coverage

| Required scope | Evidence |
| --- | --- |
| Greenfield | Domain model, Spring Boot API, PostgreSQL/Flyway persistence, redirects, OpenAPI, Docker Compose, and initial test suite. |
| Brownfield | Redis caching, asynchronous analytics, ownership, optimistic concurrency, deletion, retry persistence, CORS correction, and UI/API refactors added to working code. |
| Test and documentation improvements | Unit/integration tests, Testcontainers, >90% JaCoCo gate, k6 scripts/report, Trivy, runbooks, OpenAPI/Swagger UI, SPA review pages, and this README. |
| Well-defined and ambiguous requirements | Exact API behaviors were implemented directly; ambiguous ownership, analytics consistency, destination safety, cloud platform, and 3,000 RPS expectations were converted into documented decisions and measurable validation. |

### 4. Core requirements and evidence

1. **Requirement understanding** — Intent was normalized into owned CRUD, secure destination policy,
   fast resolution, asynchronous analytics, operational reliability, deployment, and measurable limits.
   Ambiguities were captured rather than silently converted into assumptions.
2. **Task decomposition** — Work was sequenced from domain/persistence prerequisites through API,
   cache/event flow, worker, management, validation, deployment, and reviewer artifacts. Dependencies and
   acceptance gates were explicit for each slice.
3. **Brownfield codebase reasoning** — Enhancements mapped impacted controllers, services, repositories,
   Flyway schemas, Redis cache and stream behavior, worker retry state, API contract, frontend client,
   runtime profiles, tests, and deployment data flow before edits.
4. **AI-assisted execution** — AI supported scaffolding, implementation alternatives, debugging,
   refactoring, test generation, documentation, load scripts, UI composition, and review preparation.
   Tasks included intent, constraints, acceptance criteria, and technical context; suggestions were
   iteratively refined and passed through compilation, lint, tests, security, performance, and human review.
   Secrets were not stored in prompts, source, frontend storage, or logs. High-impact changes required
   explicit engineer approval.
5. **Engineering output generation** — Outputs include production-oriented Java/React code, API/schema
   definitions, Flyway migrations, unit/integration tests, Docker/Compose, CI/CD, load tests, Swagger UI,
   runbooks, deployment evidence, and offline documentation.
6. **Validation and risk control** — Guardrails include destination/network policy, one-way owner-token
   storage, optimistic concurrency, cache failure isolation, at-least-once/idempotent analytics, bounded
   retries/dead letters, rate limiting, graceful shutdown, health probes, >90% coverage, Testcontainers,
   Trivy, k6, immutable images, and revision health checks.
7. **Controlled oversight** — The engineer selected and approved architecture and outputs. AI did not
   autonomously merge, provision billable services, expose secrets, alter schemas, or deploy without
   approval. Correctness, maintainability, and readiness remain human responsibilities.
8. **Final engineering summary** — Plan, rationale, artifacts, scenarios, decisions, risks, trade-offs,
   validation, assumptions, limitations, and remaining work are recorded here and in `PROJECT_MEMORY.md`.

#### AI traceability: generated, edited, and rejected

| Disposition | Examples and rationale |
| --- | --- |
| Generated then accepted after validation | Initial Maven slices, test candidates, API/UI structures, runbook outlines, load scripts, and deployment commands. |
| Materially edited by the engineer | Ownership boundaries, optimistic concurrency, cache failure behavior, retry persistence, exact-origin CORS, Swagger contract source, UX hierarchy, and rollout sequencing. |
| Rejected or redirected | Autonomous orchestration, wildcard CORS, plaintext owner tokens, misleading 3,000 RPS success claims, Vercel for the Java API, and synchronous analytics as the target-scale design. |

### 5. Deliverables

| Deliverable | Artifact/evidence | Status |
| --- | --- | --- |
| Working prototype | Live SPA and Spring Boot API supporting create, redirect, async analytics, management, and deletion; Compose provides offline execution. | Complete |
| Architecture overview | This README, `/architecture` SPA route, runtime/data flows, cloud topology, delivery path, and recorded decisions. | Complete |
| Three scenarios | Greenfield, brownfield, and ambiguous cases below, each with decomposition, execution, and validation. | Complete |
| Setup instructions | Prerequisites and local/Functions/frontend commands below plus deployment and operations runbooks. | Complete |
| Testing, limitations, and trade-offs | Validation section, stress report, known risks, CI gates, and scenario results. | Complete |

#### Scenario A — Greenfield: core short-link service

- **Intent:** Build a runnable Java service with durable links, safe redirects, analytics, and an executable contract.
- **Decomposition:** Domain rules → PostgreSQL/Flyway → create/resolve APIs → Compose → contract and integration tests.
- **Execution:** Secure random codes, URL/alias/expiration validation, JDBC repositories, RFC 9457 errors,
  `302` redirects, and OpenAPI 3.1.
- **Validation:** Unit tests and PostgreSQL Testcontainers cover success and failure paths, alias conflicts,
  expiration, unsafe destinations, and redirect headers.

#### Scenario B — Brownfield: cache, asynchronous analytics, ownership, and deletion

- **Intent:** Improve latency, resilience, and lifecycle control without breaking the working API.
- **Decomposition:** Trace redirect path → add cache → emit stream events → add worker → protect management
  → add deletion migration and delayed-event behavior.
- **Execution:** Redis caching/negative caching, Redis Streams, worker retry/dead-letter/idempotency,
  SHA-256 owner-token digests, optimistic versions, cache eviction, and cascade deletion.
- **Validation:** Redis/PostgreSQL integration tests cover cache outages, pending retries across restart,
  poison records, authorization, stale writers, deletion, and late click events.

#### Scenario C — Ambiguous: “scale to 3,000 RPS” and deploy to cloud

- **Intent:** Convert an underspecified scale request into measurable load stages, thresholds, resource
  constraints, and a deployment choice without overstating evidence.
- **Decomposition:** Define k6 profile → establish 300 RPS baseline → find saturation → identify bottleneck
  → compare Functions/Container Apps → decouple analytics → deploy → smoke-test.
- **Execution:** The synchronous Azure Functions experiment was retained as evidence. The deployed design
  uses Azure Static Web Apps, public `url-api`, internal Redis/PostgreSQL, separate `url-worker`, and a
  GitHub Actions → GHCR → protected Azure CLI delivery path.
- **Validation:** 300 requested RPS passed. The 3,000 requested-RPS run failed scheduling/latency goals and
  exposed JDBC pressure. A deployed Azure 3,000 RPS run remains required.

### 6. Evaluation criteria

| Criterion | Defensible evidence |
| --- | --- |
| Effective AI-assisted execution | AI assisted across code, debugging, refactoring, tests, docs, deployment diagnosis, and review preparation under engineer control. |
| Architecture/system design quality | Explicit API/worker roles, PostgreSQL authority, Redis cache/stream, asynchronous analytics, versioned schema, and cloud boundaries. |
| Decomposition and execution depth | Dependency-ordered vertical slices with acceptance gates and incremental validation. |
| Realism/quality | The SPA calls the real API; failure modes and failed performance thresholds are visible rather than fabricated. |
| Validation and risk rigor | Layered automated tests, coverage, security scan, load test, health/CORS smoke tests, and known-risk register. |
| Clarity/defensibility | Major decisions and rejected alternatives have explicit rationale and measured evidence. |
| Core engineering principles | Modular services, testable time/policies, reliable failure isolation, secure tokens/destinations, scalable runtime roles, additive migrations, and traceable images. |
| Engineering judgment | Test evidence changed the architecture; limitations and required human approvals remain explicit. |

### 7. Production-grade expectation and final engineering summary

The prototype demonstrates production-oriented fundamentals and a realistic delivery path. PostgreSQL is
the durable authority; Redis removes repeated resolution work and buffers click events; the worker keeps
analytics writes off the user response path; one immutable image reduces release drift while independent
runtime roles retain scale and failure isolation. `302` and `Cache-Control: no-store` preserve lifecycle
control. Capability tokens avoid premature user-account scope while acknowledging the recovery limitation.

The outcome is **reviewable and deployable, but not declared unrestricted production-ready**. Human sign-off
is required before identity changes, destructive migrations, managed-service provisioning, automated
production rollout, connection/scale-budget changes, or adoption of a 3,000 RPS SLO.

Key risks and limitations:

- Deployed cloud capacity at 3,000 RPS has not been proven.
- Capability tokens have no organizational identity, recovery, or rotation.
- Current cloud PostgreSQL and Redis are internal prototype containers rather than managed HA services.
- Production needs alert rules, analytics retention, queue-lag SLOs, backup/restore rehearsal, and cost controls.
- DNS-rebinding/egress controls are required if server-side destination fetching is introduced.
- Multi-region consistency and disaster recovery remain out of scope.

Assumptions: at-least-once analytics is acceptable; redirects may be eventually reflected in analytics;
aliases are case-sensitive; hard deletion is authorized by owner token plus expected version; the current
prototype is single-region; and local load evidence is not a cloud guarantee.

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

## Package the Azure Functions API

```powershell
.\mvnw.cmd -Pazure-functions -DskipTests clean package
```

The deployable directory is `target/azure-functions/url-shortener-api-test`. The Function preserves
the existing routes, including root-level short-link redirects. It requires PostgreSQL and records
click analytics synchronously there. Redis is intentionally optional in this runtime so Flex
Consumption can scale to zero without requiring an always-on cache; the container API/worker mode
continues to use Redis caching and Redis Streams.

Set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`, `DB_SSL_MODE=require`, and
`PUBLIC_BASE_URL` as Function App settings. See [the Azure Functions deployment runbook](docs/azure-functions-deployment.md).

For the live Azure Container Apps SPA, set `FRONTEND_ORIGIN` on the API container to the exact HTTPS
origin of the deployed frontend. Multiple explicit origins may be comma-separated; wildcard CORS is not enabled.

Useful initial endpoints:

- `POST /api/v1/links` creates a short link
- `GET /api/v1/links/{code}` returns owner-visible link state
- `PATCH /api/v1/links/{code}` updates destination, status, or expiration
- `DELETE /api/v1/links/{code}?expectedVersion={version}` permanently deletes an owned link and its analytics
- `GET /{code}` returns a temporary redirect to the destination
- `GET /api/v1/links/{code}/analytics` returns the persisted click total
- `GET /openapi.yaml` returns the OpenAPI 3.1 contract
- `GET /swagger-ui.html` renders interactive API documentation from the committed OpenAPI contract
- `GET /v3/api-docs` returns springdoc-generated supplementary JSON documentation
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
Invoke-RestMethod -Method Delete -Uri "http://localhost:8080/api/v1/links/$($created.code)?expectedVersion=0" -Headers $headers
```

Updates use the response `version` for optimistic concurrency; stale writers receive `409 Conflict`.
Deletion uses the same owner token and version protection, returns `204 No Content`, evicts the redirect
cache, and cascades to stored analytics. Delayed click events for deleted links are safely ignored.
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

### Deployed Azure topology

- Azure Static Web Apps serves the React/Vite reviewer SPA.
- `url-api` is the public-ingress Azure Container App and runs the `api` Spring profile.
- Redis and PostgreSQL use internal Container Apps endpoints in the prototype environment.
- `url-worker` has no public ingress and runs the worker role from the same immutable image.
- GitHub Actions verifies, scans, and publishes SHA-tagged images with provenance/SBOM to GHCR.
- An engineer performs the protected Azure CLI revision update and verifies health before continuing.

The API and worker scale independently, but `maximum replicas × DB_POOL_SIZE` must stay within the
PostgreSQL connection budget. Moving PostgreSQL/Redis to managed HA services, establishing private DNS,
backup/restore, alerting, and validated scale limits are production follow-ups.

## Swagger UI and OpenAPI

After starting the API, open [Swagger UI](http://localhost:8080/swagger-ui.html). It renders the committed
`/openapi.yaml` contract so review and executable documentation use the same versioned source. The OpenAPI
server is relative, so Swagger “Try it out” targets the current API origin locally or in Azure.

Springdoc also exposes generated JSON at `/v3/api-docs`; this is supplementary and does not replace the
reviewed contract. Any API behavior change must update the controller, committed OpenAPI YAML, and contract
tests in the same change.

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

For a capacity-oriented ramp against the Azure Functions API, run:

```powershell
docker run --rm `
  -e BASE_URL=http://host.docker.internal:7071 `
  -e CODE=your-code `
  -v E:\Projects\url-shortener\tests\load:/scripts:ro `
  grafana/k6:latest run /scripts/url-shortener-stress.js
```

The stress profile ramps from 50 to 3,000 redirect requests/second, holds 3,000 requests/second for
20 seconds, and validates scheduling capacity, redirect status, destination header, error rate, and p95 latency. Treat local results as a regression
baseline; cloud capacity still depends on the Function plan, PostgreSQL SKU, region, and network path.
The latest evidence and production risks are recorded in [the Functions stress-test report](docs/azure-functions-stress-test.md).

### Recorded stress-test results

| Run | Outcome | Key evidence |
| --- | --- | --- |
| 300 requested RPS | Passed | 12,012 redirects, 0 failures, 23.92 ms p95, 12,012/12,012 analytics rows, health `UP`. |
| 3,000 requested RPS | Capacity target not met | 120,475 completed, 1,095.25 RPS overall, 51,024 dropped iterations, 6 JDBC-connection failures, 1.04 s p95, 1,000/1,000 VUs. |

The 3,000 RPS run is useful failure evidence: it showed that synchronous PostgreSQL lookup plus analytics
write is not a viable hot path at that target. Redis resolution caching, asynchronous event publication,
controlled scale-out, and explicit database connection budgets are the resulting design actions. The same
profile must still be repeated against Azure Container Apps before claiming the target.

## Frontend SPA

The `frontend` directory contains the React, TypeScript, and Vite single-page application centered on a
live create → redirect → asynchronous analytics demonstration, plus an interactive architecture trace.
It also supports owner-token management and deletion. Run `npm ci`, set `VITE_API_BASE_URL`, and use
`npm run dev` locally or `npm run build` for the deployable `frontend/dist` directory. See
[`frontend/README.md`](frontend/README.md) for Azure Static Web Apps configuration and the required
Function App CORS origin.

## CI/CD

GitHub Actions provides two guarded pipelines:

- `CI` runs Maven verification, packages the Azure Functions API, builds the container, and blocks on fixable high or critical vulnerabilities for pull requests, non-`master` branch pushes, and manual runs.
- `Delivery` repeats verification and Function packaging for trusted `master`, semantic version tag (`v*.*.*`), or manual runs; it retains the Function artifact and scans before publishing the container image to `ghcr.io/<owner>/<repository>` with provenance and an SBOM.

Published tags include `latest` on the default branch, the branch name, `sha-<commit>`, and semantic-version tags when a release tag is pushed. The workflow uses the scoped `GITHUB_TOKEN`; no registry password is required. Repository settings must allow Actions to write packages.

Dependabot checks GitHub Actions, Maven, and Docker dependencies weekly. Configure `Maven verification` and `Container build and vulnerability scan` as required checks on `main` to prevent unverified merges.

Runtime deployment remains a protected/manual operation until Azure federated credentials and environment approval rules are configured in GitHub.

Required-check configuration, release handling, scan triage, and rollback constraints are documented in [the CI/CD runbook](docs/ci-cd-runbook.md).

## Next slice

Replace capability tokens with organizational identity when a user model is selected, add token
rotation/recovery, move prototype data containers to managed HA services if production requires it, define
alerting/retention/backup controls, automate Azure rollout behind protected environments, and validate the
capacity target in the deployed cloud environment.
