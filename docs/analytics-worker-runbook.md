# Analytics Worker Runbook

## Purpose and delivery contract

The API writes successful redirect events to the Redis Stream `clicks:stream`. Workers in the `analytics-group` consumer group persist those events to PostgreSQL. Delivery is at least once: the analytics row uses the event UUID as its primary key, so replaying an already-persisted event is safe.

The redirect path does not wait for database analytics persistence. A Redis publication failure is logged and counted, but the visitor still receives the redirect.

## Configuration

- `ANALYTICS_CONSUMER_NAME` must be unique per concurrently running worker and stable across restarts. Docker Compose uses `worker-1`.
- `ANALYTICS_MAX_ATTEMPTS` controls in-process attempts before dead-lettering and defaults to `3`.
- `REDIS_HOST` and `REDIS_PORT` select Redis.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, and `DB_PASSWORD` select PostgreSQL.

Attempt counts are currently held in worker memory. Restarting a worker resets the attempt count, while a stable consumer name lets it resume its own pending records. Automatic claiming of records abandoned by a permanently retired consumer is not yet implemented.

## Health and metrics

The API exposes application metrics through `/actuator/metrics`. The worker currently runs with `web-application-type: none`; it registers the same counters in process but needs a production meter-registry exporter or a deliberately enabled management server before external scraping is possible:

- `url.shortener.analytics.publications` with `outcome=published|failed`
- `url.shortener.analytics.consumer.events` with `outcome=processed|retry|dead_lettered`
- `url.shortener.cache.lookups` with `result=hit|negative_hit|miss|error`

Alert on sustained publication failures, retry growth, any dead-letter growth, and Redis consumer lag after connecting the worker to an external metrics backend. The current prototype does not export a first-class lag gauge; use Redis stream inspection until one is added.

## Inspect the stream and pending records

Run these commands from the repository directory when using Docker Compose:

```powershell
docker compose exec redis redis-cli XINFO STREAM clicks:stream
docker compose exec redis redis-cli XINFO GROUPS clicks:stream
docker compose exec redis redis-cli XPENDING clicks:stream analytics-group
docker compose exec redis redis-cli XPENDING clicks:stream analytics-group - + 20
```

Inspect an individual pending record with its record ID:

```powershell
docker compose exec redis redis-cli XRANGE clicks:stream <record-id> <record-id>
```

Do not acknowledge a failed record manually unless its data has been durably preserved or intentionally discarded under an approved incident decision.

## Inspect dead letters

```powershell
docker compose exec redis redis-cli XLEN clicks:dead-letter
docker compose exec redis redis-cli XRANGE clicks:dead-letter - + COUNT 20
```

Each dead letter retains the original event fields plus `sourceRecordId`, `attempts`, `failure`, and `failedAt`. Treat IP prefixes and User-Agent values as potentially sensitive operational data even though IP addresses are coarsened.

## Replay a dead letter

1. Identify and fix the underlying failure.
2. Read the dead-letter entry and record its dead-letter ID and event ID.
3. Re-add only the original event fields to `clicks:stream`:

```powershell
docker compose exec redis redis-cli XADD clicks:stream * eventId <event-id> linkId <link-id> timestamp <timestamp> ip <ip-prefix> ua <user-agent>
```

4. Confirm the worker processed the event and the analytics row exists. Reusing the same event ID preserves idempotency.
5. Remove the dead-letter entry only after confirmation:

```powershell
docker compose exec redis redis-cli XDEL clicks:dead-letter <dead-letter-id>
```

Keep an incident record of manual replay or deletion. For bulk replay, build a reviewed script with a dry-run mode instead of copying commands by hand.

## Recover records from a retired consumer

First inspect consumer ownership and idle time:

```powershell
docker compose exec redis redis-cli XINFO CONSUMERS clicks:stream analytics-group
docker compose exec redis redis-cli XPENDING clicks:stream analytics-group - + 100 <retired-consumer-name>
```

Redis `XAUTOCLAIM` can transfer sufficiently idle records to a live consumer, but this is an operator action in the current prototype. Verify that the old worker is permanently stopped, choose a conservative minimum idle time, and capture the returned records before proceeding. A mistaken claim can cause concurrent delivery; database event IDs make persistence idempotent, but logs and retry counters may duplicate.

## Shutdown behavior

Spring graceful shutdown invokes the worker's shutdown hook, interrupts the blocking Redis read, and waits up to six seconds for the consumer thread. During deployments, wait for normal termination before forcing the container down. A forced stop can leave a record pending; the stable consumer name processes it after restart.

## Known operational gaps

- Retry budgets do not survive worker restarts.
- Abandoned messages are not automatically claimed from retired consumers.
- Stream trimming and analytics retention are not configured.
- No automated dead-letter replay exists.
- Metrics do not yet include consumer lag or pending-record gauges.
