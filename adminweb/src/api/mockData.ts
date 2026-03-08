import type { CHV, House, ModuleProgress, WeeklyReport, AnalyticsStats, DailyReport, Clinic } from '../types';

export const mockClinics: Clinic[] = [
  { id: 'clinic-1', name: 'Guatemala Demo Clinic', location: 'Guatemala' },
];

export const mockCHVs: CHV[] = [
  {
    id: 'chv-1', name: 'Amina', email: 'amina@afyaquest.org', phone: '+254700000001',
    clinic: 'Guatemala Demo', organization: 'guatemala-demo-org', assignedHouses: 12, completedVisits: 9, pendingVisits: 3,
    completionRate: 74, lastActive: '2 days ago', isActive: true,
    status: 'active', flags: [],
  },
  {
    id: 'chv-2', name: 'Joseph', email: 'joseph@afyaquest.org', phone: '+254700000002',
    clinic: 'Guatemala Demo', organization: 'guatemala-demo-org', assignedHouses: 10, completedVisits: 4, pendingVisits: 6,
    completionRate: 42, lastActive: 'Yesterday', isActive: true,
    status: 'caution', flags: ['caution'],
  },
  {
    id: 'chv-3', name: 'Fatima', email: 'fatima@afyaquest.org', phone: '+254700000003',
    clinic: 'Guatemala Demo', organization: 'guatemala-demo-org', assignedHouses: 8, completedVisits: 6, pendingVisits: 2,
    completionRate: 74, lastActive: '9 days ago', isActive: false,
    status: 'inactive', flags: ['alert', 'danger'],
  },
  {
    id: 'chv-4', name: 'David', email: 'david@afyaquest.org', phone: '+254700000004',
    clinic: 'Guatemala Demo', organization: 'guatemala-demo-org', assignedHouses: 14, completedVisits: 12, pendingVisits: 2,
    completionRate: 86, lastActive: '2 days ago', isActive: true,
    status: 'active', flags: ['active'],
  },
  {
    id: 'chv-5', name: 'Faith', email: 'faith@afyaquest.org', phone: '+254700000005',
    clinic: 'Guatemala Demo', organization: 'guatemala-demo-org', assignedHouses: 6, completedVisits: 0, pendingVisits: 6,
    completionRate: 0, lastActive: '15 days ago', isActive: false,
    status: 'inactive', flags: ['danger'],
  },
];

export const mockHouses: Record<string, House[]> = {
  'chv-1': [
    { id: 'H-1023', assignedCHV: 'chv-1', assignedCHVName: 'Amina', latitude: 14.6349, longitude: -90.5069, distance: 0.4, visitStatus: 'pending', priority: 'high' },
    { id: 'H-1034', assignedCHV: 'chv-1', assignedCHVName: 'Amina', latitude: 14.6360, longitude: -90.5080, distance: 1.2, visitStatus: 'completed', priority: 'medium' },
    { id: 'H-1098', assignedCHV: 'chv-1', assignedCHVName: 'Amina', latitude: 14.6370, longitude: -90.5090, distance: 2.1, visitStatus: 'pending', priority: 'medium', daysPending: 4 },
  ],
  'chv-2': [
    { id: 'H-1034', assignedCHV: 'chv-2', assignedCHVName: 'Joseph', latitude: 14.6340, longitude: -90.5060, distance: 0.8, visitStatus: 'pending', priority: 'medium' },
    { id: 'H-1045', assignedCHV: 'chv-2', assignedCHVName: 'Joseph', latitude: 14.6355, longitude: -90.5075, distance: 1.5, visitStatus: 'overdue', priority: 'high', daysPending: 7 },
  ],
  'chv-3': [
    { id: 'H-1068', assignedCHV: 'chv-3', assignedCHVName: 'Faith', latitude: 14.6380, longitude: -90.5100, distance: 1.8, visitStatus: 'pending', priority: 'high', daysPending: 9 },
  ],
};

export const mockAnalyticsStats: AnalyticsStats = {
  avgVideoCompletion: 82,
  atRiskCHVs: 3,
  modulesAssigned: 14,
  reportsSubmitted: 92,
};

export const mockModuleProgress: ModuleProgress[] = [
  {
    chvId: 'chv-1', chvName: 'Amina',
    modules: [
      { moduleId: 'video-1', completed: true, score: 90 },
      { moduleId: 'video-2', completed: true, score: 60 },
    ],
    overallProgress: 60,
    flag: { type: 'warning', value: '69%' },
  },
  {
    chvId: 'chv-2', chvName: 'Joseph',
    modules: [
      { moduleId: 'video-1', completed: true, score: 100 },
      { moduleId: 'video-2', completed: true, score: 100 },
    ],
    overallProgress: 100,
    flag: { type: 'success', value: '100' },
  },
  {
    chvId: 'chv-5', chvName: 'Faith',
    modules: [
      { moduleId: 'video-1', completed: false, score: 0 },
      { moduleId: 'video-2', completed: false, score: 0 },
    ],
    overallProgress: 0,
  },
];

export const mockWeeklyReports: WeeklyReport[] = [
  { chvId: 'chv-1', chvName: 'Amina', week: 'Feb 1-7', highRisk: 'Child fever', notes: 1, flag: { type: 'warning', value: '67%' }, completionStatus: false },
  { chvId: 'chv-2', chvName: 'Joseph', week: 'Feb 1-7', highRisk: '', notes: 0, flag: { type: 'success', value: '' }, completionStatus: true },
  { chvId: 'chv-3', chvName: 'Fatima', week: 'Feb 1-7', highRisk: 'Malaria', notes: 2, flag: { type: 'danger', value: '45%' }, completionStatus: false },
];

export const mockDailyReports: DailyReport[] = [
  { id: 'r-1', userId: 'chv-1', userName: 'Amina', date: '2026-02-07', patientsVisited: 8, vaccinationsGiven: 3, healthEducation: 5, challenges: 'Transport issues', notes: 'Covered all assigned areas', isSynced: true },
  { id: 'r-2', userId: 'chv-2', userName: 'Joseph', date: '2026-02-07', patientsVisited: 12, vaccinationsGiven: 6, healthEducation: 4, challenges: '', notes: '', isSynced: true },
  { id: 'r-3', userId: 'chv-1', userName: 'Amina', date: '2026-02-06', patientsVisited: 6, vaccinationsGiven: 2, healthEducation: 3, challenges: 'Rain delay', notes: 'Child fever case reported', isSynced: true },
  { id: 'r-4', userId: 'chv-4', userName: 'David', date: '2026-02-07', patientsVisited: 15, vaccinationsGiven: 8, healthEducation: 7, challenges: '', notes: 'All visits complete', isSynced: true },
  { id: 'r-5', userId: 'chv-3', userName: 'Fatima', date: '2026-02-05', patientsVisited: 4, vaccinationsGiven: 1, healthEducation: 2, challenges: 'Community resistance', notes: 'Need follow-up', isSynced: false },
];

// Use mock data when API calls fail (dev mode)
export const useMockData = true;
