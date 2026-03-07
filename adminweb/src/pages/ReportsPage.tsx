import { useState, useEffect } from 'react';
import { Download, Eye, Plus, X } from 'lucide-react';
import { api } from '../api/api';
import StatusBadge from '../components/common/StatusBadge';
import type { DailyReport } from '../types';

export default function ReportsPage() {
  const [reports, setReports] = useState<DailyReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filterCHV, setFilterCHV] = useState('all');
  const [filterDate, setFilterDate] = useState('');
  const [selectedReport, setSelectedReport] = useState<string | null>(null);
  const [itineraryModal, setItineraryModal] = useState(false);
  const [itChvId, setItChvId] = useState('');
  const [itDate, setItDate] = useState('');
  const [itStops, setItStops] = useState('');
  const [itSubmitting, setItSubmitting] = useState(false);

  useEffect(() => {
    setLoading(true);
    setError('');
    api.getReports()
      .then(res => setReports(res.reports || []))
      .catch(err => setError(err.message || 'Failed to load reports'))
      .finally(() => setLoading(false));
  }, []);

  const filtered = reports.filter(r => {
    if (filterCHV !== 'all' && r.userId !== filterCHV) return false;
    if (filterDate && r.date !== filterDate) return false;
    return true;
  });

  const uniqueCHVs = [...new Set(reports.map(r => r.userName))];

  const handleCreateItinerary = async () => {
    if (!itChvId || !itDate) return;
    setItSubmitting(true);
    try {
      const stops = itStops ? JSON.parse(itStops) : [];
      await api.createItinerary(itChvId, itDate, stops);
      alert('Itinerary created successfully');
      setItineraryModal(false);
      setItChvId('');
      setItDate('');
      setItStops('');
    } catch (err) {
      alert(`Failed to create itinerary: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setItSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-text-secondary">Loading reports...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="flex items-center justify-center py-20">
        <div className="text-center">
          <p className="text-danger mb-2">{error}</p>
          <button onClick={() => window.location.reload()} className="text-sm text-primary hover:underline">
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-text-primary">Reports Archive</h1>
        <button
          onClick={() => setItineraryModal(true)}
          className="flex items-center gap-1.5 px-4 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark"
        >
          <Plus size={14} /> Create Itinerary
        </button>
      </div>

      {/* Filters */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-4 mb-6">
        <div className="flex items-center gap-4">
          <div>
            <label className="block text-xs text-text-secondary mb-1">CHV</label>
            <select
              value={filterCHV}
              onChange={e => setFilterCHV(e.target.value)}
              className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            >
              <option value="all">All CHVs</option>
              {uniqueCHVs.map(name => (
                <option key={name} value={reports.find(r => r.userName === name)?.userId}>{name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-xs text-text-secondary mb-1">Date</label>
            <input
              type="date"
              value={filterDate}
              onChange={e => setFilterDate(e.target.value)}
              className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
          <button className="mt-4 flex items-center gap-1.5 px-4 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary">
            <Download size={14} /> Export CSV
          </button>
        </div>
      </div>

      {/* Reports table */}
      <div className="bg-white rounded-xl border border-border shadow-sm overflow-hidden">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-gray-50 border-b border-border">
              <th className="text-left py-3 px-4 text-text-secondary font-medium">CHV</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Date</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Patients</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Vaccinations</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Health Ed.</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Challenges</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium">Synced</th>
              <th className="text-left py-3 px-4 text-text-secondary font-medium"></th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(r => (
              <tr key={r.id} className="border-b border-border last:border-0 hover:bg-gray-50">
                <td className="py-3 px-4 font-medium text-text-primary">{r.userName}</td>
                <td className="py-3 px-4 text-text-secondary">{r.date}</td>
                <td className="py-3 px-4">{r.patientsVisited}</td>
                <td className="py-3 px-4">{r.vaccinationsGiven}</td>
                <td className="py-3 px-4">{r.healthEducation}</td>
                <td className="py-3 px-4 text-text-secondary max-w-[200px] truncate">{r.challenges || '-'}</td>
                <td className="py-3 px-4">
                  <StatusBadge type={r.isSynced ? 'success' : 'warning'} />
                </td>
                <td className="py-3 px-4">
                  <button
                    onClick={() => setSelectedReport(selectedReport === r.id ? null : r.id)}
                    className="p-1.5 rounded hover:bg-gray-100"
                  >
                    <Eye size={16} className="text-text-secondary" />
                  </button>
                </td>
              </tr>
            ))}
            {filtered.length === 0 && (
              <tr>
                <td colSpan={8} className="py-8 text-center text-text-secondary">No reports found</td>
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
              <h3 className="text-lg font-semibold mb-4">Report Detail - {r.userName}</h3>
              <div className="space-y-3 text-sm">
                <div className="flex justify-between"><span className="text-text-secondary">Date</span><span>{r.date}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">Patients Visited</span><span>{r.patientsVisited}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">Vaccinations Given</span><span>{r.vaccinationsGiven}</span></div>
                <div className="flex justify-between"><span className="text-text-secondary">Health Education</span><span>{r.healthEducation}</span></div>
                <div><span className="text-text-secondary block mb-1">Challenges</span><p>{r.challenges || 'None'}</p></div>
                <div><span className="text-text-secondary block mb-1">Notes</span><p>{r.notes || 'None'}</p></div>
              </div>
              <button onClick={() => setSelectedReport(null)} className="mt-6 w-full py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
                Close
              </button>
            </div>
          </div>
        );
      })()}

      {/* Create Itinerary modal */}
      {itineraryModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setItineraryModal(false)}>
          <div className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">Create Itinerary</h3>
              <button onClick={() => setItineraryModal(false)} className="p-1 rounded hover:bg-gray-100">
                <X size={20} className="text-text-secondary" />
              </button>
            </div>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1.5">CHV ID</label>
                <input
                  type="text"
                  value={itChvId}
                  onChange={e => setItChvId(e.target.value)}
                  placeholder="e.g. chv-1"
                  className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1.5">Date</label>
                <input
                  type="date"
                  value={itDate}
                  onChange={e => setItDate(e.target.value)}
                  className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-text-primary mb-1.5">Stops (JSON array)</label>
                <textarea
                  value={itStops}
                  onChange={e => setItStops(e.target.value)}
                  placeholder='[{"order":1,"houseId":"H-1023","label":"House 1","address":"123 St","latitude":-1.31,"longitude":36.78}]'
                  rows={4}
                  className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary font-mono"
                />
              </div>
            </div>
            <button
              onClick={handleCreateItinerary}
              disabled={itSubmitting || !itChvId || !itDate}
              className="mt-6 w-full py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50"
            >
              {itSubmitting ? 'Creating...' : 'Create Itinerary'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
