import { Activity, AlertTriangle, CheckCircle2, Gauge } from 'lucide-react';

const baseline = [
  ['Completed redirects', '12,012'],
  ['HTTP failures', '0'],
  ['Successful checks', '100%'],
  ['Average latency', '12.34 ms'],
  ['p95 latency', '23.92 ms'],
  ['Analytics persisted', '12,012 / 12,012'],
];

const capacity = [
  ['Completed requests', '120,475'],
  ['Achieved throughput', '1,095.25 RPS overall'],
  ['Dropped iterations', '51,024'],
  ['HTTP failures', '6'],
  ['p95 latency', '1.04 s'],
  ['Peak virtual users', '1,000 / 1,000'],
];

function MetricTable({ rows }: { rows: string[][] }) {
  return <dl className="mt-5 divide-y divide-slate-200">{rows.map(([label, value]) => <div key={label} className="flex items-center justify-between gap-4 py-3 text-sm">
    <dt className="text-slate-500">{label}</dt><dd className="text-right font-bold tabular-nums text-slate-950">{value}</dd>
  </div>)}</dl>;
}

export default function PerformanceEvidence() {
  return <section className="mt-14" aria-labelledby="performance-heading">
    <div className="max-w-3xl">
      <p className="text-xs font-bold uppercase tracking-[0.2em] text-indigo-600">Measured performance evidence</p>
      <h2 id="performance-heading" className="mt-2 text-3xl font-bold text-slate-950">The 3,000 RPS run found the boundary—it did not pass.</h2>
      <p className="mt-4 leading-7 text-slate-600">These k6 results came from the packaged Java Azure Functions runtime running locally against PostgreSQL 17. Redirect following was disabled. They are a regression and capacity-design input, not a claim about deployed Azure Container Apps capacity.</p>
    </div>

    <div className="mt-7 grid gap-5 lg:grid-cols-2">
      <article className="rounded-3xl border border-emerald-200 bg-emerald-50/50 p-6">
        <div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-emerald-100 text-emerald-700"><CheckCircle2 className="h-5 w-5" /></span><div><p className="text-xs font-bold uppercase tracking-wider text-emerald-700">Passed baseline</p><h3 className="text-xl font-bold text-slate-950">300 requested RPS</h3></div></div>
        <MetricTable rows={baseline} />
        <p className="mt-4 rounded-xl bg-white p-3 text-sm leading-6 text-emerald-900">All thresholds passed: error rate below 1%, checks above 99%, p95 below 500 ms, and complete analytics persistence.</p>
      </article>
      <article className="rounded-3xl border border-amber-200 bg-amber-50/50 p-6">
        <div className="flex items-center gap-3"><span className="grid h-10 w-10 place-items-center rounded-xl bg-amber-100 text-amber-700"><AlertTriangle className="h-5 w-5" /></span><div><p className="text-xs font-bold uppercase tracking-wider text-amber-700">Capacity limit exposed</p><h3 className="text-xl font-bold text-slate-950">3,000 requested RPS</h3></div></div>
        <MetricTable rows={capacity} />
        <p className="mt-4 rounded-xl bg-white p-3 text-sm leading-6 text-amber-900">The 500 ms p95 threshold failed. k6 exhausted 1,000 VUs near 2,300 requested RPS; database connection acquisition caused all six HTTP failures.</p>
      </article>
    </div>

    <div className="mt-5 grid gap-4 md:grid-cols-3">
      {[
        [Gauge, 'Hot-path change', 'Resolve through Redis and keep analytics writes off the redirect response path.'],
        [Activity, 'Controlled scale-out', 'Budget max replicas × JDBC pool size below the PostgreSQL connection ceiling.'],
        [AlertTriangle, 'Next validation', 'Repeat the same profile in Azure while observing cold starts, 429/5xx, DB CPU/connections, queue lag, and recovery.'],
      ].map(([Icon, title, body]) => {
        const ItemIcon = Icon as typeof Gauge;
        return <article key={title as string} className="rounded-2xl border border-slate-200 bg-white p-5"><ItemIcon className="h-5 w-5 text-indigo-600" /><h3 className="mt-4 font-bold text-slate-950">{title as string}</h3><p className="mt-2 text-sm leading-6 text-slate-600">{body as string}</p></article>;
      })}
    </div>
  </section>;
}
