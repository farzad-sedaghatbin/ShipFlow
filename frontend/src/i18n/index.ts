import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';
import HttpBackend from 'i18next-http-backend';

// Import translation files
import enTranslations from './locales/en.json';
import faTranslations from './locales/fa.json';

export const SUPPORTED_LANGUAGES = {
  en: { name: 'English', nativeName: 'English', dir: 'ltr' as const, calendar: 'gregorian' as const },
  fa: { name: 'Persian', nativeName: 'فارسی', dir: 'rtl' as const, calendar: 'jalali' as const },
} as const;

export type SupportedLanguage = keyof typeof SUPPORTED_LANGUAGES;

/**
 * Check if a language is RTL
 */
export function isRTLLanguage(lang: string): boolean {
  return SUPPORTED_LANGUAGES[lang as SupportedLanguage]?.dir === 'rtl';
}

/**
 * Check if a language uses Jalali calendar
 */
export function usesJalaliCalendar(lang: string): boolean {
  return SUPPORTED_LANGUAGES[lang as SupportedLanguage]?.calendar === 'jalali';
}

i18n
  .use(HttpBackend)
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: { translation: enTranslations },
      fa: { translation: faTranslations },
    },
    fallbackLng: 'en',
    debug: process.env.NODE_ENV === 'development',
    
    interpolation: {
      escapeValue: false, // React already escapes values
    },
    
    detection: {
      order: ['localStorage', 'navigator', 'htmlTag'],
      lookupLocalStorage: 'shipflow-language',
      caches: ['localStorage'],
    },
  });

// Handle RTL languages and document attributes
i18n.on('languageChanged', (lng) => {
  const langConfig = SUPPORTED_LANGUAGES[lng as SupportedLanguage];
  const dir = langConfig?.dir || 'ltr';
  
  // Set document direction and language
  document.documentElement.dir = dir;
  document.documentElement.lang = lng;
  
  // Add body class for additional RTL styling hooks
  if (dir === 'rtl') {
    document.body.classList.add('rtl');
    document.body.classList.remove('ltr');
  } else {
    document.body.classList.add('ltr');
    document.body.classList.remove('rtl');
  }
  
  // Log for development
  if (process.env.NODE_ENV === 'development') {
    console.log(`Language changed to: ${lng}, Direction: ${dir}, Calendar: ${langConfig?.calendar || 'gregorian'}`);
  }
});

export default i18n;
