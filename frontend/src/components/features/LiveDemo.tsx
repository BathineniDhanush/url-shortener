/* oxlint-disable react/set-state-in-effect -- the effect probes the remote API on mount */
import { useEffect, useState } from 'react';
import { Activity, Check, Copy, ExternalLink, LoaderCircle, MousePointerClick, RefreshCw, RotateCcw } from 'lucide-react';
import { API_BASE_URL } from '../../api/client';
import { apiErrorMessage } from '../../api/errors';
import { linkService } from '../../api/linkService';
import type { AnalyticsResponse, CreateLinkRequest, LinkResponse } from '../../types';

type HealthState = 'checking' | 'healthy' | 'unavailable';

export default function LiveDemo() {
  const [health, setHealth] = useState<HealthState>('checking');
  const [request, setRequest] = useState<CreateLinkRequest>({
    destinationUrl: 'https://example.com/docs',
    customAlias: '',
    expiresAt: '',
  });
  const [created, setCreated] = useState<LinkResponse | null>(null);
  const [analytics, setAnalytics] = useState<AnalyticsResponse | null>(null);
  const [creating, setCreating] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState<string | null>(null);

  const checkHealth = async () => {
    setHealth('checking');
    try {
      const response = await linkService.getSystemInfo();
      setHealth(response.runtimeRole === 'API' ? 'healthy' : 'unavailable');
    } catch {
      setHealth('unavailable');
    }
  };

  useEffect(() => {
    void checkHealth();
  }, []);

  const createLink = async (event: React.FormEvent) => {
    event.preventDefault();
    setCreating(true);
    setError(null);
    setAnalytics(null);
    try {
      const result = await linkService.createLink({
        destinationUrl: request.destinationUrl.trim(),
        customAlias: request.customAlias?.trim() || null,
        expiresAt: request.expiresAt ? new Date(request.expiresAt).toISOString() : null,
      });
      if (!result.code || !result.shortUrl || !result.ownerToken) {
        throw new Error('The API response is missing the short URL or one-time owner token.');
      }
      setCreated(result);
    } catch (caught) {
      setError(apiErrorMessage(caught, caught instanceof Error ? caught.message : 'Unable to create the short link.'));
    } finally {
      setCreating(false);
    }
  };

  const refreshAnalytics = async () => {
    if (!created?.ownerToken) return;
    setRefreshing(true);
    setError(null);
    try {
      setAnalytics(await linkService.getLinkAnalytics(created.code, created.ownerToken));
    } catch (caught) {
      setError(apiErrorMessage(caught, 'Unable to refresh analytics.'));
    } finally {
      setRefreshing(false);
    }
  };

  const copy = async (value: string, label: string) => {
    await navigator.clipboard.writeText(value);
    setCopied(label);
    window.setTimeout(() => setCopied(null), 1600);
  };

  const reset = () => {
    setCreated(null);
    setAnalytics(null);
    setError(null);
  };

  return (
    <section id="live-demo" className="mx-auto max-w-6xl px-4 pb-20 sm:px-6">
      <div className="mb-6 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="mb-2 text-xs font-semibold uppercase tracking-[0.2em] text-indigo-600">Live data</p>
          <h2 className="text-3xl font-bold tracking-tight text-slate-950 sm:text-4xl">Run the complete story</h2>
          <p className="mt-2 max-w-2xl text-slate-600">Create a real link, generate a redirect, then watch the worker persist its click asynchronously.</p>
        </div>
        <button onClick={checkHealth} className="inline-flex items-center gap-2 self-start rounded-full border border-slate-200 bg-white px-3 py-2 text-sm font-medium text-slate-700 shadow-sm transition hover:border-slate-300 focus:outline-none focus:ring-2 focus:ring-indigo-500">
          <span className={`h-2.5 w-2.5 rounded-full ${health === 'healthy' ? 'bg-emerald-500' : health === 'checking' ? 'animate-pulse bg-amber-400' : 'bg-rose-500'}`} />
          API {health === 'checking' ? 'checking…' : health}
          <RefreshCw className="h-3.5 w-3.5" />
        </button>
      </div>

      <div className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-200/50">
        <div className="grid border-b border-slate-200 bg-slate-50 sm:grid-cols-3">
          {['Create a short URL', 'Generate clicks', 'Refresh analytics'].map((label, index) => {
            const complete = index === 0 ? Boolean(created) : index === 1 ? Boolean(analytics) : Boolean(analytics);
            return <div key={label} className="flex items-center gap-3 border-b border-slate-200 px-5 py-4 last:border-0 sm:border-b-0 sm:border-r">
              <span className={`grid h-7 w-7 place-items-center rounded-full text-xs font-bold ${complete ? 'bg-emerald-500 text-white' : 'bg-slate-200 text-slate-600'}`}>{complete ? <Check className="h-4 w-4" /> : index + 1}</span>
              <span className="text-sm font-semibold text-slate-700">{label}</span>
            </div>;
          })}
        </div>

        <div className="grid lg:grid-cols-[1.05fr_.95fr]">
          <form onSubmit={createLink} className="space-y-5 border-b border-slate-200 p-6 sm:p-8 lg:border-b-0 lg:border-r">
            <div>
              <label htmlFor="destination" className="mb-2 block text-sm font-semibold text-slate-800">Destination URL</label>
              <input id="destination" type="url" required value={request.destinationUrl} onChange={(event) => setRequest({ ...request, destinationUrl: event.target.value })} className="w-full rounded-xl border border-slate-300 px-4 py-3 text-slate-950 outline-none transition placeholder:text-slate-400 focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100" />
            </div>
            <div className="grid gap-4 sm:grid-cols-2">
              <div>
                <label htmlFor="alias" className="mb-2 block text-sm font-semibold text-slate-800">Custom alias <span className="font-normal text-slate-400">optional</span></label>
                <input id="alias" value={request.customAlias || ''} onChange={(event) => setRequest({ ...request, customAlias: event.target.value })} placeholder="docs-2026" pattern="[A-Za-z0-9_-]{4,32}" className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100" />
              </div>
              <div>
                <label htmlFor="expires" className="mb-2 block text-sm font-semibold text-slate-800">Expires <span className="font-normal text-slate-400">optional</span></label>
                <input id="expires" type="datetime-local" value={request.expiresAt || ''} onChange={(event) => setRequest({ ...request, expiresAt: event.target.value })} className="w-full rounded-xl border border-slate-300 px-4 py-3 outline-none transition focus:border-indigo-500 focus:ring-4 focus:ring-indigo-100" />
              </div>
            </div>
            {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-700">{error}</div>}
            <button disabled={creating || health === 'unavailable'} className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-slate-950 px-5 py-3.5 font-semibold text-white transition hover:bg-indigo-700 focus:outline-none focus:ring-4 focus:ring-indigo-200 disabled:cursor-not-allowed disabled:opacity-50">
              {creating ? <LoaderCircle className="h-5 w-5 animate-spin" /> : <Activity className="h-5 w-5" />}
              {creating ? 'Creating with the live API…' : 'Create real short URL'}
            </button>
            <p className="text-xs text-slate-500">API origin: <code className="rounded bg-slate-100 px-1.5 py-0.5">{API_BASE_URL || 'same origin'}</code></p>
          </form>

          <div className="flex min-h-[430px] flex-col p-6 sm:p-8">
            {!created ? (
              <div className="m-auto max-w-sm text-center">
                <div className="mx-auto mb-5 grid h-14 w-14 place-items-center rounded-2xl bg-indigo-50 text-indigo-600"><MousePointerClick className="h-7 w-7" /></div>
                <h3 className="text-lg font-bold text-slate-900">The response appears here</h3>
                <p className="mt-2 text-sm leading-6 text-slate-500">This panel only displays data returned by the deployed API. No demo values are fabricated.</p>
              </div>
            ) : (
              <div className="space-y-5">
                <div className="flex items-start justify-between gap-4">
                  <div><p className="text-xs font-semibold uppercase tracking-wider text-emerald-600">Created by live API</p><h3 className="mt-1 text-xl font-bold text-slate-950">/{created.code}</h3></div>
                  <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">{created.status}</span>
                </div>
                <div className="rounded-2xl border border-indigo-100 bg-indigo-50 p-4">
                  <p className="text-xs font-semibold uppercase tracking-wider text-indigo-600">Short URL</p>
                  <a href={created.shortUrl} target="_blank" rel="noreferrer" className="mt-1 block break-all font-semibold text-indigo-800 hover:underline">{created.shortUrl}</a>
                  <div className="mt-3 flex flex-wrap gap-2">
                    <a href={created.shortUrl} target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 rounded-lg bg-indigo-600 px-3 py-2 text-sm font-semibold text-white hover:bg-indigo-700"><ExternalLink className="h-4 w-4" /> Open / generate click</a>
                    <button onClick={() => void copy(created.shortUrl, 'url')} className="inline-flex items-center gap-2 rounded-lg border border-indigo-200 bg-white px-3 py-2 text-sm font-semibold text-indigo-700"><Copy className="h-4 w-4" /> {copied === 'url' ? 'Copied' : 'Copy URL'}</button>
                  </div>
                </div>
                <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-amber-800">Owner token — returned once</p>
                  <code className="mt-2 block break-all text-xs text-amber-950">{created.ownerToken}</code>
                  <button onClick={() => void copy(created.ownerToken!, 'token')} className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-amber-800"><Copy className="h-4 w-4" /> {copied === 'token' ? 'Copied' : 'Copy token'}</button>
                  <p className="mt-2 text-xs text-amber-700">Store it securely. The backend persists only its SHA-256 digest.</p>
                </div>
                <div className="rounded-2xl border border-slate-200 p-4">
                  <div className="flex items-center justify-between gap-4">
                    <div><p className="text-sm font-semibold text-slate-700">Persisted click count</p><p className="mt-1 text-4xl font-bold tabular-nums text-slate-950">{analytics?.totalClicks ?? '—'}</p></div>
                    <button onClick={() => void refreshAnalytics()} disabled={refreshing} className="inline-flex items-center gap-2 rounded-xl border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"><RefreshCw className={`h-4 w-4 ${refreshing ? 'animate-spin' : ''}`} /> Refresh analytics</button>
                  </div>
                  <p className="mt-3 text-xs leading-5 text-slate-500">After opening the short URL, the count can take a moment: Redis Streams buffers the event while the worker writes it to PostgreSQL.</p>
                </div>
                <button onClick={reset} className="inline-flex items-center gap-2 text-sm font-semibold text-slate-500 hover:text-slate-900"><RotateCcw className="h-4 w-4" /> Reset demo</button>
              </div>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
