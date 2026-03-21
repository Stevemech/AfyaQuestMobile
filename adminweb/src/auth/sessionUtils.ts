/**
 * Clears admin session storage and navigates to the login page.
 * Used when API returns 401 (expired/invalid JWT).
 */
export function clearAdminSessionAndRedirectToLogin(): void {
  localStorage.removeItem('adminToken');
  localStorage.removeItem('adminRefreshToken');
  localStorage.removeItem('adminUser');
  localStorage.removeItem('adminOrg');

  const base = (import.meta.env.BASE_URL || '/').replace(/\/?$/, '/');
  const url = `${window.location.origin}${base}login?session=expired`;
  window.location.replace(url);
}
