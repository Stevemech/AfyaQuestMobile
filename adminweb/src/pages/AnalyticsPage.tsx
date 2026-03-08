import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { X, Send, CheckCircle } from 'lucide-react';
import StatsCards from '../components/analytics/StatsCards';
import CHVProgress from '../components/analytics/CHVProgress';
import WeeklyReports from '../components/analytics/WeeklyReports';
import { api, ALL_MODULES } from '../api/api';
import type { AnalyticsStats, ModuleProgress, WeeklyReport } from '../types';

export default function AnalyticsPage() {
  const { t } = useTranslation();
  const [stats, setStats] = useState<AnalyticsStats>({ avgVideoCompletion: 0, atRiskCHVs: 0, modulesAssigned: 0, reportsSubmitted: 0 });
  const [progress, setProgress] = useState<ModuleProgress[]>([]);
  const [weeklyReports, setWeeklyReports] = useState<WeeklyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Assign modal state
  const [assignChvId, setAssignChvId] = useState<string | null>(null);
  const [assignChvName, setAssignChvName] = useState('');
  const [assignModuleId, setAssignModuleId] = useState('');
  const [assignLoading, setAssignLoading] = useState(false);
  const [assignError, setAssignError] = useState('');
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  useEffect(() => {
    setLoading(true);
    setError('');
    api.getAnalytics()
      .then(res => {
        if (res.stats) setStats(res.stats);
        if (res.progress) setProgress(res.progress);
        if (res.reports) setWeeklyReports(res.reports);
      })
      .catch(err => setError(err.message || 'Failed to load analytics'))
      .finally(() => setLoading(false));
  }, []);

  const handleAssign = (chvId: string) => {
    const chv = progress.find(p => p.chvId === chvId);
    setAssignChvId(chvId);
    setAssignChvName(chv?.chvName || chvId);
    setAssignModuleId('');
    setAssignError('');
  };

  const handleConfirmAssign = async () => {
    if (!assignChvId || !assignModuleId) {
      setAssignError(t('itinerary.selectModuleError'));
      return;
    }
    setAssignLoading(true);
    setAssignError('');
    try {
      const selected = ALL_MODULES.find(m => m.id === assignModuleId);
      if (selected?.type === 'lesson') {
        await api.assignLesson(assignChvId, assignModuleId);
      } else {
        await api.assignModule(assignChvId, assignModuleId);
      }
      showToast(t('analytics.moduleAssigned'));
      setAssignChvId(null);
      // Refresh analytics data
      api.getAnalytics().then(res => {
        if (res.stats) setStats(res.stats);
        if (res.progress) setProgress(res.progress);
        if (res.reports) setWeeklyReports(res.reports);
      });
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : t('analytics.failedAssign'));
    } finally {
      setAssignLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-text-secondary">{t('analytics.loading')}</p>
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

  return (
    <div>
      {/* Toast */}
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-lg shadow-lg text-sm font-medium ${
          toast.type === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
        }`}>
          {toast.type === 'success' ? <CheckCircle size={16} /> : <X size={16} />}
          {toast.message}
        </div>
      )}

      {/* Tab header */}
      <div className="inline-block mb-6">
        <div className="bg-white rounded-lg border border-border px-4 py-2">
          <span className="text-sm font-medium text-text-primary">{t('analytics.title')}</span>
        </div>
      </div>

      <div className="space-y-6">
        <StatsCards stats={stats} />
        <CHVProgress progress={progress} onAssign={handleAssign} />
        <WeeklyReports reports={weeklyReports} />
      </div>

      {/* Assign Module Modal */}
      {assignChvId && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setAssignChvId(null)}>
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{t('itinerary.assignModuleTo', { name: assignChvName })}</h3>
              <button onClick={() => setAssignChvId(null)} className="p-1 rounded hover:bg-gray-100">
                <X size={20} className="text-text-secondary" />
              </button>
            </div>
            {assignError && <div className="mb-3 p-2 bg-danger-light text-danger text-sm rounded-lg">{assignError}</div>}
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1.5">{t('itinerary.module')}</label>
              <select
                value={assignModuleId}
                onChange={e => { setAssignModuleId(e.target.value); setAssignError(''); }}
                className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
              >
                <option value="">{t('settings.selectModule')}</option>
                <optgroup label={t('itinerary.videoModules')}>
                  {ALL_MODULES.filter(m => m.type === 'video').map(m => (
                    <option key={m.id} value={m.id}>{m.name}</option>
                  ))}
                </optgroup>
                <optgroup label={t('itinerary.interactiveLessons')}>
                  {ALL_MODULES.filter(m => m.type === 'lesson').map(m => (
                    <option key={m.id} value={m.id}>{m.name}</option>
                  ))}
                </optgroup>
              </select>
            </div>
            <button
              onClick={handleConfirmAssign}
              disabled={assignLoading || !assignModuleId}
              className="mt-6 w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <Send size={14} /> {assignLoading ? t('settings.assigning') : t('itinerary.assignModule')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
