const API_BASE = 'https://gc6iib7ck2.execute-api.af-south-1.amazonaws.com/prod';

function getToken(): string | null {
  return localStorage.getItem('adminToken');
}

function getOrganization(): string | null {
  return localStorage.getItem('adminOrg');
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });
  if (!res.ok) {
    const body = await res.text();
    // Parse JSON error responses to extract user-friendly message
    try {
      const parsed = JSON.parse(body);
      throw new Error(parsed.details || parsed.error || parsed.message || res.statusText);
    } catch (parseErr) {
      if (parseErr instanceof SyntaxError) {
        throw new Error(body || res.statusText);
      }
      throw parseErr;
    }
  }
  return res.json();
}

/** Build a query string that always includes the admin's organization */
function orgParams(extra?: Record<string, string>): string {
  const org = getOrganization();
  const params = new URLSearchParams();
  if (org) params.set('organization', org);
  if (extra) {
    Object.entries(extra).forEach(([k, v]) => { if (v) params.set(k, v); });
  }
  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

// Video modules matching the mobile app (only 8, 9, 10 are implemented)
const VIDEO_MODULES = [
  { id: 'video-8', name: 'Sistema Reproductor Masculino / Male Reproductive System', nameEs: 'Sistema Reproductor Masculino', nameEn: 'Male Reproductive System', type: 'video' as const },
  { id: 'video-9', name: 'Sistema Reproductor Femenino / Female Reproductive System', nameEs: 'Sistema Reproductor Femenino', nameEn: 'Female Reproductive System', type: 'video' as const },
  { id: 'video-10', name: 'Sistema Urinario / Urinary System', nameEs: 'Sistema Urinario', nameEn: 'Urinary System', type: 'video' as const },
];

// Interactive lessons matching the mobile app (IDs 1-6)
const INTERACTIVE_LESSONS = [
  { id: 'lesson-1', name: 'Técnicas de Lavado de Manos / Handwashing Techniques', nameEs: 'Técnicas de Lavado de Manos', nameEn: 'Proper Handwashing Techniques', type: 'lesson' as const },
  { id: 'lesson-2', name: 'Dieta Balanceada para Niños / Balanced Diet for Children', nameEs: 'Dieta Balanceada para Niños', nameEn: 'Balanced Diet for Children', type: 'lesson' as const },
  { id: 'lesson-3', name: 'Cuidados Prenatales / Prenatal Care Essentials', nameEs: 'Cuidados Prenatales Esenciales', nameEn: 'Prenatal Care Essentials', type: 'lesson' as const },
  { id: 'lesson-4', name: 'Calendario de Vacunación / Child Vaccination Schedule', nameEs: 'Calendario de Vacunación Infantil', nameEn: 'Child Vaccination Schedule', type: 'lesson' as const },
  { id: 'lesson-5', name: 'Prevención de Malaria / Malaria Prevention', nameEs: 'Prevención de Malaria', nameEn: 'Malaria Prevention', type: 'lesson' as const },
  { id: 'lesson-6', name: 'RCP Básico / CPR Basics', nameEs: 'RCP Básico', nameEn: 'CPR Basics', type: 'lesson' as const },
];

export const ALL_MODULES = [...VIDEO_MODULES, ...INTERACTIVE_LESSONS];

export const api = {
  // Auth
  login: (email: string, password: string) =>
    request<{
      idToken: string;
      accessToken: string;
      refreshToken: string;
      expiresIn: number;
      user: {
        id: string;
        name: string;
        role: string;
        email?: string;
        phone?: string;
        organization?: string;
      };
    }>(
      '/auth/login',
      { method: 'POST', body: JSON.stringify({ email, password }) }
    ),

  getCurrentUser: () => request<{ user: Record<string, string> }>('/auth/me'),

  // Admin endpoints
  getCHVs: () =>
    request<{ chvs: import('../types').CHV[] }>(`/admin/chvs${orgParams()}`),

  getCHVDetail: (chvId: string) =>
    request<{
      chv: import('../types').CHV;
      houses: import('../types').House[];
      itineraries?: import('../types').Itinerary[];
      assignments?: import('../types').CHVAssignment[];
      clockHistory?: import('../types').ClockEvent[];
    }>(`/admin/chvs/${chvId}`),

  getAnalytics: () =>
    request<{ stats: import('../types').AnalyticsStats; progress: import('../types').ModuleProgress[]; reports: import('../types').WeeklyReport[] }>(
      `/admin/analytics${orgParams()}`
    ),

  getReports: (params?: { week?: string; chvId?: string }) =>
    request<{ reports: import('../types').DailyReport[] }>(
      `/admin/reports${orgParams(params as Record<string, string>)}`
    ).then(res => ({
      reports: (res.reports || []).map(r => ({
        ...r,
        isSynced: r.isSynced !== undefined ? r.isSynced : true, // reports in DynamoDB are synced
      })),
    })),

  assignModule: (chvId: string, moduleId: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'module', data: { moduleId } }),
    }),

  assignLesson: (chvId: string, lessonId: string, dueDate?: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'lesson', data: { lessonId, dueDate: dueDate || null } }),
    }),

  requestReport: (chvId: string, dueDate?: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'report', data: { reportType: 'daily', dueDate: dueDate || null } }),
    }),

  createItinerary: (chvId: string, date: string, stops: import('../types').ItineraryStop[]) =>
    request('/admin/itineraries', {
      method: 'POST',
      body: JSON.stringify({ chvId, date, stops }),
    }),

  deleteItinerary: (chvId: string, date: string) =>
    request('/admin/itineraries', {
      method: 'POST',
      body: JSON.stringify({ action: 'delete', chvId, date }),
    }),

  deleteAssignment: (chvId: string, type: 'module' | 'lesson', itemId: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type, action: 'delete', data: type === 'module' ? { moduleId: itemId } : { lessonId: itemId } }),
    }),

  mandateLesson: (chvId: string, lessonId: string, dueDate: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'lesson', data: { lessonId, dueDate } }),
    }),

  getClinics: () =>
    request<{ clinics: import('../types').Clinic[] }>('/admin/clinics'),

  getOrganizations: () =>
    request<{ organizations: { id: string; name: string; location?: string }[] }>('/organizations'),
};
