import { ChevronDown, Check } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import type { WeeklyReport } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface WeeklyReportsProps {
  reports: WeeklyReport[];
}

export default function WeeklyReports({ reports }: WeeklyReportsProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<'this' | 'last' | 'by-chv'>('this');
  const [selectedCHV, setSelectedCHV] = useState<string>('all');
  const [showCHVDropdown, setShowCHVDropdown] = useState(false);
  const [comparisonCHV, setComparisonCHV] = useState<string>('all');
  const [showCompDropdown, setShowCompDropdown] = useState(false);
  const chvDropdownRef = useRef<HTMLDivElement>(null);
  const compDropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (chvDropdownRef.current && !chvDropdownRef.current.contains(e.target as Node)) setShowCHVDropdown(false);
      if (compDropdownRef.current && !compDropdownRef.current.contains(e.target as Node)) setShowCompDropdown(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const uniqueCHVs = [...new Set(reports.map(r => r.chvName))];

  // Filter reports based on active tab
  const filteredReports = reports.filter(r => {
    if (activeTab === 'by-chv' && selectedCHV !== 'all' && r.chvName !== selectedCHV) return false;
    return true;
  });

  // For comparison, filter by selected CHV
  const comparisonReports = comparisonCHV === 'all'
    ? reports.slice(0, 2)
    : reports.filter(r => r.chvName === comparisonCHV);

  return (
    <div className="bg-white rounded-xl border border-border shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-text-primary">{t('weeklyReports.title')}</h3>
        <div className="flex items-center border border-border rounded-lg overflow-hidden">
          <button
            onClick={() => { setActiveTab('this'); setSelectedCHV('all'); }}
            className={`px-3 py-1.5 text-xs font-medium ${activeTab === 'this' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
          >
            {t('weeklyReports.thisWeek')}
          </button>
          <button
            onClick={() => { setActiveTab('last'); setSelectedCHV('all'); }}
            className={`px-3 py-1.5 text-xs font-medium border-x border-border ${activeTab === 'last' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
          >
            {t('weeklyReports.lastWeek')}
          </button>
          <div className="relative" ref={chvDropdownRef}>
            <button
              onClick={() => { setActiveTab('by-chv'); setShowCHVDropdown(!showCHVDropdown); }}
              className={`px-3 py-1.5 text-xs font-medium flex items-center gap-0.5 ${activeTab === 'by-chv' ? 'bg-primary text-white' : 'text-text-secondary hover:bg-gray-50'}`}
            >
              {t('weeklyReports.byCHV')} <ChevronDown size={12} />
            </button>
            {showCHVDropdown && (
              <div className="absolute right-0 top-full mt-1 bg-white border border-border rounded-lg shadow-lg z-20 min-w-[140px]">
                <button
                  onClick={() => { setSelectedCHV('all'); setShowCHVDropdown(false); }}
                  className={`block w-full text-left px-3 py-1.5 text-xs hover:bg-gray-50 ${selectedCHV === 'all' ? 'text-primary font-medium' : 'text-text-secondary'}`}
                >
                  {t('reportsPage.allCHVs')}
                </button>
                {uniqueCHVs.map(name => (
                  <button
                    key={name}
                    onClick={() => { setSelectedCHV(name); setShowCHVDropdown(false); }}
                    className={`block w-full text-left px-3 py-1.5 text-xs hover:bg-gray-50 ${selectedCHV === name ? 'text-primary font-medium' : 'text-text-secondary'}`}
                  >
                    {name}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>

      {activeTab === 'last' && (
        <div className="mb-3 p-2 bg-blue-50 text-blue-700 text-xs rounded-lg">
          {t('weeklyReports.showingLastWeek')}
        </div>
      )}

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('weeklyReports.chv')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('weeklyReports.week')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('weeklyReports.reports')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('weeklyReports.highRisk')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('weeklyReports.flag')}</th>
          </tr>
        </thead>
        <tbody>
          {filteredReports.length > 0 ? filteredReports.map((r, i) => (
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
          )) : (
            <tr>
              <td colSpan={5} className="py-6 text-center text-text-secondary text-sm">{t('weeklyReports.noReports')}</td>
            </tr>
          )}
        </tbody>
      </table>

      {/* Comparison section */}
      <div className="mt-6 pt-4 border-t border-border">
        <div className="flex items-center gap-2 mb-3">
          <span className="text-sm text-text-secondary">{t('weeklyReports.chv')}</span>
          <div className="relative" ref={compDropdownRef}>
            <button
              onClick={() => setShowCompDropdown(!showCompDropdown)}
              className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary"
            >
              {comparisonCHV === 'all' ? t('weeklyReports.comparison') : comparisonCHV} <ChevronDown size={14} />
            </button>
            {showCompDropdown && (
              <div className="absolute left-0 top-full mt-1 bg-white border border-border rounded-lg shadow-lg z-20 min-w-[140px]">
                <button
                  onClick={() => { setComparisonCHV('all'); setShowCompDropdown(false); }}
                  className={`block w-full text-left px-3 py-1.5 text-xs hover:bg-gray-50 ${comparisonCHV === 'all' ? 'text-primary font-medium' : 'text-text-secondary'}`}
                >
                  {t('reportsPage.allCHVs')}
                </button>
                {uniqueCHVs.map(name => (
                  <button
                    key={name}
                    onClick={() => { setComparisonCHV(name); setShowCompDropdown(false); }}
                    className={`block w-full text-left px-3 py-1.5 text-xs hover:bg-gray-50 ${comparisonCHV === name ? 'text-primary font-medium' : 'text-text-secondary'}`}
                  >
                    {name}
                  </button>
                ))}
              </div>
            )}
          </div>
        </div>

        <table className="w-full text-sm">
          <tbody>
            {comparisonReports.map((r, i) => (
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
