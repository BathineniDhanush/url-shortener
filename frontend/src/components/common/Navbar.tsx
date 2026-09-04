import { Boxes, ClipboardCheck, Link2 } from 'lucide-react';
import { Link, NavLink } from 'react-router-dom';

export default function Navbar() {
  const navClass = ({ isActive }: { isActive: boolean }) => `rounded-lg px-3 py-2 transition ${isActive ? 'bg-slate-100 text-slate-950' : 'hover:text-indigo-700'}`;
  return <nav className="sticky top-0 z-30 border-b border-slate-200/80 bg-white/90 px-4 py-3 backdrop-blur-lg">
    <div className="mx-auto flex max-w-6xl items-center justify-between">
      <Link to="/" className="flex items-center gap-2 text-lg font-black tracking-tight text-slate-950">
        <span className="grid h-9 w-9 place-items-center rounded-xl bg-indigo-600 text-white"><Link2 className="h-5 w-5" /></span>
        <span>Shortstack</span>
      </Link>
      <div className="flex items-center gap-1 text-sm font-semibold text-slate-600">
        <NavLink to="/" end className={navClass}>Live demo</NavLink>
        <NavLink to="/architecture" className={navClass}><span className="inline-flex items-center gap-1.5"><Boxes className="h-4 w-4" /><span className="hidden sm:inline">Architecture</span></span></NavLink>
        <NavLink to="/engineering-review" className={navClass}><span className="inline-flex items-center gap-1.5"><ClipboardCheck className="h-4 w-4" /><span className="hidden sm:inline">Review</span></span></NavLink>
        <NavLink to="/manage" className={({ isActive }) => `${navClass({ isActive })} hidden sm:block`}>Manage</NavLink>
      </div>
    </div>
  </nav>;
}
