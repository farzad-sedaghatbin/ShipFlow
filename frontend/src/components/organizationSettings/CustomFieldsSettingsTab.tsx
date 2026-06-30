import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Plus, Pencil, Trash2, X, Sliders, Loader2 } from 'lucide-react';
import { Badge } from '../ui/badge';
import { Button } from '../ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../ui/select';
import { Switch } from '../ui/switch';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogFooter } from '../ui/dialog';
import { ConfirmDialog } from '../ui/confirm-dialog';
import { customFieldService } from '../../services/customFieldService';
import {
  CustomFieldDefinition,
  CustomFieldEntityType,
  CustomFieldType,
  CreateCustomFieldDefinitionRequest,
  UpdateCustomFieldDefinitionRequest,
} from '../../types';
import { useToast } from '../../contexts';

const ENTITY_TYPES: CustomFieldEntityType[] = ['TASK', 'PITCH', 'BUG'];
const FIELD_TYPES: CustomFieldType[] = [
  'TEXT', 'NUMBER', 'DATE', 'SELECT', 'MULTISELECT', 'CHECKBOX', 'URL',
];

const definitionSchema = z
  .object({
    name: z.string().min(1).max(255),
    description: z.string().optional(),
    fieldType: z.enum(['TEXT', 'NUMBER', 'DATE', 'SELECT', 'MULTISELECT', 'CHECKBOX', 'URL']),
    entityType: z.enum(['TASK', 'PITCH', 'BUG']),
    required: z.boolean(),
    sortOrder: z.number().int(),
    options: z.array(z.string()),
  })
  .refine(
    (d) =>
      !['SELECT', 'MULTISELECT'].includes(d.fieldType) || d.options.length > 0,
    { message: 'Options required for SELECT / MULTISELECT', path: ['options'] },
  );

type DefinitionFormValues = z.infer<typeof definitionSchema>;

export function CustomFieldsSettingsTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();

  const [definitions, setDefinitions] = useState<CustomFieldDefinition[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeEntityType, setActiveEntityType] = useState<CustomFieldEntityType>('TASK');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingDef, setEditingDef] = useState<CustomFieldDefinition | null>(null);
  const [deleteTarget, setDeleteTarget] = useState<CustomFieldDefinition | null>(null);
  const [optionInput, setOptionInput] = useState('');
  const [saving, setSaving] = useState(false);

  const form = useForm<DefinitionFormValues>({
    resolver: zodResolver(definitionSchema),
    defaultValues: {
      name: '',
      fieldType: 'TEXT',
      entityType: 'TASK',
      required: false,
      sortOrder: 0,
      options: [],
    },
  });

  const watchedFieldType = form.watch('fieldType');
  const watchedOptions = form.watch('options');

  const load = async () => {
    setLoading(true);
    try {
      const res = await customFieldService.getDefinitions(activeEntityType);
      setDefinitions(res.data);
    } catch {
      showToast(t('customFields.saveFailed'), 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [activeEntityType]);

  const openCreate = () => {
    setEditingDef(null);
    form.reset({
      name: '',
      fieldType: 'TEXT',
      entityType: activeEntityType,
      required: false,
      sortOrder: 0,
      options: [],
    });
    setOptionInput('');
    setDialogOpen(true);
  };

  const openEdit = (def: CustomFieldDefinition) => {
    setEditingDef(def);
    form.reset({
      name: def.name,
      description: def.description ?? '',
      fieldType: def.fieldType,
      entityType: def.entityType,
      required: def.required,
      sortOrder: def.sortOrder,
      options: def.options ?? [],
    });
    setOptionInput('');
    setDialogOpen(true);
  };

  const addOption = () => {
    const trimmed = optionInput.trim();
    if (!trimmed) return;
    const current = form.getValues('options') ?? [];
    if (!current.includes(trimmed)) {
      form.setValue('options', [...current, trimmed]);
    }
    setOptionInput('');
  };

  const removeOption = (opt: string) => {
    form.setValue(
      'options',
      (form.getValues('options') ?? []).filter((o) => o !== opt),
    );
  };

  const onSubmit = async (values: DefinitionFormValues) => {
    setSaving(true);
    try {
      if (editingDef) {
        const req: UpdateCustomFieldDefinitionRequest = {
          name: values.name,
          description: values.description,
          required: values.required,
          sortOrder: values.sortOrder,
          options: values.options,
        };
        await customFieldService.updateDefinition(editingDef.id, req);
        showToast(t('customFields.saveSuccess'), 'success');
      } else {
        const req: CreateCustomFieldDefinitionRequest = {
          name: values.name,
          description: values.description,
          fieldType: values.fieldType,
          entityType: values.entityType,
          required: values.required,
          sortOrder: values.sortOrder,
          options: values.options,
        };
        await customFieldService.createDefinition(req);
        showToast(t('customFields.saveSuccess'), 'success');
      }
      setDialogOpen(false);
      load();
    } catch {
      showToast(t('customFields.saveFailed'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteTarget) return;
    try {
      await customFieldService.deleteDefinition(deleteTarget.id);
      showToast(t('customFields.saveSuccess'), 'success');
      setDeleteTarget(null);
      load();
    } catch {
      showToast(t('customFields.saveFailed'), 'error');
    }
  };

  const fieldTypeLabel = (type: CustomFieldType) =>
    t(`customFields.fieldType.${type}`, type);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-lg font-semibold flex items-center gap-2">
            <Sliders className="h-5 w-5" />
            {t('customFields.title')}
          </h3>
          <p className="text-sm text-muted-foreground mt-1">{t('customFields.subtitle')}</p>
        </div>
        <Button size="sm" className="gap-2" onClick={openCreate}>
          <Plus className="h-4 w-4" />
          {t('customFields.createField')}
        </Button>
      </div>

      {/* Entity type tabs */}
      <div className="flex gap-2 border-b pb-2">
        {ENTITY_TYPES.map((et) => (
          <button
            key={et}
            onClick={() => setActiveEntityType(et)}
            className={`px-3 py-1.5 text-sm rounded-md transition-colors ${
              activeEntityType === et
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            }`}
          >
            {t(`customFields.entityType.${et}`)}
          </button>
        ))}
      </div>

      {/* Definition list */}
      {loading ? (
        <div className="flex justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : definitions.length === 0 ? (
        <p className="text-sm text-muted-foreground py-8 text-center">{t('customFields.noFields')}</p>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {definitions.map((def) => (
            <Card key={def.id}>
              <CardHeader className="pb-2 flex flex-row items-start justify-between gap-2">
                <div className="min-w-0">
                  <CardTitle className="text-base truncate">{def.name}</CardTitle>
                  {def.description && (
                    <p className="text-xs text-muted-foreground mt-0.5 truncate">{def.description}</p>
                  )}
                </div>
                <div className="flex gap-1 shrink-0">
                  <Button variant="ghost" size="icon" className="h-7 w-7" onClick={() => openEdit(def)}>
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-7 w-7 text-destructive hover:text-destructive"
                    onClick={() => setDeleteTarget(def)}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              </CardHeader>
              <CardContent className="pt-0 flex flex-wrap gap-1.5">
                <Badge variant="outline">{fieldTypeLabel(def.fieldType)}</Badge>
                {def.required && (
                  <Badge variant="secondary">{t('customFields.required')}</Badge>
                )}
                {def.projectId ? (
                  <Badge variant="secondary" className="text-xs">{def.projectName}</Badge>
                ) : (
                  <Badge variant="secondary" className="text-xs">{t('customFields.orgWide')}</Badge>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create / Edit dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>
              {editingDef ? t('customFields.editField') : t('customFields.createField')}
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            {/* Name */}
            <div className="space-y-1.5">
              <Label>{t('customFields.fieldName')}</Label>
              <Input {...form.register('name')} />
              {form.formState.errors.name && (
                <p className="text-xs text-destructive">{form.formState.errors.name.message}</p>
              )}
            </div>

            {/* Field type — immutable after creation */}
            {!editingDef && (
              <div className="space-y-1.5">
                <Label>{t('customFields.fieldType')}</Label>
                <Select
                  value={form.watch('fieldType')}
                  onValueChange={(v) => form.setValue('fieldType', v as CustomFieldType)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {FIELD_TYPES.map((ft) => (
                      <SelectItem key={ft} value={ft}>
                        {fieldTypeLabel(ft)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Entity type — immutable after creation */}
            {!editingDef && (
              <div className="space-y-1.5">
                <Label>{t('customFields.entityType')}</Label>
                <Select
                  value={form.watch('entityType')}
                  onValueChange={(v) => form.setValue('entityType', v as CustomFieldEntityType)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ENTITY_TYPES.map((et) => (
                      <SelectItem key={et} value={et}>
                        {t(`customFields.entityType.${et}`)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {/* Options (SELECT / MULTISELECT only) */}
            {(watchedFieldType === 'SELECT' || watchedFieldType === 'MULTISELECT') && (
              <div className="space-y-1.5">
                <Label>{t('customFields.options')}</Label>
                <div className="flex gap-2">
                  <Input
                    value={optionInput}
                    onChange={(e) => setOptionInput(e.target.value)}
                    placeholder={t('customFields.optionsHint')}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter') {
                        e.preventDefault();
                        addOption();
                      }
                    }}
                  />
                  <Button type="button" variant="outline" size="sm" onClick={addOption}>
                    <Plus className="h-4 w-4" />
                  </Button>
                </div>
                <div className="flex flex-wrap gap-1.5 mt-1">
                  {(watchedOptions ?? []).map((opt) => (
                    <Badge key={opt} variant="secondary" className="gap-1">
                      {opt}
                      <button type="button" onClick={() => removeOption(opt)}>
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  ))}
                </div>
                {form.formState.errors.options && (
                  <p className="text-xs text-destructive">
                    {form.formState.errors.options.message}
                  </p>
                )}
              </div>
            )}

            {/* Required toggle */}
            <div className="flex items-center gap-3">
              <Switch
                checked={form.watch('required')}
                onCheckedChange={(v) => form.setValue('required', v)}
              />
              <Label>{t('customFields.required')}</Label>
            </div>

            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setDialogOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={saving}>
                {saving && <Loader2 className="h-4 w-4 animate-spin mr-2" />}
                {t('customFields.saveFields')}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete confirmation */}
      <ConfirmDialog
        open={!!deleteTarget}
        onOpenChange={(open) => !open && setDeleteTarget(null)}
        title={t('customFields.deleteField')}
        description={t('customFields.deleteConfirm', { name: deleteTarget?.name ?? '' })}
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
}
