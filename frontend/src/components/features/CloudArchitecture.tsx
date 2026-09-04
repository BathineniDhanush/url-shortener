import { Cloud, Container, Database, GitBranch, Globe2, RadioTower, ShieldCheck } from 'lucide-react';

const cloudNodes = [
  [Globe2, 'Azure Static Web Apps', 'React/Vite SPA', 'Serves the live demo and engineering review. VITE_API_BASE_URL binds the browser to the public API origin at build time.'],
  [Container, 'url-api', 'Azure Container App · public ingress', 'Spring Boot API handles ownership-protected CRUD, cache-aware redirects, validation, CORS, Swagger UI, metrics, and event publication.'],
  [RadioTower, 'Redis', 'Internal Container App', 'Caches short-code resolution and provides the Redis Stream that decouples click capture from analytics persistence.'],
  [Container, 'url-worker', 'Azure Container App · no public ingress', 'Runs the worker Spring profile, claims stream messages, retries failures, dead-letters poison events, and writes idempotently.'],
  [Database, 'PostgreSQL', 'Internal Container App · system of record', 'Stores links, owner-token hashes, optimistic versions, click events, and Flyway schema history.'],
];

export default function CloudArchitecture() {
  return <section className="mt-14" aria-labelledby="cloud-heading">
    <div className="max-w-3xl">
      <p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Deployed cloud architecture</p>
      <h2 id="cloud-heading" className="mt-2 text-3xl font-bold text-slate-950">One environment, two independently operated runtimes.</h2>
      <p className="mt-4 leading-7 text-slate-600">The API and analytics worker use the same immutable image from GHCR but different Spring runtime roles. Only the API has public ingress; data services and the worker remain internal to the Container Apps environment.</p>
    </div>

    <div className="mt-7 grid gap-4 lg:grid-cols-5">
      {cloudNodes.map(([Icon, title, subtitle, body], index) => {
        const NodeIcon = Icon as typeof Cloud;
        return <article key={title as string} className="relative rounded-2xl border border-slate-200 bg-white p-5">
          <span className="grid h-10 w-10 place-items-center rounded-xl bg-indigo-50 text-indigo-600"><NodeIcon className="h-5 w-5" /></span>
          <p className="mt-4 font-bold text-slate-950">{title as string}</p><p className="mt-1 text-xs font-semibold text-indigo-600">{subtitle as string}</p><p className="mt-3 text-sm leading-6 text-slate-600">{body as string}</p>
          {index < cloudNodes.length - 1 && <span className="absolute -right-3 top-9 z-10 hidden h-px w-6 bg-indigo-300 lg:block" />}
        </article>;
      })}
    </div>

    <div className="mt-5 grid gap-5 lg:grid-cols-2">
      <article className="rounded-2xl bg-slate-950 p-6 text-slate-100"><div className="flex items-center gap-2"><GitBranch className="h-5 w-5 text-indigo-400" /><h3 className="font-bold">Supply chain and rollout</h3></div><p className="mt-4 leading-7 text-slate-300">Git push → Maven and frontend verification → container build → Trivy gate → GHCR image with provenance and SBOM → explicit Azure CLI revision update. SHA tags support audit and rollback; health is checked before the worker rollout.</p></article>
      <article className="rounded-2xl border border-slate-200 bg-white p-6"><div className="flex items-center gap-2"><ShieldCheck className="h-5 w-5 text-indigo-600" /><h3 className="font-bold text-slate-950">Boundaries and controls</h3></div><p className="mt-4 leading-7 text-slate-600">Exact-origin CORS, private destination blocking, hashed owner capability tokens, optimistic concurrency, rate limiting, internal data endpoints, graceful shutdown, health probes, and bounded connection pools protect the prototype. Production still needs managed data services, identity, alert rules, and a measured cloud capacity envelope.</p></article>
    </div>
  </section>;
}
