import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import { Home, BarChart3, FileText, Settings, LogOut } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useAuth } from '../../auth/AuthContext';

export default function Sidebar() {
  const { user, logout } = useAuth();
  const { t } = useTranslation();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);

  const navItems = [
    { to: '/operations', label: t('nav.operations'), icon: Home },
    { to: '/analytics', label: t('nav.chvAnalytics'), icon: BarChart3 },
    { to: '/reports', label: t('nav.reportsArchive'), icon: FileText },
    { to: '/settings', label: t('nav.settings'), icon: Settings },
  ];

  return (
    <aside className="w-60 bg-white border-r border-border flex flex-col h-screen sticky top-0">
      {/* Logo */}
      <div className="px-5 py-5 flex items-center gap-2">
        <svg width="32" height="32" viewBox="0 0 32 32" className="shrink-0">
          <rect width="32" height="32" rx="6" fill="#2D9F7C" />
          <path d="M8 20l4-8 4 4 4-6 4 10" stroke="white" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
        <span className="text-lg font-bold text-text-primary">AfyaQuest</span>
      </div>

      {/* Navigation */}
      <nav className="flex-1 px-3 py-2 space-y-1">
        {navItems.map(({ to, label, icon: Icon }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-primary-light text-primary'
                  : 'text-text-secondary hover:bg-gray-50 hover:text-text-primary'
              }`
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}
      </nav>

      {/* User section */}
      <div className="border-t border-border px-4 py-3">
        <div className="flex items-center gap-3 w-full">
          <div className="w-10 h-10 rounded-full bg-primary-light flex items-center justify-center text-primary font-semibold text-sm">
            {user?.name?.charAt(0) || 'A'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-text-primary truncate">{t('admin')}</p>
            <p className="text-xs text-text-secondary truncate">{user?.name || 'Admin User'}</p>
          </div>
          <button
            onClick={() => setShowLogoutConfirm(true)}
            className="p-1.5 rounded-lg hover:bg-gray-100 text-text-secondary hover:text-danger transition-colors"
            title={t('sidebar.signOut')}
          >
            <LogOut size={16} />
          </button>
        </div>
      </div>

      {/* Logout confirmation dialog */}
      {showLogoutConfirm && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setShowLogoutConfirm(false)}>
          <div className="bg-white rounded-xl shadow-xl max-w-sm w-full mx-4 p-6" onClick={e => e.stopPropagation()}>
            <h3 className="text-lg font-semibold text-text-primary mb-2">{t('sidebar.signOutTitle')}</h3>
            <p className="text-sm text-text-secondary mb-6">{t('sidebar.signOutConfirm')}</p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowLogoutConfirm(false)}
                className="flex-1 py-2 border border-border rounded-lg text-sm font-medium text-text-primary hover:bg-gray-50"
              >
                {t('sidebar.cancel')}
              </button>
              <button
                onClick={() => { setShowLogoutConfirm(false); logout(); }}
                className="flex-1 py-2 bg-danger text-white rounded-lg text-sm font-medium hover:bg-red-600"
              >
                {t('sidebar.signOut')}
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
