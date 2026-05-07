import { useEffect, useRef } from 'react';

const TOKEN_KEY = 'shipflow_token';

/** How long to wait before attempting a reconnect after a stream failure (ms). */
const RECONNECT_DELAY_MS = 5_000;

/**
 * Hook that opens a Server-Sent Events connection to the notification stream
 * ({@code GET /api/notifications/stream}) and calls {@code onNewNotification}
 * whenever a {@code notification} event arrives.
 *
 * The connection is torn down when the component unmounts or when the user logs
 * out (token disappears). If the stream fails, a single reconnect attempt is
 * scheduled after {@link RECONNECT_DELAY_MS} milliseconds — this provides
 * graceful degradation while avoiding infinite tight loops.
 *
 * @param onNewNotification called with the raw parsed payload each time a
 *   {@code notification} SSE event is received
 */
export function useNotificationStream(onNewNotification: (payload: unknown) => void): void {
  // Use a ref so the callback can be updated without restarting the stream
  const callbackRef = useRef(onNewNotification);
  callbackRef.current = onNewNotification;

  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return;

    let aborted = false;
    const controller = new AbortController();
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;

    // Fix 6: guard against stacking multiple timers
    function scheduleReconnect() {
      if (aborted) return;
      if (reconnectTimer !== null) return; // already scheduled
      reconnectTimer = setTimeout(() => {
        reconnectTimer = null;
        if (!aborted) connect();
      }, RECONNECT_DELAY_MS);
    }

    async function connect() {
      if (aborted) return;
      // Fix 6: clear any pending timer when a new connection attempt starts
      if (reconnectTimer !== null) {
        clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      try {
        const response = await fetch('/api/notifications/stream', {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: 'text/event-stream',
          },
          signal: controller.signal,
        });

        if (!response.ok || !response.body) {
          // Non-2xx or no body — schedule one reconnect and bail
          scheduleReconnect();
          return;
        }

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
                  callbackRef.current(payload);
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
  }, []); // Only run once on mount — token is read inside the effect
}
