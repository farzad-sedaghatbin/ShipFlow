import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { ChevronDown, ChevronRight, Loader2, ExternalLink, Sliders } from 'lucide-react';
import { Button } from './ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './ui/card';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Switch } from './ui/switch';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { customFieldService } from '../services/customFieldService';
import {
  CustomFieldDefinition,
  CustomFieldEntityType,
  CustomFieldValue,
} from '../types';
import { useToast } from '../contexts';

interface CustomFieldsSectionProps {
  entityType: CustomFieldEntityType;
  entityId: number;
  projectId?: number;
  readonly?: boolean;
}

export function CustomFieldsSection({
  entityType,
  entityId,
  projectId,
  readonly = false,
}: CustomFieldsSectionProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const queryClient = useQueryClient();

  const [collapsed, setCollapsed] = useState(false);
  const [definitions, setDefinitions] = useState<CustomFieldDefinition[]>([]);
  const [values, setValues] = useState<Record<number, string>>({});
  const [pending, setPending] = useState<Record<number, string>>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState<Record<number, string>>({});

  const load = async () => {
    setLoading(true);
    try {
      const [defsRes, valsRes] = await Promise.all([
        customFieldService.getDefinitions(entityType, projectId),
        customFieldService.getValues(entityType, entityId),
      ]);
      setDefinitions(defsRes.data);
      const valMap: Record<number, string> = {};
      valsRes.data.forEach((v: CustomFieldValue) => {
        if (v.value !== undefined && v.value !== null) {
          valMap[v.definitionId] = v.value;
        }
      });
      setValues(valMap);
      setPending({});
    } catch {
      // silent — entity may have no custom fields
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [entityType, entityId, projectId]);

  if (!loading && definitions.length === 0) return null;

  const handleChange = (defId: number, encoded: string) => {
    setPending((p) => ({ ...p, [defId]: encoded }));
    setErrors((e) => { const n = { ...e }; delete n[defId]; return n; });
  };

  const currentValue = (defId: number) =>
    defId in pending ? pending[defId] : (values[defId] ?? '');

  const validate = (): boolean => {
    const newErrors: Record<number, string> = {};
    definitions.forEach((def) => {
      const v = currentValue(def.id);
      if (def.required && (!v || v.trim() === '')) {
        newErrors[def.id] = t('customFields.required.error', { name: def.name });
      }
    });
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const save = async () => {
    if (!validate()) return;
    if (Object.keys(pending).length === 0) return;
    setSaving(true);
    try {
      await customFieldService.bulkUpsertValues({
        entityType,
        entityId,
        values: pending,
      });
      setValues((v) => ({ ...v, ...pending }));
      setPending({});
      showToast(t('customFields.saveSuccess'), 'success');
      queryClient.invalidateQueries({ queryKey: ['customFieldValues', entityType, entityId] });
    } catch {
      showToast(t('customFields.saveFailed'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const renderField = (def: CustomFieldDefinition) => {
    const val = currentValue(def.id);
    const isReadonly = readonly;

    switch (def.fieldType) {
      case 'TEXT':
        return (
          <Input
            value={val}
            onChange={(e) => handleChange(def.id, e.target.value)}
            disabled={isReadonly}
          />
        );
      case 'NUMBER':
        return (
          <Input
            type="number"
            value={val}
            onChange={(e) => handleChange(def.id, e.target.value)}
            disabled={isReadonly}
          />
        );
      case 'DATE':
        return (
          <Input
            type="date"
            value={val}
            onChange={(e) => handleChange(def.id, e.target.value)}
            disabled={isReadonly}
          />
        );
      case 'URL':
        return (
          <div className="flex gap-2">
            <Input
              type="url"
              value={val}
              onChange={(e) => handleChange(def.id, e.target.value)}
              disabled={isReadonly}
              className="flex-1"
            />
            {val && (
              <a href={val} target="_blank" rel="noopener noreferrer">
                <Button type="button" variant="outline" size="icon" className="shrink-0">
                  <ExternalLink className="h-4 w-4" />
                </Button>
              </a>
            )}
          </div>
        );
      case 'CHECKBOX':
        return (
          <Switch
            checked={val === 'true'}
            onCheckedChange={(checked) => handleChange(def.id, checked ? 'true' : 'false')}
            disabled={isReadonly}
          />
        );
      case 'SELECT':
        return (
          <Select
            value={val}
            onValueChange={(v) => handleChange(def.id, v)}
            disabled={isReadonly}
          >
            <SelectTrigger>
              <SelectValue placeholder="—" />
            </SelectTrigger>
            <SelectContent>
              {(def.options ?? []).map((opt) => (
                <SelectItem key={opt} value={opt}>
                  {opt}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        );
      case 'MULTISELECT': {
        let selected: string[] = [];
        try {
          selected = val ? JSON.parse(val) : [];
        } catch {
          selected = [];
        }
        const toggle = (opt: string) => {
          const next = selected.includes(opt)
            ? selected.filter((s) => s !== opt)
            : [...selected, opt];
          handleChange(def.id, JSON.stringify(next));
        };
        return (
          <div className="flex flex-wrap gap-1.5">
            {(def.options ?? []).map((opt) => (
              <button
                key={opt}
                type="button"
                disabled={isReadonly}
                onClick={() => toggle(opt)}
                className={`px-2.5 py-0.5 text-xs rounded-full border transition-colors ${
                  selected.includes(opt)
                    ? 'bg-primary text-primary-foreground border-primary'
                    : 'border-border hover:border-primary/50'
                } disabled:opacity-50 disabled:cursor-not-allowed`}
              >
                {opt}
              </button>
            ))}
          </div>
        );
      }
      default:
        return null;
    }
  };

  const hasPendingChanges = Object.keys(pending).length > 0;

  return (
    <Card className="mt-4">
      <CardHeader
        className="pb-2 cursor-pointer select-none"
        onClick={() => setCollapsed((c) => !c)}
      >
        <CardTitle className="text-sm font-medium flex items-center gap-2">
          <Sliders className="h-4 w-4" />
          {t('customFields.title')}
          <span className="ml-auto">
            {collapsed ? (
              <ChevronRight className="h-4 w-4 text-muted-foreground" />
            ) : (
              <ChevronDown className="h-4 w-4 text-muted-foreground" />
            )}
          </span>
        </CardTitle>
      </CardHeader>

      {!collapsed && (
        <CardContent className="space-y-4 pt-2">
          {loading ? (
            <div className="flex justify-center py-4">
              <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
            </div>
          ) : (
            <>
              {definitions.map((def) => (
                <div key={def.id} className="space-y-1">
                  <Label className="flex items-center gap-1">
                    {def.name}
                    {def.required && (
                      <span className="text-destructive" title="Required">*</span>
                    )}
                  </Label>
                  {renderField(def)}
                  {errors[def.id] && (
                    <p className="text-xs text-destructive">{errors[def.id]}</p>
                  )}
                </div>
              ))}

              {!readonly && (
                <div className="flex justify-end pt-2">
                  <Button
                    size="sm"
                    onClick={save}
                    disabled={!hasPendingChanges || saving}
                  >
                    {saving && <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />}
                    {t('customFields.saveFields')}
                  </Button>
                </div>
              )}
            </>
          )}
        </CardContent>
      )}
    </Card>
  );
}
