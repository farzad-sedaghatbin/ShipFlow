/**
 * The backlog's cycle selection as it appears in a shareable URL (`?cycle=`).
 *
 * "All cycles" has to be written out as `cycle=all`. It used to be left out of the URL, but a link
 * with no `cycle` param means "pick the project's first/active cycle" — so a filter shared while on
 * "All cycles" opened on the active cycle instead, showing a different list than the sender saw.
 */
export type CycleSelection = number | 'all';

/** Value to put in `?cycle=`, or null when the URL should not carry one (Kanban has no cycles). */
export function cycleParamValue(selected: CycleSelection, isKanbanProject: boolean): string | null {
  if (isKanbanProject) return null;
  return String(selected);
}

/** Reads `?cycle=`: null means "not in the link", so the page falls back to the active cycle. */
export function parseCycleParam(value: string | null): CycleSelection | null {
  if (!value) return null;
  return value === 'all' ? 'all' : (Number(value) || null);
}
