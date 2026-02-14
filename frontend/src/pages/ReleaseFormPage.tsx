import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Save, Package } from 'lucide-react';
import { releaseService } from '../services/releaseService';
import { cycleService } from '../services/cycleService';
import { CreateReleaseRequest, ReleaseStatus, ReleaseRiskLevel, Cycle } from '../types';
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
import { Checkbox } from '../components/ui/checkbox';
import { Skeleton } from '../components/ui/skeleton';

export default function ReleaseFormPage() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { currentProject } = useProject();
  const { showSuccess, showError } = useToast();
  
  const isEditing = !!id;
  const [loading, setLoading] = useState(isEditing);
  const [saving, setSaving] = useState(false);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [selectedCycleIds, setSelectedCycleIds] = useState<number[]>([]);
  
  const [formData, setFormData] = useState<CreateReleaseRequest>({
    name: '',
    version: '',
    description: '',
    status: 'PLANNING',
    riskLevel: 'LOW',
    projectId: currentProject?.id || 0,
    targetDate: '',
    releaseDate: '',
    releaseNotes: '',
    cycleIds: [],
  });

  useEffect(() => {
    if (currentProject) {
      loadCycles();
    }
    if (isEditing) {
      loadRelease();
    } else if (currentProject) {
      setFormData(prev => ({ ...prev, projectId: currentProject.id }));
    }
  }, [id, currentProject]);

  const loadCycles = async () => {
    if (!currentProject) return;
    try {
      const response = await cycleService.getByProject(currentProject.id);
      setCycles(response.data);
    } catch (error) {
      console.error('Failed to load cycles:', error);
    }
  };

  const loadRelease = async () => {
    try {
      setLoading(true);
      const response = await releaseService.getById(Number(id));
      const release = response.data;
      setFormData({
        name: release.name,
        version: release.version,
        description: release.description || '',
        status: release.status,
        riskLevel: release.riskLevel,
        projectId: release.projectId,
        targetDate: release.targetDate || '',
        releaseDate: release.releaseDate || '',
        releaseNotes: release.releaseNotes || '',
        cycleIds: release.cycles?.map(c => c.id) || [],
      });
      setSelectedCycleIds(release.cycles?.map(c => c.id) || []);
    } catch (error) {
      console.error('Failed to load release:', error);
      showError(t('releases.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleCycleToggle = (cycleId: number, checked: boolean) => {
    setSelectedCycleIds(prev =>
      checked ? [...prev, cycleId] : prev.filter(id => id !== cycleId)
    );
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.version.trim()) {
      showError(t('releases.nameVersionRequired'));
      return;
    }
    
    const data = { ...formData, cycleIds: selectedCycleIds };
    
    try {
      setSaving(true);
      if (isEditing) {
        await releaseService.update(Number(id), data);
        showSuccess(t('releases.updated'));
        navigate(`/releases/${id}`);
      } else {
        const response = await releaseService.create(data);
        showSuccess(t('releases.created'));
        navigate(`/releases/${response.data.id}`);
      }
    } catch (error) {
      showError(isEditing ? t('releases.updateError') : t('releases.createError'));
      console.error('Failed to save release:', error);
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
            <Package className="h-5 w-5" />
            {isEditing ? t('releases.edit') : t('releases.create')}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="version">{t('releases.version')} *</Label>
                <Input
                  id="version"
                  value={formData.version}
                  onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                  placeholder="v1.0.0"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="name">{t('releases.name')} *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  placeholder={t('releases.namePlaceholder')}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">{t('releases.descriptionLabel')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={t('releases.descriptionPlaceholder')}
                rows={3}
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label>{t('releases.statusLabel')}</Label>
                <Select
                  value={formData.status}
                  onValueChange={(v) => setFormData({ ...formData, status: v as ReleaseStatus })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PLANNING">{t('releases.status.planning')}</SelectItem>
                    <SelectItem value="IN_PROGRESS">{t('releases.status.inProgress')}</SelectItem>
                    <SelectItem value="STAGING">{t('releases.status.staging')}</SelectItem>
                    <SelectItem value="RELEASED">{t('releases.status.released')}</SelectItem>
                    <SelectItem value="CANCELLED">{t('releases.status.cancelled')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>{t('releases.riskLevel')}</Label>
                <Select
                  value={formData.riskLevel}
                  onValueChange={(v) => setFormData({ ...formData, riskLevel: v as ReleaseRiskLevel })}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="LOW">{t('releases.risk.low')}</SelectItem>
                    <SelectItem value="MEDIUM">{t('releases.risk.medium')}</SelectItem>
                    <SelectItem value="HIGH">{t('releases.risk.high')}</SelectItem>
                    <SelectItem value="CRITICAL">{t('releases.risk.critical')}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="targetDate">{t('releases.targetDate')}</Label>
                <Input
                  id="targetDate"
                  type="date"
                  value={formData.targetDate}
                  onChange={(e) => setFormData({ ...formData, targetDate: e.target.value })}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="releaseDate">{t('releases.releaseDate')}</Label>
                <Input
                  id="releaseDate"
                  type="date"
                  value={formData.releaseDate}
                  onChange={(e) => setFormData({ ...formData, releaseDate: e.target.value })}
                />
              </div>
            </div>

            {/* Linked Cycles */}
            <div className="space-y-2">
              <Label>{t('releases.linkedCycles')}</Label>
              <Card className="p-4">
                {cycles.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t('releases.noCyclesAvailable')}</p>
                ) : (
                  <div className="space-y-2 max-h-48 overflow-y-auto">
                    {cycles.map((cycle) => (
                      <div key={cycle.id} className="flex items-center gap-2">
                        <Checkbox
                          id={`cycle-${cycle.id}`}
                          checked={selectedCycleIds.includes(cycle.id)}
                          onCheckedChange={(checked) => handleCycleToggle(cycle.id, !!checked)}
                        />
                        <Label htmlFor={`cycle-${cycle.id}`} className="font-normal cursor-pointer">
                          {cycle.name}
                        </Label>
                      </div>
                    ))}
                  </div>
                )}
              </Card>
            </div>

            <div className="space-y-2">
              <Label htmlFor="releaseNotes">{t('releases.releaseNotes')}</Label>
              <Textarea
                id="releaseNotes"
                value={formData.releaseNotes}
                onChange={(e) => setFormData({ ...formData, releaseNotes: e.target.value })}
                placeholder={t('releases.releaseNotesPlaceholder')}
                rows={4}
              />
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
