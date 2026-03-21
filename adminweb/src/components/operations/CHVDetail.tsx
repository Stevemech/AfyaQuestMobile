import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronDown, ArrowUpDown, RotateCcw, Send, MapPin, Plus, Trash2, X, ChevronRight, CheckCircle, Navigation, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api, ALL_MODULES } from '../../api/api';
import type { CHV, House, Itinerary, CHVAssignment, ClockEvent } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVDetailProps {
  chv: CHV;
  houses: House[];
  itineraries?: Itinerary[];
  assignments?: CHVAssignment[];
  clockHistory?: ClockEvent[];
  onDataChanged?: () => void;
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

const MAPS_KEY = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || 'AIzaSyDe3Xcf1pyQwTBvqis4HJuldLrrJJTz-Ig';

// --------------- Google Maps script loader ---------------
let mapsLoadPromise: Promise<void> | null = null;
function loadGoogleMaps(): Promise<void> {
  if ((window as any).google?.maps?.places) return Promise.resolve();
  if (mapsLoadPromise) return mapsLoadPromise;
  if (!MAPS_KEY) return Promise.reject(new Error('No API key'));
  mapsLoadPromise = new Promise((resolve, reject) => {
    const existing = document.querySelector('script[src*="maps.googleapis.com/maps/api/js"]');
    if (existing) {
      const check = setInterval(() => {
        if ((window as any).google?.maps?.places) { clearInterval(check); resolve(); }
      }, 100);
      return;
    }
    const s = document.createElement('script');
    s.src = `https://maps.googleapis.com/maps/api/js?key=${MAPS_KEY}&libraries=places`;
    s.async = true;
    s.onload = () => {
      const check = setInterval(() => {
        if ((window as any).google?.maps?.places) { clearInterval(check); resolve(); }
      }, 50);
    };
    s.onerror = () => reject(new Error('Failed to load Google Maps'));
    document.head.appendChild(s);
  });
  return mapsLoadPromise;
}

// --------------- Address Autocomplete input ---------------
function AddressInput({ stopIndex, stop, onUpdate }: {
  stopIndex: number;
  stop: StopEntry;
  onUpdate: (index: number, fields: Partial<StopEntry>) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const acRef = useRef<google.maps.places.Autocomplete | null>(null);
  const { t } = useTranslation();

  useEffect(() => {
    if (!MAPS_KEY || acRef.current) return;
    let cancelled = false;
    loadGoogleMaps().then(() => {
      if (cancelled || !inputRef.current || acRef.current) return;
      const ac = new google.maps.places.Autocomplete(inputRef.current, {
        fields: ['formatted_address', 'geometry', 'name'],
      });
      ac.addListener('place_changed', () => {
        const place = ac.getPlace();
        if (place.geometry?.location) {
          const lat = place.geometry.location.lat();
          const lng = place.geometry.location.lng();
          const addr = place.formatted_address || inputRef.current?.value || '';
          onUpdate(stopIndex, {
            address: addr,
            latitude: lat.toFixed(6),
            longitude: lng.toFixed(6),
          });
        }
      });
      acRef.current = ac;
    }).catch(() => { /* no maps key or failed to load - plain input is fine */ });
    return () => { cancelled = true; };
  }, [stopIndex, onUpdate]);

  // Sync the input value from React state when it changes externally
  // (e.g., after place selection updates the state)
  useEffect(() => {
    if (inputRef.current && document.activeElement !== inputRef.current) {
      inputRef.current.value = stop.address;
    }
  }, [stop.address]);

  return (
    <div>
      <label className="block text-xs text-text-secondary mb-1">
        {t('itinerary.address')}
        {MAPS_KEY && <span className="ml-1 text-primary text-[10px]">Google Maps</span>}
      </label>
      <input
        ref={inputRef}
        type="text"
        defaultValue={stop.address}
        onBlur={e => {
          // Sync manual typing back to state on blur
          if (e.target.value !== stop.address) {
            onUpdate(stopIndex, { address: e.target.value });
          }
        }}
        placeholder={MAPS_KEY ? t('itinerary.addressPlaceholder') : t('itinerary.addressPlaceholderManual')}
        className="w-full px-3 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
      />
    </div>
  );
}

// --------------- Google Map showing itinerary stops + houses ---------------
function LocationMap({ houses, itineraries }: { houses: House[]; itineraries: Itinerary[] }) {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<google.maps.Map | null>(null);
  const { t } = useTranslation();
  const [mapReady, setMapReady] = useState(false);

  // Collect all points: houses + itinerary stops
  const allPoints: { lat: number; lng: number; label: string; color: string; info: string }[] = [];

  houses.filter(h => h.latitude && h.longitude).forEach(h => {
    const color = h.visitStatus === 'completed' ? '#22c55e' : h.visitStatus === 'overdue' ? '#ef4444' : '#f59e0b';
    allPoints.push({
      lat: h.latitude, lng: h.longitude,
      label: h.id, color,
      info: `<strong>${h.id}</strong><br/><span style="color:${color};text-transform:capitalize">${h.visitStatus}</span><br/>${h.distance} km`,
    });
  });

  itineraries.forEach(it => {
    (it.stops || []).filter(s => s.latitude && s.longitude).forEach(s => {
      allPoints.push({
        lat: s.latitude, lng: s.longitude,
        label: s.label || `Stop ${s.order}`, color: '#6366f1',
        info: `<strong>${s.label || 'Stop ' + s.order}</strong><br/>${it.date}<br/>${s.address || ''}`,
      });
    });
  });

  useEffect(() => {
    if (!MAPS_KEY || !mapRef.current || mapInstanceRef.current) return;
    let cancelled = false;
    loadGoogleMaps().then(() => {
      if (cancelled || !mapRef.current) return;
      let center = { lat: 14.6349, lng: -90.5069 };
      if (allPoints.length > 0) {
        center = {
          lat: allPoints.reduce((s, p) => s + p.lat, 0) / allPoints.length,
          lng: allPoints.reduce((s, p) => s + p.lng, 0) / allPoints.length,
        };
      }

      const map = new google.maps.Map(mapRef.current, {
        center,
        zoom: allPoints.length > 0 ? 14 : 12,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: true,
        zoomControl: true,
        styles: [{ featureType: 'poi', stylers: [{ visibility: 'off' }] }],
      });
      mapInstanceRef.current = map;

      const bounds = new google.maps.LatLngBounds();
      allPoints.forEach(p => {
        const pos = { lat: p.lat, lng: p.lng };
        bounds.extend(pos);
        const marker = new google.maps.Marker({
          position: pos, map, title: p.label,
          icon: { path: google.maps.SymbolPath.CIRCLE, scale: 8, fillColor: p.color, fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2 },
        });
        const info = new google.maps.InfoWindow({
          content: `<div style="font-size:13px;font-family:Inter,sans-serif;padding:2px 0">${p.info}</div>`,
        });
        marker.addListener('click', () => info.open(map, marker));
      });

      if (allPoints.length > 1) map.fitBounds(bounds, 40);
      setMapReady(true);
    }).catch(() => {});
    return () => { cancelled = true; };
  }, [houses.length, itineraries.length]);

  if (!MAPS_KEY) {
    return (
      <div className="bg-gray-100 rounded-lg flex items-center justify-center min-h-[280px] border border-border">
        <div className="text-center text-text-secondary p-4">
          <MapPin size={28} className="mx-auto mb-2 text-text-secondary" />
          <p className="text-sm font-medium">{t('chvDetail.mapView')}</p>
          <p className="text-xs mt-1">{t('chvDetail.mapNoKey')}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative">
      <div ref={mapRef} className="rounded-lg border border-border min-h-[280px] w-full" />
      {!mapReady && (
        <div className="absolute inset-0 flex items-center justify-center bg-gray-100 rounded-lg">
          <p className="text-sm text-text-secondary">{t('loading')}</p>
        </div>
      )}
      {allPoints.length === 0 && mapReady && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/10 rounded-lg">
          <p className="text-sm text-text-secondary bg-white px-3 py-1.5 rounded-lg shadow">{t('chvDetail.noLocations')}</p>
        </div>
      )}
      <div className="absolute bottom-2 left-2 bg-white/90 rounded-lg px-2.5 py-1.5 text-[10px] flex items-center gap-3 shadow">
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#6366f1] inline-block" /> {t('chvDetail.itineraryStops')}</span>
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#f59e0b] inline-block" /> {t('chvDetail.visitStatus_pending')}</span>
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#22c55e] inline-block" /> {t('chvDetail.visitStatus_completed')}</span>
      </div>
    </div>
  );
}

// --------------- Main component ---------------
export default function CHVDetail({ chv, houses, itineraries = [], assignments = [], clockHistory = [], onDataChanged }: CHVDetailProps) {
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

  // Request report modal
  const [showReportModal, setShowReportModal] = useState(false);
  const [reportDueDate, setReportDueDate] = useState('');
  const [reportLoading, setReportLoading] = useState(false);
  const [reportError, setReportError] = useState('');

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
      if (assignModuleId.startsWith('full-module-')) {
        const moduleNumber = parseInt(assignModuleId.replace('full-module-', ''), 10);
        await api.assignFullModule(chv.id, moduleNumber);
      } else {
        const selected = ALL_MODULES.find(m => m.id === assignModuleId);
        if (selected?.type === 'lesson') {
          await api.assignLesson(chv.id, assignModuleId);
        } else {
          await api.assignModule(chv.id, assignModuleId);
        }
      }
      showToast(t('settings.moduleAssigned'));
      setShowAssignModal(false);
      setAssignModuleId('');
      onDataChanged?.();
    } catch (err) {
      setAssignError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setAssignLoading(false);
    }
  };

  // Request daily report
  const handleRequestReport = async () => {
    setReportError('');
    setReportLoading(true);
    try {
      await api.requestReport(chv.id, reportDueDate || undefined);
      showToast(t('chvDetail.reportRequested'));
      setShowReportModal(false);
      setReportDueDate('');
      onDataChanged?.();
    } catch (err) {
      setReportError(err instanceof Error ? err.message : 'Failed');
    } finally {
      setReportLoading(false);
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
      onDataChanged?.();
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

  const updateStopFields = useCallback((index: number, fields: Partial<StopEntry>) => {
    setItStops(prev => prev.map((s, i) => i === index ? { ...s, ...fields } : s));
  }, []);

  const updateStop = (index: number, field: keyof StopEntry, value: string) => {
    updateStopFields(index, { [field]: value });
  };

  const addStop = () => setItStops([...itStops, emptyStop()]);
  const removeStop = (index: number) => {
    if (itStops.length <= 1) return;
    setItStops(itStops.filter((_, i) => i !== index));
  };

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
              onClick={() => { setShowReportModal(true); setReportDueDate(''); setReportError(''); }}
              className="flex items-center gap-1.5 px-3 py-2 bg-amber-500 text-white rounded-lg text-sm font-medium hover:bg-amber-600"
            >
              <FileText size={14} /> {t('chvDetail.requestReport')}
            </button>
            <button
              onClick={() => { setShowItineraryModal(true); setItDate(''); setItStops([emptyStop()]); setItRawJson(''); setItShowAdvanced(false); setItError(''); }}
              className="flex items-center gap-1.5 px-3 py-2 border border-primary text-primary rounded-lg text-sm font-medium hover:bg-primary-light"
            >
              <MapPin size={14} /> {t('itinerary.createItinerary')}
            </button>
          </div>
        </div>

        {/* Level, XP, lives, streak (from mobile app profile) */}
        <div className="border-t border-border pt-4 mt-4">
          <p className="text-xs font-semibold text-text-secondary uppercase tracking-wide mb-3">
            {t('chvDetail.appProgress')}
          </p>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="rounded-lg border border-border bg-primary-light/20 p-4 text-center">
              <p className="text-xs text-text-secondary mb-1">{t('chvDetail.statLevel')}</p>
              <p className="text-2xl font-bold text-primary tabular-nums">{chv.level ?? 0}</p>
            </div>
            <div className="rounded-lg border border-border bg-primary-light/20 p-4 text-center">
              <p className="text-xs text-text-secondary mb-1">{t('chvDetail.statTotalXP')}</p>
              <p className="text-2xl font-bold text-primary tabular-nums">{chv.totalPoints ?? 0}</p>
            </div>
            <div className="rounded-lg border border-border bg-amber-50 p-4 text-center">
              <p className="text-xs text-text-secondary mb-1">{t('chvDetail.statLives')}</p>
              <p className="text-2xl font-bold text-amber-800 tabular-nums">{chv.lives ?? 0}</p>
            </div>
            <div className="rounded-lg border border-border bg-orange-50 p-4 text-center">
              <p className="text-xs text-text-secondary mb-1">{t('chvDetail.statStreak')}</p>
              <p className="text-2xl font-bold text-orange-800 tabular-nums">{chv.currentStreak ?? 0}</p>
            </div>
          </div>
        </div>

        {/* Stats bar — field visits */}
        <div className="grid grid-cols-3 gap-4 mt-6">
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

      {/* === Clock-In Status & History === */}
      <div className="bg-white rounded-xl border border-border shadow-sm p-5">
        <h3 className="font-semibold text-text-primary mb-4 flex items-center gap-2">
          <span className={`w-3 h-3 rounded-full ${chv.manualStatus === 'active' ? 'bg-green-500' : chv.manualStatus === 'inactive' ? 'bg-gray-400' : 'bg-gray-300'}`} />
          {t('chvDetail.clockStatus')}
        </h3>

        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-5">
          <div className="border border-border rounded-lg p-3">
            <p className="text-xs text-text-secondary">{t('chvDetail.currentStatus')}</p>
            <p className={`text-lg font-bold mt-1 ${chv.manualStatus === 'active' ? 'text-green-600' : chv.manualStatus === 'inactive' ? 'text-gray-500' : 'text-gray-400'}`}>
              {chv.manualStatus === 'active' ? t('chvList.clockedIn') : chv.manualStatus === 'inactive' ? t('chvList.clockedOut') : '—'}
            </p>
          </div>
          <div className="border border-border rounded-lg p-3">
            <p className="text-xs text-text-secondary">{t('chvDetail.lastClockIn')}</p>
            <p className="text-sm font-medium mt-1">
              {chv.lastClockIn ? new Date(chv.lastClockIn).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'}
            </p>
          </div>
          <div className="border border-border rounded-lg p-3">
            <p className="text-xs text-text-secondary">{t('chvDetail.lastClockOut')}</p>
            <p className="text-sm font-medium mt-1">
              {chv.lastClockOut ? new Date(chv.lastClockOut).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'}
            </p>
          </div>
          <div className="border border-border rounded-lg p-3">
            <p className="text-xs text-text-secondary">{t('chvDetail.totalSessions')}</p>
            <p className="text-lg font-bold mt-1">
              {clockHistory.filter(e => e.action === 'clock_in').length}
            </p>
          </div>
        </div>

        {clockHistory.length > 0 ? (
          <div>
            <h4 className="text-sm font-medium text-text-secondary mb-2">{t('chvDetail.clockHistory')}</h4>
            <div className="max-h-[240px] overflow-y-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-border">
                    <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('date')}</th>
                    <th className="text-left py-2 px-3 text-text-secondary font-medium">{t('chvDetail.clockStatus')}</th>
                    <th className="text-left py-2 px-3 text-text-secondary font-medium">Time</th>
                  </tr>
                </thead>
                <tbody>
                  {clockHistory.slice(0, 50).map((event, i) => (
                    <tr key={i} className="border-b border-border last:border-0 hover:bg-gray-50">
                      <td className="py-2 px-3">{event.date}</td>
                      <td className="py-2 px-3">
                        <span className={`inline-flex items-center gap-1.5 px-2 py-0.5 rounded-full text-xs font-medium ${
                          event.action === 'clock_in'
                            ? 'bg-green-100 text-green-700'
                            : 'bg-gray-100 text-gray-600'
                        }`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${event.action === 'clock_in' ? 'bg-green-500' : 'bg-gray-400'}`} />
                          {t(`chvDetail.clockAction_${event.action}`)}
                        </span>
                      </td>
                      <td className="py-2 px-3 text-text-secondary">
                        {new Date(event.timestamp).toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' })}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        ) : (
          <p className="text-sm text-text-secondary text-center py-4">{t('chvDetail.noClockHistory')}</p>
        )}
      </div>

      {/* === Map + Itineraries === */}
      {itineraries.length > 0 && (
        <>
          <div className="bg-white rounded-xl border border-border shadow-sm p-5">
            <h3 className="font-semibold text-text-primary mb-4">{t('chvDetail.mapView')}</h3>
            <LocationMap key={chv.id} houses={houses} itineraries={itineraries} />
          </div>

          <div className="bg-white rounded-xl border border-border shadow-sm p-5">
            <h3 className="font-semibold text-text-primary mb-4">{t('chvDetail.itineraries')} ({itineraries.length})</h3>
            <div className="space-y-4">
              {itineraries.map((it, i) => (
                <div key={i} className="border border-border rounded-lg overflow-hidden">
                  <div className="flex items-center justify-between px-4 py-3 bg-gray-50 border-b border-border">
                    <div className="flex items-center gap-3">
                      <MapPin size={16} className="text-primary" />
                      <div>
                        <p className="text-sm font-semibold text-text-primary">{it.date}</p>
                        <p className="text-xs text-text-secondary">
                          {it.stops.filter(s => s.completed).length}/{it.stops.length} {t('itinerary.stops').toLowerCase()} visited
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <StatusBadge type={it.status === 'active' ? 'success' : 'warning'} label={it.status || 'active'} />
                      <button
                        onClick={async () => {
                          if (!confirm(t('chvDetail.confirmDeleteItinerary', { date: it.date }))) return;
                          try {
                            await api.deleteItinerary(chv.id, it.date);
                            showToast(t('chvDetail.itineraryDeleted'));
                            onDataChanged?.();
                          } catch (err) {
                            showToast(err instanceof Error ? err.message : 'Failed', 'error');
                          }
                        }}
                        className="p-1.5 rounded hover:bg-danger-light text-text-secondary hover:text-danger"
                        title={t('chvDetail.deleteItinerary')}
                      >
                        <Trash2 size={14} />
                      </button>
                    </div>
                  </div>
                  <div className="divide-y divide-border">
                    {it.stops.map((stop, j) => (
                      <div key={j} className={`px-4 py-2.5 flex items-start gap-3 ${stop.completed ? 'bg-green-50' : ''}`}>
                        <span className={`w-6 h-6 rounded-full text-white text-xs flex items-center justify-center font-semibold shrink-0 mt-0.5 ${stop.completed ? 'bg-green-500' : 'bg-primary'}`}>
                          {stop.completed ? '✓' : (stop.order || j + 1)}
                        </span>
                        <div className="flex-1 min-w-0">
                          <p className={`text-sm font-medium ${stop.completed ? 'text-green-700' : 'text-text-primary'}`}>{stop.label}</p>
                          {stop.address && <p className="text-xs text-text-secondary truncate">{stop.address}</p>}
                          {stop.description && <p className="text-xs text-text-secondary">{stop.description}</p>}
                        </div>
                        <div className="flex items-center gap-2 shrink-0">
                          <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${stop.completed ? 'bg-green-100 text-green-700' : 'bg-amber-100 text-amber-700'}`}>
                            {stop.completed ? 'Visited' : 'To Visit'}
                          </span>
                          {stop.latitude && stop.longitude ? (
                            <a href={`https://www.google.com/maps/dir/?api=1&destination=${stop.latitude},${stop.longitude}`}
                              target="_blank" rel="noopener noreferrer" className="text-primary hover:text-primary-dark" title={t('itinerary.viewOnMap')}>
                              <Navigation size={14} />
                            </a>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                  {it.createdAt && (
                    <div className="px-4 py-2 bg-gray-50 border-t border-border text-xs text-text-secondary">
                      {t('chvDetail.createdOn')} {new Date(it.createdAt).toLocaleString()}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </div>
        </>
      )}

      {/* === Assigned Modules & Lessons === */}
      {assignments.length > 0 && (
        <div className="bg-white rounded-xl border border-border shadow-sm p-5">
          <h3 className="font-semibold text-text-primary mb-4">{t('chvDetail.assignedModules')} ({assignments.length})</h3>
          <div className="space-y-2">
            {assignments.map((a, i) => {
              const rawId = a.moduleId || a.lessonId || '';
              const mod = ALL_MODULES.find(m => m.id === rawId);
              const displayName = mod?.name || rawId.replace(/[-_]/g, ' ').replace(/\b\w/g, c => c.toUpperCase()) || 'Unknown';
              return (
                <div key={i} className="flex items-center justify-between p-3 border border-border rounded-lg">
                  <div>
                    <p className="text-sm font-medium text-text-primary">{displayName}</p>
                    <p className="text-xs text-text-secondary">
                      {a.type === 'module' ? 'Video Module' : a.type === 'lesson' ? 'Interactive Lesson' : a.type}
                      {a.assignedAt && <span className="ml-2">{new Date(a.assignedAt).toLocaleDateString()}</span>}
                    </p>
                  </div>
                  <div className="flex items-center gap-2">
                    <StatusBadge type={a.status === 'completed' ? 'success' : 'warning'} label={a.status} />
                    <button
                      onClick={async () => {
                        if (!confirm(`Remove "${displayName}" from this CHV?`)) return;
                        try {
                          const itemId = a.moduleId || a.lessonId || '';
                          const assignType = a.type === 'module' ? 'module' as const : 'lesson' as const;
                          await api.deleteAssignment(chv.id, assignType, itemId);
                          onDataChanged?.();
                        } catch (err) {
                          alert('Failed to remove assignment');
                        }
                      }}
                      className="p-1.5 text-red-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                      title="Remove assignment"
                    >
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Distance Matrix - only show when houses exist */}
      {houses.length > 0 && (
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
                <th className="text-left py-2 px-3 text-text-secondary font-medium"></th>
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
                  <td className="py-2.5 px-3">
                    {h.latitude && h.longitude ? (
                      <a
                        href={`https://www.google.com/maps/dir/?api=1&destination=${h.latitude},${h.longitude}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-primary hover:text-primary-dark"
                        title={t('itinerary.viewOnMap')}
                      >
                        <Navigation size={14} />
                      </a>
                    ) : null}
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
      )}

      {/* Pending Houses - only show when houses exist */}
      {houses.length > 0 && (<>
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
        </div>
      </div>

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
      </>)}

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
                <optgroup label="Assign Entire Module">
                  {[1,2,3,4,5,6].map(n => {
                    const modVideos = ALL_MODULES.filter(m => m.type === 'video' && 'module' in m && (m as any).module === n);
                    const modName = modVideos.length > 0 ? (modVideos[0] as any).moduleName : `Module ${n}`;
                    return <option key={`full-mod-${n}`} value={`full-module-${n}`}>Module {n}: {modName} ({modVideos.length} videos)</option>;
                  })}
                </optgroup>
                {[1,2,3,4,5,6].map(n => {
                  const modVideos = ALL_MODULES.filter(m => m.type === 'video' && 'module' in m && (m as any).module === n);
                  const modName = modVideos.length > 0 ? (modVideos[0] as any).moduleName : `Module ${n}`;
                  return (
                    <optgroup key={`mod-${n}`} label={`Module ${n}: ${modName}`}>
                      {modVideos.map(m => (
                        <option key={m.id} value={m.id}>{m.name}</option>
                      ))}
                    </optgroup>
                  );
                })}
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
                          <AddressInput stopIndex={i} stop={stop} onUpdate={updateStopFields} />
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
                          <label className="block text-xs text-text-secondary mb-1">
                            {t('itinerary.latitude')}
                            {stop.latitude && <span className="ml-1 text-success text-[10px]">{t('itinerary.filled')}</span>}
                          </label>
                          <input type="text" value={stop.latitude} onChange={e => updateStop(i, 'latitude', e.target.value)}
                            placeholder="14.6349"
                            className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:border-primary font-mono ${stop.latitude ? 'border-success/50 bg-success/5' : 'border-border'}`} />
                        </div>
                        <div>
                          <label className="block text-xs text-text-secondary mb-1">
                            {t('itinerary.longitude')}
                            {stop.longitude && <span className="ml-1 text-success text-[10px]">{t('itinerary.filled')}</span>}
                          </label>
                          <input type="text" value={stop.longitude} onChange={e => updateStop(i, 'longitude', e.target.value)}
                            placeholder="-90.5069"
                            className={`w-full px-3 py-2 border rounded-lg text-sm focus:outline-none focus:border-primary font-mono ${stop.longitude ? 'border-success/50 bg-success/5' : 'border-border'}`} />
                        </div>
                      </div>
                      {stop.latitude && stop.longitude && (
                        <div className="mt-2 pt-2 border-t border-border/50 flex items-center justify-between">
                          <a
                            href={`https://www.google.com/maps?q=${stop.latitude},${stop.longitude}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary hover:underline text-xs inline-flex items-center gap-1"
                          >
                            <MapPin size={10} /> {t('itinerary.viewOnMap')}
                          </a>
                          <span className="text-[10px] text-text-secondary font-mono">{stop.latitude}, {stop.longitude}</span>
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

      {/* === Request Report Modal === */}
      {showReportModal && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setShowReportModal(false)}>
          <div className="bg-white rounded-xl shadow-xl max-w-md w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold">{t('chvDetail.requestReportFrom', { name: chv.name })}</h3>
              <button onClick={() => setShowReportModal(false)} className="p-1 rounded hover:bg-gray-100"><X size={20} className="text-text-secondary" /></button>
            </div>

            <p className="text-sm text-text-secondary mb-4">{t('chvDetail.requestReportDesc')}</p>

            {reportError && <div className="mb-3 p-2 bg-danger-light text-danger text-sm rounded-lg">{reportError}</div>}

            <div>
              <label className="block text-sm font-medium text-text-primary mb-1.5">{t('chvDetail.reportDueDate')}</label>
              <input
                type="date"
                value={reportDueDate}
                onChange={e => setReportDueDate(e.target.value)}
                className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
              />
              <p className="text-xs text-text-secondary mt-1">{t('chvDetail.reportDueDateHint')}</p>
            </div>

            <button
              onClick={handleRequestReport}
              disabled={reportLoading}
              className="mt-6 w-full py-2.5 bg-amber-500 text-white rounded-lg text-sm font-medium hover:bg-amber-600 disabled:opacity-50 flex items-center justify-center gap-2"
            >
              <FileText size={14} /> {reportLoading ? t('settings.creating') : t('chvDetail.requestReport')}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
