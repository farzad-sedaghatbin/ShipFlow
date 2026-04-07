import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface BacklogDeleteDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onCancel: () => void;
  onConfirm: () => void;
}

export function BacklogDeleteDialog({
  open,
  onOpenChange,
  onCancel,
  onConfirm,
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
