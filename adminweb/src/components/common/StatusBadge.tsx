import { CheckCircle, AlertTriangle, XCircle } from 'lucide-react';

interface StatusBadgeProps {
  type: string;
  label?: string;
  size?: 'sm' | 'md';
}

const config: Record<string, { bg: string; text: string; icon: typeof CheckCircle }> = {
  success: { bg: 'bg-success-light', text: 'text-success', icon: CheckCircle },
  active: { bg: 'bg-success-light', text: 'text-success', icon: CheckCircle },
  warning: { bg: 'bg-warning-light', text: 'text-warning', icon: AlertTriangle },
  caution: { bg: 'bg-warning-light', text: 'text-warning', icon: AlertTriangle },
  alert: { bg: 'bg-warning-light', text: 'text-warning', icon: AlertTriangle },
  danger: { bg: 'bg-danger-light', text: 'text-danger', icon: XCircle },
  inactive: { bg: 'bg-danger-light', text: 'text-danger', icon: XCircle },
};

export default function StatusBadge({ type, label, size = 'md' }: StatusBadgeProps) {
  const c = config[type] || config.warning;
  const Icon = c.icon;
  const iconSize = size === 'sm' ? 12 : 16;

  if (!label) {
    return (
      <span className={`inline-flex items-center justify-center w-6 h-6 rounded-full ${c.bg}`}>
        <Icon size={iconSize} className={c.text} />
      </span>
    );
  }

  return (
    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${c.bg} ${c.text}`}>
      <Icon size={iconSize} />
      {label}
    </span>
  );
}
