import { useState } from 'react';
import { Save, Plus, Trash2, Send, MapPin } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import { useAuth } from '../auth/AuthContext';

export default function SettingsPage() {
  const { user } = useAuth();
  const { t } = useTranslation();
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
      alert(t('settings.moduleAssigned'));
      setAssignChvId('');
      setAssignModuleId('');
    } catch (err) {
      alert(`${t('settings.failedAssign')} ${err instanceof Error ? err.message : 'Unknown error'}`);
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
      alert(t('settings.itineraryCreated'));
      setItChvId('');
      setItDate('');
      setItStops('');
    } catch (err) {
      alert(`${t('settings.failedItinerary')} ${err instanceof Error ? err.message : 'Unknown error'}`);
    } finally {
      setItLoading(false);
    }
  };

  return (
    <div className="max-w-3xl">
      <h1 className="text-xl font-semibold text-text-primary mb-6">{t('settings.title')}</h1>

      {/* Organization Info */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">{t('settings.organization')}</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.organization')}</label>
            <input
              type="text"
              value={organization}
              readOnly
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm bg-gray-50 text-text-secondary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('admin')}</label>
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
        <h2 className="text-lg font-semibold text-text-primary mb-4">{t('settings.assignModuleToCHV')}</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.chvId')}</label>
            <input
              type="text"
              value={assignChvId}
              onChange={e => setAssignChvId(e.target.value)}
              placeholder="e.g. chv-1"
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.moduleId')}</label>
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
          <Send size={14} /> {assignLoading ? t('settings.assigning') : t('settings.assignModule')}
        </button>
      </div>

      {/* Create Itinerary */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <h2 className="text-lg font-semibold text-text-primary mb-4">{t('settings.createItinerary')}</h2>
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.chvId')}</label>
            <input
              type="text"
              value={itChvId}
              onChange={e => setItChvId(e.target.value)}
              placeholder="e.g. chv-1"
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-text-primary mb-1.5">{t('date')}</label>
            <input
              type="date"
              value={itDate}
              onChange={e => setItDate(e.target.value)}
              className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
            />
          </div>
        </div>
        <div className="mt-4">
          <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.stopsJson')}</label>
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
          <MapPin size={14} /> {itLoading ? t('settings.creating') : t('settings.createItinerary')}
        </button>
      </div>

      {/* Module Management */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-text-primary">{t('settings.moduleManagement')}</h2>
          <button className="flex items-center gap-1.5 px-3 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
            <Plus size={14} /> {t('settings.addModule')}
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
                  {t('settings.mandatory')}
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
        <h2 className="text-lg font-semibold text-text-primary mb-4">{t('settings.reportSettings')}</h2>
        <div>
          <label className="block text-sm font-medium text-text-primary mb-1.5">{t('settings.reportSchedule')}</label>
          <select
            value={reportSchedule}
            onChange={e => setReportSchedule(e.target.value)}
            className="px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
          >
            <option value="daily">{t('settings.daily')}</option>
            <option value="weekly">{t('settings.weekly')}</option>
            <option value="biweekly">{t('settings.biWeekly')}</option>
          </select>
        </div>
      </div>

      {/* Save */}
      <button className="flex items-center gap-2 px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark">
        <Save size={16} /> {t('settings.saveSettings')}
      </button>
    </div>
  );
}
