import React from 'react';
import { BarChart3, TrendingUp, MousePointer2 } from 'lucide-react';
import type { AnalyticsResponse } from '../../types';

interface AnalyticsCardProps {
  data: AnalyticsResponse;
}

const AnalyticsCard: React.FC<AnalyticsCardProps> = ({ data }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
      <div className="p-6 bg-white rounded-2xl border border-gray-200 shadow-sm">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-indigo-100 rounded-lg">
            <MousePointer2 className="w-5 h-5 text-indigo-600" />
          </div>
          <span className="text-sm font-medium text-gray-500">Total Clicks</span>
        </div>
        <div className="text-4xl font-bold text-gray-900">{data.totalClicks.toLocaleString()}</div>
        <div className="mt-2 flex items-center gap-1 text-xs text-green-600 font-medium">
          <TrendingUp className="w-3 h-3" />
          <span>Real-time tracking active</span>
        </div>
      </div>

      <div className="p-6 bg-white rounded-2xl border border-gray-200 shadow-sm">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-blue-100 rounded-lg">
            <BarChart3 className="w-5 h-5 text-blue-600" />
          </div>
          <span className="text-sm font-medium text-gray-500">Short Code</span>
        </div>
        <div className="text-2xl font-mono font-bold text-gray-900">{data.code}</div>
        <div className="mt-2 text-xs text-gray-400">Unique identifier</div>
      </div>

      <div className="p-6 bg-white rounded-2xl border border-gray-200 shadow-sm">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 bg-purple-100 rounded-lg">
            <TrendingUp className="w-5 h-5 text-purple-600" />
          </div>
          <span className="text-sm font-medium text-gray-500">Status</span>
        </div>
        <div className="text-2xl font-bold text-gray-900">Active</div>
        <div className="mt-2 text-xs text-gray-400">Link is resolvable</div>
      </div>
    </div>
  );
};

export default AnalyticsCard;
