import { ChevronDown, Check } from 'lucide-react';
import { useState } from 'react';
import type { WeeklyReport } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface WeeklyReportsProps {
  reports: WeeklyReport[];
}

export default function WeeklyReports({ reports }: WeeklyReportsProps) {
  const [activeTab, setActiveTab] = useState<'this' | 'last' | 'by-chv'>('this');

  return (
    <div className="bg-white rounded-xl border border-border shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-text-primary">Weekly Reports</h3>
        <div className="flex items-center border border-border rounded-lg overflow-hidden">
          <button
            onClick={() => setActiveTab('this')}
            className={`px-3 py-1.5 text-xs font-medium ${activeTab === 'this' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
          >
            This Week
          </button>
          <button
            onClick={() => setActiveTab('last')}
            className={`px-3 py-1.5 text-xs font-medium border-x border-border ${activeTab === 'last' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
          >
            Last Week
          </button>
          <button
            onClick={() => setActiveTab('by-chv')}
            className={`px-3 py-1.5 text-xs font-medium ${activeTab === 'by-chv' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
          >
            By CHV <ChevronDown size={12} className="inline ml-0.5" />
          </button>
        </div>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            <th className="text-left py-2 px-3 text-text-secondary font-medium">CHV</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">Week</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">Reports</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">High Risk</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">Flag</th>
          </tr>
        </thead>
        <tbody>
          {reports.map((r, i) => (
            <tr key={i} className="border-b border-border last:border-0 hover:bg-gray-50">
              <td className="py-2.5 px-3 font-semibold text-text-primary">{r.chvName}</td>
              <td className="py-2.5 px-3 text-text-secondary">{r.week}</td>
              <td className="py-2.5 px-3 text-text-secondary">{r.reportsCount ?? r.notes ?? '-'}</td>
              <td className="py-2.5 px-3 text-text-secondary">{r.highRisk || '-'}</td>
              <td className="py-2.5 px-3">
                <div className="flex items-center gap-1.5">
                  {r.flag?.value && <StatusBadge type={r.flag.type} label={r.flag.value} />}
                  {(r.completionStatus || r.submitted) && (
                    <span className="w-5 h-5 rounded-full bg-success-light flex items-center justify-center">
                      <Check size={12} className="text-success" />
                    </span>
                  )}
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {/* Comparison section */}
      <div className="mt-6 pt-4 border-t border-border">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-sm text-text-secondary">CHV</span>
          <button className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
            Comparison <ChevronDown size={14} />
          </button>
        </div>

        <table className="w-full text-sm">
          <tbody>
            {reports.slice(0, 2).map((r, i) => (
              <tr key={i} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-2.5 px-3 font-semibold text-text-primary">{r.chvName}</td>
                <td className="py-2.5 px-3 text-text-secondary">{r.week}</td>
                <td className="py-2.5 px-3 text-text-secondary">{r.highRisk || '-'}</td>
                <td className="py-2.5 px-3">
                  <div className="flex items-center gap-1.5">
                    {r.flag?.value && <StatusBadge type={r.flag.type} label={r.flag.value} />}
                    {(r.completionStatus || r.submitted) && (
                      <span className="w-5 h-5 rounded-full bg-success-light flex items-center justify-center">
                        <Check size={12} className="text-success" />
                      </span>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
