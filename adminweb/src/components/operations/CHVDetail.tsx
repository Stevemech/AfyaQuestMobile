import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronDown, ArrowUpDown, RotateCcw, Send, MapPin, Plus, Trash2, X, ChevronRight, CheckCircle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api, ALL_MODULES } from '../../api/api';
import type { CHV, House } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVDetailProps {
  chv: CHV;
  houses: House[];
}

interface StopEntry {
  label: string;
  address: string;
  description: string;
  notes: string;
  latitude: string;
  longitude: string;
}

const emptyStop = (): StopEntry => ({ label: '', address: '', description: '', notes: '', latitude: '', longitude: '' });

// Google Maps Places Autocomplete hook
function useGooglePlaces() {
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    if ((window as any).google?.maps?.places) {
      setLoaded(true);
      return;
    }
    // Check if script is already loading
    if (document.querySelector('script[src*="maps.googleapis.com/maps/api/js"]')) {
      const check = setInterval(() => {
        if ((window as any).google?.maps?.places) {
          setLoaded(true);
          clearInterval(check);
        }
      }, 200);
      return () => clearInterval(check);
    }
    // Load Google Maps script - uses env variable
    const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY;
    if (!key) return;
    const script = document.createElement('script');
    script.src = `https://maps.googleapis.com/maps/api/js?key=${key}&libraries=places`;
    script.async = true;
    script.onload = () => setLoaded(true);
    document.head.appendChild(script);
  }, []);

  return loaded;
}

function AddressAutocomplete({ value, onChange, onPlaceSelect, placeholder, className }: {
  value: string;
  onChange: (val: string) => void;
  onPlaceSelect: (address: string, lat: number, lng: number) => void;
  placeholder: string;
  className: string;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const autocompleteRef = useRef<google.maps.places.Autocomplete | null>(null);
  const placesLoaded = useGooglePlaces();

  useEffect(() => {
    if (!placesLoaded || !inputRef.current || autocompleteRef.current) return;
    try {
      const ac = new google.maps.places.Autocomplete(inputRef.current, {
        types: ['address'],
        fields: ['formatted_address', 'geometry'],
      });
      ac.addListener('place_changed', () => {
        const place = ac.getPlace();
        if (place.geometry?.location) {
          const lat = place.geometry.location.lat();
          const lng = place.geometry.location.lng();
          onPlaceSelect(place.formatted_address || '', lat, lng);
        }
      });
      autocompleteRef.current = ac;
    } catch {
      // Google Maps not available, fall back to plain input
    }
  }, [placesLoaded, onPlaceSelect]);

  return (
    <input
      ref={inputRef}
      type="text"
      value={value}
      onChange={e => onChange(e.target.value)}
      placeholder={placeholder}
      className={className}
    />
  );
}

export default function CHVDetail({ chv, houses }: CHVDetailProps) {
  const { t } = useTranslation();
  const [sortField, setSortField] = useState<'distance' | 'priority'>('distance');
  const [sortAsc, setSortAsc] = useState(true);
  const [pendingFilter, setPendingFilter] = useState<'all' | 'overdue' | 'high'>('all');
  const [showOptimizeMsg, setShowOptimizeMsg] = useState(false);
  const [showReassignMsg, setShowReassignMsg] = useState(false);

  // Assign module modal
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [assignModuleId, setAssignModuleId] = useState('');
  const [assignLoading, setAssignLoading] = useState(false);
  const [assignError, setAssignError] = useState('');

  // Create itinerary modal
  const [showItineraryModal, setShowItineraryModal] = useState(false);
  const [itDate, setItDate] = useState('');
  const [itStops, setItStops] = useState<StopEntry[]>([emptyStop()]);
  const [itShowAdvanced, setItShowAdvanced] = useState(false);
  const [itRawJson, setItRawJson] = useState('');
  const [itLoading, setItLoading] = useState(false);
  const [itError, setItError] = useState('');

  // Toast
  const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' } | null>(null);
  const showToast = (message: string, type: 'success' | 'error' = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  };

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
    if (sortField === field) setSortAsc(!sortAsc);
    else { setSortField(field); setSortAsc(true); }
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

  // Assign module or lesson
  const handleAssignModule = async () => {
    setAssignError('');
    if (!assignModuleId) { setAssignError(t('itinerary.selectModuleError')); return; }
    setAssignLoading(true);
    try {
      const selected = ALL_MODULES.find(m => m.id === assignModuleId);
      if (selected?.type === 'lesson') {
        await api.assignLesson(chv.id, assignModuleId);
      } else {
        await api.assignModule(chv.id, assignModuleId);
      }
      showToast(t('settings.moduleAssigned'));
      setShowAssignModal(false);
      setAssignModuleId('');
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setAssignLoading(false);
    }
  };

  // Create itinerary
  const handleCreateItinerary = async () => {
    setItError('');
    if (!itDate) { setItError(t('settings.dateRequired')); return; }

    setItLoading(true);
    try {
      let stops;
      if (itShowAdvanced && itRawJson.trim()) {
        stops = JSON.parse(itRawJson);
      } else {
        const validStops = itStops.filter(s => s.label.trim());
        if (validStops.length === 0) { setItError(t('itinerary.addAtLeastOneStop')); setItLoading(false); return; }
        stops = validStops.map((s, i) => ({
          order: i + 1,
          houseId: `H-${Date.now()}-${i}`,
          label: s.label.trim(),
          address: s.address.trim(),
          description: s.description.trim() || undefined,
          notes: s.notes.trim() || undefined,
          latitude: s.latitude ? parseFloat(s.latitude) : 0,
          longitude: s.longitude ? parseFloat(s.longitude) : 0,
        }));
      }
      await api.createItinerary(chv.id, itDate, stops);
      showToast(t('settings.itineraryCreated'));
      setShowItineraryModal(false);
      setItDate('');
      setItStops([emptyStop()]);
      setItRawJson('');
      setItShowAdvanced(false);
    } catch (err) {
      setItError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setItLoading(false);
    }
  };

  const updateStop = (index: number, field: keyof StopEntry, value: string) => {
    setItStops(itStops.map((s, i) => i === index ? { ...s, [field]: value } : s));
  };

  const handlePlaceSelect = useCallback((index: number, address: string, lat: number, lng: number) => {
    setItStops(prev => prev.map((s, i) => i === index ? { ...s, address, latitude: lat.toString(), longitude: lng.toString() } : s));
  }, []);

  const addStop = () => setItStops([...itStops, emptyStop()]);
  const removeStop = (index: number) => {
    if (itStops.length <= 1) return;
    setItStops(itStops.filter((_, i) => i !== index));
  };

  const hasGoogleMapsKey = !!import.meta.env.VITE_GOOGLE_MAPS_API_KEY;

  return (
    <div className="space-y-5">
      {/* Toast */}
      {toast && (
        <div className={`fixed top-4 right-4 z-50 flex items-center gap-2 px-4 py-3 rounded-lg shadow-lg text-sm font-medium ${
          toast.type === 'success' ? 'bg-green-50 text-green-700 border border-green-200' : 'bg-red-50 text-red-700 border border-red-200'
        }`}>
          {toast.type === 'success' ? <CheckCircle size={16} /> : <X size={16} />}
          {toast.message}
        </div>
      )}

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
          <div className="flex items-center gap-2">
            <button
              onClick={() => { setShowAssignModal(true); setAssignModuleId(''); setAssignError(''); }}
              className="flex items-center gap-1.5 px-3 py-2 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark"
            >
              <Send size={14} /> {t('itinerary.assignModule')}
            </button>
            <button
              onClick={() => { setShowItineraryModal(true); setItDate(''); setItStops([emptyStop()]); setItRawJson(''); setItShowAdvanced(false); setItError(''); }}
              className="flex items-center gap-1.5 px-3 py-2 border border-primary text-primary rounded-lg text-sm font-medium hover:bg-primary-light"
            >
              <MapPin size={14} /> {t('itinerary.createItinerary')}
            </button>
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
                <th className="text-left py-2 px-3 text-text-secondary font-medium cursor-pointer hover:text-primary" onClick={() => handleSort('distance')}>
                  {t('chvDetail.distanceKm')} {sortField === 'distance' && (sortAsc ? '\u2191' : '\u2193')}
                </th>
                <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.visitStatus')}</th>
                <th className="text-left py-2 px-3 text-text-secondary font-medium cursor-pointer hover:text-primary" onClick={() => handleSort('priority')}>
                  {t('chvDetail.priority')} {sortField === 'priority' && (sortAsc ? '\u2191' : '\u2193')}
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
                      <StatusBadge type={h.priority === 'high' ? 'danger' : h.priority === 'medium' ? 'warning' : 'success'} />
                      <span className="capitalize">{t(`chvDetail.priority_${h.priority}`)}</span>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        <div className="flex gap-2 mt-4 pt-4 border-t border-border">
          <button onClick={() => handleSort('distance')} className={`flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm hover:border-primary ${sortField === 'distance' ? 'text-primary border-primary' : 'text-text-secondary'}`}>
            <ArrowUpDown size={14} /> {t('chvDetail.sortByDistance')}
          </button>
          <button onClick={handleAutoOptimize} className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-primary hover:bg-primary-light">
            {t('chvDetail.autoOptimize')}
          </button>
          <button onClick={handleReassign} className="flex items-center gap-1.5 px-3 py-2 border border-border rounded-lg text-sm text-text-secondary hover:border-primary ml-auto">
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
            <button onClick={() => setPendingFilter(pendingFilter === 'overdue' ? 'all' : 'overdue')} className={`px-2 py-1 border rounded hover:border-primary ${pendingFilter === 'overdue' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'}`}>
              {t('chvDetail.overdue')}
            </button>
            <button onClick={() => setPendingFilter(pendingFilter === 'high' ? 'all' : 'high')} className={`px-2 py-1 border rounded hover:border-primary ${pendingFilter === 'high' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'}`}>
              {t('chvDetail.highPriority')}
            </button>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4">
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
          <div className="bg-gray-100 rounded-lg flex items-center justify-center min-h-[200px] border border-border">
            <div className="text-center text-text-secondary">
              <div className="text-3xl mb-2">&#x1F5FA;</div>
              <p className="text-sm">{t('chvDetail.mapView')}</p>
              <p className="text-xs mt-1">{chv.organization || chv.clinic || ''}</p>
              <p className="text-xs mt-2 text-text-secondary italic">{t('chvDetail.mapComingSoon')}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Extended pending houses table */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <div className="flex items-center justify-between mb-4">
          <h3 className="font-semibold text-text-primary">{t('chvDetail.pendingHousesAllCHVs')}</h3>
          <div className="flex items-center gap-2 text-xs">
            <button onClick={() => setPendingFilter(pendingFilter === 'overdue' ? 'all' : 'overdue')} className={`px-2 py-1 border rounded hover:border-primary ${pendingFilter === 'overdue' ? 'border-primary text-primary bg-primary-light' : 'border-border text-text-secondary'}`}>
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
              <tr><td colSpan={4} className="py-6 text-center text-text-secondary text-sm">{t('chvDetail.noPendingHouses')}</td></tr>
            )}
          </tbody>
        </table>
      </div>

      {/* === Assign Module Modal === */}
      {showAssignModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setShowAssignModal(false)}>
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{t('itinerary.assignModuleTo', { name: chv.name })}</h3>
              <button onClick={() => setShowAssignModal(false)} className="p-1 rounded hover:bg-gray-100"><X size={20} className="text-text-secondary" /></button>
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
              onClick={handleAssignModule}
              disabled={assignLoading || !assignModuleId}
              className="mt-6 w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <Send size={14} /> {assignLoading ? t('settings.assigning') : t('itinerary.assignModule')}
            </button>
          </div>
        </div>
      )}

      {/* === Create Itinerary Modal === */}
      {showItineraryModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setShowItineraryModal(false)}>
          <div className="bg-white rounded-xl shadow-xl max-w-2xl w-full mx-4 p-6 max-h-[90vh] overflow-y-auto" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{t('itinerary.createFor', { name: chv.name })}</h3>
              <button onClick={() => setShowItineraryModal(false)} className="p-1 rounded hover:bg-gray-100"><X size={20} className="text-text-secondary" /></button>
            </div>

            {itError && <div className="mb-3 p-2 bg-danger-light text-danger text-sm rounded-lg">{itError}</div>}

            {/* Date */}
            <div className="mb-5">
              <label className="block text-sm font-medium text-text-primary mb-1.5">{t('itinerary.visitDate')}</label>
              <input
                type="date"
                value={itDate}
                onChange={e => { setItDate(e.target.value); setItError(''); }}
                className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
              />
            </div>

            {/* Stops */}
            {!itShowAdvanced && (
              <div>
                <div className="flex items-center justify-between mb-3">
                  <label className="text-sm font-medium text-text-primary">{t('itinerary.stops')}</label>
                  <button onClick={addStop} className="flex items-center gap-1 text-xs text-primary hover:underline">
                    <Plus size={12} /> {t('itinerary.addStop')}
                  </button>
                </div>

                <div className="space-y-4">
                  {itStops.map((stop, i) => (
                    <div key={i} className="border border-border rounded-lg p-4 relative">
                      <div className="flex items-center justify-between mb-3">
                        <span className="text-xs font-semibold text-text-secondary uppercase">{t('itinerary.stopNumber', { n: i + 1 })}</span>
                        {itStops.length > 1 && (
                          <button onClick={() => removeStop(i)} className="p-1 rounded hover:bg-danger-light text-text-secondary hover:text-danger">
                            <Trash2 size={14} />
                          </button>
                        )}
                      </div>
                      <div className="grid grid-cols-2 gap-3">
                        <div className="col-span-2">
                          <label className="block text-xs text-text-secondary mb-1">{t('itinerary.houseName')}</label>
                          <input type="text" value={stop.label} onChange={e => updateStop(i, 'label', e.target.value)}
                            placeholder={t('itinerary.houseNamePlaceholder')}
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary" />
                        </div>
                        <div className="col-span-2">
                          <label className="block text-xs text-text-secondary mb-1">
                            {t('itinerary.address')}
                            {hasGoogleMapsKey && <span className="ml-1 text-primary">({t('itinerary.autocompleteEnabled')})</span>}
                          </label>
                          <AddressAutocomplete
                            value={stop.address}
                            onChange={val => updateStop(i, 'address', val)}
                            onPlaceSelect={(address, lat, lng) => handlePlaceSelect(i, address, lat, lng)}
                            placeholder={t('itinerary.addressPlaceholder')}
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
                          />
                        </div>
                        <div className="col-span-2">
                          <label className="block text-xs text-text-secondary mb-1">{t('itinerary.description')}</label>
                          <input type="text" value={stop.description} onChange={e => updateStop(i, 'description', e.target.value)}
                            placeholder={t('itinerary.descriptionPlaceholder')}
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary" />
                        </div>
                        <div className="col-span-2">
                          <label className="block text-xs text-text-secondary mb-1">{t('itinerary.notesToCHV')}</label>
                          <textarea value={stop.notes} onChange={e => updateStop(i, 'notes', e.target.value)}
                            placeholder={t('itinerary.notesPlaceholder')}
                            rows={2}
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary" />
                        </div>
                        <div>
                          <label className="block text-xs text-text-secondary mb-1">{t('itinerary.latitude')}</label>
                          <input type="text" value={stop.latitude} onChange={e => updateStop(i, 'latitude', e.target.value)}
                            placeholder="14.6349"
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary font-mono" />
                        </div>
                        <div>
                          <label className="block text-xs text-text-secondary mb-1">{t('itinerary.longitude')}</label>
                          <input type="text" value={stop.longitude} onChange={e => updateStop(i, 'longitude', e.target.value)}
                            placeholder="-90.5069"
                            className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary font-mono" />
                        </div>
                      </div>
                      {stop.latitude && stop.longitude && (
                        <div className="mt-2 text-xs text-text-secondary">
                          <a
                            href={`https://www.google.com/maps?q=${stop.latitude},${stop.longitude}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary hover:underline inline-flex items-center gap-1"
                          >
                            <MapPin size={10} /> {t('itinerary.viewOnMap')}
                          </a>
                        </div>
                      )}
                    </div>
                  ))}
                </div>

                <button onClick={addStop} className="mt-3 w-full py-2 border border-dashed border-border rounded-lg text-sm text-text-secondary hover:border-primary hover:text-primary flex items-center justify-center gap-1.5">
                  <Plus size={14} /> {t('itinerary.addAnotherStop')}
                </button>
              </div>
            )}

            {/* Advanced JSON toggle */}
            <div className="mt-4 border-t border-border pt-4">
              <button
                onClick={() => setItShowAdvanced(!itShowAdvanced)}
                className="flex items-center gap-1.5 text-xs text-text-secondary hover:text-text-primary"
              >
                <ChevronRight size={14} className={`transition-transform ${itShowAdvanced ? 'rotate-90' : ''}`} />
                {t('itinerary.advancedJson')}
              </button>
              {itShowAdvanced && (
                <div className="mt-3">
                  <label className="block text-xs text-text-secondary mb-1">{t('itinerary.rawJsonLabel')}</label>
                  <textarea
                    value={itRawJson}
                    onChange={e => setItRawJson(e.target.value)}
                    placeholder='[{"order":1,"houseId":"H-1023","label":"House 1","address":"123 St","latitude":14.63,"longitude":-90.50}]'
                    rows={5}
                    className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary font-mono"
                  />
                  <p className="text-xs text-text-secondary mt-1">{t('itinerary.rawJsonHint')}</p>
                </div>
              )}
            </div>

            <button
              onClick={handleCreateItinerary}
              disabled={itLoading || !itDate}
              className="mt-6 w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <MapPin size={14} /> {itLoading ? t('settings.creating') : t('itinerary.createItinerary')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
