import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import LinkManager from '../components/features/LinkManager';
import Input from '../components/common/UI/Input';
import Button from '../components/common/UI/Button';
import { Lock } from 'lucide-react';

const Manage: React.FC = () => {
  const { code } = useParams<{ code: string }>();
  const navigate = useNavigate();
  const [linkCode, setLinkCode] = useState('');
  const [token, setToken] = useState('');
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    // The simplest way to "authenticate" is to attempt a request to the backend
    // The LinkManager component will handle the actual fetching, but we can do
    // a preliminary check or just let the LinkManager handle it.
    // For better UX, we'll just set isAuthenticated to true and let LinkManager fail if token is wrong.
    if (token) {
      setIsAuthenticated(true);
    } else {
      setError('Please enter your owner token.');
    }
    setLoading(false);
  };

  if (!code) {
    return (
      <div className="mx-auto max-w-md px-4 py-20 text-center">
        <div className="mb-6 inline-flex h-16 w-16 items-center justify-center rounded-full bg-indigo-100">
          <Lock className="h-8 w-8 text-indigo-600" />
        </div>
        <h1 className="mb-3 text-3xl font-bold text-gray-900">Manage a short link</h1>
        <p className="mb-8 text-gray-600">Enter the short code. Your owner token is requested on the next screen.</p>
        <form className="space-y-4" onSubmit={(event) => {
          event.preventDefault();
          if (linkCode.trim()) navigate(`/manage/${encodeURIComponent(linkCode.trim())}`);
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
          <h1 className="text-3xl font-bold text-gray-900 mb-4">Manage Your Link</h1>
          <p className="text-gray-600 mb-8">
            Enter your owner token to access management controls for link <span className="font-mono font-bold text-indigo-600">{code}</span>.
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
              Unlock Management
            </Button>
          </form>
        </div>
      ) : (
        <div className="space-y-8">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-3xl font-bold text-gray-900">Link Management</h1>
              <p className="text-gray-600">Update your destination, status, or expiration date.</p>
            </div>
            <Button variant="secondary" onClick={() => setIsAuthenticated(false)}>
              Switch Token
            </Button>
          </div>
          <LinkManager
            initialCode={code || ''}
            token={token}
            onSuccess={() => {}}
          />
        </div>
      )}
    </div>
  );
};

export default Manage;
