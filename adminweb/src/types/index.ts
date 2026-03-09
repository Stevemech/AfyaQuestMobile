export interface User {
  id: string;
  email: string;
  name: string;
  phone?: string;
  role: 'cha' | 'supervisor' | 'admin';
  language?: string;
  level?: number;
  totalPoints?: number;
  rank?: string;
  currentStreak?: number;
  isActive?: boolean;
  location?: string;
  supervisorId?: string;
  profilePictureUrl?: string;
  lastActiveDate?: string;
  createdAt?: string;
}

export interface CHV {
  id: string;
  name: string;
  email: string;
  phone?: string;
  clinic: string;
  organization?: string;
  role?: string;
  level?: number;
  totalPoints?: number;
  lives?: number;
  currentStreak?: number;
  lastActive: string;
  isActive: boolean;
  status: 'active' | 'caution' | 'inactive';
  manualStatus?: 'active' | 'inactive' | null;
  lastClockIn?: string | null;
  lastClockOut?: string | null;
  language?: string;
  createdAt?: string;
  // Legacy fields for backward compatibility with detail views
  assignedHouses?: number;
  completedVisits?: number;
  pendingVisits?: number;
  completionRate?: number;
  profilePictureUrl?: string;
  flags?: string[];
}

export interface ClockEvent {
  action: 'clock_in' | 'clock_out';
  timestamp: string;
  date: string;
}

export interface House {
  id: string;
  assignedCHV: string;
  assignedCHVName: string;
  latitude: number;
  longitude: number;
  distance: number;
  visitStatus: 'pending' | 'completed' | 'overdue';
  priority: 'high' | 'medium' | 'low';
  daysPending?: number;
  lastVisit?: string;
}

export interface ModuleProgress {
  chvId: string;
  chvName: string;
  level?: number;
  totalPoints?: number;
  modules: { id?: string; moduleId?: string; type?: string; status?: string; completed: boolean; score?: number }[];
  overallProgress: number;
  flag?: { type: 'success' | 'warning' | 'danger'; value: string };
}

export interface WeeklyReport {
  chvId: string;
  chvName: string;
  week: string;
  reportsCount?: number;
  totalPatients?: number;
  highRisk: string | number;
  notes?: number;
  submitted?: boolean;
  flag?: { type: 'success' | 'warning' | 'danger'; value: string };
  completionStatus?: boolean;
}

export interface AnalyticsStats {
  avgVideoCompletion: number;
  atRiskCHVs: number;
  modulesAssigned: number;
  reportsSubmitted: number;
}

export interface Clinic {
  id: string;
  name: string;
  location: string;
}

export interface Itinerary {
  id?: string;
  chvId?: string;
  date: string;
  stops: ItineraryStop[];
  completedStops?: string[];
  status?: string;
  createdAt?: string;
  createdBy?: string;
}

export interface CHVAssignment {
  type: 'module' | 'lesson' | 'report';
  moduleId?: string | null;
  lessonId?: string | null;
  status: string;
  mandatory?: boolean;
  dueDate?: string | null;
  assignedAt?: string;
  assignedBy?: string;
}

export interface ItineraryStop {
  order: number;
  houseId: string;
  label: string;
  address: string;
  description?: string;
  notes?: string;
  latitude: number;
  longitude: number;
  completed?: boolean;
}

export interface DailyReport {
  id: string;
  userId: string;
  userName: string;
  date: string;
  patientsVisited: number;
  vaccinationsGiven: number;
  healthEducation: number;
  challenges: string;
  notes: string;
  isSynced: boolean;
  createdAt?: string;
}

export interface AssignmentRequest {
  chvId: string;
  type: 'module' | 'report' | 'itinerary';
  data: Record<string, unknown>;
}
