import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import { cycleService } from '../services/cycleService';
import projectService from '../services/projectService';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { CreateCycleRequest, CyclePhase, Project } from '../types';
import { useProject, useToast, useAuth } from '../contexts';
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
  const { user } = useAuth();

  const [formData, setFormData] = useState<CreateCycleRequest>({
    projectId: currentProject?.id || 0,
    name: '',
    startDate: '',
    endDate: '',
    phase: 'BUILD',
  });
  const [startDate, setStartDate] = useState<string>('');
  const [endDate, setEndDate] = useState<string>('');
  const [useAutoEndDate, setUseAutoEndDate] = useState<boolean>(true);
  const [defaultCycleLengthWeeks, setDefaultCycleLengthWeeks] = useState<number>(6);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [loadingData, setLoadingData] = useState(isEdit);
  const [projects, setProjects] = useState<Project[]>([]);
  const [projectsLoading, setProjectsLoading] = useState(true);

  // Check if user can override cycle dates
  const canOverrideDates = user?.role === 'ADMIN' || user?.role === 'PROJECT_MANAGER';

  useEffect(() => {
    loadProjects();
    loadOrganizationSettings();
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

  // Helper function to calculate end date from start date
  const calculateEndDate = (startDateStr: string): string => {
    const start = new Date(startDateStr);
    const calculatedEnd = new Date(start);
    calculatedEnd.setDate(calculatedEnd.getDate() + (defaultCycleLengthWeeks * 7));
    return calculatedEnd.toISOString().split('T')[0];
  };

  // Auto-calculate end date when start date changes and auto mode is enabled
  useEffect(() => {
    if (useAutoEndDate && startDate && !isEdit) {
      setEndDate(calculateEndDate(startDate));
    }
  }, [startDate, useAutoEndDate, defaultCycleLengthWeeks, isEdit]);

  const loadOrganizationSettings = async () => {
    try {
      const response = await organizationSettingsService.getSettings();
      setDefaultCycleLengthWeeks(response.data.defaultCycleLengthWeeks || 6);
    } catch (err) {
      // Fallback to default if settings can't be loaded
      setDefaultCycleLengthWeeks(6);
    }
  };

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
      setUseAutoEndDate(false); // Editing always uses manual mode
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

    // End date is only required if not using auto-calculation or if user can override
    if (!useAutoEndDate && !endDate) {
      errors.endDate = 'End date is required when not using auto-calculation';
    } else if (endDate && startDate && endDate < startDate) {
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
      // Only send endDate if not using auto-calculation or if editing
      endDate: useAutoEndDate && !isEdit ? undefined : endDate,
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
            <div className="space-y-4">
              {/* Auto-calculation toggle (only for create mode and if user has permission) */}
              {!isEdit && canOverrideDates && (
                <div className="flex items-center gap-3 p-3 rounded-lg bg-muted/50 border">
                  <input
                    type="checkbox"
                    id="useAutoEndDate"
                    checked={useAutoEndDate}
                    onChange={(e) => {
                      setUseAutoEndDate(e.target.checked);
                      if (e.target.checked && startDate) {
                        // Recalculate end date
                        setEndDate(calculateEndDate(startDate));
                      }
                    }}
                    className="h-4 w-4 rounded border-gray-300"
                  />
                  <Label htmlFor="useAutoEndDate" className="cursor-pointer text-sm font-normal">
                    Auto-calculate end date ({defaultCycleLengthWeeks} weeks from start date)
                  </Label>
                </div>
              )}

              {/* Info for users who can't override */}
              {!isEdit && !canOverrideDates && (
                <Alert>
                  <AlertDescription className="text-sm">
                    End date will be automatically calculated as {defaultCycleLengthWeeks} weeks from the start date based on organization settings.
                  </AlertDescription>
                </Alert>
              )}

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
                  <Label htmlFor="endDate">
                    End Date {useAutoEndDate && !isEdit ? '(Auto)' : '*'}
                  </Label>
                  <Input
                    id="endDate"
                    type="date"
                    value={endDate}
                    onChange={(e) => {
                      setEndDate(e.target.value);
                      setFieldErrors((prev) => ({ ...prev, endDate: '' }));
                    }}
                    min={startDate || undefined}
                    disabled={useAutoEndDate && !isEdit}
                    aria-label={
                      useAutoEndDate && !isEdit
                        ? 'End date - automatically calculated from start date'
                        : 'End date'
                    }
                    aria-describedby={
                      useAutoEndDate && !isEdit ? 'endDate-auto-hint' : undefined
                    }
                    className={fieldErrors.endDate ? 'border-destructive' : ''}
                  />
                  {fieldErrors.endDate && (
                    <p className="text-xs text-destructive">{fieldErrors.endDate}</p>
                  )}
                  {useAutoEndDate && !isEdit && (
                    <p id="endDate-auto-hint" className="text-xs text-muted-foreground">
                      Automatically calculated from start date
                    </p>
                  )}
                </div>
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
