import { useState } from 'react';
import { ArrowDown, ArrowRight, Box, Braces, Database, ExternalLink, FileJson, GitBranch, Layers3, RadioTower, ServerCog } from 'lucide-react';
import { API_BASE_URL } from '../api/client';
import CloudArchitecture from '../components/features/CloudArchitecture';
import PerformanceEvidence from '../components/features/PerformanceEvidence';

type NodeKey = 'spa' | 'api' | 'redis' | 'worker' | 'postgres';

const nodes: Record<NodeKey, { title: string; subtitle: string; detail: string; icon: typeof Box }> = {
  spa: { title: 'React SPA', subtitle: 'Presentation', detail: 'Live API client and interactive architecture showcase. It displays backend responses without fabricating infrastructure state.', icon: Braces },
  api: { title: 'Spring Boot API', subtitle: 'profile=api', detail: 'Owns link CRUD, validation, authorization, cache-aware redirects, and event publication on the synchronous request path.', icon: ServerCog },
  redis: { title: 'Redis', subtitle: 'Cache + Streams', detail: 'Keeps repeated redirects off PostgreSQL and buffers privacy-filtered click events in a Redis Stream.', icon: RadioTower },
  worker: { title: 'url-worker', subtitle: 'profile=worker', detail: 'Consumes click events with at-least-once delivery and idempotently persists analytics using the same application image.', icon: GitBranch },
  postgres: { title: 'PostgreSQL', subtitle: 'System of record', detail: 'Durably stores links, ownership digests, versions, analytics, and Flyway schema history.', icon: Database },
};

const createFlow = ['Client', 'POST /api/v1/links', 'Validate destination', 'Generate short code', 'Generate owner token', 'SHA-256 digest', 'PostgreSQL', '201 response'];
const redirectFlow = ['GET /{code}', 'Spring Boot API', 'Redis cache lookup', '302 redirect'];
const asyncFlow = ['Publish click event', 'Redis Stream', 'url-worker', 'PostgreSQL analytics'];

function ArchitectureNode({ id, selected, onSelect }: { id: NodeKey; selected: boolean; onSelect: () => void }) {
  const node = nodes[id];
  const Icon = node.icon;
  return <button onClick={onSelect} aria-pressed={selected} className={`min-w-[180px] rounded-2xl border p-4 text-left transition focus:outline-none focus:ring-4 focus:ring-indigo-100 ${selected ? 'border-indigo-500 bg-indigo-50 shadow-md' : 'border-slate-200 bg-white hover:border-indigo-300'}`}>
    <Icon className={`mb-3 h-5 w-5 ${selected ? 'text-indigo-600' : 'text-slate-500'}`} />
    <span className="block font-bold text-slate-900">{node.title}</span>
    <span className="mt-1 block text-xs font-medium text-slate-500">{node.subtitle}</span>
  </button>;
}

function Flow({ items, tone = 'indigo' }: { items: string[]; tone?: 'indigo' | 'amber' }) {
  return <div className="flex min-w-max items-center gap-2 py-2">{items.map((item, index) => <div key={item} className="flex items-center gap-2">
    <span className={`rounded-lg border px-3 py-2 text-sm font-semibold ${tone === 'amber' ? 'border-amber-200 bg-amber-50 text-amber-900' : 'border-indigo-200 bg-indigo-50 text-indigo-900'}`}>{item}</span>
    {index < items.length - 1 && <ArrowRight className={`h-4 w-4 ${tone === 'amber' ? 'text-amber-400' : 'text-indigo-400'}`} />}
  </div>)}</div>;
}

export default function Architecture() {
  const [selected, setSelected] = useState<NodeKey>('api');
  const [flow, setFlow] = useState<'create' | 'redirect'>('redirect');
  const detail = nodes[selected];
  return <main className="mx-auto max-w-6xl px-4 py-14 sm:px-6 sm:py-20">
    <header className="mb-12 max-w-3xl">
      <p className="mb-2 text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Architecture explanation</p>
      <h1 className="text-4xl font-black tracking-tight text-slate-950 sm:text-5xl">Follow every request through the system.</h1>
      <p className="mt-5 text-lg leading-8 text-slate-600">The redirect path stays fast because user response delivery and analytics persistence are deliberately decoupled.</p>
    </header>

    <section aria-labelledby="runtime-heading" className="rounded-3xl border border-slate-200 bg-slate-50 p-5 sm:p-8">
      <h2 id="runtime-heading" className="mb-6 text-xl font-bold text-slate-950">Runtime topology</h2>
      <div className="overflow-x-auto pb-3">
        <div className="mx-auto flex min-w-[1120px] items-center justify-center gap-3">
          <ArchitectureNode id="spa" selected={selected === 'spa'} onSelect={() => setSelected('spa')} /><ArrowRight className="text-slate-400" />
          <ArchitectureNode id="api" selected={selected === 'api'} onSelect={() => setSelected('api')} /><ArrowRight className="text-slate-400" />
          <ArchitectureNode id="redis" selected={selected === 'redis'} onSelect={() => setSelected('redis')} />
          <div className="flex flex-col items-center gap-1 text-indigo-500"><ArrowRight /><span className="text-[10px] font-bold uppercase tracking-wider">Redis Stream</span></div>
          <ArchitectureNode id="worker" selected={selected === 'worker'} onSelect={() => setSelected('worker')} /><ArrowRight className="text-slate-400" />
          <ArchitectureNode id="postgres" selected={selected === 'postgres'} onSelect={() => setSelected('postgres')} />
        </div>
      </div>
      <p className="mt-2 rounded-xl border border-dashed border-slate-300 bg-white px-4 py-3 text-sm text-slate-600"><strong className="text-slate-900">Direct data path:</strong> the API also reads and writes PostgreSQL for link CRUD and Redis cache misses.</p>
      <div className="mt-5 rounded-2xl border border-indigo-100 bg-white p-5" aria-live="polite">
        <p className="text-xs font-bold uppercase tracking-wider text-indigo-600">{detail.subtitle}</p>
        <h3 className="mt-1 text-lg font-bold text-slate-950">{detail.title}</h3>
        <p className="mt-2 max-w-3xl leading-7 text-slate-600">{detail.detail}</p>
      </div>
    </section>

    <CloudArchitecture />

    <PerformanceEvidence />

    <section className="mt-12" aria-labelledby="flow-heading">
      <div className="mb-5 flex flex-wrap items-end justify-between gap-4">
        <div><p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Interactive trace</p><h2 id="flow-heading" className="mt-2 text-3xl font-bold text-slate-950">Request flow explorer</h2></div>
        <div className="flex rounded-xl bg-slate-100 p-1">
          <button onClick={() => setFlow('create')} className={`rounded-lg px-4 py-2 text-sm font-semibold ${flow === 'create' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`}>Create link</button>
          <button onClick={() => setFlow('redirect')} className={`rounded-lg px-4 py-2 text-sm font-semibold ${flow === 'redirect' ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500'}`}>Redirect + analytics</button>
        </div>
      </div>
      <div className="overflow-x-auto rounded-2xl border border-slate-200 bg-white p-5">
        {flow === 'create' ? <Flow items={createFlow} /> : <>
          <p className="mb-2 text-xs font-bold uppercase tracking-wider text-indigo-600">Synchronous user path</p><Flow items={redirectFlow} />
          <div className="my-2 flex items-center gap-2 pl-4 text-sm font-semibold text-slate-500"><ArrowDown className="h-4 w-4" /> emitted without delaying the redirect</div>
          <p className="mb-2 text-xs font-bold uppercase tracking-wider text-amber-600">Asynchronous analytics path</p><Flow items={asyncFlow} tone="amber" />
        </>}
      </div>
    </section>

    <section className="mt-12 grid gap-4 md:grid-cols-2" aria-label="Design decisions">
      {[
        ['Why PostgreSQL?', 'Durable authority for relational link and analytics data, ownership, transactions, and Flyway-managed evolution.'],
        ['Why Redis?', 'A low-latency redirect cache plus Redis Streams event transport reduces database load and buffers traffic spikes.'],
        ['Why async analytics?', 'Redirect latency is isolated from analytics writes; the worker can scale and recover independently.'],
        ['Why one application image?', 'One build and version, deployed as url-api and url-worker with separate Spring profiles and scaling responsibilities.'],
        ['Why Azure Container Apps?', 'It directly hosts containerized Spring Boot and the long-running Redis Stream worker while letting API and worker scale independently. Azure Functions remains an alternative if the runtime architecture is redesigned around HTTP and event triggers.'],
      ].map(([title, body]) => <article key={title} className="rounded-2xl border border-slate-200 bg-white p-6"><h3 className="font-bold text-slate-950">{title}</h3><p className="mt-2 leading-7 text-slate-600">{body}</p></article>)}
    </section>

    <section className="mt-12 grid gap-6 lg:grid-cols-2">
      <article className="rounded-2xl bg-slate-950 p-6 text-slate-100">
        <div className="flex items-center gap-2"><Layers3 className="h-5 w-5 text-indigo-400" /><h2 className="font-bold">Flyway migration path</h2></div>
        <Flow items={['Startup', 'schema history', 'pending migrations', 'application ready']} />
        <ul className="mt-4 space-y-2 text-sm text-slate-300"><li>V1 — create links</li><li>V2 — create analytics</li><li>V3 — ownership and version</li><li>V4 — cascade link deletion</li></ul>
      </article>
      <article className="rounded-2xl border border-slate-200 bg-white p-6">
        <div className="flex items-center gap-2"><Box className="h-5 w-5 text-indigo-600" /><h2 className="font-bold text-slate-950">Delivery path</h2></div>
        <p className="mt-4 leading-7 text-slate-600">GitHub push → Maven verify → Function package → Docker build → Trivy scan → GHCR publish with provenance and SBOM.</p>
        <p className="mt-4 rounded-xl bg-amber-50 p-3 text-sm font-medium text-amber-800">Azure rollout is protected and manual: an engineer selects the immutable SHA image, updates Container App revisions with Azure CLI, then verifies health before continuing.</p>
      </article>
    </section>

    <section className="mt-12 rounded-3xl border border-indigo-200 bg-indigo-50 p-6 sm:p-8">
      <div className="flex items-center gap-3"><FileJson className="h-6 w-6 text-indigo-700" /><h2 className="text-2xl font-bold text-slate-950">Executable API documentation</h2></div>
      <p className="mt-4 max-w-3xl leading-7 text-slate-700">The backend serves the committed OpenAPI 3.1 contract and renders it through Swagger UI. The relative server URL makes “Try it out” target the same API origin in local and deployed environments.</p>
      <div className="mt-5 flex flex-wrap gap-3">
        <a href={`${API_BASE_URL}/swagger-ui.html`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-xl bg-indigo-700 px-4 py-2.5 text-sm font-bold text-white hover:bg-indigo-800">Open Swagger UI <ExternalLink className="h-4 w-4" /></a>
        <a href={`${API_BASE_URL}/openapi.yaml`} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-xl border border-indigo-300 bg-white px-4 py-2.5 text-sm font-bold text-indigo-800 hover:bg-indigo-100">View OpenAPI YAML <ExternalLink className="h-4 w-4" /></a>
      </div>
    </section>
  </main>;
}
