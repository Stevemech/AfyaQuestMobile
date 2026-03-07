import { Search, Bell, Building2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import LanguageSwitcher from '../common/LanguageSwitcher';

interface HeaderProps {
  organization: string;
  searchQuery: string;
  onSearchChange: (q: string) => void;
}

export default function Header({ organization, searchQuery, onSearchChange }: HeaderProps) {
  const { user } = useAuth();
  const { t } = useTranslation();
  const initial = user?.name?.charAt(0)?.toUpperCase() || 'A';

  return (
    <header className="h-16 bg-white border-b border-border px-6 flex items-center justify-between sticky top-0 z-10">
      {/* Organization display */}
      <div className="flex items-center gap-2 px-4 py-2 border border-border rounded-full">
        <Building2 size={16} className="text-primary" />
        <span className="text-sm font-medium">{organization || t('header.noOrganization')}</span>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-4">
        {/* Search */}
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-secondary" />
          <input
            type="text"
            placeholder={t('header.searchCHV')}
            value={searchQuery}
            onChange={e => onSearchChange(e.target.value)}
            className="pl-9 pr-4 py-2 border border-border rounded-lg text-sm w-48 focus:outline-none focus:border-primary transition-colors"
          />
        </div>

        {/* Language Switcher */}
        <LanguageSwitcher />

        {/* Notifications */}
        <button className="relative p-2 rounded-lg hover:bg-gray-50 transition-colors">
          <Bell size={20} className="text-text-secondary" />
          <span className="absolute top-1 right-1 w-2 h-2 bg-danger rounded-full" />
        </button>

        {/* Avatar + Name */}
        <div className="flex items-center gap-2">
          <div className="w-9 h-9 rounded-full bg-primary-light flex items-center justify-center">
            <span className="text-primary font-semibold text-sm">{initial}</span>
          </div>
          {user?.name && (
            <span className="text-sm font-medium text-text-primary hidden md:block">{user.name}</span>
          )}
        </div>
      </div>
    </header>
  );
}
