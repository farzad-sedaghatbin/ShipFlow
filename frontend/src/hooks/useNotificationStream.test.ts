import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { renderHook } from '@testing-library/react';
import { useNotificationStream } from './useNotificationStream';

const TOKEN_KEY = 'shipflow_token';

interface TimerCall {
  cb: () => void;
  delay: number;
}

interface FakeReader {
  read: () => Promise<{ done: boolean; value?: Uint8Array }>;
}

interface FakeResponse {
  ok: boolean;
  body: { getReader: () => FakeReader } | null;
}

let timerCalls: TimerCall[];

/** Flush the microtask queue a number of times so pending awaits inside connect() resolve. */
async function flushMicrotasks(times = 10): Promise<void> {
  for (let i = 0; i < times; i++) {
    await Promise.resolve();
  }
}

/**
 * jsdom's real `localStorage.setItem`/`removeItem` schedule an internal `setTimeout(0)`
 * (for cross-window storage-event dispatch), which would pollute the captured
 * `timerCalls` used to deterministically drive reconnect backoff below. Swap in a
 * trivial in-memory implementation for these tests instead.
 */
function stubFakeLocalStorage(): void {
  let store: Record<string, string> = {};
  vi.stubGlobal('localStorage', {
    getItem: (key: string) => (key in store ? store[key] : null),
    setItem: (key: string, value: string) => {
      store[key] = value;
    },
    removeItem: (key: string) => {
      delete store[key];
    },
    clear: () => {
      store = {};
    },
  } as Storage);
}

function encode(text: string): Uint8Array {
  return new TextEncoder().encode(text);
}

/** Builds a fake reader that yields the given frames in order, then `done: true` forever after. */
function makeReader(frames: Array<{ value?: Uint8Array; done: boolean }>): FakeReader {
  let index = 0;
  return {
    read: () => {
      const frame = frames[index] ?? { done: true, value: undefined };
      index += 1;
      return Promise.resolve(frame);
    },
  };
}

function makeOkResponse(reader: FakeReader): FakeResponse {
  return { ok: true, body: { getReader: () => reader } };
}

function lastFetchAuthHeader(fetchMock: ReturnType<typeof vi.fn>, callIndex: number): string | undefined {
  const call = fetchMock.mock.calls[callIndex] as [string, RequestInit] | undefined;
  const headers = call?.[1]?.headers as Record<string, string> | undefined;
  return headers?.Authorization;
}

describe('useNotificationStream', () => {
  beforeEach(() => {
    timerCalls = [];
    stubFakeLocalStorage();
    vi.stubGlobal(
      'setTimeout',
      ((cb: () => void, delay: number) => {
        const id = timerCalls.length;
        timerCalls.push({ cb, delay });
        return id as unknown as ReturnType<typeof setTimeout>;
      }) as typeof setTimeout
    );
    vi.stubGlobal('clearTimeout', (() => {}) as typeof clearTimeout);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('escalates the reconnect delay with each failure and plateaus at the cap', async () => {
    localStorage.setItem(TOKEN_KEY, 'tok');
    vi.stubGlobal(
      'fetch',
      vi.fn(() => Promise.reject(new Error('network down')))
    );

    renderHook(() => useNotificationStream(vi.fn()));
    await flushMicrotasks();

    expect(timerCalls.length).toBe(1);
    expect(timerCalls[0].delay).toBeGreaterThanOrEqual(500);
    expect(timerCalls[0].delay).toBeLessThanOrEqual(1000);

    // capped = min(30000, 1000 * 2^attempt); delay is jittered to [capped/2, capped]
    const expectedRanges: Array<[number, number]> = [
      [1000, 2000], // attempt 1
      [2000, 4000], // attempt 2
      [4000, 8000], // attempt 3
      [8000, 16000], // attempt 4
      [15000, 30000], // attempt 5 — cap reached (2^5 * 1000 = 32000 -> capped to 30000)
      [15000, 30000], // attempt 6 — plateaus at the cap
    ];

    for (let i = 0; i < expectedRanges.length; i++) {
      timerCalls[i].cb();
      await flushMicrotasks();
      const [lo, hi] = expectedRanges[i];
      const next = timerCalls[i + 1];
      expect(next).toBeDefined();
      expect(next.delay).toBeGreaterThanOrEqual(lo);
      expect(next.delay).toBeLessThanOrEqual(hi);
    }
  });

  it('resets the backoff counter after a successful handshake', async () => {
    localStorage.setItem(TOKEN_KEY, 'tok');
    const fetchMock = vi.fn();
    fetchMock.mockRejectedValueOnce(new Error('fail 1'));
    fetchMock.mockRejectedValueOnce(new Error('fail 2'));
    fetchMock.mockResolvedValueOnce(makeOkResponse(makeReader([{ done: true }])));
    vi.stubGlobal('fetch', fetchMock);

    renderHook(() => useNotificationStream(vi.fn()));
    await flushMicrotasks();

    // First failure — base delay range.
    expect(timerCalls[0].delay).toBeGreaterThanOrEqual(500);
    expect(timerCalls[0].delay).toBeLessThanOrEqual(1000);

    timerCalls[0].cb();
    await flushMicrotasks();

    // Second failure — escalated delay range.
    expect(timerCalls[1].delay).toBeGreaterThanOrEqual(1000);
    expect(timerCalls[1].delay).toBeLessThanOrEqual(2000);

    // Third attempt succeeds (handshake ok), then the server immediately closes the
    // stream (done: true) — this still schedules a reconnect, but the counter must
    // have been reset by the successful handshake before that reconnect is scheduled.
    timerCalls[1].cb();
    await flushMicrotasks();

    expect(timerCalls.length).toBe(3);
    expect(timerCalls[2].delay).toBeGreaterThanOrEqual(500);
    expect(timerCalls[2].delay).toBeLessThanOrEqual(1000);
  });

  it('re-reads the auth token from localStorage on every connection attempt', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-a');
    const fetchMock = vi.fn(() => Promise.reject(new Error('down')));
    vi.stubGlobal('fetch', fetchMock);

    renderHook(() => useNotificationStream(vi.fn()));
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(lastFetchAuthHeader(fetchMock, 0)).toBe('Bearer token-a');

    localStorage.setItem(TOKEN_KEY, 'token-b');
    timerCalls[0].cb();
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(lastFetchAuthHeader(fetchMock, 1)).toBe('Bearer token-b');
  });

  it('stops retrying once the token is removed (logout)', async () => {
    localStorage.setItem(TOKEN_KEY, 'tok');
    const fetchMock = vi.fn(() => Promise.reject(new Error('down')));
    vi.stubGlobal('fetch', fetchMock);

    renderHook(() => useNotificationStream(vi.fn()));
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(1);

    localStorage.removeItem(TOKEN_KEY);
    timerCalls[0].cb();
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('aborts the in-flight request on unmount and blocks any further reconnect', async () => {
    localStorage.setItem(TOKEN_KEY, 'tok');
    const fetchMock = vi.fn(() => Promise.reject(new Error('down')));
    vi.stubGlobal('fetch', fetchMock);
    const abortSpy = vi.spyOn(AbortController.prototype, 'abort');

    const { unmount } = renderHook(() => useNotificationStream(vi.fn()));
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const pendingReconnect = timerCalls[0];

    unmount();
    expect(abortSpy).toHaveBeenCalled();

    pendingReconnect.cb();
    await flushMicrotasks();

    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('reassembles an SSE frame split across two chunks and fires the callback exactly once', async () => {
    localStorage.setItem(TOKEN_KEY, 'tok');
    const payload = { id: 1, message: 'hello' };
    const full = `event: notification\ndata: ${JSON.stringify(payload)}\n\n`;
    const splitAt = full.indexOf('"id"'); // split mid `data:` line
    const chunk1 = full.slice(0, splitAt);
    const chunk2 = full.slice(splitAt);

    const reader = makeReader([
      { done: false, value: encode(chunk1) },
      { done: false, value: encode(chunk2) },
      { done: true },
    ]);
    const fetchMock = vi.fn(() => Promise.resolve(makeOkResponse(reader)));
    vi.stubGlobal('fetch', fetchMock);

    const onNewNotification = vi.fn();
    renderHook(() => useNotificationStream(onNewNotification));
    await flushMicrotasks();

    expect(onNewNotification).toHaveBeenCalledTimes(1);
    expect(onNewNotification).toHaveBeenCalledWith('notification', payload);
  });
});
