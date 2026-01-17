import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Edit, Plus, X } from 'lucide-react';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import permissionService, {
  UserRole,
  ResourceType,
  PermissionType,
  CreatePermissionRequest,
} from '../services/permissionService';

interface PermissionEditDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSave: () => void;
  mode: 'create' | 'edit';
  permission?: {
    id: number;
    role: UserRole;
    resourceType: ResourceType;
    permissionType: PermissionType;
    description?: string;
  };
}

export function PermissionEditDialog({
  open,
  onOpenChange,
  onSave,
  mode,
  permission,
}: PermissionEditDialogProps) {
  const { t } = useTranslation();
  const [formData, setFormData] = useState<CreatePermissionRequest>({
    role: permission?.role || 'ADMIN',
    resourceType: permission?.resourceType || 'CYCLE',
    permissionType: permission?.permissionType || 'CREATE',
    description: permission?.description || '',
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const handleSave = async () => {
    setSaving(true);
    setError('');
    try {
      if (mode === 'create') {
        await permissionService.createPermission(formData);
      } else if (permission) {
        await permissionService.updatePermission(permission.id, {
          description: formData.description || '',
        });
      }
      onSave();
      onOpenChange(false);
    } catch (err: any) {
      setError(err.response?.data?.message || t('errors.savePermissionFailed'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>
            {mode === 'create' ? (
              <>
                <Plus className="h-5 w-5 inline mr-2" />
                {t('permissions.dialog.createTitle')}
              </>
            ) : (
              <>
                <Edit className="h-5 w-5 inline mr-2" />
                {t('permissions.dialog.editTitle')}
              </>
            )}
          </DialogTitle>
          <DialogDescription>
            {mode === 'create'
              ? t('permissions.dialog.createDescription')
              : t('permissions.dialog.editDescription')}
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4 py-4">
          {error && (
            <div className="bg-red-50 text-red-600 p-3 rounded text-sm">{error}</div>
          )}

          <div className="space-y-2">
            <Label htmlFor="role">{t('permissions.dialog.role')}</Label>
            <Select
              value={formData.role}
              onValueChange={(value) =>
                setFormData({ ...formData, role: value as UserRole })
              }
              disabled={mode === 'edit'}
            >
              <SelectTrigger id="role">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {permissionService.getUserRoles().map((role) => (
                  <SelectItem key={role} value={role}>
                    {role}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="resource">{t('permissions.dialog.resourceType')}</Label>
            <Select
              value={formData.resourceType}
              onValueChange={(value) =>
                setFormData({ ...formData, resourceType: value as ResourceType })
              }
              disabled={mode === 'edit'}
            >
              <SelectTrigger id="resource">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {permissionService.getResourceTypes().map((resource) => (
                  <SelectItem key={resource} value={resource}>
                    {permissionService.getResourceLabel(resource)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="permission">{t('permissions.dialog.permissionType')}</Label>
            <Select
              value={formData.permissionType}
              onValueChange={(value) =>
                setFormData({ ...formData, permissionType: value as PermissionType })
              }
              disabled={mode === 'edit'}
            >
              <SelectTrigger id="permission">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {permissionService.getPermissionTypes().map((perm) => (
                  <SelectItem key={perm} value={perm}>
                    {permissionService.getPermissionLabel(perm)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">{t('permissions.dialog.description')}</Label>
            <Input
              id="description"
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              placeholder={t('permissions.dialog.descriptionPlaceholder')}
            />
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={saving}
          >
            <X className="h-4 w-4 mr-2" />
            {t('permissions.dialog.cancel')}
          </Button>
          <Button onClick={handleSave} disabled={saving}>
            {saving ? t('permissions.dialog.saving') : t(mode === 'create' ? 'permissions.dialog.create' : 'permissions.dialog.update')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
