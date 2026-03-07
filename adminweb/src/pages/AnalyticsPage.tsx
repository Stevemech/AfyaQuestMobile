import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import StatsCards from '../components/analytics/StatsCards';
import CHVProgress from '../components/analytics/CHVProgress';
import WeeklyReports from '../components/analytics/WeeklyReports';
import { api } from '../api/api';
import type { AnalyticsStats, ModuleProgress, WeeklyReport } from '../types';

export default function AnalyticsPage() {
  const { t } = useTranslation();
  const [stats, setStats] = useState<AnalyticsStats>({ avgVideoCompletion: 0, atRiskCHVs: 0, modulesAssigned: 0, reportsSubmitted: 0 });
  const [progress, setProgress] = useState<ModuleProgress[]>([]);
  const [weeklyReports, setWeeklyReports] = useState<WeeklyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

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
    const moduleId = prompt(t('analytics.enterModuleId'));
    if (!moduleId) return;
    api.assignModule(chvId, moduleId)
      .then(() => alert(t('analytics.moduleAssigned')))
      .catch(err => alert(`${t('analytics.failedAssign')} ${err.message}`));
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
    </div>
  );
}
