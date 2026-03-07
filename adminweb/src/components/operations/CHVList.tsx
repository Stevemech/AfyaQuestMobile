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

export default function CHVList({ chvs, selectedCHV, onSelectCHV, searchQuery, onSearchChange }: CHVListProps) {
  const { t } = useTranslation();
  const filtered = chvs.filter(c =>
    c.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

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
        <div className="flex gap-2 mt-3 text-xs">
          <span className="text-text-secondary">{t('chvList.status')}</span>
          <button className="flex items-center gap-1 px-2 py-1 border border-border rounded text-text-secondary hover:border-primary">
            {t('chvList.active')} <ChevronDown size={12} />
          </button>
          <button className="flex items-center gap-1 px-2 py-1 border border-border rounded text-text-secondary hover:border-primary">
            {t('chvList.lowActivity')} <ChevronDown size={12} />
          </button>
        </div>
        <div className="flex gap-2 mt-2 text-xs">
          <span className="text-text-secondary">{t('chvList.filter')}</span>
          <button className="flex items-center gap-1 px-2 py-1 border border-border rounded text-text-secondary hover:border-primary">
            {t('chvList.distance')}: {t('chvList.distance')} <ChevronDown size={12} />
          </button>
          <button className="flex items-center gap-1 px-2 py-1 border border-border rounded text-text-secondary hover:border-primary">
            {t('chvList.dating')} <ChevronDown size={12} />
          </button>
        </div>
      </div>

      {/* CHV List */}
      <div className="max-h-[calc(100vh-380px)] overflow-y-auto">
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
                  <div className="flex items-center gap-1">
                    {(chv.flags || []).map((f, i) => (
                      <StatusBadge key={i} type={f as 'active' | 'caution' | 'danger'} label={f === 'caution' ? 'Caution' : f === 'active' ? 'Active' : undefined} />
                    ))}
                    {(!chv.flags || chv.flags.length === 0) && (
                      <StatusBadge type={chv.status === 'active' ? 'active' : chv.status === 'caution' ? 'caution' : 'inactive'} label={chv.status} />
                    )}
                  </div>
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
                    {chv.totalPoints != null ? `${chv.totalPoints} pts` : `${chv.completionRate ?? 0}%`}
                  </span>
                </div>
                <p className="text-xs text-text-secondary mt-0.5">
                  {t('chvList.lastActive')} {chv.lastActive}
                </p>
              </div>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
