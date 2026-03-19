import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import LanguageSwitcher from '../components/common/LanguageSwitcher';

export default function SignUpPage() {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [organizationName, setOrganizationName] = useState('');
  const [phone, setPhone] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (password !== confirmPassword) {
      setError(t('signUp.passwordMismatch'));
      return;
    }

    setLoading(true);
    try {
      await api.registerAdmin({ email, password, name, organizationName, phone: phone || undefined });
      setSuccess(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : t('signUp.failed'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-body-bg flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="flex justify-end mb-4">
          <LanguageSwitcher />
        </div>

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

        <div className="bg-white rounded-xl shadow-sm border border-border p-8">
          {success ? (
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
                <svg className="w-8 h-8 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-text-primary mb-2">{t('signUp.successHeading')}</h2>
              <p className="text-sm text-text-secondary mb-2">{t('signUp.successVerify')}</p>
              <p className="text-sm text-text-secondary mb-6">{t('signUp.successApproval')}</p>
              <Link
                to="/login"
                className="inline-block w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors text-center"
              >
                {t('signUp.backToLogin')}
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit}>
              <h2 className="text-xl font-semibold text-text-primary mb-6">{t('signUp.heading')}</h2>

              {error && (
                <div className="mb-4 p-3 bg-danger-light text-danger text-sm rounded-lg">{error}</div>
              )}

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('signUp.name')}</label>
                  <input
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('signUp.namePlaceholder')}
                    required
                  />
                </div>

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
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('signUp.orgName')}</label>
                  <input
                    type="text"
                    value={organizationName}
                    onChange={e => setOrganizationName(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('signUp.orgNamePlaceholder')}
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">
                    {t('signUp.phone')} <span className="text-text-secondary font-normal">({t('signUp.optional')})</span>
                  </label>
                  <input
                    type="tel"
                    value={phone}
                    onChange={e => setPhone(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('signUp.phonePlaceholder')}
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
                    minLength={8}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('signUp.confirmPassword')}</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('signUp.confirmPasswordPlaceholder')}
                    required
                    minLength={8}
                  />
                </div>
              </div>

              <p className="text-xs text-text-secondary mt-2">{t('signUp.passwordRequirements')}</p>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors disabled:opacity-50"
              >
                {loading ? t('signUp.submitting') : t('signUp.submit')}
              </button>

              <p className="text-center mt-4">
                <Link to="/login" className="text-sm text-primary hover:underline">
                  {t('signUp.haveAccount')}
                </Link>
              </p>
            </form>
          )}
        </div>
      </div>
    </div>
  );
}
