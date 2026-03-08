import { useState, useEffect } from 'react';
import { Download, Eye, X, ArrowUpDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import StatusBadge from '../components/common/StatusBadge';
import type { DailyReport } from '../types';

type SortField = 'date' | 'patientsVisited' | 'vaccinationsGiven';
type SortDir = 'asc' | 'desc';

export default function ReportsPage() {
  const { t } = useTranslation();
  const [reports, setReports] = useState<DailyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterCHV, setFilterCHV] = useState('all');
  const [filterDate, setFilterDate] = useState('');
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const [sortField, setSortField] = useState<SortField>('date');
  const [sortDir, setSortDir] = useState<SortDir>('desc');

  useEffect(() => {
    setLoading(true);
    setError('');
    const timeout = setTimeout(() => {
      if (loading) setError(t('reportsPage.loadingTimeout'));
    }, 15000);
    api.getReports()
      .then(res => setReports(res.reports || []))
      .catch(err => setError(err.message || 'Failed to load reports'))
      .finally(() => { setLoading(false); clearTimeout(timeout); });
    return () => clearTimeout(timeout);
  }, []);

  // Filter out admin users from reports
  const nonAdminReports = reports.filter(r => {
    const name = r.userName?.toLowerCase() || '';
    return !name.includes('admin') && !name.includes('afyaadmin');
  });

  const filtered = nonAdminReports
    .filter(r => {
      if (filterCHV !== 'all' && r.userId !== filterCHV) return false;
      if (filterDate && r.date !== filterDate) return false;
      return true;
    })
    .sort((a, b) => {
      const dir = sortDir === 'asc' ? 1 : -1;
      if (sortField === 'date') return dir * a.date.localeCompare(b.date);
      return dir * ((a[sortField] ?? 0) - (b[sortField] ?? 0));
    });

  const uniqueCHVs = [...new Set(nonAdminReports.map(r => r.userName))];

  const handleSort = (field: SortField) => {
    if (sortField === field) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDir('desc');
    }
  };

  const handleExportCSV = () => {
    if (filtered.length === 0) return;
    const headers = ['CHV', 'Date', 'Patients Visited', 'Vaccinations Given', 'Health Education', 'Challenges', 'Notes', 'Created At', 'Synced'];
    const rows = filtered.map(r => [
      r.userName,
      r.date,
      r.patientsVisited,
      r.vaccinationsGiven,
      r.healthEducation,
      `"${(r.challenges || '').replace(/"/g, '""')}"`,
      `"${(r.notes || '').replace(/"/g, '""')}"`,
      r.createdAt || '',
      r.isSynced ? 'Yes' : 'No',
    ]);
    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `reports_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-text-secondary">{t('reportsPage.loading')}</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-center">
          <p className="text-danger mb-2">{error}</p>
          <button onClick={() => window.location.reload()} className="text-sm text-primary hover:underline">
            {t('retry')}
          </button>
        </div>
      </div>
    );
  }

  const SortHeader = ({ field, label }: { field: SortField; label: string }) => (
    <th
      className="text-left py-3 px-4 text-text-secondary font-medium cursor-pointer hover:text-primary select-none"
      onClick={() => handleSort(field)}
    >
      <span className="inline-flex items-center gap-1">
        {label}
        {sortField === field && (
          <ArrowUpDown size={12} className="text-primary" />
        )}
      </span>
    </th>
  );

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-text-primary">{t('reportsPage.title')}</h1>
        <span className="text-sm text-text-secondary">{filtered.length} {t('reportsPage.reportsFound')}</span>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-4 mb-6">
        <div className="flex items-center gap-4">
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('reportsPage.chv')}</label>
            <select
              value={filterCHV}
              onChange={e => setFilterCHV(e.target.value)}
              className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            >
              <option value="all">{t('reportsPage.allCHVs')}</option>
              {uniqueCHVs.map(name => (
                <option key={name} value={nonAdminReports.find(r => r.userName === name)?.userId}>{name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">{t('date')}</label>
            <input
              type="date"
              value={filterDate}
              onChange={e => setFilterDate(e.target.value)}
              className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
          {filterDate && (
            <button
              onClick={() => setFilterDate('')}
              className="mt-4 text-xs text-primary hover:underline"
            >
              {t('reportsPage.clearDate')}
            </button>
          )}
          <button
            onClick={handleExportCSV}
            disabled={filtered.length === 0}
            className="mt-4 flex items-center gap-1.5 px-4 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary disabled:opacity-50"
          >
            <Download size={14} /> {t('reportsPage.exportCSV')}
          </button>
        </div>
      </div>

      {/* Reports table */}
      <div className="bg-white rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-border">
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.chv')}</th>
              <SortHeader field="date" label={t('date')} />
              <SortHeader field="patientsVisited" label={t('reportsPage.patients')} />
              <SortHeader field="vaccinationsGiven" label={t('reportsPage.vaccinations')} />
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.healthEd')}</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.challenges')}</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.notes')}</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.createdAt')}</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">{t('reportsPage.synced')}</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(r => (
              <tr key={r.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-3 px-4 font-medium text-text-primary">{r.userName}</td>
                <td className="py-3 px-4 text-text-secondary">{new Date(r.date).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' })}</td>
                <td className="py-3 px-4">{r.patientsVisited}</td>
                <td className="py-3 px-4">{r.vaccinationsGiven}</td>
                <td className="py-3 px-4">{r.healthEducation}</td>
                <td className="py-3 px-4 text-text-secondary max-w-[150px] truncate">{r.challenges || '-'}</td>
                <td className="py-3 px-4 text-text-secondary max-w-[150px] truncate">{r.notes || '-'}</td>
                <td className="py-3 px-4 text-text-secondary text-xs">
                  {r.createdAt ? new Date(r.createdAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '-'}
                </td>
                <td className="py-3 px-4">
                  <span title={r.isSynced ? t('reportsPage.syncedStatus') : t('reportsPage.notSyncedStatus')}>
                    <StatusBadge type={r.isSynced ? 'success' : 'warning'} />
                  </span>
                </td>
                <td className="py-3 px-4">
                  <button
                    onClick={() => setSelectedReport(selectedReport === r.id ? null : r.id)}
                    className="p-1.5 rounded hover:bg-gray-100"
                    title={t('reportsPage.viewReport')}
                  >
                    <Eye size={16} className="text-text-secondary" />
                  </button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={10} className="py-8 text-center text-text-secondary">{t('reportsPage.noReportsFound')}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* Report detail modal */}
      {selectedReport && (() => {
        const r = reports.find(rep => rep.id === selectedReport);
        if (!r) return null;
        return (
          <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setSelectedReport(null)}>
            <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold">{t('reportsPage.reportDetail')} - {r.userName}</h3>
                <button onClick={() => setSelectedReport(null)} className="p-1 rounded hover:bg-gray-100">
                  <X size={20} className="text-text-secondary" />
                </button>
              </div>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between"><span className="text-text-secondary">{t('reportsPage.chv')}</span><span className="font-medium">{r.userName}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">{t('date')}</span><span>{r.date}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">{t('reportsPage.patientsVisited')}</span><span>{r.patientsVisited}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">{t('reportsPage.vaccinationsGiven')}</span><span>{r.vaccinationsGiven}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">{t('reportsPage.healthEducation')}</span><span>{r.healthEducation}</span></div>
                <div><span className="text-text-secondary block mb-1">{t('reportsPage.challenges')}</span><p className="bg-gray-50 rounded p-2">{r.challenges || t('none')}</p></div>
                <div><span className="text-text-secondary block mb-1">{t('reportsPage.notes')}</span><p className="bg-gray-50 rounded p-2">{r.notes || t('none')}</p></div>
                {r.createdAt && (
                  <div className="flex justify-between"><span className="text-text-secondary">{t('reportsPage.createdAt')}</span><span>{new Date(r.createdAt).toLocaleString()}</span></div>
                )}
                <div className="flex justify-between">
                  <span className="text-text-secondary">{t('reportsPage.synced')}</span>
                  <StatusBadge type={r.isSynced ? 'success' : 'warning'} label={r.isSynced ? t('reportsPage.syncedStatus') : t('reportsPage.notSyncedStatus')} />
                </div>
              </div>
              <button onClick={() => setSelectedReport(null)} className="mt-6 w-full py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
                {t('close')}
              </button>
            </div>
          </div>
        );
      })()}

    </div>
  );
}
