import { NavLink } from 'react-router-dom';
import { Home, BarChart3, FileText, Settings, ChevronDown } from 'lucide-react';
import { useAuth } from '../../auth/AuthContext';

const navItems = [
  { to: '/settings', label: 'Settings', icon: Settings },
  { to: '/operations', label: 'Operations', icon: Home },
  { to: '/analytics', label: 'CHV Analytics', icon: BarChart3 },
  { to: '/reports', label: 'Reports Archive', icon: FileText },
];

export default function Sidebar() {
  const { user, logout } = useAuth();

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

      {/* Bottom settings link */}
      <div className="px-3 py-2">
        <NavLink
          to="/settings"
          className={({ isActive }) =>
            `flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
              isActive
                ? 'bg-primary-light text-primary'
                : 'text-text-secondary hover:bg-gray-50'
            }`
          }
        >
          <Settings size={18} />
          Settings
        </NavLink>
      </div>

      {/* User section */}
      <div className="border-t border-border px-4 py-3">
        <button onClick={logout} className="flex items-center gap-3 w-full text-left group">
          <div className="w-10 h-10 rounded-full bg-primary-light flex items-center justify-center text-primary font-semibold text-sm">
            {user?.name?.charAt(0) || 'A'}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium text-text-primary truncate">Admin</p>
            <p className="text-xs text-text-secondary truncate">{user?.name || 'Admin User'}</p>
          </div>
          <ChevronDown size={16} className="text-text-secondary" />
        </button>
      </div>
    </aside>
  );
}
