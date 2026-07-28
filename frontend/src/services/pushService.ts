import api from './api';

export interface VapidPublicKeyResponse {
  publicKey: string;
  /**
   * False when the server has no VAPID keys configured. The "enable push"
   * UI must be hidden entirely in that case, not shown as a broken toggle.
   */
  enabled: boolean;
}

export interface PushSubscribeRequest {
  endpoint: string;
  p256dhKey: string;
  authKey: string;
  userAgent?: string;
}

export interface PushSubscriptionDTO {
  id: number;
  endpoint: string;
  createdAt: string;
}

/** Thrown by `enablePushNotifications` when the browser lacks Service Worker/Push API support (e.g. Safari desktop). */
export class PushNotSupportedError extends Error {
  constructor(message = 'Push notifications are not supported in this browser.') {
    super(message);
    this.name = 'PushNotSupportedError';
  }
}

/** Thrown when the server has no VAPID keys configured — `getVapidPublicKey().enabled === false`. */
export class PushNotConfiguredError extends Error {
  constructor(message = 'Push notifications are not configured on this server.') {
    super(message);
    this.name = 'PushNotConfiguredError';
  }
}

/** Thrown when the user denies (or has previously denied) the browser notification permission prompt. */
export class PushPermissionDeniedError extends Error {
  constructor(message = 'Notification permission was denied.') {
    super(message);
    this.name = 'PushPermissionDeniedError';
  }
}

function isPushSupported(): boolean {
  return typeof navigator !== 'undefined' && 'serviceWorker' in navigator && typeof window !== 'undefined' && 'PushManager' in window;
}

// ─── base64url helper (VAPID applicationServerKey) ─────────────────────────
// A VAPID public key (as produced by `web-push generate-vapid-keys` and by
// the backend's `nl.martijndwars:web-push` library) is base64url-encoded —
// confirmed by decompiling that library's `Utils.loadPublicKey`, which
// decodes via `Base64.getUrlDecoder()`, not the standard decoder. A plain
// `atob()` call throws on the `-`/`_` characters a real VAPID key contains,
// so this must be url-safe-aware, unlike a naive "Push API uses plain
// base64" assumption would suggest. This is the same encoding WebAuthn uses
// (see lib/webauthn.ts's `base64urlToBuffer`) — kept as a separate local
// copy rather than a shared import so this module has no dependency on the
// WebAuthn module, but the algorithm must match.
function base64urlToUint8Array(base64url: string): Uint8Array {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const padLength = (4 - (base64.length % 4)) % 4;
  const padded = base64 + '='.repeat(padLength);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

export const pushService = {
  getVapidPublicKey: async (): Promise<VapidPublicKeyResponse> => {
    const response = await api.get<VapidPublicKeyResponse>('/push/vapid-public-key');
    return response.data;
  },

  subscribe: async (request: PushSubscribeRequest): Promise<PushSubscriptionDTO> => {
    const response = await api.post<PushSubscriptionDTO>('/push/subscribe', request);
    return response.data;
  },

  unsubscribe: async (endpoint: string): Promise<void> => {
    await api.delete('/push/unsubscribe', { data: { endpoint } });
  },
};

/**
 * Full "enable" orchestration: feature-detect → fetch the VAPID key (and bail
 * out with `PushNotConfiguredError` *before* prompting for permission or
 * touching the service worker if the server has none configured) → request
 * notification permission → get the active SW registration → subscribe →
 * persist the subscription server-side.
 */
export async function enablePushNotifications(): Promise<void> {
  if (!isPushSupported()) {
    throw new PushNotSupportedError();
  }

  const { publicKey, enabled } = await pushService.getVapidPublicKey();
  if (!enabled) {
    throw new PushNotConfiguredError();
  }

  const permission = await Notification.requestPermission();
  if (permission !== 'granted') {
    throw new PushPermissionDeniedError();
  }

  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    // Uint8Array is a valid BufferSource at runtime; the cast works around a
    // TS lib.dom generic-parameter mismatch (Uint8Array<ArrayBufferLike> vs.
    // the ArrayBufferView<ArrayBuffer> that PushSubscriptionOptionsInit expects).
    applicationServerKey: base64urlToUint8Array(publicKey) as BufferSource,
  });

  // `PushSubscription.toJSON()` is a browser-native method (part of the Push
  // API spec) that already base64url-encodes `keys.p256dh`/`keys.auth` for
  // us — using it instead of manually building base64 via `getKey()` avoids
  // re-implementing that encoding a third time and guarantees it matches
  // what the backend's web-push library expects (see the base64url comment
  // above). `endpoint`/`keys` are typed as optional in lib.dom's
  // `PushSubscriptionJSON` even though the spec guarantees them here.
  const json = subscription.toJSON();
  const p256dhKey = json.keys?.p256dh;
  const authKey = json.keys?.auth;
  if (!p256dhKey || !authKey) {
    throw new Error('Push subscription is missing its encryption keys.');
  }

  await pushService.subscribe({
    endpoint: subscription.endpoint,
    p256dhKey,
    authKey,
    userAgent: navigator.userAgent,
  });
}

/**
 * Full "disable" orchestration: unsubscribe browser-side first (so the
 * browser stops delivering pushes even if the server call below fails), then
 * tell the server to drop the subscription record.
 */
export async function disablePushNotifications(): Promise<void> {
  if (!isPushSupported()) {
    return;
  }

  const registration = await navigator.serviceWorker.ready;
  const subscription = await registration.pushManager.getSubscription();
  if (!subscription) {
    return;
  }

  const endpoint = subscription.endpoint;
  await subscription.unsubscribe();
  await pushService.unsubscribe(endpoint);
}
