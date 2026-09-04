import React, { useState } from 'react';
import { linkService } from '../../api/linkService';
import type { CreateLinkRequest, LinkResponse } from '../../types';
import Button from '../common/UI/Button';
import Input from '../common/UI/Input';
import { Copy, ExternalLink, CheckCircle } from 'lucide-react';

const CreateLinkForm: React.FC = () => {
  const [request, setRequest] = useState<CreateLinkRequest>({
    destinationUrl: '',
    customAlias: '',
    expiresAt: '',
  });
  const [response, setResponse] = useState<LinkResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResponse(null);

    try {
      const result = await linkService.createLink(request);
      setResponse(result);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'An unexpected error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const copyToClipboard = async (text: string) => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  if (response) {
    return (
      <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200 text-center animate-in fade-in zoom-in duration-300">
        <div className="flex justify-center mb-4">
          <div className="bg-green-100 p-3 rounded-full">
            <CheckCircle className="w-8 h-8 text-green-600" />
          </div>
        </div>
        <h3 className="text-2xl font-bold text-gray-900 mb-2">Link Created!</h3>
        <p className="text-gray-600 mb-6">Your short URL is ready to use.</p>

        <div className="space-y-4 max-w-md mx-auto">
          <div className="flex items-center gap-2 p-3 bg-gray-50 border border-gray-200 rounded-lg">
            <span className="flex-1 font-medium text-indigo-600 truncate">
              {response.shortUrl}
            </span>
            <a
              href={response.shortUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="p-2 text-gray-400 hover:text-indigo-600 transition-colors"
            >
              <ExternalLink className="w-5 h-5" />
            </a>
          </div>

          <div className="text-left p-4 bg-indigo-50 rounded-lg border border-indigo-100">
            <div className="flex justify-between items-center mb-2">
              <span className="text-xs font-semibold text-indigo-600 uppercase tracking-wider">Owner Token</span>
              <button
                onClick={() => copyToClipboard(response.ownerToken!)}
                className="flex items-center gap-1 text-xs text-indigo-500 hover:text-indigo-700 transition-colors"
              >
                {copied ? <span className="text-green-600">Copied!</span> : <><Copy className="w-3 h-3" /> Copy</>}
              </button>
            </div>
            <div className="font-mono text-sm break-all bg-white p-2 rounded border border-indigo-200 text-gray-700">
              {response.ownerToken}
            </div>
            <p className="text-[11px] text-indigo-400 mt-2 leading-relaxed">
              Store this token securely. You need it to manage your link or view analytics.
              It cannot be recovered if lost.
            </p>
          </div>
        </div>

        <Button
          variant="secondary"
          className="mt-8 text-sm"
          onClick={() => setResponse(null)}
        >
          Create Another Link
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit} className="bg-white p-8 rounded-2xl shadow-sm border border-gray-200 space-y-6 max-w-2xl mx-auto w-full">
      <div className="space-y-2">
        <h3 className="text-xl font-bold text-gray-900">Create a Short Link</h3>
        <p className="text-gray-500 text-sm">Paste your long URL below to generate a shareable short link.</p>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 text-red-600 text-sm rounded-lg animate-in slide-in-from-top-2 duration-200">
          {error}
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="md:col-span-2">
          <Input
            label="Destination URL"
            placeholder="https://example.com/very-long-page-url"
            value={request.destinationUrl}
            onChange={(e) => setRequest({ ...request, destinationUrl: e.target.value })}
            required
          />
        </div>
        <Input
          label="Custom Alias (Optional)"
          placeholder="my-cool-link"
          value={request.customAlias || ''}
          onChange={(e) => setRequest({ ...request, customAlias: e.target.value })}
        />
        <Input
          label="Expires At (Optional)"
          type="datetime-local"
          value={request.expiresAt || ''}
          onChange={(e) => setRequest({ ...request, expiresAt: e.target.value })}
        />
      </div>

      <Button
        type="submit"
        className="w-full py-3 text-lg"
        isLoading={loading}
      >
        Shorten URL
      </Button>
    </form>
  );
};

export default CreateLinkForm;
