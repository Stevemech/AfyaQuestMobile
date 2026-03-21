import { useState, useRef, useEffect, useMemo } from 'react';
import { Search, Bell, Building2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';
import LanguageSwitcher from '../common/LanguageSwitcher';
import { useNotifications } from '../../notifications/NotificationContext';
import { formatNotificationMessage } from '../../notifications/formatNotification';

interface HeaderProps {
  organization: string;
  searchQuery: string;
  onSearchChange: (q: string) => void;
}

export default function Header({ organization, searchQuery, onSearchChange }: HeaderProps) {
  const { user } = useAuth();
  const { t } = useTranslation();
  const { notifications, unreadCount, loading, error, markAllRead, refresh } = useNotifications();
  const initial = user?.name?.charAt(0)?.toUpperCase() || 'A';
  const [showNotifications, setShowNotifications] = useState(false);
  const notifRef = useRef<HTMLDivElement>(null);

  const sortedNotifications = useMemo(
    () => [...notifications].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
    [notifications]
  );

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (notifRef.current && !notifRef.current.contains(e.target as Node)) setShowNotifications(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const hasUnread = unreadCount > 0;

  return (
    <header className="h-16 bg-white border-b border-border px-6 flex items-center justify-between sticky top-0 z-10">
      {/* Organization display */}
      <div className="flex items-center gap-2 px-4 py-2 border border-border rounded-full">
        <Building2 size={16} className="text-primary" />
        <span className="text-sm font-medium">{organization || t('header.noOrganization')}</span>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-4">
        {/* Global Search */}
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
        <div className="relative" ref={notifRef}>
          <button
            type="button"
            onClick={() => setShowNotifications(!showNotifications)}
            className="relative p-2 rounded-lg hover:bg-gray-50 transition-colors"
            aria-label={t('header.notifications')}
          >
            <Bell size={20} className="text-text-secondary" />
            {hasUnread && (
              <span
                className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 flex items-center justify-center bg-danger text-white text-[10px] font-bold rounded-full border-2 border-white"
                aria-hidden
              >
                {unreadCount > 99 ? '99+' : unreadCount}
              </span>
            )}
          </button>
          {showNotifications && (
            <div className="absolute right-0 top-full mt-1 w-80 max-h-[min(70vh,28rem)] overflow-y-auto bg-white border border-border rounded-xl shadow-lg z-30 flex flex-col">
              <div className="p-4 border-b border-border flex items-center justify-between gap-2">
                <h4 className="text-sm font-semibold text-text-primary">{t('header.notifications')}</h4>
                <div className="flex items-center gap-2 shrink-0">
                  {hasUnread && (
                    <button
                      type="button"
                      onClick={() => void markAllRead()}
                      className="text-xs font-medium text-primary hover:underline"
                    >
                      {t('notifications.markAllRead')}
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={() => void refresh()}
                    className="text-xs text-text-secondary hover:text-text-primary"
                  >
                    {t('notifications.refresh')}
                  </button>
                </div>
              </div>
              <div className="p-2">
                {loading && (
                  <p className="text-sm text-text-secondary px-2 py-3">{t('loading')}</p>
                )}
                {error && (
                  <p className="text-sm text-danger px-2 py-3">{error}</p>
                )}
                {!loading && !error && sortedNotifications.length === 0 && (
                  <p className="text-sm text-text-secondary px-2 py-3">{t('header.noNotifications')}</p>
                )}
                {!loading &&
                  !error &&
                  sortedNotifications.map(n => (
                    <div
                      key={n.id}
                      className={`px-3 py-2.5 rounded-lg text-sm mb-1 last:mb-0 ${
                        n.read ? 'text-text-secondary' : 'bg-primary-light/40 text-text-primary'
                      }`}
                    >
                      <p className="leading-snug">{formatNotificationMessage(n, t)}</p>
                      <p className="text-xs text-text-secondary mt-1">
                        {new Date(n.createdAt).toLocaleString()}
                      </p>
                    </div>
                  ))}
              </div>
            </div>
          )}
        </div>

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
