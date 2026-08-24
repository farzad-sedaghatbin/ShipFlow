import { describe, it, expect, vi, afterEach } from 'vitest';
import {
  bufferToBase64url,
  base64urlToBuffer,
  isWebAuthnSupported,
  isConditionalMediationAvailable,
  loginWithPasskey,
  WebAuthnCeremonyError,
  type PasskeyLoginOptionsResponse,
} from './webauthn';

function bytesOf(...values: number[]): ArrayBuffer {
  return new Uint8Array(values).buffer;
}

function toArray(buffer: ArrayBuffer): number[] {
  return Array.from(new Uint8Array(buffer));
}

describe('webauthn base64url helpers', () => {
  it('round-trips arbitrary bytes through bufferToBase64url -> base64urlToBuffer', () => {
    const original = bytesOf(0, 1, 2, 3, 255, 254, 128, 127, 16, 32);
    const encoded = bufferToBase64url(original);
    const decoded = base64urlToBuffer(encoded);
    expect(toArray(decoded)).toEqual(toArray(original));
  });

  it('produces a URL-safe string — no "+", "/", or "=" padding', () => {
    // Byte sequences deliberately chosen so plain base64 would contain both
    // "+" and "/" (and trigger "=" padding) — proves the url-safe substitution
    // (and padding strip) actually ran, not just a pass-through of btoa().
    const bytesTriggeringPlusAndSlash = bytesOf(0xfb, 0xff, 0xbf, 0xff);
    const plainBase64 = btoa(String.fromCharCode(...new Uint8Array(bytesTriggeringPlusAndSlash)));
    expect(plainBase64).toMatch(/[+/]/); // sanity check on the test fixture itself

    const encoded = bufferToBase64url(bytesTriggeringPlusAndSlash);
    expect(encoded).not.toContain('+');
    expect(encoded).not.toContain('/');
    expect(encoded).not.toContain('=');

    const decoded = base64urlToBuffer(encoded);
    expect(toArray(decoded)).toEqual(toArray(bytesTriggeringPlusAndSlash));
  });

  it('round-trips an empty buffer', () => {
    const original = bytesOf();
    const encoded = bufferToBase64url(original);
    const decoded = base64urlToBuffer(encoded);
    expect(toArray(decoded)).toEqual([]);
  });

  it('round-trips a buffer whose length requires padding when re-encoded as base64', () => {
    // 5 bytes -> base64 needs padding ("=") internally; base64url must still
    // decode correctly without the padding characters being present.
    const original = bytesOf(10, 20, 30, 40, 50);
    const encoded = bufferToBase64url(original);
    expect(encoded).not.toContain('=');
    const decoded = base64urlToBuffer(encoded);
    expect(toArray(decoded)).toEqual(toArray(original));
  });

  it('decodes a known base64url string to the expected bytes', () => {
    // "f-_A" in base64url is the url-safe form of the plain-base64 string
    // "f+/A", which decodes to [0x7f, 0xef, 0xc0] — a fixed-vector check
    // independent of our own encoder (verified against Node's Buffer).
    const decoded = base64urlToBuffer('f-_A');
    expect(toArray(decoded)).toEqual(toArray(bytesOf(0x7f, 0xef, 0xc0)));
  });
});

describe('isWebAuthnSupported', () => {
  it('returns true when window.PublicKeyCredential is defined (jsdom test env may or may not define it)', () => {
    const original = (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential;
    try {
      (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = class {};
      expect(isWebAuthnSupported()).toBe(true);
    } finally {
      (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = original;
    }
  });

  it('returns false when window.PublicKeyCredential is undefined', () => {
    const original = (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential;
    try {
      (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = undefined;
      expect(isWebAuthnSupported()).toBe(false);
    } finally {
      (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = original;
    }
  });
});

describe('isConditionalMediationAvailable', () => {
  const original = (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential;

  afterEach(() => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = original;
  });

  it('returns true when the browser reports conditional mediation support', async () => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = {
      isConditionalMediationAvailable: vi.fn().mockResolvedValue(true),
    };
    await expect(isConditionalMediationAvailable()).resolves.toBe(true);
  });

  it('returns false when the browser reports no conditional mediation support', async () => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = {
      isConditionalMediationAvailable: vi.fn().mockResolvedValue(false),
    };
    await expect(isConditionalMediationAvailable()).resolves.toBe(false);
  });

  it('returns false when the browser has no isConditionalMediationAvailable at all (e.g. Firefox)', async () => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = class {};
    await expect(isConditionalMediationAvailable()).resolves.toBe(false);
  });

  it('returns false when window.PublicKeyCredential itself is undefined', async () => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = undefined;
    await expect(isConditionalMediationAvailable()).resolves.toBe(false);
  });

  it('returns false rather than throwing when the feature-detect call itself rejects', async () => {
    (window as unknown as { PublicKeyCredential?: unknown }).PublicKeyCredential = {
      isConditionalMediationAvailable: vi.fn().mockRejectedValue(new Error('boom')),
    };
    await expect(isConditionalMediationAvailable()).resolves.toBe(false);
  });
});

describe('loginWithPasskey — ceremony error classification', () => {
  const options: PasskeyLoginOptionsResponse = {
    challenge: 'Y2hhbA', // "chal"
    rpId: 'localhost',
    timeout: 60000,
    allowCredentials: [],
    userVerification: 'preferred',
  };

  // jsdom doesn't implement the Credential Management API, so `navigator.credentials` isn't a
  // real object to `vi.spyOn` — stub the whole property instead.
  function mockCredentialsGet(rejection: unknown) {
    Object.defineProperty(navigator, 'credentials', {
      value: { get: vi.fn().mockRejectedValue(rejection) },
      configurable: true,
    });
  }

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('surfaces a cancelled WebAuthnCeremonyError when the platform prompt is dismissed (NotAllowedError)', async () => {
    mockCredentialsGet(Object.assign(new Error('dismissed'), { name: 'NotAllowedError' }));

    await expect(loginWithPasskey(options)).rejects.toMatchObject({
      name: 'WebAuthnCeremonyError',
      cancelled: true,
    });
  });

  it('surfaces a cancelled WebAuthnCeremonyError when a conditional request is aborted (AbortError)', async () => {
    mockCredentialsGet(Object.assign(new Error('aborted'), { name: 'AbortError' }));

    await expect(
      loginWithPasskey(options, { mediation: 'conditional', signal: new AbortController().signal }),
    ).rejects.toMatchObject({ name: 'WebAuthnCeremonyError', cancelled: true });
  });

  it('surfaces a non-cancelled WebAuthnCeremonyError for any other ceremony failure', async () => {
    mockCredentialsGet(Object.assign(new Error('security key unplugged'), { name: 'NotSupportedError' }));

    await expect(loginWithPasskey(options)).rejects.toBeInstanceOf(WebAuthnCeremonyError);
    await expect(loginWithPasskey(options)).rejects.toMatchObject({ cancelled: false });
  });
});
