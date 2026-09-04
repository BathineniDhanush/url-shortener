import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { linkService } from '../api/linkService';
import type { AnalyticsResponse } from '../types';
import AnalyticsCard from '../components/features/AnalyticsCard';
import Input from '../components/common/UI/Input';
import Button from '../components/common/UI/Button';
import { BarChart3, Lock } from 'lucide-react';

const Analytics: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const [linkCode, setLinkCode] = useState('');
  const [token, setToken] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(false);
  const [data, setData] = useState<AnalyticsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const result = await linkService.getLinkAnalytics(code || '', token);
      setData(result);
      setIsAuthenticated(true);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Invalid token or link not found.');
    } finally {
      setLoading(false);
    }
  };

  if (!code) {
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center">
        <div className="mb-6 inline-flex h-16 w-16 items-center justify-center rounded-full bg-indigo-100">
          <BarChart3 className="h-8 w-8 text-indigo-600" />
        </div>
        <h1 className="mb-3 text-3xl font-bold text-gray-900">View link analytics</h1>
        <p className="mb-8 text-gray-600">Enter the short code. Your owner token is requested on the next screen.</p>
        <form className="space-y-4" onSubmit={(event) => {
          event.preventDefault();
          if (linkCode.trim()) navigate(`/analytics/${encodeURIComponent(linkCode.trim())}`);
        }}>
          <Input label="Short code" value={linkCode} onChange={(event) => setLinkCode(event.target.value)} placeholder="docs-2026" required />
          <Button className="w-full py-3" type="submit">Continue</Button>
        </form>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      {!isAuthenticated ? (
        <div className="max-w-md mx-auto text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-indigo-100 rounded-full mb-6">
            <Lock className="w-8 h-8 text-indigo-600" />
          </div>
          <h1 className="text-3xl font-bold text-gray-900 mb-4">Link Analytics</h1>
          <p className="text-gray-600 mb-8">
            Enter your owner token to view the click statistics for link <span className="font-mono font-bold text-indigo-600">{code}</span>.
          </p>

          <form onSubmit={handleAuth} className="space-y-4">
            <Input
              label="Owner Token"
              type="password"
              placeholder="Enter your secret token"
              value={token}
              onChange={(e) => setToken(e.target.value)}
              required
            />
            {error && <p className="text-red-500 text-sm text-left">{error}</p>}
            <Button type="submit" className="w-full py-3" isLoading={loading}>
              View Analytics
            </Button>
          </form>
        </div>
      ) : (
        <div className="space-y-8">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-indigo-100 rounded-lg">
                <BarChart3 className="w-6 h-6 text-indigo-600" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-gray-900">Analytics Dashboard</h1>
                <p className="text-gray-600">Real-time performance metrics for your link.</p>
              </div>
            </div>
            <Button variant="secondary" onClick={() => setIsAuthenticated(false)}>
              Switch Token
            </Button>
          </div>
          <AnalyticsCard data={data!} />
        </div>
      )}
    </div>
  );
};

export default Analytics;
