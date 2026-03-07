import { ChevronDown, ArrowUpDown, RotateCcw } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { CHV, House } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVDetailProps {
  chv: CHV;
  houses: House[];
}

export default function CHVDetail({ chv, houses }: CHVDetailProps) {
  const { t } = useTranslation();
  const pending = houses.filter(h => h.visitStatus === 'pending');
  const completed = houses.filter(h => h.visitStatus === 'completed');
  const totalHouses = chv.assignedHouses ?? houses.length;
  const completedCount = chv.completedVisits ?? completed.length;
  const pendingCount = chv.pendingVisits ?? pending.length;

  return (
    <div className="space-y-5">
      {/* CHV Profile Header */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-gray-200 flex items-center justify-center text-2xl font-bold text-text-secondary">
              {chv.name.charAt(0)}
            </div>
            <div>
              <h2 className="text-xl font-bold text-text-primary">{chv.name}</h2>
              <p className="text-sm text-text-secondary">
                {chv.organization || chv.clinic}
                {chv.level != null && <span className="ml-2">{t('chvList.level')} {chv.level}</span>}
                {chv.totalPoints != null && <span className="ml-2">{chv.totalPoints} pts</span>}
              </p>
            </div>
          </div>
          <button className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
            {chv.name} <ChevronDown size={14} />
          </button>
        </div>

        {/* Stats bar */}
        <div className="grid grid-cols-3 gap-4">
          <div>
            <p className="text-sm text-text-secondary">{t('chvDetail.assignedHouses')}</p>
            <p className="text-xl font-bold">{chv.assignedHouses ?? houses.length}</p>
            <div className="w-full h-2 bg-gray-200 rounded-full mt-1 overflow-hidden">
              <div className="h-full bg-success rounded-full" style={{ width: '100%' }} />
            </div>
          </div>
          <div>
            <p className="text-sm text-text-secondary">{t('chvDetail.completedVisits')}</p>
            <p className="text-xl font-bold">{chv.completedVisits ?? completed.length}</p>
            <div className="w-full h-2 bg-gray-200 rounded-full mt-1 overflow-hidden">
              <div className="h-full bg-primary rounded-full" style={{ width: `${totalHouses > 0 ? (completedCount / totalHouses) * 100 : 0}%` }} />
            </div>
          </div>
          <div>
            <p className="text-sm text-text-secondary">{t('chvDetail.pending')}</p>
            <p className="text-xl font-bold">{chv.pendingVisits ?? pending.length}</p>
            <div className="w-full h-2 bg-gray-200 rounded-full mt-1 overflow-hidden">
              <div className="h-full bg-warning rounded-full" style={{ width: `${totalHouses > 0 ? (pendingCount / totalHouses) * 100 : 0}%` }} />
            </div>
          </div>
        </div>
      </div>

      {/* Distance Matrix */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.distanceMatrix')}</h3>
          <div className="flex items-center gap-2">
            <span className="text-xs text-text-secondary">{t('chvDetail.sortBy')}</span>
            <button className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
              {t('chvDetail.distance')} <ChevronDown size={14} />
            </button>
            <button className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
              <RotateCcw size={14} /> {t('chvDetail.reassignHouse')}
            </button>
          </div>
        </div>

        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border">
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.houseId')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.distanceKm')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.visitStatus')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.priority')}</th>
            </tr>
          </thead>
          <tbody>
            {houses.map(h => (
              <tr key={h.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-2.5 px-3 font-medium">{h.id}</td>
                <td className="py-2.5 px-3">{h.distance} km</td>
                <td className="py-2.5 px-3">
                  <span className={`capitalize ${h.visitStatus === 'completed' ? 'text-success' : h.visitStatus === 'overdue' ? 'text-danger' : 'text-text-secondary'}`}>
                    {h.visitStatus}
                  </span>
                </td>
                <td className="py-2.5 px-3">
                  <div className="flex items-center gap-2">
                    <StatusBadge
                      type={h.priority === 'high' ? 'danger' : h.priority === 'medium' ? 'warning' : 'success'}
                    />
                    <span className="capitalize">{h.priority}</span>
                    <ChevronDown size={14} className="text-text-secondary ml-auto" />
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {/* Action buttons */}
        <div className="flex gap-2 mt-4 pt-4 border-t border-border">
          <button className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
            <ArrowUpDown size={14} /> {t('chvDetail.sortByDistance')}
          </button>
          <button className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-primary hover:bg-primary-light">
            {t('chvDetail.autoOptimize')}
          </button>
          <button className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary ml-auto">
            <RotateCcw size={14} /> {t('chvDetail.reassignHouse')}
          </button>
        </div>
      </div>

      {/* Pending Houses */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.pendingHousesAll')}</h3>
          <div className="flex items-center gap-2 text-xs">
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.overdue')}</button>
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.highPrio')}</button>
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.sz')}</button>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          {/* Table */}
          <table className="text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.house')}</th>
                <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.assignedCHV')}</th>
                <th className="text-left py-2 px-3 text-text-secondary font-medium"></th>
              </tr>
            </thead>
            <tbody>
              {pending.length > 0 ? pending.map(h => (
                <tr key={h.id} className="border-b border-border last:border-0">
                  <td className="py-2 px-3 font-medium">{h.id}</td>
                  <td className="py-2 px-3">{h.assignedCHVName}</td>
                  <td className="py-2 px-3"><StatusBadge type="success" /></td>
                </tr>
              )) : (
                <tr><td colSpan={3} className="py-4 px-3 text-center text-text-secondary">{t('chvDetail.noPendingHouses')}</td></tr>
              )}
              {completed.map(h => (
                <tr key={h.id} className="border-b border-border last:border-0 bg-gray-50/50">
                  <td className="py-2 px-3 font-medium">{h.id}</td>
                  <td className="py-2 px-3">{h.assignedCHVName}</td>
                  <td className="py-2 px-3"><StatusBadge type="success" /></td>
                </tr>
              ))}
            </tbody>
          </table>

          {/* Map placeholder */}
          <div className="bg-gray-100 rounded-lg flex items-center justify-center min-h-[200px] border border-border">
            <div className="text-center text-text-secondary">
              <div className="text-3xl mb-2">&#x1F5FA;</div>
              <p className="text-sm">{t('chvDetail.mapView')}</p>
              <p className="text-xs mt-1">Kibera East</p>
            </div>
          </div>
        </div>
      </div>

      {/* Extended pending houses table */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.pendingHousesAll')}</h3>
          <div className="flex items-center gap-2 text-xs">
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.overdueCHVs')}</button>
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.mornings')}</button>
            <button className="px-2 py-1 border border-border rounded hover:border-primary text-text-secondary">{t('chvDetail.priors')}</button>
          </div>
        </div>

        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-border">
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.house')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.assignedCHV')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.distanceFromClinic')}</th>
              <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.daysPending')}</th>
            </tr>
          </thead>
          <tbody>
            {houses.filter(h => h.visitStatus !== 'completed').map(h => (
              <tr key={h.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-2.5 px-3 font-medium">{h.id}</td>
                <td className="py-2.5 px-3">{h.assignedCHVName}</td>
                <td className="py-2.5 px-3">{h.distance} km</td>
                <td className="py-2.5 px-3">
                  {h.daysPending ? (
                    <StatusBadge type="danger" label={`${h.daysPending} days`} />
                  ) : (
                    <span className="text-text-secondary">-</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
