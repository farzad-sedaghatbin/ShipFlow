import api from './api';

// Types for User Preferences
export type ThemeMode = 'LIGHT' | 'DARK' | 'SYSTEM';

export interface UserPreferenceDTO {
  id: number;
  userId: number;
  username: string;
  themeMode: ThemeMode;
  primaryColor?: string;
  secondaryColor?: string;
  compactView: boolean;
  enableAnimations: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UpdatePreferenceRequest {
  themeMode?: ThemeMode;
  primaryColor?: string;
  secondaryColor?: string;
  compactView?: boolean;
  enableAnimations?: boolean;
}

// User Preference Service
export const preferenceService = {
  /**
   * Get current user's preferences
   */
  getPreferences: async (): Promise<UserPreferenceDTO> => {
    const response = await api.get<UserPreferenceDTO>('/preferences');
    return response.data;
  },

  /**
   * Update user preferences
   */
  updatePreferences: async (request: UpdatePreferenceRequest): Promise<UserPreferenceDTO> => {
    const response = await api.put<UserPreferenceDTO>('/preferences', request);
    return response.data;
  },

  /**
   * Update just the theme mode
   */
  updateTheme: async (themeMode: ThemeMode): Promise<UserPreferenceDTO> => {
    const response = await api.put<UserPreferenceDTO>('/preferences/theme', { themeMode });
    return response.data;
  },

  /**
   * Reset preferences to defaults
   */
  resetPreferences: async (): Promise<UserPreferenceDTO> => {
    const response = await api.post<UserPreferenceDTO>('/preferences/reset');
    return response.data;
  },
};
