import { useState, useRef, useEffect } from 'react';
import { Search, ChevronDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import type { CHV } from '../../types';
import StatusBadge from '../common/StatusBadge';

interface CHVListProps {
  chvs: CHV[];
  selectedCHV: CHV | null;
  onSelectCHV: (chv: CHV) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
}

function formatLastActive(lastActive: string, t: (key: string) => string): string {
  // If already human-readable (e.g. "2 days ago"), return as-is
  if (!lastActive.includes('T') && !lastActive.match(/^\d{4}-\d{2}-\d{2}/)) {
    return lastActive;
  }
  // Parse ISO date
  const date = new Date(lastActive);
  if (isNaN(date.getTime())) return lastActive;

  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffDays === 0) return t('chvList.today');
  if (diffDays === 1) return t('chvList.yesterday');
  if (diffDays < 7) return `${diffDays} ${t('chvList.daysAgo')}`;

  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

type StatusFilter = 'all' | 'active' | 'inactive' | 'caution';
type SortOption = 'name' | 'lastActive' | 'completionRate';

function Dropdown({ label, options, value, onChange }: {
  label: string;
  options: { value: string; label: string }[];
  value: string;
  onChange: (v: string) => void;
}) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const selected = options.find(o => o.value === value);

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1 px-2 py-1 border border-border rounded text-text-secondary hover:border-primary"
      >
        {label}{selected ? `: ${selected.label}` : ''} <ChevronDown size={12} />
      </button>
      {open && (
        <div className="absolute top-full left-0 mt-1 bg-white border border-border rounded-lg shadow-lg z-20 min-w-[120px]">
          {options.map(opt => (
            <button
              key={opt.value}
              onClick={() => { onChange(opt.value); setOpen(false); }}
              className={`block w-full text-left px-3 py-1.5 text-xs hover:bg-gray-50 ${
                value === opt.value ? 'text-primary font-medium' : 'text-text-secondary'
              }`}
            >
              {opt.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

export default function CHVList({ chvs, selectedCHV, onSelectCHV, searchQuery, onSearchChange }: CHVListProps) {
  const { t } = useTranslation();
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('all');
  const [sortBy, setSortBy] = useState<SortOption>('name');

  const filtered = chvs
    .filter(c => {
      if (!c.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
      if (statusFilter !== 'all' && c.status !== statusFilter) return false;
      return true;
    })
    .sort((a, b) => {
      if (sortBy === 'name') return a.name.localeCompare(b.name);
      if (sortBy === 'completionRate') return (b.completionRate ?? 0) - (a.completionRate ?? 0);
      return 0;
    });

  const statusOptions = [
    { value: 'all', label: t('reportsPage.allCHVs') },
    { value: 'active', label: t('chvList.active') },
    { value: 'caution', label: t('chvList.caution') },
    { value: 'inactive', label: t('chvList.inactive') },
  ];

  const sortOptions = [
    { value: 'name', label: t('chvList.name') },
    { value: 'completionRate', label: t('chvList.completionRate') },
  ];

  return (
    <div className="bg-white rounded-xl border border-border shadow-sm overflow-hidden">
      {/* Header */}
      <div className="p-5 border-b border-border">
        <h2 className="text-lg font-semibold text-text-primary">{t('chvList.title')}</h2>
        <p className="text-sm text-text-secondary mt-0.5">
          {chvs[0]?.organization || chvs[0]?.clinic || t('chvList.organization')} &middot; {chvs.length}
        </p>

        {/* Search */}
        <div className="relative mt-3">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary" />
          <input
            type="text"
            placeholder={t('chvList.searchCHV')}
            value={searchQuery}
            onChange={e => onSearchChange(e.target.value)}
            className="w-full pl-9 pr-4 py-2 border border-border rounded-lg text-sm focus:outline-none focus:border-primary"
          />
        </div>

        {/* Filters */}
        <div className="flex gap-2 mt-3 text-xs items-center">
          <span className="text-text-secondary">{t('chvList.status')}</span>
          <Dropdown
            label={t('chvList.status').replace(':', '')}
            options={statusOptions}
            value={statusFilter}
            onChange={v => setStatusFilter(v as StatusFilter)}
          />
        </div>
        <div className="flex gap-2 mt-2 text-xs items-center">
          <span className="text-text-secondary">{t('chvList.sortLabel')}</span>
          <Dropdown
            label={t('chvDetail.sortBy').replace(':', '')}
            options={sortOptions}
            value={sortBy}
            onChange={v => setSortBy(v as SortOption)}
          />
        </div>
      </div>

      {/* CHV List */}
      <div className="max-h-[calc(100vh-380px)] overflow-y-auto">
        {filtered.length === 0 && (
          <div className="p-6 text-center text-sm text-text-secondary">
            {t('chvList.noCHVsFound')}
          </div>
        )}
        {filtered.map(chv => (
          <button
            key={chv.id}
            onClick={() => onSelectCHV(chv)}
            className={`w-full text-left p-4 border-b border-border hover:bg-gray-50 transition-colors ${
              selectedCHV?.id === chv.id ? 'bg-primary-light/50' : ''
            }`}
          >
            <div className="flex items-start gap-3">
              {/* Avatar */}
              <div className="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center text-lg font-semibold text-text-secondary shrink-0">
                {chv.name.charAt(0)}
              </div>

              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className="font-semibold text-text-primary">{chv.name}</span>
                    {chv.status === 'active' && (
                      <span className="w-3 h-3 rounded-full bg-success inline-block" />
                    )}
                  </div>
                  <StatusBadge
                    type={chv.status === 'active' ? 'active' : chv.status === 'caution' ? 'caution' : 'inactive'}
                    label={t(`chvList.status_${chv.status}`)}
                  />
                </div>
                <p className="text-xs text-text-secondary mt-0.5">{chv.clinic}</p>
                <div className="flex items-center justify-between mt-1">
                  <div className="text-xs text-text-secondary">
                    {chv.level != null ? `${t('chvList.level')} ${chv.level}` : 'Progress'}
                    <span className="ml-1">
                      <span className="inline-block w-12 h-1.5 rounded bg-gray-200 relative overflow-hidden">
                        <span
                          className="absolute left-0 top-0 h-full bg-primary rounded"
                          style={{ width: `${chv.completionRate ?? 0}%` }}
                        />
                      </span>
                    </span>
                  </div>
                  <span className="text-lg font-bold text-text-primary">
                    {chv.totalPoints != null ? `${chv.totalPoints} ${t('chvList.points')}` : `${chv.completionRate ?? 0}%`}
                  </span>
                </div>
                <p className="text-xs text-text-secondary mt-0.5">
                  {t('chvList.lastActive')} {formatLastActive(chv.lastActive, t)}
                </p>
              </div>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
