import React, { useState } from 'react';
import { linkService } from '../../api/linkService';
import type { LinkResponse, UpdateLinkRequest } from '../../types';
import Button from '../common/UI/Button';
import Input from '../common/UI/Input';
import { Save, AlertCircle } from 'lucide-react';

interface LinkManagerProps {
  initialCode: string;
  token: string;
  onSuccess: () => void;
}

const LinkManager: React.FC<LinkManagerProps> = ({ initialCode, token, onSuccess }) => {
  const [link, setLink] = useState<LinkResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [form, setForm] = useState<Partial<UpdateLinkRequest>>({});

  React.useEffect(() => {
    fetchLink();
  }, [initialCode, token]);

  const fetchLink = async () => {
    try {
      const data = await linkService.getLinkDetails(initialCode, token);
      setLink(data);
      setForm({
        destinationUrl: data.destinationUrl,
        status: data.status,
        expiresAt: data.expiresAt ? data.expiresAt.slice(0, 16) : '',
      });
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to fetch link details. Please check your token.');
    } finally {
      setLoading(false);
    }
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setUpdating(true);
    setError(null);

    try {
      await linkService.updateLink(initialCode, token, {
        expectedVersion: link?.version || 0,
        ...form,
      });
      await fetchLink();
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to update link.');
    } finally {
      setUpdating(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-4 bg-red-50 border border-red-200 text-red-600 rounded-lg flex items-center gap-3">
        <AlertCircle className="w-5 h-5" />
        <span>{error}</span>
      </div>
    );
  }

  if (!link) return null;

  return (
    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-200 max-w-2xl mx-auto">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-xl font-bold text-gray-900">Manage Link: {link.code}</h3>
        <span className={`px-2 py-1 rounded-full text-xs font-bold ${
          link.status === 'ACTIVE' ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'
        }`}>
          {link.status}
        </span>
      </div>

      <form onSubmit={handleUpdate} className="space-y-6">
        <div className="space-y-4">
          <Input
            label="Destination URL"
            value={form.destinationUrl || ''}
            onChange={(e) => setForm({ ...form, destinationUrl: e.target.value })}
            required
          />
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="flex flex-col gap-1">
              <label className="text-sm font-medium text-gray-700">Status</label>
              <select
                className="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500 transition-all"
                value={form.status || link.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as any })}
              >
                <option value="ACTIVE">Active</option>
                <option value="DISABLED">Disabled</option>
              </select>
            </div>
            <Input
              label="Expires At"
              type="datetime-local"
              value={form.expiresAt || ''}
              onChange={(e) => setForm({ ...form, expiresAt: e.target.value })}
            />
          </div>
        </div>

        <div className="pt-4 flex justify-end gap-3">
          <Button
            variant="secondary"
            type="button"
            onClick={() => setForm({
              destinationUrl: link.destinationUrl,
              status: link.status,
              expiresAt: link.expiresAt?.slice(0, 16) || '',
            })}
          >
            Reset
          </Button>
          <Button
            type="submit"
            isLoading={updating}
          >
            <div className="flex items-center gap-2">
              <Save className="w-4 h-4" />
              Save Changes
            </div>
          </Button>
        </div>
      </form>
    </div>
  );
};

export default LinkManager;
