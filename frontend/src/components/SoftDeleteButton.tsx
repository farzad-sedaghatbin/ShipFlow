import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Trash2, Loader2 } from 'lucide-react';
import { Button } from '../components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { pitchService } from '../services/pitchService';
import { taskService } from '../services/taskService';
import qaTestManagementService from '../services/qaTestManagementService';
import { useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';

interface SoftDeleteButtonProps {
  entityType: 'pitch' | 'task' | 'testCase';
  entityId: number;
  entityTitle: string;
  disabled?: boolean;
  onSuccess?: () => void;
  redirectPath?: string;
  size?: 'sm' | 'default' | 'lg';
  variant?: 'default' | 'outline' | 'destructive' | 'secondary' | 'ghost' | 'link';
}

export function SoftDeleteButton({
  entityType,
  entityId,
  entityTitle,
  disabled = false,
  onSuccess,
  redirectPath,
  size = 'sm',
  variant = 'outline'
}: SoftDeleteButtonProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showSuccess, showError } = useToast();
  const [deleteDialog, setDeleteDialog] = useState(false);
  const [deleting, setDeleting] = useState(false);

  const handleDelete = async () => {
    setDeleting(true);
    try {
      switch (entityType) {
        case 'pitch':
          await pitchService.delete(entityId);
          break;
        case 'task':
          await taskService.delete(entityId);
          break;
        case 'testCase':
          await qaTestManagementService.deleteTestCase(entityId);
          break;
        default:
          throw new Error(`Unknown entity type: ${entityType}`);
      }
      
      showSuccess(t(`softDelete.${entityType}Deleted`));
      
      if (onSuccess) {
        onSuccess();
      }
      
      if (redirectPath) {
        navigate(redirectPath);
      }
    } catch (error) {
      showError(getUserFriendlyError(error, t(`softDelete.${entityType}DeleteFailed`)));
    } finally {
      setDeleting(false);
      setDeleteDialog(false);
    }
  };

  return (
    <>
      <Button
        variant={variant}
        size={size}
        onClick={() => setDeleteDialog(true)}
        disabled={disabled}
        className={variant === 'outline' ? 'text-red-600 hover:text-red-700 border-red-200 hover:border-red-300' : ''}
      >
        <Trash2 className="h-4 w-4 mr-2" />
        {t('common.delete')}
      </Button>

      <Dialog open={deleteDialog} onOpenChange={setDeleteDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t(`softDelete.delete${entityType.charAt(0).toUpperCase() + entityType.slice(1)}`)}</DialogTitle>
            <DialogDescription>
              {t(`softDelete.delete${entityType.charAt(0).toUpperCase() + entityType.slice(1)}Confirmation`, { title: entityTitle })}
              <br /><br />
              <span className="text-amber-600 font-medium">
                {t('softDelete.softDeleteNote')}
              </span>
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setDeleteDialog(false)}
              disabled={deleting}
            >
              {t('common.cancel')}
            </Button>
            <Button
              variant="destructive"
              onClick={handleDelete}
              disabled={deleting}
            >
              {deleting && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
              {t('common.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}