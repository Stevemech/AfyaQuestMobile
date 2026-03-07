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
    throw new Error(body || res.statusText);
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
    request<{ chv: import('../types').CHV; houses: import('../types').House[] }>(`/admin/chvs/${chvId}`),

  getAnalytics: () =>
    request<{ stats: import('../types').AnalyticsStats; progress: import('../types').ModuleProgress[]; reports: import('../types').WeeklyReport[] }>(
      `/admin/analytics${orgParams()}`
    ),

  getReports: (params?: { week?: string; chvId?: string }) =>
    request<{ reports: import('../types').DailyReport[] }>(
      `/admin/reports${orgParams(params as Record<string, string>)}`
    ),

  assignModule: (chvId: string, moduleId: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'module', data: { moduleId } }),
    }),

  createItinerary: (chvId: string, date: string, stops: import('../types').ItineraryStop[]) =>
    request('/admin/itineraries', {
      method: 'POST',
      body: JSON.stringify({ chvId, date, stops }),
    }),

  mandateLesson: (chvId: string, lessonId: string, dueDate: string) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'lesson', data: { lessonId, dueDate } }),
    }),

  getClinics: () =>
    request<{ clinics: import('../types').Clinic[] }>('/admin/clinics'),

  getOrganizations: () =>
    request<{ organizations: { id: string; name: string }[] }>('/organizations'),
};
