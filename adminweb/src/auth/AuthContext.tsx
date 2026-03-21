import { createContext, useContext, useState, useEffect, type ReactNode } from 'react';
import { api } from '../api/api';

interface AdminUser {
  id: string;
  name: string;
  role: string;
  email?: string;
  phone?: string;
  organization?: string;
}

interface AuthState {
  isAuthenticated: boolean;
  isLoading: boolean;
  user: AdminUser | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthState | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AdminUser | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('adminToken');
    const savedUser = localStorage.getItem('adminUser');
    if (token && savedUser) {
      try {
        const u = JSON.parse(savedUser) as AdminUser;
        if (u.organization) {
          localStorage.setItem('adminOrg', u.organization);
        }
        setUser(u);
      } catch {
        setIsLoading(false);
        return;
      }
    }
    setIsLoading(false);
  }, []);

  const login = async (email: string, password: string) => {
    const res = await api.login(email, password);
    if (res.user.role !== 'admin') {
      throw new Error('Only admin users can access this portal');
    }
    localStorage.setItem('adminToken', res.idToken);
    localStorage.setItem('adminRefreshToken', res.refreshToken);
    localStorage.setItem('adminUser', JSON.stringify(res.user));
    if (res.user.organization) {
      localStorage.setItem('adminOrg', res.user.organization);
    }
    setUser(res.user);
  };

  const logout = () => {
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminRefreshToken');
    localStorage.removeItem('adminUser');
    localStorage.removeItem('adminOrg');
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated: !!user, isLoading, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
