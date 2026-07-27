// WebAuthn (passkey) client helpers — S59 "Web Push + Passkey Auth" session.
//
// The browser's WebAuthn API (`navigator.credentials.create/get`) deals in
// `ArrayBuffer`/`Uint8Array`. The backend (`PasskeyController`) deals in
// base64url strings (challenge, credential ids, attestation/assertion bytes).
// Every binary field must be converted at this boundary — see
// `bufferToBase64url`/`base64urlToBuffer` below.
//
// NOTE: this is base64**url** encoding (RFC 4648 §5, `-`/`_`, no padding),
// NOT the plain base64 used by the Push API's `applicationServerKey` and
// subscription keys (see `services/pushService.ts` for that — a distinct,
// intentionally separate pair of helpers per the two specs' differing
// conventions).

/** Converts an ArrayBuffer/TypedArray to a base64url string (RFC 4648 §5, unpadded). */
export function bufferToBase64url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  const base64 = btoa(binary);
  return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/** Converts a base64url string back to an ArrayBuffer. */
export function base64urlToBuffer(value: string): ArrayBuffer {
  const base64 = value.replace(/-/g, '+').replace(/_/g, '/');
  const padLength = (4 - (base64.length % 4)) % 4;
  const padded = base64 + '='.repeat(padLength);
  const binary = atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

/**
 * Feature-detect WebAuthn support. Passkey UI (both the Login.tsx option and
 * Profile.tsx's "Add a Passkey" button) must be hidden entirely when this is
 * false rather than shown and left to throw on click.
 */
export function isWebAuthnSupported(): boolean {
  return typeof window !== 'undefined' && window.PublicKeyCredential !== undefined;
}

/**
 * Thrown by `registerPasskey`/`loginWithPasskey` when the ceremony fails.
 * `cancelled` is true for `NotAllowedError` (user dismissed the platform
 * prompt or it timed out) so callers can show a friendly "cancelled" message
 * instead of a raw `DOMException` string.
 */
export class WebAuthnCeremonyError extends Error {
  readonly cancelled: boolean;

  constructor(message: string, cancelled: boolean) {
    super(message);
    this.name = 'WebAuthnCeremonyError';
    this.cancelled = cancelled;
  }
}

function toCeremonyError(err: unknown): WebAuthnCeremonyError {
  const name = (err as { name?: string } | undefined)?.name;
  const cancelled = name === 'NotAllowedError';
  const message = cancelled
    ? 'Passkey ceremony was cancelled or timed out.'
    : (err as Error | undefined)?.message || 'Passkey ceremony failed.';
  return new WebAuthnCeremonyError(message, cancelled);
}

// ─── Registration (POST /api/passkeys/register/options → verify) ──────────

export interface PasskeyRegistrationOptionsResponse {
  rp: { id: string; name: string };
  user: { id: string; name: string; displayName: string };
  challenge: string;
  pubKeyCredParams: { type: 'public-key'; alg: number }[];
  timeout: number;
  excludeCredentials: { id: string; type: 'public-key'; transports?: string[] }[];
  authenticatorSelection: { residentKey?: string; userVerification?: string };
  attestation: string;
}

export interface PasskeyRegistrationVerifyRequest {
  deviceName?: string;
  credentialId: string;
  clientDataJSON: string;
  attestationObject: string;
  transports?: string[];
}

/**
 * Runs `navigator.credentials.create(...)` from a decoded registration
 * options response and encodes the result back into the base64url shape the
 * `/api/passkeys/register/verify` endpoint expects.
 */
export async function registerPasskey(
  options: PasskeyRegistrationOptionsResponse,
  deviceName?: string,
): Promise<PasskeyRegistrationVerifyRequest> {
  const publicKey: PublicKeyCredentialCreationOptions = {
    rp: options.rp,
    user: {
      id: base64urlToBuffer(options.user.id),
      name: options.user.name,
      displayName: options.user.displayName,
    },
    challenge: base64urlToBuffer(options.challenge),
    pubKeyCredParams: options.pubKeyCredParams as PublicKeyCredentialParameters[],
    timeout: options.timeout,
    excludeCredentials: options.excludeCredentials.map((c) => ({
      id: base64urlToBuffer(c.id),
      type: 'public-key' as const,
      transports: c.transports as AuthenticatorTransport[] | undefined,
    })),
    authenticatorSelection: options.authenticatorSelection as AuthenticatorSelectionCriteria,
    attestation: options.attestation as AttestationConveyancePreference,
  };

  let credential: PublicKeyCredential | null;
  try {
    credential = (await navigator.credentials.create({ publicKey })) as PublicKeyCredential | null;
  } catch (err) {
    throw toCeremonyError(err);
  }

  if (!credential) {
    throw new WebAuthnCeremonyError('No credential returned from the authenticator.', false);
  }

  const response = credential.response as AuthenticatorAttestationResponse;

  return {
    deviceName: deviceName?.trim() || undefined,
    credentialId: bufferToBase64url(credential.rawId),
    clientDataJSON: bufferToBase64url(response.clientDataJSON),
    attestationObject: bufferToBase64url(response.attestationObject),
    transports: response.getTransports?.(),
  };
}

// ─── Login (POST /api/auth/passkeys/login/options → verify) ───────────────

export interface PasskeyLoginOptionsResponse {
  challenge: string;
  rpId: string;
  timeout: number;
  allowCredentials: { id: string; type: 'public-key'; transports?: string[] }[];
  userVerification: string;
}

/** The credential-ceremony half of the login/verify request — caller adds `username`. */
export interface PasskeyLoginCredential {
  credentialId: string;
  authenticatorData: string;
  clientDataJSON: string;
  signature: string;
  userHandle?: string;
}

/**
 * Runs `navigator.credentials.get(...)` from a decoded login options response
 * and encodes the assertion back into the base64url shape the
 * `/api/auth/passkeys/login/verify` endpoint expects. Caller merges in
 * `username` before POSTing (see `passkeyService.verifyLogin`).
 */
export async function loginWithPasskey(
  options: PasskeyLoginOptionsResponse,
): Promise<PasskeyLoginCredential> {
  const publicKey: PublicKeyCredentialRequestOptions = {
    challenge: base64urlToBuffer(options.challenge),
    rpId: options.rpId,
    timeout: options.timeout,
    allowCredentials: options.allowCredentials.map((c) => ({
      id: base64urlToBuffer(c.id),
      type: 'public-key' as const,
      transports: c.transports as AuthenticatorTransport[] | undefined,
    })),
    userVerification: options.userVerification as UserVerificationRequirement,
  };

  let credential: PublicKeyCredential | null;
  try {
    credential = (await navigator.credentials.get({ publicKey })) as PublicKeyCredential | null;
  } catch (err) {
    throw toCeremonyError(err);
  }

  if (!credential) {
    throw new WebAuthnCeremonyError('No credential returned from the authenticator.', false);
  }

  const response = credential.response as AuthenticatorAssertionResponse;

  return {
    credentialId: bufferToBase64url(credential.rawId),
    authenticatorData: bufferToBase64url(response.authenticatorData),
    clientDataJSON: bufferToBase64url(response.clientDataJSON),
    signature: bufferToBase64url(response.signature),
    userHandle: response.userHandle ? bufferToBase64url(response.userHandle) : undefined,
  };
}
