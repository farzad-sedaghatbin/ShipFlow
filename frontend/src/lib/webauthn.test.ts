import { describe, it, expect } from 'vitest';
import { bufferToBase64url, base64urlToBuffer, isWebAuthnSupported } from './webauthn';

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
