import { AlertTriangle, CheckCircle2, ClipboardCheck, GitBranch, UserCheck } from 'lucide-react';

const scenarios = [
  {
    kind: 'Greenfield',
    title: 'Core short-link service',
    intent: 'Create a runnable Spring Boot service with durable links, redirects, analytics, and an executable API contract.',
    decomposition: 'Domain policies → PostgreSQL schema → create/resolve APIs → Docker Compose → contract and integration tests.',
    execution: 'Implemented secure random codes, destination validation, JDBC repositories, Flyway, RFC 9457 errors, and 302 redirects.',
    validation: 'Unit tests plus PostgreSQL Testcontainers cover creation, alias conflicts, expiration, unsafe destinations, and redirects.',
  },
  {
    kind: 'Brownfield',
    title: 'Caching, asynchronous analytics, ownership, and deletion',
    intent: 'Improve latency and reliability without breaking the existing API/data flow.',
    decomposition: 'Map redirect hot path → add Redis cache → publish click stream → add worker → protect management → migrate deletion semantics.',
    execution: 'Added read-through caching, Redis Streams, retry/dead-letter handling, idempotent writes, owner-token hashes, optimistic versions, and Flyway V4 cascade deletion.',
    validation: 'Redis/PostgreSQL integration tests cover cache failures, pending retries across restarts, poison records, concurrency, authorization, and delayed events after deletion.',
  },
  {
    kind: 'Ambiguous',
    title: '“Scale to 3,000 RPS” and choose cloud hosting',
    intent: 'Turn an undefined scale request into a measurable target while separating prototype evidence from production claims.',
    decomposition: 'Define k6 stages and thresholds → test baseline → find saturation → identify bottleneck → compare Functions and Container Apps → deploy and smoke-test.',
    execution: 'The synchronous Function experiment was retained as evidence; the deployed design uses separate API/worker Container Apps, Redis caching/streaming, PostgreSQL, GHCR, and Static Web Apps.',
    validation: '300 RPS passed. The 3,000 requested-RPS run failed its latency/scheduling target and exposed JDBC pressure; cloud 3,000 RPS validation remains explicitly open.',
  },
];

const requirements = [
  ['1. Requirement understanding', 'Normalized the assignment into a secure, measurable URL-shortening system: owned CRUD, low-latency redirects, durable asynchronous click counting, deployment evidence, and explicit capacity limits. Ambiguities were recorded instead of silently assumed.'],
  ['2. Task decomposition', 'Sequenced vertical slices by dependency: skeleton → persistence/API → cache and stream → worker → ownership/management → deletion → quality gates → cloud rollout → reviewer SPA.'],
  ['3. Brownfield codebase reasoning', 'Changes traced controllers, application services, repositories, Flyway schemas, Redis cache/stream flows, worker retry state, OpenAPI, frontend clients, tests, and deployment profiles before implementation.'],
  ['4. AI-assisted execution', 'AI accelerated scaffolding, code alternatives, debugging, test generation, documentation, UI composition, and review preparation. Prompts carried intent, constraints, acceptance criteria, and context; outputs were inspected, edited, rejected, and passed through human-controlled gates.'],
  ['5. Engineering output generation', 'Delivered modular Spring Boot code, React SPA, OpenAPI 3.1, Swagger UI, Flyway migrations, Docker/Compose, unit and integration tests, load scripts, CI/CD, runbooks, and deployment memory.'],
  ['6. Validation and risk control', 'Applied lint, compilation, Maven verification, >90% coverage gate, Testcontainers, Trivy, load tests, health/CORS checks, immutable image tags, revision rollouts, and rollback-friendly database migrations.'],
  ['7. Controlled oversight', 'The engineer selected architecture, approved credentials and cloud mutations, reviewed results, and retained responsibility for correctness. High-impact pushes, deployments, schema changes, and destructive cleanup required explicit control.'],
  ['8. Final engineering summary', 'This page and README record the plan, rationale, artifacts, scenarios, decisions, risks, trade-offs, validation evidence, assumptions, limitations, and remaining production work.'],
];

export default function EngineeringReview() {
  return <main className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-20">
    <header className="max-w-4xl">
      <p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Final engineering review</p>
      <h1 className="mt-3 text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">Requirement → evidence → reviewable outcome.</h1>
      <p className="mt-5 text-lg leading-8 text-slate-600">A working URL shortener was built and improved through engineer-led, AI-accelerated execution. AI assisted within bounded tasks; the engineer owned architecture, validation, security, deployment, and sign-off.</p>
      <div className="mt-6 flex flex-wrap gap-2">{['Runnable end-to-end', '49 backend tests', '>90% coverage gate', 'Trivy gated', 'OpenAPI + Swagger UI', 'Azure deployed'].map(item => <span key={item} className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold text-emerald-800">{item}</span>)}</div>
    </header>

    <section className="mt-14 grid gap-5 lg:grid-cols-2">
      <article className="rounded-3xl bg-slate-950 p-7 text-white"><ClipboardCheck className="h-6 w-6 text-indigo-400" /><h2 className="mt-5 text-2xl font-bold">Objective and normalized problem</h2><p className="mt-4 leading-7 text-slate-300">Produce a reviewable engineering outcome in a 2–3 day prototype window: a secure URL-shortening API with owner-controlled lifecycle, fast redirects, eventually consistent analytics, failure isolation, measurable quality, reproducible setup, and deployable artifacts.</p></article>
      <article className="rounded-3xl border border-slate-200 bg-white p-7"><GitBranch className="h-6 w-6 text-indigo-600" /><h2 className="mt-5 text-2xl font-bold text-slate-950">Scenario and scope</h2><p className="mt-4 leading-7 text-slate-600">The work began greenfield, then deliberately exercised brownfield enhancements, refactors, bug fixes, testing/documentation improvements, and ambiguous requirements. The result includes both new-system design and evidence of safe change management.</p></article>
    </section>

    <section className="mt-14" aria-labelledby="scenarios-heading"><p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Required scenarios</p><h2 id="scenarios-heading" className="mt-2 text-3xl font-bold text-slate-950">Decomposition, execution, and validation</h2><div className="mt-7 grid gap-5 lg:grid-cols-3">{scenarios.map(scenario => <article key={scenario.kind} className="rounded-3xl border border-slate-200 bg-white p-6"><span className="rounded-full bg-indigo-50 px-3 py-1 text-xs font-bold uppercase tracking-wider text-indigo-700">{scenario.kind}</span><h3 className="mt-4 text-xl font-bold text-slate-950">{scenario.title}</h3>{[['Intent', scenario.intent], ['Decomposition', scenario.decomposition], ['Execution', scenario.execution], ['Validation', scenario.validation]].map(([label, body]) => <div key={label} className="mt-5"><p className="text-xs font-bold uppercase tracking-wider text-slate-400">{label}</p><p className="mt-1 text-sm leading-6 text-slate-600">{body}</p></div>)}</article>)}</div></section>

    <section className="mt-14" aria-labelledby="requirements-heading"><p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Core requirements</p><h2 id="requirements-heading" className="mt-2 text-3xl font-bold text-slate-950">Requirement-to-evidence map</h2><div className="mt-7 grid gap-4 md:grid-cols-2">{requirements.map(([title, body]) => <article key={title} className="rounded-2xl border border-slate-200 bg-white p-5"><div className="flex gap-3"><CheckCircle2 className="mt-0.5 h-5 w-5 shrink-0 text-emerald-600" /><div><h3 className="font-bold text-slate-950">{title}</h3><p className="mt-2 text-sm leading-6 text-slate-600">{body}</p></div></div></article>)}</div></section>

    <section className="mt-14 grid gap-5 lg:grid-cols-2">
      <article className="rounded-3xl border border-amber-200 bg-amber-50 p-7"><div className="flex items-center gap-3"><AlertTriangle className="h-6 w-6 text-amber-700" /><h2 className="text-2xl font-bold text-slate-950">Risks, assumptions, and limitations</h2></div><ul className="mt-5 space-y-3 text-sm leading-6 text-amber-950"><li>Cloud capacity at 3,000 RPS is not yet proven.</li><li>Capability tokens lack user identity, recovery, and rotation.</li><li>Current PostgreSQL and Redis are internal prototype containers, not managed HA services.</li><li>DNS rebinding requires network egress controls if server-side fetching is ever added.</li><li>Analytics need retention policy, alerting, queue-lag SLOs, and operator rehearsal.</li><li>Multi-region consistency and disaster recovery remain out of scope.</li></ul></article>
      <article className="rounded-3xl bg-slate-950 p-7 text-white"><div className="flex items-center gap-3"><UserCheck className="h-6 w-6 text-emerald-400" /><h2 className="text-2xl font-bold">Final engineering summary and sign-off</h2></div><p className="mt-5 leading-7 text-slate-300">The prototype meets its review objective: it is runnable, documented, testable, secured proportionally, and deployed with traceable artifacts. Its architecture deliberately separates redirect latency from analytics durability. The evidence supports production-oriented design quality, not unrestricted production readiness.</p><p className="mt-4 leading-7 text-slate-300">Human sign-off is still required before identity changes, destructive migrations, managed-service provisioning, automated production rollout, connection/scale-budget changes, or a declared 3,000 RPS SLO. The engineer remains accountable for every release.</p></article>
    </section>
  </main>;
}
