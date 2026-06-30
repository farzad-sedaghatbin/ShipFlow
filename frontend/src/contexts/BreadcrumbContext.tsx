import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from 'react';

/**
 * BreadcrumbContext lets detail pages publish a human-readable label for a route
 * path (e.g. `/cycles/12` → "Q3 Payments Cycle") so the global <Breadcrumbs />
 * resolver can render names instead of raw numeric IDs ("Cycle #12", "#1 › #1").
 *
 * A detail page calls `useBreadcrumbLabel(path, name)` once its entity has loaded;
 * the label is registered while the page is mounted and cleared on unmount.
 */

type LabelMap = Record<string, string>;

interface BreadcrumbContextValue {
  labels: LabelMap;
  setLabel: (path: string, label: string) => void;
  clearLabel: (path: string) => void;
}

const BreadcrumbContext = createContext<BreadcrumbContextValue | undefined>(
  undefined,
);

export function BreadcrumbProvider({ children }: { children: React.ReactNode }) {
  const [labels, setLabels] = useState<LabelMap>({});

  const setLabel = useCallback((path: string, label: string) => {
    setLabels((prev) => (prev[path] === label ? prev : { ...prev, [path]: label }));
  }, []);

  const clearLabel = useCallback((path: string) => {
    setLabels((prev) => {
      if (!(path in prev)) return prev;
      const next = { ...prev };
      delete next[path];
      return next;
    });
  }, []);

  const value = useMemo(
    () => ({ labels, setLabel, clearLabel }),
    [labels, setLabel, clearLabel],
  );

  return (
    <BreadcrumbContext.Provider value={value}>
      {children}
    </BreadcrumbContext.Provider>
  );
}

/** Read the map of registered path → label. Returns {} when no provider is mounted. */
export function useBreadcrumbLabels(): LabelMap {
  const ctx = useContext(BreadcrumbContext);
  return ctx ? ctx.labels : {};
}

/**
 * Register a resolved breadcrumb label for `path` while the calling component is
 * mounted. No-op when `path` or `label` is falsy (e.g. entity still loading), so
 * it is safe to call with values that arrive asynchronously.
 */
export function useBreadcrumbLabel(
  path: string | undefined,
  label: string | null | undefined,
) {
  const ctx = useContext(BreadcrumbContext);
  // Pull the stable callbacks out so the effect doesn't re-run on every label
  // change in the map (which would otherwise clear + re-set in a loop).
  const setLabel = ctx?.setLabel;
  const clearLabel = ctx?.clearLabel;

  useEffect(() => {
    if (!setLabel || !clearLabel || !path || !label) return;
    setLabel(path, label);
    return () => clearLabel(path);
  }, [setLabel, clearLabel, path, label]);
}
