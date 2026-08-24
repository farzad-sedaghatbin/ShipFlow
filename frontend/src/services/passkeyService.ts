import api from './api';
import type { LoginResponse } from './authService';
import type {
  PasskeyLoginOptionsResponse,
  PasskeyRegistrationOptionsResponse,
  PasskeyRegistrationVerifyRequest,
  PasskeyLoginCredential,
} from '../lib/webauthn';

// Re-exported so callers only need to import types from one place.
export type {
  PasskeyLoginOptionsResponse,
  PasskeyRegistrationOptionsResponse,
  PasskeyRegistrationVerifyRequest,
  PasskeyLoginCredential,
};

/**
 * The full login/verify request body — `PasskeyLoginCredential` plus the username. `username` is
 * omitted for a discoverable-credential (conditional UI/autofill) login: the backend resolves the
 * account from `userHandle` instead — see `getDiscoverableLoginOptions` below.
 */
export interface PasskeyLoginVerifyRequest extends PasskeyLoginCredential {
  username?: string;
}

/** A registered passkey's public view — never includes the credential id or public key bytes. */
export interface PasskeyDTO {
  id: number;
  deviceName: string;
  createdAt: string;
  lastUsedAt?: string;
  transports: string[];
}

export const passkeyService = {
  /** No auth required — first step of passwordless login. */
  getLoginOptions: async (username: string): Promise<PasskeyLoginOptionsResponse> => {
    const response = await api.post<PasskeyLoginOptionsResponse>('/auth/passkeys/login/options', { username });
    return response.data;
  },

  /**
   * No auth required — usernameless counterpart for conditional UI/autofill: no username is known
   * yet, so `allowCredentials` comes back empty and the browser resolves candidates from its own
   * discoverable-credential store.
   */
  getDiscoverableLoginOptions: async (): Promise<PasskeyLoginOptionsResponse> => {
    const response = await api.post<PasskeyLoginOptionsResponse>('/auth/passkeys/login/options/discoverable');
    return response.data;
  },

  /** No auth required — verifies the assertion and returns the same shape as password login. */
  verifyLogin: async (request: PasskeyLoginVerifyRequest): Promise<LoginResponse> => {
    const response = await api.post<LoginResponse>('/auth/passkeys/login/verify', request);
    return response.data;
  },

  /** Authenticated — begins registering a new passkey for the current user. */
  getRegistrationOptions: async (): Promise<PasskeyRegistrationOptionsResponse> => {
    const response = await api.post<PasskeyRegistrationOptionsResponse>('/passkeys/register/options');
    return response.data;
  },

  /** Authenticated — completes passkey registration. */
  verifyRegistration: async (request: PasskeyRegistrationVerifyRequest): Promise<PasskeyDTO> => {
    const response = await api.post<PasskeyDTO>('/passkeys/register/verify', request);
    return response.data;
  },

  /** Authenticated — lists the current user's registered passkeys. */
  listPasskeys: async (): Promise<PasskeyDTO[]> => {
    const response = await api.get<PasskeyDTO[]>('/passkeys');
    return response.data;
  },

  /** Authenticated — revokes a registered passkey. */
  deletePasskey: async (id: number): Promise<void> => {
    await api.delete(`/passkeys/${id}`);
  },
};
