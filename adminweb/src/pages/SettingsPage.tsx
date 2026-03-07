import { useState } from 'react';
import { Save, Plus, Trash2, Send, MapPin } from 'lucide-react';
import { api } from '../api/api';
import { useAuth } from '../auth/AuthContext';

export default function SettingsPage() {
  const { user } = useAuth();
  const organization = user?.organization || localStorage.getItem('adminOrg') || '';

  const [modules, setModules] = useState([
    { id: 'mod-1', name: 'Basic Health Training', category: 'BASICS', mandatory: true },
    { id: 'mod-2', name: 'Sanitation & Hygiene', category: 'SANITATION', mandatory: true },
    { id: 'mod-3', name: 'Maternal Health', category: 'MATERNAL', mandatory: false },
    { id: 'mod-4', name: 'Immunization Protocols', category: 'IMMUNIZATION', mandatory: false },
  ]);
  const [reportSchedule, setReportSchedule] = useState('daily');

  // Assign module to CHV state
  const [assignChvId, setAssignChvId] = useState('');
  const [assignModuleId, setAssignModuleId] = useState('');
  const [assignLoading, setAssignLoading] = useState(false);

  // Create itinerary state
  const [itChvId, setItChvId] = useState('');
  const [itDate, setItDate] = useState('');
  const [itStops, setItStops] = useState('');
  const [itLoading, setItLoading] = useState(false);

  const handleAssignModule = async () => {
    if (!assignChvId || !assignModuleId) return;
    setAssignLoading(true);
    try {
      await api.assignModule(assignChvId, assignModuleId);
      alert('Module assigned successfully');
      setAssignChvId('');
      setAssignModuleId('');
    } catch (err) {
      alert(`Failed to assign module: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setAssignLoading(false);
    }
  };

  const handleCreateItinerary = async () => {
    if (!itChvId || !itDate) return;
    setItLoading(true);
    try {
      const stops = itStops ? JSON.parse(itStops) : [];
      await api.createItinerary(itChvId, itDate, stops);
      alert('Itinerary created successfully');
      setItChvId('');
      setItDate('');
      setItStops('');
    } catch (err) {
      alert(`Failed to create itinerary: ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setItLoading(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="text-xl font-semibold text-text-primary mb-6">Settings</h1>

      {/* Organization Info */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">Organization</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">Organization</label>
            <input
              type="text"
              value={organization}
              readOnly
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm bg-gray-50 text-text-secondary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">Admin</label>
            <input
              type="text"
              value={user?.name || ''}
              readOnly
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm bg-gray-50 text-text-secondary"
            />
          </div>
        </div>
      </div>

      {/* Assign Module to CHV */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">Assign Module to CHV</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">CHV ID</label>
            <input
              type="text"
              value={assignChvId}
              onChange={e => setAssignChvId(e.target.value)}
              placeholder="e.g. chv-1"
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">Module ID</label>
            <input
              type="text"
              value={assignModuleId}
              onChange={e => setAssignModuleId(e.target.value)}
              placeholder="e.g. mod-1"
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
        </div>
        <button
          onClick={handleAssignModule}
          disabled={assignLoading || !assignChvId || !assignModuleId}
          className="mt-4 flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50"
        >
          <Send size={14} /> {assignLoading ? 'Assigning...' : 'Assign Module'}
        </button>
      </div>

      {/* Create Itinerary */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">Create Itinerary</h2>
        <div className="grid grid-cols-2 gap-4">
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
        </div>
        <div className="mt-4">
          <label className="block text-sm font-medium text-text-primary mb-1.5">Stops (JSON array)</label>
          <textarea
            value={itStops}
            onChange={e => setItStops(e.target.value)}
            placeholder='[{"order":1,"houseId":"H-1023","label":"House 1","address":"123 St","latitude":-1.31,"longitude":36.78}]'
            rows={4}
            className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary font-mono"
          />
        </div>
        <button
          onClick={handleCreateItinerary}
          disabled={itLoading || !itChvId || !itDate}
          className="mt-4 flex items-center gap-2 px-4 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50"
        >
          <MapPin size={14} /> {itLoading ? 'Creating...' : 'Create Itinerary'}
        </button>
      </div>

      {/* Module Management */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-text-primary">Module Management</h2>
          <button className="flex items-center gap-1.5 px-3 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
            <Plus size={14} /> Add Module
          </button>
        </div>
        <div className="space-y-3">
          {modules.map(mod => (
            <div key={mod.id} className="flex items-center justify-between p-3 border border-border rounded-lg">
              <div>
                <p className="text-sm font-medium text-text-primary">{mod.name}</p>
                <p className="text-xs text-text-secondary">{mod.category}</p>
              </div>
              <div className="flex items-center gap-3">
                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={mod.mandatory}
                    onChange={e => {
                      setModules(modules.map(m => m.id === mod.id ? { ...m, mandatory: e.target.checked } : m));
                    }}
                    className="rounded border-border text-primary focus:ring-primary"
                  />
                  Mandatory
                </label>
                <button className="p-1.5 rounded hover:bg-danger-light text-text-secondary hover:text-danger">
                  <Trash2 size={14} />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Report Settings */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">Report Settings</h2>
        <div>
          <label className="block text-sm font-medium text-text-primary mb-1.5">Report Schedule</label>
          <select
            value={reportSchedule}
            onChange={e => setReportSchedule(e.target.value)}
            className="px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
          >
            <option value="daily">Daily</option>
            <option value="weekly">Weekly</option>
            <option value="biweekly">Bi-Weekly</option>
          </select>
        </div>
      </div>

      {/* Save */}
      <button className="flex items-center gap-2 px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
        <Save size={16} /> Save Settings
      </button>
    </div>
  );
}
