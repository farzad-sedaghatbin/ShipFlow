import { useTranslation } from 'react-i18next';
import { Button } from '../ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/dialog';

export interface BacklogDeleteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onConfirm: () => void;
  onCancel: () => void;
}

export function BacklogDeleteDialog({
  open,
  onOpenChange,
  onConfirm,
  onCancel,
}: BacklogDeleteDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{t('backlogPage.deleteTask')}</DialogTitle>
          <DialogDescription>
            {t('backlogPage.confirmDeleteMessage')}
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button variant="outline" onClick={onCancel}>
            {t('backlogPage.cancel')}
          </Button>
          <Button variant="destructive" onClick={onConfirm}>
            {t('backlogPage.delete')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
