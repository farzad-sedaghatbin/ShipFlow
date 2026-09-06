import api from './api';

export interface PublicAppConfig {
  demoModeEnabled: boolean;
}

/**
 * Anonymous, pre-login config flags (e.g. whether to show the demo-login hint). Safe to call
 * before authentication — mounted under the same permitAll `/api/public/**` prefix as `/sso/providers`.
 */
export const getPublicConfig = (): Promise<PublicAppConfig> =>
  api.get<PublicAppConfig>('/public/config').then((r) => r.data);
