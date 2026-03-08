import { useState } from 'react';
import { Save, Plus, Trash2, CheckCircle, X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../auth/AuthContext';
import { ALL_MODULES } from '../api/api';

interface ToastState {
  message: string;
  type: 'success' | 'error';
}

export default function SettingsPage() {
  const { user } = useAuth();
  const { t } = useTranslation();
  const organization = user?.organization || localStorage.getItem('adminOrg') || '';

  const [modules, setModules] = useState(
    ALL_MODULES.map(m => ({
      id: m.id,
      name: m.name,
      category: m.type === 'video' ? 'VIDEO' : 'LESSON',
      mandatory: ['video-1', 'video-2', 'video-3', 'video-4'].includes(m.id),
    }))
  );
  const [reportSchedule, setReportSchedule] = useState('daily');
  const [toast, setToast] = useState<ToastState | null>(null);

  // Add module state
  const [showAddModule, setShowAddModule] = useState(false);
  const [newModuleName, setNewModuleName] = useState('');
  const [newModuleCategory, setNewModuleCategory] = useState('');

  // Delete confirm
  const [deleteConfirmId, setDeleteConfirmId] = useState<string | null>(null);

  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

  const handleAddModule = () => {
    if (!newModuleName.trim() || !newModuleCategory.trim()) return;
    const id = `mod-${Date.now()}`;
    setModules([...modules, { id, name: newModuleName.trim(), category: newModuleCategory.trim().toUpperCase(), mandatory: false }]);
    setNewModuleName('');
    setNewModuleCategory('');
    setShowAddModule(false);
    showToast(t('settings.moduleAdded'));
  };

  const handleDeleteModule = (id: string) => {
    setModules(modules.filter(m => m.id !== id));
    setDeleteConfirmId(null);
    showToast(t('settings.moduleDeleted'));
  };

  const handleSaveSettings = () => {
    showToast(t('settings.settingsSaved'));
  };

  return (
    <div className="max-w-3xl">
      {/* Toast notification */}
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-lg shadow-lg text-sm font-medium ${
          toast.type === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
        }`}>
          {toast.type === 'success' ? <CheckCircle size={16} /> : <X size={16} />}
          {toast.message}
        </div>
      )}

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

      {/* Module Management */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-6 mb-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-semibold text-text-primary">{t('settings.moduleManagement')}</h2>
          <button
            onClick={() => setShowAddModule(!showAddModule)}
            className="flex items-center gap-1.5 px-3 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark"
          >
            <Plus size={14} /> {t('settings.addModule')}
          </button>
        </div>

        {/* Add Module Form */}
        {showAddModule && (
          <div className="mb-4 p-4 border border-border rounded-lg bg-gray-50">
            <div className="grid grid-cols-2 gap-3 mb-3">
              <input
                type="text"
                value={newModuleName}
                onChange={e => setNewModuleName(e.target.value)}
                placeholder={t('settings.moduleName')}
                className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
              />
              <input
                type="text"
                value={newModuleCategory}
                onChange={e => setNewModuleCategory(e.target.value)}
                placeholder={t('settings.moduleCategory')}
                className="px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
              />
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleAddModule}
                disabled={!newModuleName.trim() || !newModuleCategory.trim()}
                className="px-3 py-1.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50"
              >
                {t('settings.addModule')}
              </button>
              <button
                onClick={() => { setShowAddModule(false); setNewModuleName(''); setNewModuleCategory(''); }}
                className="px-3 py-1.5 border border-border rounded-lg text-sm text-text-secondary hover:bg-gray-100"
              >
                {t('sidebar.cancel')}
              </button>
            </div>
          </div>
        )}

        <div className="space-y-3">
          {modules.map(mod => (
            <div key={mod.id} className="flex items-center justify-between p-3 border border-border rounded-lg">
              <div>
                <p className="text-sm font-medium text-text-primary">{mod.name}</p>
                <p className="text-xs text-text-secondary">{mod.category} &middot; {mod.id}</p>
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
                {deleteConfirmId === mod.id ? (
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => handleDeleteModule(mod.id)}
                      className="px-2 py-1 bg-danger text-white rounded text-xs font-medium"
                    >
                      {t('settings.confirmDelete')}
                    </button>
                    <button
                      onClick={() => setDeleteConfirmId(null)}
                      className="px-2 py-1 border border-border rounded text-xs text-text-secondary"
                    >
                      {t('sidebar.cancel')}
                    </button>
                  </div>
                ) : (
                  <button
                    onClick={() => setDeleteConfirmId(mod.id)}
                    className="p-1.5 rounded hover:bg-danger-light text-text-secondary hover:text-danger"
                  >
                    <Trash2 size={14} />
                  </button>
                )}
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
      <button
        onClick={handleSaveSettings}
        className="flex items-center gap-2 px-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark"
      >
        <Save size={16} /> {t('settings.saveSettings')}
      </button>
    </div>
  );
}
