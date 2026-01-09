import api from './api';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  userId: number;
  username: string;
  role: string;
  personId?: number;
  personName?: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  role: string;
  personId?: number;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email?: string;
  role: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
  personId?: number;
  personName?: string;
  avatarUrl?: string;
  department?: string;
  bio?: string;
  skills?: string;
}

export const authService = {
  login: (data: LoginRequest) => 
    api.post<LoginResponse>('/auth/login', data),

  register: (data: RegisterRequest) => 
    api.post('/auth/register', data),

  getCurrentUser: () => 
    api.get('/auth/me'),

  resetPassword: (data: ResetPasswordRequest) => 
    api.post('/auth/reset-password', data),

  validateResetToken: (token: string) => 
    api.post<{ valid: boolean }>('/auth/validate-reset-token', null, { params: { token } }),

  changePassword: (userId: number, data: ChangePasswordRequest) => 
    api.put(`/users/${userId}/password`, data),

  getProfile: (userId: number) => 
    api.get<UserProfile>(`/users/${userId}/profile`),

  getMyProfile: () => 
    api.get<UserProfile>('/users/me/profile'),

  updateProfile: (userId: number, data: Partial<UserProfile>) => 
    api.put<UserProfile>(`/users/${userId}/profile`, data),

  updateMyProfile: (data: Partial<UserProfile>) => 
    api.put<UserProfile>('/users/me/profile', data),
};
