import { useTranslation } from 'react-i18next';
import { Globe } from 'lucide-react';
import { useState, useRef, useEffect } from 'react';

const languages = [
  { code: 'en', label: 'English' },
  { code: 'es', label: 'Espanol' },
  { code: 'cak', label: 'Kaqchikel' },
];

export default function LanguageSwitcher() {
  const { i18n } = useTranslation();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const changeLanguage = (code: string) => {
    i18n.changeLanguage(code);
    localStorage.setItem('lang', code);
    setOpen(false);
  };

  const current = languages.find(l => l.code === i18n.language) || languages[0];

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setOpen(!open)}
        className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-border text-sm text-text-secondary hover:border-primary transition-colors"
      >
        <Globe size={16} />
        <span className="hidden sm:inline">{current.label}</span>
      </button>
      {open && (
        <div className="absolute right-0 top-full mt-1 bg-white border border-border rounded-lg shadow-lg overflow-hidden z-50 min-w-[140px]">
          {languages.map(lang => (
            <button
              key={lang.code}
              onClick={() => changeLanguage(lang.code)}
              className={`w-full text-left px-4 py-2.5 text-sm hover:bg-gray-50 transition-colors ${
                i18n.language === lang.code ? 'bg-primary-light text-primary font-medium' : 'text-text-primary'
              }`}
            >
              {lang.label}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
