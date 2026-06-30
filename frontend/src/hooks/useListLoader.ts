import { useRef, useState } from 'react';

/**
 * Tracks whether a list page is on its first load (show skeleton) or
 * a subsequent filter/sort refresh (show progress bar, keep rows visible).
 *
 * Usage:
 *   const { loading, refreshing, startLoad, finishLoad, resetInitial } = useListLoader();
 *
 *   const fetch = async () => {
 *     startLoad();
 *     try { ... } finally { finishLoad(); }
 *   };
 *
 *   // In your effect deps array, call resetInitial() when the entity
 *   // changes (project switch, etc.) so the next fetch shows the full skeleton.
 */
export function useListLoader() {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const initialDone = useRef(false);

  const startLoad = () => {
    if (!initialDone.current) {
      setLoading(true);
    } else {
      setRefreshing(true);
    }
  };

  const finishLoad = () => {
    if (!initialDone.current) {
      initialDone.current = true;
    }
    setLoading(false);
    setRefreshing(false);
  };

  const resetInitial = () => {
    initialDone.current = false;
  };

  return { loading, refreshing, startLoad, finishLoad, resetInitial };
}
