import { Outlet } from 'react-router-dom';
import { useState } from 'react';
import Sidebar from './Sidebar';
import Header from './Header';
import { useAuth } from '../../auth/AuthContext';

export default function Layout() {
  const { user } = useAuth();
  const organization = user?.organization || localStorage.getItem('adminOrg') || '';
  const [searchQuery, setSearchQuery] = useState('');

  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex-1 flex flex-col min-w-0">
        <Header
          organization={organization}
          searchQuery={searchQuery}
          onSearchChange={setSearchQuery}
        />
        <main className="flex-1 p-6 overflow-auto">
          <Outlet context={{ organization, searchQuery }} />
        </main>
      </div>
    </div>
  );
}
