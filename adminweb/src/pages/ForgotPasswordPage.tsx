import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { api } from '../api/api';
import LanguageSwitcher from '../components/common/LanguageSwitcher';

type Step = 'email' | 'code' | 'success';

export default function ForgotPasswordPage() {
  const { t } = useTranslation();
  const [step, setStep] = useState<Step>('email');
  const [email, setEmail] = useState('');
  const [code, setCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSendCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.forgotPassword(email);
      setStep('code');
    } catch (err) {
      setError(err instanceof Error ? err.message : t('forgotPassword.sendFailed'));
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (newPassword !== confirmPassword) {
      setError(t('forgotPassword.passwordMismatch'));
      return;
    }

    setLoading(true);
    try {
      await api.confirmForgotPassword(email, code, newPassword);
      setStep('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : t('forgotPassword.resetFailed'));
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
          <p className="text-text-secondary mt-2">{t('forgotPassword.title')}</p>
        </div>

        <div className="bg-white rounded-xl shadow-sm border border-border p-8">
          {step === 'email' && (
            <form onSubmit={handleSendCode}>
              <h2 className="text-xl font-semibold text-text-primary mb-2">{t('forgotPassword.heading')}</h2>
              <p className="text-sm text-text-secondary mb-6">{t('forgotPassword.emailPrompt')}</p>

              {error && (
                <div className="mb-4 p-3 bg-danger-light text-danger text-sm rounded-lg">{error}</div>
              )}

              <div className="mb-4">
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

              <button
                type="submit"
                disabled={loading}
                className="w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors disabled:opacity-50"
              >
                {loading ? t('forgotPassword.sending') : t('forgotPassword.sendCode')}
              </button>

              <p className="text-center mt-4">
                <Link to="/login" className="text-sm text-primary hover:underline">
                  {t('forgotPassword.backToLogin')}
                </Link>
              </p>
            </form>
          )}

          {step === 'code' && (
            <form onSubmit={handleResetPassword}>
              <h2 className="text-xl font-semibold text-text-primary mb-2">{t('forgotPassword.resetHeading')}</h2>
              <p className="text-sm text-text-secondary mb-6">{t('forgotPassword.codePrompt')}</p>

              {error && (
                <div className="mb-4 p-3 bg-danger-light text-danger text-sm rounded-lg">{error}</div>
              )}

              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('forgotPassword.verificationCode')}</label>
                  <input
                    type="text"
                    value={code}
                    onChange={e => setCode(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('forgotPassword.codePlaceholder')}
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('forgotPassword.newPassword')}</label>
                  <input
                    type="password"
                    value={newPassword}
                    onChange={e => setNewPassword(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('forgotPassword.newPasswordPlaceholder')}
                    required
                    minLength={8}
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-text-primary mb-1.5">{t('forgotPassword.confirmPassword')}</label>
                  <input
                    type="password"
                    value={confirmPassword}
                    onChange={e => setConfirmPassword(e.target.value)}
                    className="w-full px-4 py-2.5 border border-border rounded-lg text-sm focus:outline-none focus:border-primary transition-colors"
                    placeholder={t('forgotPassword.confirmPasswordPlaceholder')}
                    required
                    minLength={8}
                  />
                </div>
              </div>

              <p className="text-xs text-text-secondary mt-2">{t('forgotPassword.passwordRequirements')}</p>

              <button
                type="submit"
                disabled={loading}
                className="w-full mt-6 py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors disabled:opacity-50"
              >
                {loading ? t('forgotPassword.resetting') : t('forgotPassword.resetPassword')}
              </button>

              <button
                type="button"
                onClick={() => { setError(''); handleSendCode({ preventDefault: () => {} } as React.FormEvent); }}
                className="w-full mt-2 py-2.5 text-primary text-sm font-medium hover:underline"
              >
                {t('forgotPassword.resendCode')}
              </button>
            </form>
          )}

          {step === 'success' && (
            <div className="text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-green-100 rounded-full mb-4">
                <svg className="w-8 h-8 text-primary" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                  <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                </svg>
              </div>
              <h2 className="text-xl font-semibold text-text-primary mb-2">{t('forgotPassword.successHeading')}</h2>
              <p className="text-sm text-text-secondary mb-6">{t('forgotPassword.successMessage')}</p>
              <Link
                to="/login"
                className="inline-block w-full py-2.5 bg-primary text-white rounded-lg text-sm font-medium hover:bg-primary-dark transition-colors text-center"
              >
                {t('forgotPassword.backToLogin')}
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
