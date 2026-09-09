import { useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';

/**
 * Returns a `goBack` callback that navigates to wherever the user actually
 * came from, falling back to a fixed parent route when that's unknown.
 *
 * Replaces two previously inconsistent "back" patterns across detail/form
 * pages:
 *  - `navigate(-1)` (browser history) — breaks on a deep link, notification
 *    link, or freshly opened tab where there's no history entry to pop.
 *  - a hardcoded parent route — always discards whatever filter/tab/sort
 *    state the user had on the list page they came from.
 *
 * The entry point is threaded through router state instead: a list/parent
 * page navigates in with `state: { from: `${location.pathname}${location.search}` }`,
 * and this hook reads it back. If a page is opened without that state (deep
 * link, notification, fresh tab), `fallbackPath` is used instead — matching
 * today's hardcoded-route behavior exactly for that case.
 */
export function useBackNavigation(fallbackPath: string) {
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from;

  return useCallback(() => {
    navigate(from || fallbackPath);
  }, [navigate, from, fallbackPath]);
}
