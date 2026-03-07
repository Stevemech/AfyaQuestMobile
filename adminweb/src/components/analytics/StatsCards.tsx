import { Play, AlertTriangle, BookOpen, FileCheck, TrendingUp } from 'lucide-react';
import type { AnalyticsStats } from '../../types';

interface StatsCardsProps {
  stats: AnalyticsStats;
}

const cards = [
  {
    key: 'avgVideoCompletion' as const,
    label: 'Avg Video Completion',
    icon: Play,
    bg: 'bg-orange-50',
    iconBg: 'bg-orange-100',
    iconColor: 'text-orange-500',
    suffix: '%',
    trendIcon: TrendingUp,
  },
  {
    key: 'atRiskCHVs' as const,
    label: 'At-Risk CHVs',
    icon: AlertTriangle,
    bg: 'bg-blue-50',
    iconBg: 'bg-blue-100',
    iconColor: 'text-blue-500',
    suffix: '',
    trendIcon: TrendingUp,
  },
  {
    key: 'modulesAssigned' as const,
    label: 'Modules Assigned',
    icon: BookOpen,
    bg: 'bg-pink-50',
    iconBg: 'bg-pink-100',
    iconColor: 'text-pink-500',
    suffix: '',
    trendIcon: null,
  },
  {
    key: 'reportsSubmitted' as const,
    label: 'Reports Submitted',
    icon: FileCheck,
    bg: 'bg-teal-50',
    iconBg: 'bg-teal-100',
    iconColor: 'text-teal-600',
    suffix: '%',
    trendIcon: null,
  },
];

export default function StatsCards({ stats }: StatsCardsProps) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {cards.map(card => {
        const Icon = card.icon;
        return (
          <div key={card.key} className={`${card.bg} rounded-xl p-5 relative overflow-hidden`}>
            <div className="flex items-start justify-between">
              <div>
                <div className={`w-10 h-10 rounded-lg ${card.iconBg} flex items-center justify-center mb-3`}>
                  <Icon size={20} className={card.iconColor} />
                </div>
                <p className="text-xs text-text-secondary font-medium">{card.label}</p>
                <p className="text-2xl font-bold text-text-primary mt-1">
                  {stats[card.key]}{card.suffix}
                </p>
              </div>
              {card.trendIcon && (
                <div className="w-8 h-8 rounded-lg bg-white/60 flex items-center justify-center">
                  <TrendingUp size={16} className="text-primary" />
                </div>
              )}
              {card.key === 'reportsSubmitted' && (
                <div className="w-8 h-8 rounded-lg bg-white/60 flex items-center justify-center">
                  <FileCheck size={16} className="text-success" />
                </div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
