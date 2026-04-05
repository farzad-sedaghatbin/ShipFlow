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

    async function connect() {
      if (aborted) return;
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

        // Read the stream chunk by chunk
        const read = async (): Promise<void> => {
          if (aborted) return;
          const { done, value } = await reader.read();
          if (done || aborted) return;

          const text = decoder.decode(value, { stream: true });
          // SSE frames: one or more "field: value\n" lines, terminated by "\n\n"
          const lines = text.split('\n');
          let eventName = '';
          for (const line of lines) {
            if (line.startsWith('event:')) {
              eventName = line.slice(6).trim();
            } else if (line.startsWith('data:')) {
              const raw = line.slice(5).trim();
              if (eventName === 'notification' || (!eventName && raw)) {
                try {
                  const payload: unknown = JSON.parse(raw);
                  callbackRef.current(payload);
                } catch {
                  // Ignore malformed JSON frames
                }
              }
              // Reset event name after consuming data line
              eventName = '';
            }
          }

          return read();
        };

        await read();
      } catch (err) {
        if (aborted) return; // Normal cleanup on unmount
        // Schedule a reconnect for transient network errors
        scheduleReconnect();
      }
    }

    function scheduleReconnect() {
      if (aborted) return;
      reconnectTimer = setTimeout(() => {
        if (!aborted) connect();
      }, RECONNECT_DELAY_MS);
    }

    connect();

    return () => {
      aborted = true;
      controller.abort();
      if (reconnectTimer !== null) clearTimeout(reconnectTimer);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Only run once on mount — token is read inside the effect
}
