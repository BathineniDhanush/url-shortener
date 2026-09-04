import { ArrowRight, Boxes, Database, RadioTower } from 'lucide-react';
import { Link } from 'react-router-dom';
import LiveDemo from '../components/features/LiveDemo';

const stack = ['Java 17', 'Spring Boot', 'PostgreSQL', 'Redis', 'Redis Streams', 'Docker', 'Azure Container Apps', 'React', 'TypeScript'];

export default function Home() {
  return <>
    <section className="mx-auto max-w-6xl px-4 pb-8 pt-10 sm:px-6 sm:pt-14">
      <div className="grid items-end gap-10 lg:grid-cols-[1fr_360px]">
        <div>
          <div className="mb-5 inline-flex items-center gap-2 rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1.5 text-xs font-bold uppercase tracking-wider text-emerald-700">
            <RadioTower className="h-3.5 w-3.5" /> Engineering prototype
          </div>
          <h1 className="max-w-4xl text-4xl font-black leading-[1.02] tracking-[-0.045em] text-slate-950 sm:text-5xl lg:text-6xl">
            Short links fast.<br /><span className="text-indigo-600">Analytics asynchronously.</span>
          </h1>
          <p className="mt-4 max-w-3xl text-base leading-7 text-slate-600">A Spring Boot URL shortener using PostgreSQL as the system of record, Redis for redirect caching and event streaming, and a dedicated worker for click analytics.</p>
          <div className="mt-5 flex flex-wrap gap-3">
            <a href="#live-demo" className="inline-flex items-center gap-2 rounded-xl bg-slate-950 px-5 py-3 font-semibold text-white hover:bg-indigo-700">Try the live API <ArrowRight className="h-4 w-4" /></a>
            <Link to="/architecture" className="inline-flex items-center gap-2 rounded-xl border border-slate-300 bg-white px-5 py-3 font-semibold text-slate-700 hover:border-indigo-300 hover:text-indigo-700"><Boxes className="h-4 w-4" /> Explore architecture</Link>
          </div>
        </div>
        <aside className="rounded-2xl border border-slate-800 bg-slate-950 p-5 text-slate-100 shadow-xl">
          <div className="mb-5 flex items-center gap-3"><Database className="h-5 w-5 text-indigo-400" /><p className="font-semibold">Runtime in one glance</p></div>
          <ol className="space-y-4 text-sm text-slate-300">
            {['API resolves through Redis cache', 'Redirect returns immediately', 'Click enters a Redis Stream', 'Worker persists analytics'].map((item, index) => <li key={item}><span className="mr-3 text-indigo-400">0{index + 1}</span>{item}</li>)}
          </ol>
        </aside>
      </div>
      <div className="mt-8 flex flex-wrap gap-2 border-t border-slate-200 pt-4">{stack.map(item => <span key={item} className="rounded-md bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600">{item}</span>)}</div>
    </section>
    <LiveDemo />
  </>;
}
