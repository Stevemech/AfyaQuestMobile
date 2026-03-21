import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react';
import { useTranslation } from 'react-i18next';
import { api } from '../api/api';
import type { AdminNotification } from '../types';
import { formatNotificationMessage } from './formatNotification';

type ToastItem = { id: string; message: string };

interface NotificationState {
  notifications: AdminNotification[];
  unreadCount: number;
  loading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  markRead: (sks: string[]) => Promise<void>;
  markAllRead: () => Promise<void>;
}

const NotificationContext = createContext<NotificationState | null>(null);

export function NotificationProvider({
  organization,
  children,
}: {
  organization: string;
  children: ReactNode;
}) {
  const { t } = useTranslation();
  const [notifications, setNotifications] = useState<AdminNotification[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const seenIdsRef = useRef<Set<string>>(new Set());
  const bootstrappedRef = useRef(false);

  const pushToast = useCallback((message: string) => {
    const id = crypto.randomUUID();
    setToasts(prev => [...prev, { id, message }]);
    window.setTimeout(() => {
      setToasts(prev => prev.filter(x => x.id !== id));
    }, 6500);
  }, []);

  const fetchNotifications = useCallback(async () => {
    if (!organization) {
      setNotifications([]);
      setUnreadCount(0);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const res = await api.getNotifications(organization);
      const list = res.notifications || [];
      setNotifications(list);
      setUnreadCount(res.unreadCount ?? list.filter(n => !n.read).length);

      if (!bootstrappedRef.current) {
        bootstrappedRef.current = true;
        list.forEach(n => seenIdsRef.current.add(n.id));
        return;
      }

      for (const n of list) {
        if (!seenIdsRef.current.has(n.id)) {
          seenIdsRef.current.add(n.id);
          pushToast(formatNotificationMessage(n, t));
        }
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load notifications');
    } finally {
      setLoading(false);
    }
  }, [organization, pushToast, t]);

  useEffect(() => {
    seenIdsRef.current = new Set();
    bootstrappedRef.current = false;
  }, [organization]);

  useEffect(() => {
    if (!organization) return;

    void fetchNotifications();
    const id = window.setInterval(() => {
      void fetchNotifications();
    }, 45_000);
    return () => window.clearInterval(id);
  }, [organization, fetchNotifications]);

  const markRead = useCallback(
    async (sks: string[]) => {
      if (!sks.length) return;
      await api.markNotificationsRead(sks, organization);
      await fetchNotifications();
    },
    [fetchNotifications]
  );

  const markAllRead = useCallback(async () => {
    const unread = notifications.filter(n => !n.read).map(n => n.sk);
    if (!unread.length) return;
    await api.markNotificationsRead(unread, organization);
    await fetchNotifications();
  }, [notifications, fetchNotifications]);

  const dismissToast = useCallback((id: string) => {
    setToasts(prev => prev.filter(x => x.id !== id));
  }, []);

  const value = useMemo(
    () => ({
      notifications,
      unreadCount,
      loading,
      error,
      refresh: fetchNotifications,
      markRead,
      markAllRead,
    }),
    [notifications, unreadCount, loading, error, fetchNotifications, markRead, markAllRead]
  );

  return (
    <NotificationContext.Provider value={value}>
      {children}
      <div
        className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 items-end pointer-events-none max-w-[min(100vw-2rem,22rem)]"
        aria-live="polite"
      >
        {toasts.map(item => (
          <button
            key={item.id}
            type="button"
            onClick={() => dismissToast(item.id)}
            className="pointer-events-auto text-left w-full rounded-xl border border-border bg-white shadow-lg px-4 py-3 text-sm text-text-primary animate-notification-in hover:bg-gray-50/90 transition-colors"
          >
            <p className="font-medium text-text-primary leading-snug">{item.message}</p>
            <p className="text-xs text-text-secondary mt-1">{t('notifications.tapToDismiss')}</p>
          </button>
        ))}
      </div>
    </NotificationContext.Provider>
  );
}

export function useNotifications() {
  const ctx = useContext(NotificationContext);
  if (!ctx) throw new Error('useNotifications must be used within NotificationProvider');
  return ctx;
}
