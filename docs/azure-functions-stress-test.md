# Azure Functions stress-test report

Date: 2026-09-03

## Scope

This test exercised the packaged Java Azure Function through Azure Functions Core Tools. The target
used Java 17, a disposable PostgreSQL 17 container, and no Redis. Each
successful redirect performed a PostgreSQL lookup and synchronous analytics insert.

Redirect following was disabled so the destination site was not load-tested.

## 300 requests/second baseline

The first run used a Hikari pool size of 10 and ramped from 10 to 300 requested iterations per second
over 105 seconds.

| Metric | Result |
| --- | ---: |
| Completed redirects | 12,012 |
| HTTP failures | 0 |
| Successful checks | 24,024 / 24,024 (100%) |
| Average latency | 12.34 ms |
| Median latency | 10.29 ms |
| p90 latency | 18.09 ms |
| p95 latency | 23.92 ms |
| Maximum latency | 126.15 ms |
| Maximum active virtual users | 5 |
| Persisted analytics rows | 12,012 / 12,012 |
| Post-test health | `UP` |

All configured thresholds passed: less than 1% HTTP failures, more than 99% checks, and p95 below
500 ms. The test did not find a saturation point at or below the 300 requests/second ramp.

## 3,000 requests/second capacity run

The second run used a Hikari pool size of 20. It ramped from 50 through 100, 500, 1,000, 2,000, and
3,000 requested iterations per second, then held 3,000 for 20 seconds before ramping down.

| Metric | Result |
| --- | ---: |
| Completed requests | 120,475 |
| Overall achieved throughput | 1,095.25 requests/second |
| Dropped scheduled iterations | 51,024 |
| HTTP failures | 6 |
| Successful checks | 240,938 / 240,950 (99.99%) |
| Average latency | 386.43 ms |
| Median latency | 319.34 ms |
| p90 latency | 877.33 ms |
| p95 latency | 1.04 s |
| Maximum latency | 2.6 s |
| Maximum active virtual users | 1,000 / 1,000 |
| Successful redirects | 120,469 |
| Persisted analytics rows | 120,468 |
| Post-test health | `UP` |

The error-rate and check thresholds passed, but the 500 ms p95 threshold failed. k6 exhausted its
1,000-VU ceiling near 2,300 requested requests/second. The Function continued accepting work during
the 3,000 target plateau, but completed only about 1,500-1,800 requests/second while the backlog grew.
All six failed requests were caused by inability to obtain a JDBC connection. One successful redirect
also lost its best-effort analytics insert.

## Interpretation and risks

- This is a local regression baseline, not an Azure capacity guarantee. Azure PostgreSQL latency,
  Function cold starts, network paths, and scale-out behavior are not represented.
- Synchronous analytics preserves functionality without an always-on Redis service, but every redirect
  consumes an extra database write. A queue-backed analytics path should be evaluated before sustained
  production traffic.
- The current single-worker, synchronous PostgreSQL design does not meet 3,000 requests/second. Meeting
  that target requires Function scale-out plus removal of database work from the redirect hot path:
  cache link resolution and enqueue analytics to a durable asynchronous service such as Azure Service Bus.
- Each scaled Function worker owns its own JDBC pool. Pool size multiplied by Function instance count
  must remain below the PostgreSQL connection budget. Start with `DB_POOL_SIZE=5` and a conservative
  Function maximum-instance limit.
- The local Functions host emitted per-invocation logs, producing substantial log volume during the
  run. Configure Application Insights sampling and retention before cloud stress testing to control cost.
- A subsequent cloud test should separately measure cold-start latency, warm steady state, 429/5xx
  behavior, database CPU/connections, and recovery after database throttling.
