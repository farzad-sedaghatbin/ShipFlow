import React, { createContext, useContext, useState, useEffect, useMemo, ReactNode } from 'react';
import api from '../services/api';

export type ThemeMode = 'light' | 'dark' | 'system';

interface UserPreferences {
  themeMode: ThemeMode;
  primaryColor?: string;
  secondaryColor?: string;
  compactView: boolean;
  enableAnimations: boolean;
}

interface ThemeContextType {
  mode: ThemeMode;
  actualMode: 'light' | 'dark';
  setMode: (mode: ThemeMode) => void;
  toggleTheme: () => void;
  preferences: UserPreferences;
  updatePreferences: (prefs: Partial<UserPreferences>) => void;
  isLoading: boolean;
}

const defaultPreferences: UserPreferences = {
  themeMode: 'system',
  compactView: false,
  enableAnimations: true,
};

export const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export const useTheme = (): ThemeContextType => {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within a ThemeProvider');
  }
  return context;
};

interface ThemeProviderProps {
  children: ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({ children }) => {
  const [mode, setModeState] = useState<ThemeMode>(() => {
    const saved = localStorage.getItem('themeMode');
    return (saved as ThemeMode) || 'system';
  });
  const [preferences, setPreferences] = useState<UserPreferences>(defaultPreferences);
  const [isLoading, setIsLoading] = useState(true);

  // Detect system preference
  const systemPrefersDark = useMemo(() => {
    if (typeof window !== 'undefined') {
      return window.matchMedia('(prefers-color-scheme: dark)').matches;
    }
    return false;
  }, []);

  // Calculate actual mode (resolve 'system' to 'light' or 'dark')
  const actualMode = useMemo((): 'light' | 'dark' => {
    if (mode === 'system') {
      return systemPrefersDark ? 'dark' : 'light';
    }
    return mode;
  }, [mode, systemPrefersDark]);

  // Apply theme class to document root
  useEffect(() => {
    const root = window.document.documentElement;
    root.classList.remove('light', 'dark');
    root.classList.add(actualMode);
  }, [actualMode]);

  // Listen for system theme changes
  useEffect(() => {
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    const handler = () => {
      if (mode === 'system') {
        // Force re-render when system theme changes
        setModeState('system');
      }
    };
    mediaQuery.addEventListener('change', handler);
    return () => mediaQuery.removeEventListener('change', handler);
  }, [mode]);

  // Load preferences from backend
  useEffect(() => {
    const loadPreferences = async () => {
      try {
        const token = localStorage.getItem('token');
        if (token) {
          const response = await api.get<UserPreferences>('/preferences');
          const serverPrefs = response.data;
          if (serverPrefs) {
            setPreferences({
              themeMode: serverPrefs.themeMode?.toLowerCase() as ThemeMode || 'system',
              primaryColor: serverPrefs.primaryColor,
              secondaryColor: serverPrefs.secondaryColor,
              compactView: serverPrefs.compactView ?? false,
              enableAnimations: serverPrefs.enableAnimations ?? true,
            });
            const serverMode = serverPrefs.themeMode?.toLowerCase() as ThemeMode;
            if (serverMode) {
              setModeState(serverMode);
              localStorage.setItem('themeMode', serverMode);
            }
          }
        }
      } catch {
        // Use localStorage fallback on API error
      } finally {
        setIsLoading(false);
      }
    };
    loadPreferences();
  }, []);

  const setMode = async (newMode: ThemeMode) => {
    setModeState(newMode);
    localStorage.setItem('themeMode', newMode);
    
    // Sync to backend
    try {
      const token = localStorage.getItem('token');
      if (token) {
        await api.put('/preferences/theme', { themeMode: newMode.toUpperCase() });
      }
    } catch (error) {
      console.error('Failed to sync theme preference:', error);
    }
  };

  const toggleTheme = () => {
    const nextMode = actualMode === 'light' ? 'dark' : 'light';
    setMode(nextMode);
  };

  const updatePreferences = async (newPrefs: Partial<UserPreferences>) => {
    const updated = { ...preferences, ...newPrefs };
    setPreferences(updated);
    
    if (newPrefs.themeMode) {
      setModeState(newPrefs.themeMode);
      localStorage.setItem('themeMode', newPrefs.themeMode);
    }

    // Sync to backend
    try {
      const token = localStorage.getItem('token');
      if (token) {
        await api.put('/preferences', {
          themeMode: updated.themeMode?.toUpperCase(),
          primaryColor: updated.primaryColor,
          secondaryColor: updated.secondaryColor,
          compactView: updated.compactView,
          enableAnimations: updated.enableAnimations,
        });
      }
    } catch (error) {
      console.error('Failed to sync preferences:', error);
    }
  };

  return (
    <ThemeContext.Provider
      value={{
        mode,
        actualMode,
        setMode,
        toggleTheme,
        preferences,
        updatePreferences,
        isLoading,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
};
