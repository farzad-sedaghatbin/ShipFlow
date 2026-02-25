import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Save, Layers } from 'lucide-react';
import { epicService } from '../services/epicService';
import { initiativeService } from '../services/initiativeService';
import { CreateEpicRequest, EpicStatus, Initiative } from '../types';
import { useProject, useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import MarkdownEditor from '../components/MarkdownEditor';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Skeleton } from '../components/ui/skeleton';

const COLORS = [
  // Reds & Pinks
  '#ef4444', '#dc2626', '#f43f5e', '#e11d48', '#ec4899', '#db2777',
  // Oranges & Ambers
  '#f97316', '#ea580c', '#f59e0b', '#d97706', '#eab308', '#ca8a04',
  // Greens
  '#22c55e', '#16a34a', '#10b981', '#059669', '#84cc16', '#65a30d',
  // Teals & Cyans
  '#14b8a6', '#0d9488', '#06b6d4', '#0891b2', '#22d3ee', '#67e8f9',
  // Blues
  '#3b82f6', '#2563eb', '#6366f1', '#4f46e5', '#0ea5e9', '#0284c7',
  // Purples & Violets
  '#8b5cf6', '#7c3aed', '#a855f7', '#9333ea', '#d946ef', '#c026d3',
  // Neutrals
  '#78716c', '#57534e', '#64748b', '#475569', '#6b7280', '#4b5563',
];

export default function EpicFormPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { currentProject } = useProject();
  const { showSuccess, showError } = useToast();
  
  const isEditing = !!id;
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [initiatives, setInitiatives] = useState<Initiative[]>([]);
  
  const defaultInitiativeId = searchParams.get('initiativeId');
  
  const [formData, setFormData] = useState<CreateEpicRequest>({
    name: '',
    description: '',
    status: 'DRAFT',
    color: COLORS[0],
    projectId: currentProject?.id || 0,
    initiativeId: defaultInitiativeId ? Number(defaultInitiativeId) : undefined,
    targetStartDate: '',
    targetEndDate: '',
  });

  useEffect(() => {
    if (currentProject) {
      loadInitiatives();
    }
    if (isEditing) {
      loadEpic();
    } else if (currentProject) {
      setFormData(prev => ({ ...prev, projectId: currentProject.id }));
    }
  }, [id, currentProject]);

  const loadInitiatives = async () => {
    if (!currentProject) return;
    try {
      const response = await initiativeService.getByProject(currentProject.id);
      setInitiatives(response.data);
    } catch (error) {
      console.error('Failed to load initiatives:', error);
    }
  };

  const loadEpic = async () => {
    try {
      setLoading(true);
      const response = await epicService.getById(Number(id));
      const epic = response.data;
      setFormData({
        name: epic.name,
        description: epic.description || '',
        status: epic.status,
        color: epic.color || COLORS[0],
        projectId: epic.projectId,
        initiativeId: epic.initiativeId,
        targetStartDate: epic.targetStartDate || '',
        targetEndDate: epic.targetEndDate || '',
      });
    } catch (error) {
      console.error('Failed to load epic:', error);
      showError(t('epics.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      showError(t('epics.nameRequired'));
      return;
    }
    
    try {
      setSaving(true);
      if (isEditing) {
        await epicService.update(Number(id), formData);
        showSuccess(t('epics.updated'));
        navigate(`/epics/${id}`);
      } else {
        const response = await epicService.create(formData);
        showSuccess(t('epics.created'));
        navigate(`/epics/${response.data.id}`);
      }
    } catch (error) {
      showError(isEditing ? t('epics.updateError') : t('epics.createError'));
      console.error('Failed to save epic:', error);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="container mx-auto py-8 max-w-2xl space-y-6">
        <Skeleton className="h-10 w-64" />
        <Skeleton className="h-96" />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 max-w-2xl space-y-6">
      <Button variant="ghost" onClick={() => navigate(-1)}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        {t('common.back')}
      </Button>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Layers className="h-5 w-5" />
            {isEditing ? t('epics.edit') : t('epics.create')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="name">{t('epics.name')} *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder={t('epics.namePlaceholder')}
              />
            </div>

            <div className="space-y-2">
              <Label>{t('epics.descriptionLabel')}</Label>
              <MarkdownEditor
                id="description"
                value={formData.description}
                onChange={(value) => setFormData({ ...formData, description: value })}
                placeholder={t('epics.descriptionPlaceholder')}
                rows={6}
              />
            </div>

            <div className="space-y-2">
              <Label>{t('epics.initiative')}</Label>
              <Select
                value={formData.initiativeId?.toString() || 'NO_INITIATIVE'}
                onValueChange={(v) => setFormData({ ...formData, initiativeId: v === 'NO_INITIATIVE' ? undefined : Number(v) })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('epics.selectInitiative')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NO_INITIATIVE">{t('epics.noInitiative')}</SelectItem>
                  {initiatives.map((init) => (
                    <SelectItem key={init.id} value={init.id.toString()}>
                      {init.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>{t('epics.statusLabel')}</Label>
              <Select
                value={formData.status}
                onValueChange={(v) => setFormData({ ...formData, status: v as EpicStatus })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DRAFT">{t('epics.status.draft')}</SelectItem>
                  <SelectItem value="PLANNED">{t('epics.status.planned')}</SelectItem>
                  <SelectItem value="IN_PROGRESS">{t('epics.status.inProgress')}</SelectItem>
                  <SelectItem value="COMPLETED">{t('epics.status.completed')}</SelectItem>
                  <SelectItem value="ON_HOLD">{t('epics.status.onHold')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>{t('epics.color')}</Label>
              <div className="flex flex-wrap gap-2">
                {COLORS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    className={`w-7 h-7 rounded-full border-2 transition-transform hover:scale-110 ${formData.color === color ? 'border-foreground ring-2 ring-foreground/20' : 'border-transparent'}`}
                    style={{ backgroundColor: color }}
                    onClick={() => setFormData({ ...formData, color })}
                  />
                ))}
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="startDate">{t('epics.startDate')}</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={formData.targetStartDate}
                  onChange={(e) => setFormData({ ...formData, targetStartDate: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="endDate">{t('epics.endDate')}</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={formData.targetEndDate}
                  onChange={(e) => setFormData({ ...formData, targetEndDate: e.target.value })}
                />
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button type="button" variant="outline" onClick={() => navigate(-1)}>
                {t('common.cancel')}
              </Button>
              <Button type="submit" disabled={saving}>
                <Save className="h-4 w-4 mr-2" />
                {saving ? t('common.saving') : t('common.save')}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
