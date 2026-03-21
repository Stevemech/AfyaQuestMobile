import type { TFunction } from 'i18next';
import type { AdminNotification } from '../types';
import { ALL_MODULES } from '../api/api';

function moduleLabel(itemId: string): string {
  const m = ALL_MODULES.find(x => x.id === itemId);
  if (!m) return itemId;
  if (m.type === 'lesson') return m.nameEn || m.name;
  return m.name;
}

export function formatNotificationMessage(n: AdminNotification, t: TFunction): string {
  const name = n.chvName || t('notifications.fallbackName');
  const meta = n.meta || {};

  switch (n.type) {
    case 'module_quiz_complete': {
      const itemId = String(meta.itemId || '');
      const label = moduleLabel(itemId);
      const score = meta.score;
      if (typeof score === 'number') {
        return t('notifications.moduleQuizCompleteScore', { name, label, score });
      }
      return t('notifications.moduleQuizComplete', { name, label });
    }
    case 'lesson_complete': {
      const itemId = String(meta.itemId || '');
      const label = moduleLabel(itemId);
      return t('notifications.lessonComplete', { name, label });
    }
    case 'itinerary_stop_complete': {
      const date = String(meta.date || '');
      return t('notifications.itineraryStop', { name, date });
    }
    case 'daily_report_submitted': {
      const date = String(meta.date || '');
      return t('notifications.dailyReport', { name, date });
    }
    case 'clock_in':
      return t('notifications.clockIn', { name });
    case 'clock_out':
      return t('notifications.clockOut', { name });
    default:
      return t('notifications.unknown', { name });
  }
}
