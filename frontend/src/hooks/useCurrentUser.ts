/**
 * React Query hook for the current user.
 *
 * Provides live sync of user data (role, active status, personId) from the server,
 * complementing the localStorage-based instant hydration in AuthContext.
 *
 * The server returns an ETag for GET /api/auth/me — when nothing has changed,
 * the backend responds with 304 Not Modified and zero body transfer, while the
 * Axios ETag interceptor transparently serves the cached response.
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { queryKeys } from '@/lib/queryClient';
import { authService } from '@/services/authService';

export interface CurrentUser {
  userId: number;
  username: string;
  role: string;
  personId?: number;
  personName?: string;
}

/**
 * Fetches the current user from the server and keeps it fresh.
 *
 * @param enabled - Pass `false` to disable (e.g. when not authenticated).
 *                  Default: `true`.
 * @returns React Query result with `data: CurrentUser | undefined`.
 */
export function useCurrentUser(enabled = true) {
  return useQuery<CurrentUser>({
    queryKey: queryKeys.user.current,
    queryFn: async () => {
      const response = await authService.getCurrentUser();
      return response.data;
    },
    enabled,
    staleTime: 1000 * 60, // 1 minute — short enough to detect deactivation quickly
    gcTime: 1000 * 60 * 10, // 10 minutes
    refetchOnWindowFocus: true, // Re-check when user returns to the tab
  });
}

/** Imperatively invalidate the current user cache (e.g. after profile update). */
export function useInvalidateCurrentUser() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: queryKeys.user.current });
}
