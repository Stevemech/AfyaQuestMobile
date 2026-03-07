import { useState } from 'react';
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
  const [sortField, setSortField] = useState<'distance' | 'priority'>('distance');
  const [sortAsc, setSortAsc] = useState(true);
  const [pendingFilter, setPendingFilter] = useState<'all' | 'overdue' | 'high'>('all');
  const [showOptimizeMsg, setShowOptimizeMsg] = useState(false);
  const [showReassignMsg, setShowReassignMsg] = useState(false);

  const pending = houses.filter(h => h.visitStatus === 'pending');
  const completed = houses.filter(h => h.visitStatus === 'completed');
  const totalHouses = chv.assignedHouses ?? houses.length;
  const completedCount = chv.completedVisits ?? completed.length;
  const pendingCount = chv.pendingVisits ?? pending.length;

  const priorityOrder = { high: 0, medium: 1, low: 2 };

  const sortedHouses = [...houses].sort((a, b) => {
    if (sortField === 'distance') {
      return sortAsc ? a.distance - b.distance : b.distance - a.distance;
    }
    return sortAsc
      ? (priorityOrder[a.priority] ?? 1) - (priorityOrder[b.priority] ?? 1)
      : (priorityOrder[b.priority] ?? 1) - (priorityOrder[a.priority] ?? 1);
  });

  const filteredPending = houses.filter(h => {
    if (h.visitStatus === 'completed') return false;
    if (pendingFilter === 'overdue') return h.visitStatus === 'overdue' || (h.daysPending && h.daysPending > 5);
    if (pendingFilter === 'high') return h.priority === 'high';
    return true;
  });

  const handleSort = (field: 'distance' | 'priority') => {
    if (sortField === field) {
      setSortAsc(!sortAsc);
    } else {
      setSortField(field);
      setSortAsc(true);
    }
  };

  const handleAutoOptimize = () => {
    setSortField('distance');
    setSortAsc(true);
    setShowOptimizeMsg(true);
    setTimeout(() => setShowOptimizeMsg(false), 2000);
  };

  const handleReassign = () => {
    setShowReassignMsg(true);
    setTimeout(() => setShowReassignMsg(false), 2000);
  };

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
                {chv.totalPoints != null && <span className="ml-2">{chv.totalPoints} {t('chvList.points')}</span>}
              </p>
            </div>
          </div>
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
            <button
              onClick={() => handleSort('distance')}
              className={`flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm hover:border-primary ${
                sortField === 'distance' ? 'text-primary border-primary' : 'text-text-secondary'
              }`}
            >
              {t('chvDetail.distance')} <ChevronDown size={14} className={sortField === 'distance' && !sortAsc ? 'rotate-180' : ''} />
            </button>
            <button
              onClick={handleReassign}
              className="flex items-center gap-1 px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:border-primary"
            >
              <RotateCcw size={14} /> {t('chvDetail.reassignHouse')}
            </button>
          </div>
        </div>

        {showReassignMsg && (
          <div className="mb-3 p-2 bg-blue-50 text-blue-700 text-sm rounded-lg">{t('chvDetail.reassignComingSoon')}</div>
        )}

        {sortedHouses.length === 0 ? (
          <div className="py-8 text-center text-text-secondary text-sm">
            {t('chvDetail.noHousesAssigned')}
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border">
                <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.houseId')}</th>
                <th
                  className="text-left py-2 px-3 text-text-secondary font-medium cursor-pointer hover:text-primary"
                  onClick={() => handleSort('distance')}
                >
                  {t('chvDetail.distanceKm')} {sortField === 'distance' && (sortAsc ? '↑' : '↓')}
                </th>
                <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.visitStatus')}</th>
                <th
                  className="text-left py-2 px-3 text-text-secondary font-medium cursor-pointer hover:text-primary"
                  onClick={() => handleSort('priority')}
                >
                  {t('chvDetail.priority')} {sortField === 'priority' && (sortAsc ? '↑' : '↓')}
                </th>
              </tr>
            </thead>
            <tbody>
              {sortedHouses.map(h => (
                <tr key={h.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                  <td className="py-2.5 px-3 font-medium">{h.id}</td>
                  <td className="py-2.5 px-3">{h.distance} km</td>
                  <td className="py-2.5 px-3">
                    <span className={`capitalize ${h.visitStatus === 'completed' ? 'text-success' : h.visitStatus === 'overdue' ? 'text-danger' : 'text-text-secondary'}`}>
                      {t(`chvDetail.visitStatus_${h.visitStatus}`)}
                    </span>
                  </td>
                  <td className="py-2.5 px-3">
                    <div className="flex items-center gap-2">
                      <StatusBadge
                        type={h.priority === 'high' ? 'danger' : h.priority === 'medium' ? 'warning' : 'success'}
                      />
                      <span className="capitalize">{t(`chvDetail.priority_${h.priority}`)}</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {/* Action buttons */}
        <div className="flex gap-2 mt-4 pt-4 border-t border-border">
          <button
            onClick={() => handleSort('distance')}
            className={`flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm hover:border-primary ${
              sortField === 'distance' ? 'text-primary border-primary' : 'text-text-secondary'
            }`}
          >
            <ArrowUpDown size={14} /> {t('chvDetail.sortByDistance')}
          </button>
          <button
            onClick={handleAutoOptimize}
            className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-primary hover:bg-primary-light"
          >
            {t('chvDetail.autoOptimize')}
          </button>
          <button
            onClick={handleReassign}
            className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary ml-auto"
          >
            <RotateCcw size={14} /> {t('chvDetail.reassignHouse')}
          </button>
        </div>
        {showOptimizeMsg && (
          <div className="mt-2 p-2 bg-green-50 text-green-700 text-sm rounded-lg">{t('chvDetail.routeOptimized')}</div>
        )}
      </div>

      {/* Pending Houses - CHV specific */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.pendingHousesCHV')}</h3>
          <div className="flex items-center gap-2 text-xs">
            <button
              onClick={() => setPendingFilter(pendingFilter === 'overdue' ? 'all' : 'overdue')}
              className={`px-2 py-1 border rounded hover:border-primary ${
                pendingFilter === 'overdue' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'
              }`}
            >
              {t('chvDetail.overdue')}
            </button>
            <button
              onClick={() => setPendingFilter(pendingFilter === 'high' ? 'all' : 'high')}
              className={`px-2 py-1 border rounded hover:border-primary ${
                pendingFilter === 'high' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'
              }`}
            >
              {t('chvDetail.highPriority')}
            </button>
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
                  <td className="py-2 px-3"><StatusBadge type="warning" /></td>
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
              <p className="text-xs mt-1">{chv.organization || chv.clinic || 'Kibera East'}</p>
              <p className="text-xs mt-2 text-text-secondary italic">{t('chvDetail.mapComingSoon')}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Extended pending houses table - All CHVs */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.pendingHousesAllCHVs')}</h3>
          <div className="flex items-center gap-2 text-xs">
            <button
              onClick={() => setPendingFilter(pendingFilter === 'overdue' ? 'all' : 'overdue')}
              className={`px-2 py-1 border rounded hover:border-primary ${
                pendingFilter === 'overdue' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'
              }`}
            >
              {t('chvDetail.overdueCHVs')}
            </button>
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
            {filteredPending.length > 0 ? filteredPending.map(h => (
              <tr key={h.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-2.5 px-3 font-medium">{h.id}</td>
                <td className="py-2.5 px-3">{h.assignedCHVName}</td>
                <td className="py-2.5 px-3">{h.distance} km</td>
                <td className="py-2.5 px-3">
                  {h.daysPending ? (
                    <StatusBadge type="danger" label={`${h.daysPending} ${t('chvDetail.days')}`} />
                  ) : (
                    <span className="text-text-secondary">-</span>
                  )}
                </td>
              </tr>
            )) : (
              <tr>
                <td colSpan={4} className="py-6 text-center text-text-secondary text-sm">{t('chvDetail.noPendingHouses')}</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
