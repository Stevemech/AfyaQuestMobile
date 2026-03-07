import { useState } from 'react';
import { Check, X, AlertTriangle, ChevronDown, ChevronUp, Menu } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { ModuleProgress } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVProgressProps {
  progress: ModuleProgress[];
  onAssign: (chvId: string) => void;
}

export default function CHVProgress({ progress, onAssign }: CHVProgressProps) {
  const { t } = useTranslation();
  const [expanded, setExpanded] = useState(false);

  const displayedProgress = expanded ? progress : progress.slice(0, 3);

  return (
    <div className="bg-white rounded-xl border border-border shadow-sm p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-lg font-semibold text-text-primary">{t('chvProgress.title')}</h3>
        <button
          onClick={() => setExpanded(!expanded)}
          className="flex items-center gap-1.5 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary"
        >
          <Menu size={14} /> {expanded ? t('chvProgress.collapse') : t('chvProgress.expand')} {expanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
        </button>
      </div>

      <table className="w-full text-sm">
        <thead>
          <tr className="border-b border-border">
            <th className="text-left py-2 px-3 text-text-secondary font-semibold">{t('chvProgress.chv')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-semibold">{t('chvProgress.module1')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-semibold">{t('chvProgress.module2')}</th>
            <th className="text-left py-2 px-3 text-text-secondary font-semibold">{t('chvProgress.riskFlag')}</th>
          </tr>
        </thead>
        <tbody>
          {displayedProgress.map(p => (
            <tr key={p.chvId} className="border-b border-border last:border-0 hover:bg-gray-50">
              <td className="py-3 px-3 font-semibold text-text-primary">{p.chvName}</td>
              <td className="py-3 px-3">
                <div className="flex items-center gap-2">
                  <span
                    className="w-5 h-5 rounded-full flex items-center justify-center cursor-help"
                    title={p.modules[0]?.completed
                      ? `${t('chvProgress.completed')} - ${p.modules[0]?.score ?? 0}%`
                      : t('chvProgress.notCompleted')
                    }
                  >
                    {p.modules[0]?.completed ? (
                      <span className="w-5 h-5 rounded-full bg-success-light flex items-center justify-center">
                        <Check size={12} className="text-success" />
                      </span>
                    ) : (
                      <span className="w-5 h-5 rounded-full bg-danger-light flex items-center justify-center">
                        <X size={12} className="text-danger" />
                      </span>
                    )}
                  </span>
                  {p.modules[0]?.score != null && p.modules[0]?.score < 70 && p.modules[0]?.completed && (
                    <span
                      className="w-5 h-5 rounded-full bg-warning-light flex items-center justify-center cursor-help"
                      title={`${t('chvProgress.lowScore')}: ${p.modules[0].score}%`}
                    >
                      <AlertTriangle size={12} className="text-warning" />
                    </span>
                  )}
                  <span className="flex items-center gap-1 text-text-secondary">
                    <StatusBadge type={p.overallProgress >= 80 ? 'success' : p.overallProgress >= 50 ? 'warning' : 'danger'} size="sm" />
                    {p.overallProgress}%
                  </span>
                </div>
              </td>
              <td className="py-3 px-3">
                {p.modules[1] ? (
                  <div className="flex items-center gap-2">
                    {p.modules[1].completed ? (
                      <>
                        <span
                          className="w-5 h-5 rounded-full bg-success-light flex items-center justify-center cursor-help"
                          title={`${t('chvProgress.completed')} - ${p.modules[1].score ?? 0}%`}
                        >
                          <Check size={12} className="text-success" />
                        </span>
                        <span className="text-text-secondary">{p.modules[1].score ?? 0}%</span>
                      </>
                    ) : (
                      <span className="text-text-secondary">{t('chvProgress.notStarted')}</span>
                    )}
                  </div>
                ) : '-'}
              </td>
              <td className="py-3 px-3">
                {p.flag ? (
                  <div className="flex items-center gap-2">
                    <StatusBadge type={p.flag.type} label={p.flag.value} />
                  </div>
                ) : (
                  <button
                    onClick={() => onAssign(p.chvId)}
                    className="px-4 py-1.5 bg-primary text-white rounded-lg text-xs font-medium hover:bg-primary-dark transition-colors"
                  >
                    {t('chvProgress.assign')}
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {progress.length > 3 && !expanded && (
        <button
          onClick={() => setExpanded(true)}
          className="mt-3 text-sm text-primary hover:underline"
        >
          {t('chvProgress.showAll', { count: progress.length })}
        </button>
      )}
    </div>
  );
}
