import { useState, useEffect, useRef, useCallback } from 'react';
import { ChevronDown, ArrowUpDown, RotateCcw, Send, MapPin, Plus, Trash2, X, ChevronRight, CheckCircle, Navigation } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { api, ALL_MODULES } from '../../api/api';
import type { CHV, House, Itinerary, CHVAssignment } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVDetailProps {
  chv: CHV;
  houses: House[];
  itineraries?: Itinerary[];
  assignments?: CHVAssignment[];
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

// --------------- Google Map for houses ---------------
function HousesMap({ houses }: { houses: House[] }) {
  const mapRef = useRef<HTMLDivElement>(null);
  const mapInstanceRef = useRef<google.maps.Map | null>(null);
  const { t } = useTranslation();
  const [mapReady, setMapReady] = useState(false);

  const housesWithCoords = houses.filter(h => h.latitude && h.longitude);

  useEffect(() => {
    if (!MAPS_KEY || !mapRef.current || mapInstanceRef.current) return;
    let cancelled = false;
    loadGoogleMaps().then(() => {
      if (cancelled || !mapRef.current) return;
      // Calculate center from houses, or default
      let center = { lat: 14.6349, lng: -90.5069 }; // Guatemala default
      if (housesWithCoords.length > 0) {
        const avgLat = housesWithCoords.reduce((s, h) => s + h.latitude, 0) / housesWithCoords.length;
        const avgLng = housesWithCoords.reduce((s, h) => s + h.longitude, 0) / housesWithCoords.length;
        center = { lat: avgLat, lng: avgLng };
      }

      const map = new google.maps.Map(mapRef.current, {
        center,
        zoom: housesWithCoords.length > 0 ? 14 : 12,
        mapTypeControl: false,
        streetViewControl: false,
        fullscreenControl: true,
        zoomControl: true,
        styles: [
          { featureType: 'poi', stylers: [{ visibility: 'off' }] },
        ],
      });
      mapInstanceRef.current = map;

      // Add markers for each house
      const bounds = new google.maps.LatLngBounds();
      housesWithCoords.forEach(h => {
        const pos = { lat: h.latitude, lng: h.longitude };
        bounds.extend(pos);
        const color = h.visitStatus === 'completed' ? '#22c55e'
          : h.visitStatus === 'overdue' ? '#ef4444' : '#f59e0b';
        const marker = new google.maps.Marker({
          position: pos,
          map,
          title: `${h.id} (${h.visitStatus})`,
          icon: {
            path: google.maps.SymbolPath.CIRCLE,
            scale: 8,
            fillColor: color,
            fillOpacity: 1,
            strokeColor: '#fff',
            strokeWeight: 2,
          },
        });
        const info = new google.maps.InfoWindow({
          content: `<div style="font-size:13px;font-family:Inter,sans-serif;padding:2px 0">
            <strong>${h.id}</strong><br/>
            <span style="color:${color};text-transform:capitalize">${h.visitStatus}</span><br/>
            ${t('chvDetail.distance')}: ${h.distance} km<br/>
            ${t('chvDetail.priority')}: ${h.priority}
            ${h.daysPending ? `<br/>${t('chvDetail.daysPending')}: ${h.daysPending}` : ''}
          </div>`,
        });
        marker.addListener('click', () => info.open(map, marker));
      });

      if (housesWithCoords.length > 1) {
        map.fitBounds(bounds, 40);
      }

      setMapReady(true);
    }).catch(() => { /* no key */ });
    return () => { cancelled = true; };
  }, [houses.length]);

  // Update markers when houses change (re-render doesn't re-run the full effect)
  // For simplicity, the map is created once per CHV; switching CHV remounts via key

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
      {housesWithCoords.length === 0 && mapReady && (
        <div className="absolute inset-0 flex items-center justify-center bg-black/10 rounded-lg">
          <p className="text-sm text-text-secondary bg-white px-3 py-1.5 rounded-lg shadow">{t('chvDetail.noHouseCoordinates')}</p>
        </div>
      )}
      {/* Legend */}
      <div className="absolute bottom-2 left-2 bg-white/90 rounded-lg px-2.5 py-1.5 text-[10px] flex items-center gap-3 shadow">
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#f59e0b] inline-block" /> {t('chvDetail.visitStatus_pending')}</span>
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#22c55e] inline-block" /> {t('chvDetail.visitStatus_completed')}</span>
        <span className="flex items-center gap-1"><span className="w-2.5 h-2.5 rounded-full bg-[#ef4444] inline-block" /> {t('chvDetail.visitStatus_overdue')}</span>
      </div>
    </div>
  );
}

// --------------- Main component ---------------
export default function CHVDetail({ chv, houses, itineraries = [], assignments = [], onDataChanged }: CHVDetailProps) {
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
      onDataChanged?.();
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

      {/* Pending Houses with Map */}
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
          {/* Interactive Google Map */}
          <HousesMap key={chv.id} houses={houses} />
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

      {/* === Assigned Modules & Lessons === */}
      {assignments.length > 0 && (
        <div className="bg-white rounded-xl border border-border shadow-sm p-5">
          <h3 className="font-semibold text-text-primary mb-4">{t('chvDetail.assignedModules')} ({assignments.length})</h3>
          <div className="space-y-2">
            {assignments.map((a, i) => {
              const mod = ALL_MODULES.find(m => m.id === a.moduleId || m.id === a.lessonId);
              return (
                <div key={i} className="flex items-center justify-between p-3 border border-border rounded-lg">
                  <div>
                    <p className="text-sm font-medium text-text-primary">
                      {mod?.name || a.moduleId || a.lessonId || 'Unknown'}
                    </p>
                    <p className="text-xs text-text-secondary">
                      {a.type === 'module' ? 'Video Module' : a.type === 'lesson' ? 'Interactive Lesson' : a.type}
                      {a.assignedAt && <span className="ml-2">{new Date(a.assignedAt).toLocaleDateString()}</span>}
                    </p>
                  </div>
                  <StatusBadge
                    type={a.status === 'completed' ? 'success' : 'warning'}
                    label={a.status}
                  />
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* === Itineraries === */}
      {itineraries.length > 0 && (
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
                      <p className="text-xs text-text-secondary">{it.stops.length} {t('itinerary.stops').toLowerCase()}</p>
                    </div>
                  </div>
                  <StatusBadge type={it.status === 'active' ? 'success' : 'warning'} label={it.status || 'active'} />
                </div>
                <div className="divide-y divide-border">
                  {it.stops.map((stop, j) => (
                    <div key={j} className="px-4 py-2.5 flex items-start gap-3">
                      <span className="w-6 h-6 rounded-full bg-primary text-white text-xs flex items-center justify-center font-semibold shrink-0 mt-0.5">
                        {stop.order || j + 1}
                      </span>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-text-primary">{stop.label}</p>
                        {stop.address && <p className="text-xs text-text-secondary truncate">{stop.address}</p>}
                        {stop.description && <p className="text-xs text-text-secondary">{stop.description}</p>}
                      </div>
                      <div className="text-right shrink-0">
                        {stop.latitude && stop.longitude ? (
                          <a
                            href={`https://www.google.com/maps/dir/?api=1&destination=${stop.latitude},${stop.longitude}`}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="text-primary hover:text-primary-dark"
                            title={t('itinerary.viewOnMap')}
                          >
                            <Navigation size={14} />
                          </a>
                        ) : (
                          <span className="text-xs text-text-secondary">{t('chvDetail.noCoords')}</span>
                        )}
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
      )}

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
    </div>
  );
}
