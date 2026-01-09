import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import { cycleService } from '../services/cycleService';
import projectService from '../services/projectService';
import { CreateCycleRequest, CyclePhase, Project } from '../types';
import { useProject, useToast } from '../contexts';
import { getUserFriendlyError } from '../utils/errorMessages';
import LoadingButton from '../components/LoadingButton';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Skeleton } from '../components/ui/skeleton';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';

export default function CycleForm() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const navigate = useNavigate();
  const isEdit = !!id;
  const { currentProject } = useProject();
  const { showSuccess } = useToast();

  const [formData, setFormData] = useState<CreateCycleRequest>({
    projectId: currentProject?.id || 0,
    name: '',
    startDate: '',
    endDate: '',
    phase: 'BUILD',
  });
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(isEdit);
  const [projects, setProjects] = useState<Project[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(true);

  useEffect(() => {
    loadProjects();
  }, []);

  useEffect(() => {
    if (currentProject) {
      setFormData(prev => ({ ...prev, projectId: currentProject.id }));
    }
  }, [currentProject]);

  useEffect(() => {
    if (isEdit && id) {
      loadCycle(id);
    }
  }, [id, isEdit]);

  const loadProjects = async () => {
    try {
      setProjectsLoading(true);
      const response = await projectService.getActive();
      setProjects(response);
    } catch (err) {
      setError('Unable to load projects. Please refresh the page.');
    } finally {
      setProjectsLoading(false);
    }
  };

  // Auto-select first project if projectId is 0 (none selected) and not editing
  useEffect(() => {
    if (projects.length > 0 && formData.projectId === 0 && !isEdit) {
      setFormData(prev => ({ ...prev, projectId: projects[0].id }));
    }
  }, [projects, formData.projectId, isEdit]);

  const loadCycle = async (cycleId: number) => {
    try {
      setLoadingData(true);
      const response = await cycleService.getById(cycleId);
      const cycle = response.data;
      setFormData({
        projectId: cycle.projectId || currentProject?.id || 0,
        name: cycle.name,
        startDate: cycle.startDate,
        endDate: cycle.endDate,
        phase: cycle.phase,
      });
      setStartDate(cycle.startDate);
      setEndDate(cycle.endDate);
    } catch (err) {
      setError('Unable to load cycle details. The cycle may have been deleted.');
    } finally {
      setLoadingData(false);
    }
  };

  // Validate form fields
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.name.trim()) {
      errors.name = 'Cycle name is required';
    } else if (formData.name.trim().length < 3) {
      errors.name = 'Cycle name must be at least 3 characters';
    }

    if (!formData.projectId) {
      errors.projectId = 'Please select a project';
    }

    if (!startDate) {
      errors.startDate = 'Start date is required';
    }

    if (!endDate) {
      errors.endDate = 'End date is required';
    } else if (startDate && endDate < startDate) {
      errors.endDate = 'End date must be after start date';
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!validateForm()) {
      return;
    }

    setLoading(true);

    const data: CreateCycleRequest = {
      ...formData,
      startDate,
      endDate,
    };

    try {
      if (isEdit && id) {
        await cycleService.update(id, data);
        showSuccess('Cycle updated successfully!');
      } else {
        await cycleService.create(data);
        showSuccess('Cycle created successfully!');
      }
      navigate('/cycles');
    } catch (err) {
      setError(getUserFriendlyError(err, 'Failed to save cycle. Please try again.'));
    } finally {
      setLoading(false);
    }
  };

  // Show loading skeleton while fetching data
  if (isEdit && id === null) {
    return (
      <div className="container mx-auto py-6 px-4 sm:px-6 lg:px-8">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <span className="text-sm">Invalid cycle ID</span>
        </div>
      </div>
    );
  }

  if (loadingData) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-10 w-48" />
        <Card className="max-w-xl">
          <CardContent className="pt-6 space-y-4">
            <Skeleton className="h-10 w-full" />
            <Skeleton className="h-10 w-full" />
            <div className="grid grid-cols-2 gap-4">
              <Skeleton className="h-10" />
              <Skeleton className="h-10" />
            </div>
            <Skeleton className="h-10 w-full" />
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex justify-center">
      <div className="w-full max-w-xl space-y-6">
        <h1 className="text-2xl font-bold text-foreground">
          {isEdit ? 'Edit Cycle' : 'Create New Cycle'}
        </h1>

        <Card>
          <CardContent className="pt-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <Alert variant="destructive">
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            {/* Project */}
            <div className="space-y-2">
              <Label htmlFor="project">Project *</Label>
              <Select
                value={formData.projectId?.toString() || ''}
                onValueChange={(value) => {
                  setFormData({ ...formData, projectId: parseInt(value) });
                  setFieldErrors((prev) => ({ ...prev, projectId: '' }));
                }}
                disabled={projectsLoading}
              >
                <SelectTrigger className={fieldErrors.projectId ? 'border-destructive' : ''}>
                  <SelectValue placeholder={projectsLoading ? 'Loading projects...' : 'Select project'} />
                </SelectTrigger>
                <SelectContent>
                  {projects.length === 0 ? (
                    <SelectItem value="none" disabled>No projects available</SelectItem>
                  ) : (
                    projects.map((project) => (
                      <SelectItem key={project.id} value={project.id.toString()}>
                        {project.name} ({project.projectKey})
                      </SelectItem>
                    ))
                  )}
                </SelectContent>
              </Select>
              {fieldErrors.projectId && (
                <p className="text-xs text-destructive">{fieldErrors.projectId}</p>
              )}
            </div>

            {/* Cycle Name */}
            <div className="space-y-2">
              <Label htmlFor="name">Cycle Name *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => {
                  setFormData({ ...formData, name: e.target.value });
                  setFieldErrors((prev) => ({ ...prev, name: '' }));
                }}
                placeholder="e.g., Q1 2025 - Feature Sprint"
                className={fieldErrors.name ? 'border-destructive' : ''}
              />
              {fieldErrors.name ? (
                <p className="text-xs text-destructive">{fieldErrors.name}</p>
              ) : (
                <p className="text-xs text-muted-foreground">Give your cycle a descriptive name</p>
              )}
            </div>

            {/* Date Range */}
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="startDate">Start Date *</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={(e) => {
                    setStartDate(e.target.value);
                    setFieldErrors((prev) => ({ ...prev, startDate: '' }));
                  }}
                  className={fieldErrors.startDate ? 'border-destructive' : ''}
                />
                {fieldErrors.startDate && (
                  <p className="text-xs text-destructive">{fieldErrors.startDate}</p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="endDate">End Date *</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={endDate}
                  onChange={(e) => {
                    setEndDate(e.target.value);
                    setFieldErrors((prev) => ({ ...prev, endDate: '' }));
                  }}
                  min={startDate || undefined}
                  className={fieldErrors.endDate ? 'border-destructive' : ''}
                />
                {fieldErrors.endDate && (
                  <p className="text-xs text-destructive">{fieldErrors.endDate}</p>
                )}
              </div>
            </div>

            {/* Phase */}
            <div className="space-y-2">
              <Label>Phase</Label>
              <Select
                value={formData.phase}
                onValueChange={(value) => setFormData({ ...formData, phase: value as CyclePhase })}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SHAPING">Shaping</SelectItem>
                  <SelectItem value="BETTING">Betting</SelectItem>
                  <SelectItem value="BUILD">Build</SelectItem>
                  <SelectItem value="COOLDOWN">Cooldown</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {/* Actions */}
            <div className="flex justify-end gap-3 pt-4">
              <Button 
                type="button" 
                variant="outline" 
                onClick={() => navigate('/cycles')} 
                disabled={loading}
              >
                Cancel
              </Button>
              <LoadingButton
                type="submit"
                loading={loading}
                loadingText="Saving..."
              >
                {isEdit ? 'Update Cycle' : 'Create Cycle'}
              </LoadingButton>
            </div>
          </form>
        </CardContent>
      </Card>
      </div>
    </div>
  );
}
