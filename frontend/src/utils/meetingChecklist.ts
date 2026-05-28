import { MeetingChecklistItem } from '../types';

export interface ChecklistBadgeState {
  /** Badge variant: green when ready, amber when in progress, outline when untouched. */
  variant: 'success' | 'warning' | 'outline';
  completed: number;
  total: number;
}

/**
 * Derive the display state for a DOR/DOD checklist badge.
 *
 * "Ready" keeps its meaning — all required items done; optional items don't block — but
 * the badge only turns green when there is real engagement, so an untouched all-optional
 * checklist (which is technically "ready") no longer masquerades as complete.
 */
export function getChecklistBadgeState(items?: MeetingChecklistItem[]): ChecklistBadgeState {
  const list = items ?? [];
  const total = list.length;
  const completed = list.filter(i => i.isCompleted).length;
  const hasRequired = list.some(i => i.isRequired);
  const allRequiredDone = list.filter(i => i.isRequired).every(i => i.isCompleted);

  let variant: ChecklistBadgeState['variant'];
  if (total > 0 && allRequiredDone && (hasRequired || completed > 0)) {
    variant = 'success';
  } else if (completed > 0) {
    variant = 'warning';
  } else {
    variant = 'outline';
  }

  return { variant, completed, total };
}
