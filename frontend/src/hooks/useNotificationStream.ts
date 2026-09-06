import { useEffect, useRef } from 'react';

const TOKEN_KEY = 'shipflow_token';

/** Base delay before the first reconnect attempt after a stream failure (ms). */
const BASE_RECONNECT_DELAY_MS = 1_000;
/** Upper bound on the (pre-jitter) reconnect delay (ms). */
const MAX_RECONNECT_DELAY_MS = 30_000;

/**
 * Hook that opens a Server-Sent Events connection to the notification stream
 * ({@code GET /api/notifications/stream}) and calls {@code onNewNotification}
 * whenever a {@code notification} event arrives.
 *
 * The connection is torn down when the component unmounts or when the user logs
 * out (token disappears). If the stream fails, reconnect attempts are scheduled
 * with exponential backoff (base {@link BASE_RECONNECT_DELAY_MS}, doubling per
 * attempt, capped at {@link MAX_RECONNECT_DELAY_MS}) plus equal-jitter, and retry
 * indefinitely — this avoids a thundering herd against a restarting backend while
 * still eventually recovering, without ever giving up. A successful handshake
 * resets the backoff counter back to the base delay.
 *
 * @param onNewNotification called with the raw parsed payload (and the SSE
 *   event name, when supplied by the server) each time a non-{@code connected}
 *   event arrives
 */
export function useNotificationStream(onNewNotification: (eventName: string, payload: unknown) => void): void {
  // Use a ref so the callback can be updated without restarting the stream
  const callbackRef = useRef(onNewNotification);
  callbackRef.current = onNewNotification;

  useEffect(() => {
    let aborted = false;
    const controller = new AbortController();
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    let reconnectAttempt = 0;

    // Fix 6: guard against stacking multiple timers
    // Exponential backoff with equal-jitter, capped at MAX_RECONNECT_DELAY_MS, retries forever.
    function scheduleReconnect() {
      if (aborted) return;
      if (reconnectTimer !== null) return; // already scheduled
      const capped = Math.min(MAX_RECONNECT_DELAY_MS, BASE_RECONNECT_DELAY_MS * 2 ** reconnectAttempt);
      const delay = capped / 2 + Math.random() * (capped / 2); // equal-jitter: avoids thundering herd while staying bounded
      reconnectAttempt += 1;
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        if (!aborted) connect();
      }, delay);
    }

    async function connect() {
      if (aborted) return;
      // Fix 6: clear any pending timer when a new connection attempt starts
      if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      // Read the token fresh on every (re)connection attempt so a token that
      // changes (rotation/re-login) or disappears (logout) mid-session is
      // picked up without needing a full component unmount/remount.
      const token = localStorage.getItem(TOKEN_KEY);
      if (!token) return; // logged out (or never logged in) — stop here, don't schedule a reconnect
      try {
        const response = await fetch('/api/notifications/stream', {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'text/event-stream',
          },
          signal: controller.signal,
        });

        if (!response.ok || !response.body) {
          // Non-2xx or no body — schedule a reconnect (with backoff) and bail
          scheduleReconnect();
          return;
        }

        // A successful handshake shows the backend is healthy again — reset backoff.
        reconnectAttempt = 0;

        const reader = response.body.getReader();
        const decoder = new TextDecoder();

        // Fix 5: maintain a buffer across chunks so frames split across chunks are handled correctly
        let buffer = '';

        function processBuffer() {
          const frames = buffer.split('\n\n');
          buffer = frames.pop() ?? ''; // last incomplete frame stays in buffer
          for (const frame of frames) {
            if (!frame.trim()) continue;
            let eventName = '';
            const dataLines: string[] = [];
            for (const line of frame.split('\n')) {
              if (line.startsWith('event:')) eventName = line.slice(6).trim();
              else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim());
            }
            const data = dataLines.join('\n');
            if (data) {
              try {
                const payload = JSON.parse(data);
                if (eventName !== 'connected') {
                  callbackRef.current(eventName || 'notification', payload);
                }
              } catch {
                // Ignore malformed JSON frames
              }
            }
          }
        }

        // Fix 3 & 4: iterative loop instead of recursion — avoids stack overflow on long-lived
        // connections; also reconnects when the server closes the stream (done === true).
        await (async function readStream() {
          while (true) {
            if (aborted) break;
            const { done, value } = await reader.read();
            if (done) {
              // Server closed the stream (deploy, timeout, etc.) — reconnect unless aborted
              if (!aborted) scheduleReconnect();
              break;
            }
            buffer += decoder.decode(value, { stream: true });
            processBuffer();
          }
        })();
      } catch (err) {
        if (aborted) return; // Normal cleanup on unmount
        // Schedule a reconnect for transient network errors
        scheduleReconnect();
      }
    }

    connect();

    return () => {
      aborted = true;
      controller.abort();
      if (reconnectTimer !== null) clearTimeout(reconnectTimer);
    };
  }, []); // Only run once on mount — token is read fresh inside connect() on every (re)attempt
}
