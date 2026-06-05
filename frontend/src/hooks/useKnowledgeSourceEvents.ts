import { useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useNotificationStream } from './useNotificationStream';

/**
 * Subscribes to the JWT-authenticated notification SSE stream and invalidates
 * the Knowledge Center query cache whenever a {@code knowledge.source.*} event
 * arrives. The stream is shared with the global notification stream — we just
 * filter on event name.
 */
export function useKnowledgeSourceEvents(): void {
  const qc = useQueryClient();

  const handler = useCallback(
    (_payload: unknown, eventName?: string) => {
      if (eventName && eventName.startsWith('knowledge.source.')) {
        qc.invalidateQueries({ queryKey: ['knowledge'] });
      }
    },
    [qc]
  );

  useNotificationStream(handler);
}
