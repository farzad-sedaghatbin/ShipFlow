import { useTranslation } from 'react-i18next';
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from './ui/alert-dialog';
import { Button } from './ui/button';

export interface ConflictDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** e.g. "pitch", "retro item", "wiki page" — interpolated into the dialog copy. */
  entityLabel: string;
  /** User chooses to overwrite the server's current version with their local edits. */
  onKeepMine: () => void;
  /** User chooses to discard their local edits and load the current version. */
  onDiscardMine: () => void;
}

/**
 * Generic optimistic-lock conflict dialog (v1.13.0 S64), shared by the Pitch,
 * Retro item, and Wiki page editing flows. Shown when a save fails with HTTP
 * 409 because someone else updated the entity first (see
 * `utils/conflictError.ts`).
 *
 * Deliberately simple — no content diff/merge view. A richer per-surface
 * comparison (e.g. reusing the wiki's `wikiDiff.ts` line-diff for a wiki-
 * specific conflict view) is a good follow-up, out of scope for this pass.
 */
export function ConflictDialog({ open, onOpenChange, entityLabel, onKeepMine, onDiscardMine }: ConflictDialogProps) {
  const { t } = useTranslation();

  return (
    <AlertDialog open={open} onOpenChange={onOpenChange}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{t('conflictDialog.title')}</AlertDialogTitle>
          <AlertDialogDescription>
            {t('conflictDialog.description', { entity: entityLabel })}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          {/* Fully controlled by the caller: `onKeepMine`/`onDiscardMine` are
              responsible for closing the dialog (typically by clearing the
              conflict state that drives `open`) once they're done — this
              dialog doesn't force-close itself, so a caller whose retry is
              still in flight (or fails) can choose to keep it open. */}
          <Button variant="outline" onClick={onDiscardMine}>
            {t('conflictDialog.discardMine')}
          </Button>
          <Button onClick={onKeepMine}>{t('conflictDialog.keepMine')}</Button>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}

export default ConflictDialog;
