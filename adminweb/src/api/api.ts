import { clearAdminSessionAndRedirectToLogin } from '../auth/sessionUtils';

const API_BASE = 'https://gc6iib7ck2.execute-api.af-south-1.amazonaws.com/prod';

function getToken(): string | null {
  return localStorage.getItem('adminToken');
}

function getOrganization(): string | null {
  return localStorage.getItem('adminOrg');
}

/** 401 on /auth/login is wrong password — do not treat as session expiry */
function isFailedLoginAttempt(path: string, options: RequestInit): boolean {
  return path === '/auth/login' && (options.method === 'POST' || options.method === 'post');
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch (err) {
    const isNetwork =
      err instanceof TypeError && (err.message === 'Failed to fetch' || err.name === 'TypeError');
    throw new Error(
      isNetwork
        ? 'Network error: could not reach the API. Check your connection, or ensure this endpoint is deployed in API Gateway (missing routes often show this error in the browser).'
        : err instanceof Error
          ? err.message
          : 'Network error'
    );
  }
  if (!res.ok) {
    // Expired or invalid token: log out and go to login (not for wrong-password login)
    if (res.status === 401 && !isFailedLoginAttempt(path, options)) {
      clearAdminSessionAndRedirectToLogin();
      return new Promise(() => {
        /* never resolves — page navigates away */
      }) as Promise<T>;
    }

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

const VIDEO_MODULES = [
  // Module 1: Body Systems
  { id: 'mod1-nervous-system', name: 'Nervous System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-musculoskeletal', name: 'Musculoskeletal System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-lymphatic', name: 'Lymphatic System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-endocrine', name: 'The Endocrine System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-integumentary', name: 'The Integumentary System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-urinary', name: 'Urinary System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-male-reproductive', name: 'Male Reproductive System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  { id: 'mod1-female-reproductive', name: 'Female Reproductive System', module: 1, moduleName: 'Body Systems', type: 'video' as const },
  // Module 2: Common Childhood Illnesses
  { id: 'mod2-warning-signs', name: 'Warning Signs', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  { id: 'mod2-diarrhea', name: 'Diarrhea', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  { id: 'mod2-respiratory-infections', name: 'Respiratory Infections', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  { id: 'mod2-asthma-pneumonia-tb', name: 'Asthma, Pneumonia & TB', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  { id: 'mod2-antibiotics', name: 'The Rules of Antibiotics', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  { id: 'mod2-malnutrition', name: 'Marasmus & Kwashiorkor', module: 2, moduleName: 'Common Childhood Illnesses', type: 'video' as const },
  // Module 3: Chronic & Infectious Diseases
  { id: 'mod3-chronic-illnesses', name: 'Understanding Chronic Illnesses', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  { id: 'mod3-diabetes', name: 'Diabetes', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  { id: 'mod3-heart-disease', name: 'Heart Disease, Hypertension & Strokes', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  { id: 'mod3-infectious-diseases', name: 'Introduction to Infectious Diseases', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  { id: 'mod3-reproductive-tract', name: 'Reproductive Tract Infections & Hepatitis B', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  { id: 'mod3-hiv-tb', name: 'HIV/AIDS and Tuberculosis', module: 3, moduleName: 'Chronic & Infectious Diseases', type: 'video' as const },
  // Module 4: Maternal & Reproductive Health
  { id: 'mod4-antenatal-care', name: 'Understanding Antenatal Care', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  { id: 'mod4-pregnancy', name: 'Pregnancy: Normal or Not?', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  { id: 'mod4-safe-delivery', name: 'Safe Delivery & Newborn Care', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  { id: 'mod4-birth-spacing', name: 'Birth Spacing', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  { id: 'mod4-short-term-contraception', name: 'Short-Term Contraception', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  { id: 'mod4-long-term-contraception', name: 'Long-Term Contraception', module: 4, moduleName: 'Maternal & Reproductive Health', type: 'video' as const },
  // Module 5: First Aid & Emergency Care
  { id: 'mod5-abcde-method', name: 'The ABCDE Method', module: 5, moduleName: 'First Aid & Emergency Care', type: 'video' as const },
  { id: 'mod5-treating-bleeding', name: 'Treating Bleeding', module: 5, moduleName: 'First Aid & Emergency Care', type: 'video' as const },
  { id: 'mod5-splint-bone', name: 'How to Splint a Broken Bone', module: 5, moduleName: 'First Aid & Emergency Care', type: 'video' as const },
  { id: 'mod5-choking', name: 'How to Help Someone Choking', module: 5, moduleName: 'First Aid & Emergency Care', type: 'video' as const },
  { id: 'mod5-burns-stings', name: 'First Aid: Burns & Stings', module: 5, moduleName: 'First Aid & Emergency Care', type: 'video' as const },
  // Module 6: Infection Prevention & Control
  { id: 'mod6-chain-of-infection', name: 'The Chain of Infection', module: 6, moduleName: 'Infection Prevention & Control', type: 'video' as const },
  { id: 'mod6-5fs-disease', name: "The 5 F's of Disease", module: 6, moduleName: 'Infection Prevention & Control', type: 'video' as const },
  { id: 'mod6-standard-precautions', name: 'Standard Precautions', module: 6, moduleName: 'Infection Prevention & Control', type: 'video' as const },
  { id: 'mod6-clinical-safety', name: 'Clinical Safety Rules', module: 6, moduleName: 'Infection Prevention & Control', type: 'video' as const },
  { id: 'mod6-unseen-shield', name: 'An Unseen Shield', module: 6, moduleName: 'Infection Prevention & Control', type: 'video' as const },
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

  registerAdmin: (data: { email: string; password: string; name: string; organizationName: string; phone?: string }) =>
    request<{ message: string; userId: string }>('/auth/register-admin', {
      method: 'POST',
      body: JSON.stringify(data),
    }),

  forgotPassword: (email: string) =>
    request<{ message: string }>('/auth/forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email }),
    }),

  confirmForgotPassword: (email: string, code: string, newPassword: string) =>
    request<{ message: string }>('/auth/confirm-forgot-password', {
      method: 'POST',
      body: JSON.stringify({ email, code, newPassword }),
    }),

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

  assignVideos: (chvId: string, moduleIds: string[]) =>
    request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'module', data: { moduleIds } }),
    }),

  assignFullModule: (chvId: string, moduleNumber: number) => {
    const moduleVideos = VIDEO_MODULES.filter(v => v.module === moduleNumber).map(v => v.id);
    return request('/admin/assign', {
      method: 'POST',
      body: JSON.stringify({ chvId, type: 'module', data: { moduleIds: moduleVideos, moduleNumber } }),
    });
  },

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

  /** Pass org from the layout when available so it matches the header even if adminOrg is missing from localStorage. */
  getNotifications: (organizationOverride?: string | null) => {
    const org = organizationOverride ?? getOrganization();
    if (!org) {
      return Promise.resolve({ notifications: [], unreadCount: 0 });
    }
    const params = new URLSearchParams();
    params.set('organization', org);
    return request<{ notifications: import('../types').AdminNotification[]; unreadCount: number }>(
      `/admin/notifications?${params.toString()}`
    );
  },

  markNotificationsRead: async (sks: string[], organizationOverride?: string | null) => {
    const org = organizationOverride ?? getOrganization();
    if (!org || sks.length === 0) return;
    await request('/admin/notifications/read', {
      method: 'POST',
      body: JSON.stringify({ organization: org, sks }),
    });
  },
};
