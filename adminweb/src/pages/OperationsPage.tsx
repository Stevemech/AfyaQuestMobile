import { useState, useEffect } from 'react';
import { useOutletContext } from 'react-router-dom';
import CHVList from '../components/operations/CHVList';
import CHVDetail from '../components/operations/CHVDetail';
import { api } from '../api/api';
import type { CHV, House } from '../types';

export default function OperationsPage() {
  const { searchQuery } = useOutletContext<{ organization: string; searchQuery: string }>();
  const [chvs, setChvs] = useState<CHV[]>([]);
  const [selectedCHV, setSelectedCHV] = useState<CHV | null>(null);
  const [houses, setHouses] = useState<House[]>([]);
  const [localSearch, setLocalSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Fetch CHVs on mount
  useEffect(() => {
    setLoading(true);
    setError('');
    api.getCHVs()
      .then(res => {
        setChvs(res.chvs || []);
        if (res.chvs?.length > 0) {
          setSelectedCHV(res.chvs[0]);
        }
      })
      .catch(err => setError(err.message || 'Failed to load CHVs'))
      .finally(() => setLoading(false));
  }, []);

  // Fetch houses when selected CHV changes
  useEffect(() => {
    if (!selectedCHV) { setHouses([]); return; }
    api.getCHVDetail(selectedCHV.id)
      .then(res => setHouses(res.houses || []))
      .catch(() => setHouses([]));
  }, [selectedCHV?.id]);

  const search = searchQuery || localSearch;

  if (loading) {
    return (
      <div className="flex items-center justify-center py-20">
        <p className="text-text-secondary">Loading CHVs...</p>
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
      {/* Tabs */}
      <div className="flex items-center gap-6 mb-6 border-b border-border">
        <button className="pb-3 border-b-2 border-primary text-sm font-medium text-text-primary">
          Operations
        </button>
        <button className="pb-3 text-sm font-medium text-text-secondary hover:text-text-primary">
          CHV Analytics
        </button>
        <button className="pb-3 text-sm font-medium text-text-secondary hover:text-text-primary">
          Reports: Archive
        </button>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[380px_1fr] gap-6">
        {/* Left panel - CHV List */}
        <CHVList
          chvs={chvs}
          selectedCHV={selectedCHV}
          onSelectCHV={setSelectedCHV}
          searchQuery={search}
          onSearchChange={setLocalSearch}
        />

        {/* Right panel - CHV Detail */}
        <div>
          {selectedCHV ? (
            <CHVDetail chv={selectedCHV} houses={houses} />
          ) : (
            <div className="bg-white rounded-xl border border-border shadow-sm p-12 text-center text-text-secondary">
              Select a CHV to view details
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
