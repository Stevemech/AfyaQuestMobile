import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import LanguageSwitcher from '../components/common/LanguageSwitcher';

export default function LoginPage() {
  const { login } = useAuth();
  const { t } = useTranslation();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(email, password);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-body-bg flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Language switcher */}
        <div className="flex justify-end mb-4">
          <LanguageSwitcher />
        </div>

        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center gap-2">
            <svg width="40" height="40" viewBox="0 0 32 32">
              <rect width="32" height="32" rx="6" fill="#2D9F7C" />
              <path d="M8 20l4-8 4 4 4-6 4 10" stroke="white" strokeWidth="2.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
            </svg>
            <span className="text-2xl font-bold text-text-primary">AfyaQuest</span>
          </div>
          <p className="text-text-secondary mt-2">{t('login.adminPortal')}</p>
        </div>

        {/* Login form */}
        <form onSubmit={handleSubmit} className="bg-white rounded-xl shadow-sm border border-border p-8">
          <h2 className="text-xl font-semibold text-text-primary mb-6">{t('login.signIn')}</h2>

          {error && (
            <div className="mb-4 p-3 bg-danger-light text-danger text-sm rounded-lg">{error}</div>
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1.5">{t('login.email')}</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                placeholder={t('login.emailPlaceholder')}
                required
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-text-primary mb-1.5">{t('login.password')}</label>
              <input
                type="password"
                value={password}
                onChange={e => setPassword(e.target.value)}
                className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                placeholder={t('login.passwordPlaceholder')}
                required
              />
            </div>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full mt-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors disabled:opacity-50"
          >
            {loading ? t('login.signingIn') : t('login.signIn')}
          </button>

          <p className="text-center mt-4">
            <Link to="/forgot-password" className="text-sm text-primary hover:underline">
              {t('login.forgotPassword')}
            </Link>
          </p>

          <p className="text-center mt-2">
            <Link to="/signup" className="text-sm text-text-secondary hover:text-primary hover:underline">
              {t('login.noAccount')}
            </Link>
          </p>
        </form>
      </div>
    </div>
  );
}
