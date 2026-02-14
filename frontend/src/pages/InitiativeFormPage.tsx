import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Save, Target } from 'lucide-react';
import { initiativeService } from '../services/initiativeService';
import { personService } from '../services/personService';
import { CreateInitiativeRequest, InitiativeStatus } from '../types';
import { useProject, useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
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
  '#6366f1', // Indigo
  '#8b5cf6', // Violet
  '#ec4899', // Pink
  '#ef4444', // Red
  '#f97316', // Orange
  '#eab308', // Yellow
  '#22c55e', // Green
  '#14b8a6', // Teal
  '#06b6d4', // Cyan
  '#3b82f6', // Blue
];

interface Person {
  id: number;
  name: string;
}

export default function InitiativeFormPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { currentProject } = useProject();
  const { showSuccess, showError } = useToast();
  
  const isEditing = !!id;
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [people, setPeople] = useState<Person[]>([]);
  
  const [formData, setFormData] = useState<CreateInitiativeRequest>({
    name: '',
    description: '',
    status: 'DRAFT',
    color: COLORS[0],
    projectId: currentProject?.id || 0,
    ownerId: undefined,
    targetStartDate: '',
    targetEndDate: '',
  });

  useEffect(() => {
    loadPeople();
    if (isEditing) {
      loadInitiative();
    } else if (currentProject) {
      setFormData(prev => ({ ...prev, projectId: currentProject.id }));
    }
  }, [id, currentProject]);

  const loadPeople = async () => {
    try {
      const people = await personService.getAll();
      setPeople(people);
    } catch (error) {
      console.error('Failed to load people:', error);
    }
  };

  const loadInitiative = async () => {
    try {
      setLoading(true);
      const response = await initiativeService.getById(Number(id));
      const initiative = response.data;
      setFormData({
        name: initiative.name,
        description: initiative.description || '',
        status: initiative.status,
        color: initiative.color || COLORS[0],
        projectId: initiative.projectId,
        ownerId: initiative.ownerId,
        targetStartDate: initiative.targetStartDate || '',
        targetEndDate: initiative.targetEndDate || '',
      });
    } catch (error) {
      console.error('Failed to load initiative:', error);
      showError(t('initiatives.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim()) {
      showError(t('initiatives.nameRequired'));
      return;
    }
    
    try {
      setSaving(true);
      if (isEditing) {
        await initiativeService.update(Number(id), formData);
        showSuccess(t('initiatives.updated'));
        navigate(`/initiatives/${id}`);
      } else {
        const response = await initiativeService.create(formData);
        showSuccess(t('initiatives.created'));
        navigate(`/initiatives/${response.data.id}`);
      }
    } catch (error) {
      showError(isEditing ? t('initiatives.updateError') : t('initiatives.createError'));
      console.error('Failed to save initiative:', error);
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
      {/* Back button */}
      <Button variant="ghost" onClick={() => navigate(-1)}>
        <ArrowLeft className="h-4 w-4 mr-2" />
        {t('common.back')}
      </Button>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Target className="h-5 w-5" />
            {isEditing ? t('initiatives.edit') : t('initiatives.create')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Name */}
            <div className="space-y-2">
              <Label htmlFor="name">{t('initiatives.name')} *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder={t('initiatives.namePlaceholder')}
              />
            </div>

            {/* Description */}
            <div className="space-y-2">
              <Label htmlFor="description">{t('initiatives.descriptionLabel')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={t('initiatives.descriptionPlaceholder')}
                rows={4}
              />
            </div>

            {/* Status */}
            <div className="space-y-2">
              <Label>{t('initiatives.statusLabel')}</Label>
              <Select
                value={formData.status}
                onValueChange={(v) => setFormData({ ...formData, status: v as InitiativeStatus })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="DRAFT">{t('initiatives.status.draft')}</SelectItem>
                  <SelectItem value="PLANNED">{t('initiatives.status.planned')}</SelectItem>
                  <SelectItem value="IN_PROGRESS">{t('initiatives.status.inProgress')}</SelectItem>
                  <SelectItem value="COMPLETED">{t('initiatives.status.completed')}</SelectItem>
                  <SelectItem value="ON_HOLD">{t('initiatives.status.onHold')}</SelectItem>
                  <SelectItem value="CANCELLED">{t('initiatives.status.cancelled')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Color */}
            <div className="space-y-2">
              <Label>{t('initiatives.color')}</Label>
              <div className="flex gap-2">
                {COLORS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    className={`w-8 h-8 rounded-full border-2 ${formData.color === color ? 'border-foreground' : 'border-transparent'}`}
                    style={{ backgroundColor: color }}
                    onClick={() => setFormData({ ...formData, color })}
                  />
                ))}
              </div>
            </div>

            {/* Owner */}
            <div className="space-y-2">
              <Label>{t('initiatives.owner')}</Label>
              <Select
                value={formData.ownerId?.toString() || 'NO_OWNER'}
                onValueChange={(v) => setFormData({ ...formData, ownerId: v === 'NO_OWNER' ? undefined : Number(v) })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('initiatives.selectOwner')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="NO_OWNER">{t('initiatives.noOwner')}</SelectItem>
                  {people.map((person) => (
                    <SelectItem key={person.id} value={person.id.toString()}>
                      {person.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {/* Date Range */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="startDate">{t('initiatives.startDate')}</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={formData.targetStartDate}
                  onChange={(e) => setFormData({ ...formData, targetStartDate: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="endDate">{t('initiatives.endDate')}</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={formData.targetEndDate}
                  onChange={(e) => setFormData({ ...formData, targetEndDate: e.target.value })}
                />
              </div>
            </div>

            {/* Actions */}
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
