import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Plus,
  Pencil,
  Trash2,
  Folder,
  Archive,
  ArchiveRestore,
  TrendingUp,
  Play,
  Loader2,
  Search,
  ArrowUpDown,
  Eye,
  Layers,
  Kanban,
  ListTodo,
} from 'lucide-react';
import { Card, CardContent, CardFooter } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Project, CreateProjectRequest } from '../types';
import projectService from '../services/projectService';
import { useToast, useProject } from '../contexts';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { getUserFriendlyError } from '../utils/errorMessages';
import LoadingButton from '../components/LoadingButton';
import EmptyState from '../components/EmptyState';
import { EmptyProjectsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

// Predefined colors for project selection
const PROJECT_COLORS = [
  '#4A90D9', '#50C878', '#FF6B6B', '#9B59B6', '#F39C12',
  '#1ABC9C', '#E74C3C', '#3498DB', '#2ECC71', '#E67E22',
];

export default function Projects() {
  const { t } = useTranslation();
  const [projects, setProjects] = useState<Project[]>([]);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingProject, setEditingProject] = useState<Project | null>(null);
  const [showArchived, setShowArchived] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'key' | 'cycles' | 'recent'>('name');
  const [formData, setFormData] = useState<CreateProjectRequest>({
    name: '',
    projectKey: '',
    description: '',
    color: PROJECT_COLORS[0],
    projectType: 'SHAPE_UP',
  });
  const [saving, setSaving] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; project: Project | null }>({
    open: false,
    project: null,
  });
  const { showToast } = useToast();
  const { refreshProjects } = useProject();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();

  useEffect(() => {
    const abortController = new AbortController();
    loadProjects();
    return () => abortController.abort();
  }, [showArchived]);

  const loadProjects = async () => {
    try {
      setLoading(true);
      // Use scoped access - getMyProjects returns all accessible projects (including inactive for managers)
      // getActive returns only active projects the user has access to
      const data = showArchived
        ? await projectService.getMyProjects()
        : await projectService.getActive();
      setProjects(data);
    } catch (error: any) {
      if (error.name !== 'CanceledError') {
        showToast(t('projects.loadFailed'), 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleOpenDialog = useCallback((project?: Project) => {
    if (project) {
      setEditingProject(project);
      setFormData({
        name: project.name,
        projectKey: project.projectKey,
        description: project.description || '',
        color: project.color || PROJECT_COLORS[0],
        logoUrl: project.logoUrl,
        ownerId: project.ownerId,
        projectType: project.projectType || 'SHAPE_UP',
      });
    } else {
      setEditingProject(null);
      setFormData({
        name: '',
        projectKey: '',
        description: '',
        color: PROJECT_COLORS[Math.floor(Math.random() * PROJECT_COLORS.length)],
        projectType: 'SHAPE_UP',
      });
    }
    setFieldErrors({});
    setDialogOpen(true);
  }, []);

  // Handle edit query parameter
  useEffect(() => {
    const editId = searchParams.get('edit');
    if (editId && projects.length > 0) {
      const projectToEdit = projects.find(p => p.id.toString() === editId);
      if (projectToEdit) {
        handleOpenDialog(projectToEdit);
        // Remove the query parameter after opening the dialog
        setSearchParams({}, { replace: true });
      }
    }
  }, [searchParams, projects, handleOpenDialog, setSearchParams]);

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingProject(null);
    setFieldErrors({});
  };

  // Validate form
  const validateForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!formData.name.trim()) {
      errors.name = t('projects.errors.nameRequired');
    } else if (formData.name.trim().length < 2) {
      errors.name = 'Project name must be at least 2 characters';
    }

    if (!formData.projectKey.trim()) {
      errors.projectKey = t('projects.errors.keyRequired');
    } else if (!/^[A-Z0-9]{2,10}$/.test(formData.projectKey.trim())) {
      errors.projectKey = t('projects.errors.keyFormat');
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSave = async () => {
    if (!validateForm()) {
      return;
    }

    try {
      setSaving(true);
      if (editingProject) {
        await projectService.update(editingProject.id, formData);
        showToast(t('projects.updateSuccess'), 'success');
      } else {
        await projectService.create(formData);
        showToast(t('projects.createSuccess'), 'success');
      }
      handleCloseDialog();
      loadProjects();
      refreshProjects(); // Refresh toolbar project list
    } catch (error) {
      showToast(getUserFriendlyError(error, t('projects.errors.saveFailed')), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.project) return;
    try {
      await projectService.delete(deleteDialog.project.id);
      showToast(t('projects.deleteSuccess'), 'success');
      setDeleteDialog({ open: false, project: null });
      loadProjects();
      refreshProjects(); // Refresh toolbar project list
    } catch (error) {
      showToast(getUserFriendlyError(error, t('projects.errors.deleteFailed')), 'error');
    }
  };

  const handleToggleArchive = async (project: Project) => {
    try {
      if (project.isActive) {
        await projectService.deactivate(project.id);
        showToast(t('projects.archiveSuccess'), 'success');
      } else {
        await projectService.activate(project.id);
        showToast(t('projects.activateSuccess'), 'success');
      }
      loadProjects();
      refreshProjects(); // Refresh toolbar project list
    } catch (error) {
      showToast(getUserFriendlyError(error, t('projects.errors.updateStatusFailed')), 'error');
    }
  };

  const handleViewCycles = (project: Project) => {
    // For Kanban projects, navigate to backlog instead of cycles
    if (project.projectType === 'KANBAN') {
      navigate(`/backlog`);
    } else {
      navigate(`/cycles?project=${project.id}`);
    }
  };

  const handleViewDetails = (project: Project, e?: React.MouseEvent) => {
    if (e) e.stopPropagation();
    navigate(`/projects/${project.id}`);
  };

  const handleCardClick = (project: Project) => {
    navigate(`/projects/${project.id}`);
  };

  const generateProjectKey = (name: string) => {
    // Auto-generate project key from name
    const words = name.trim().split(/\s+/);
    let key = '';
    if (words.length === 1) {
      key = words[0].substring(0, 4).toUpperCase();
    } else {
      key = words.map(w => w[0]).join('').toUpperCase().substring(0, 6);
    }
    return key.replace(/[^A-Z0-9]/g, '');
  };

  const handleNameChange = (name: string) => {
    setFormData(prev => ({
      ...prev,
      name,
      projectKey: editingProject ? prev.projectKey : generateProjectKey(name),
    }));
  };

  // Filter and sort projects
  const filteredAndSortedProjects = projects
    .filter(project => {
      if (!searchTerm) return true;
      const search = searchTerm.toLowerCase();
      return (
        project.name.toLowerCase().includes(search) ||
        project.projectKey.toLowerCase().includes(search) ||
        project.description?.toLowerCase().includes(search) ||
        project.ownerName?.toLowerCase().includes(search)
      );
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'key':
          return a.projectKey.localeCompare(b.projectKey);
        case 'cycles':
          return (b.cycleCount || 0) - (a.cycleCount || 0);
        case 'recent':
          return (b.id || 0) - (a.id || 0);
        default:
          return 0;
      }
    });

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[60vh]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex flex-col gap-4 mb-6">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Folder className="h-6 w-6" />
            {t('projects.title')}
          </h1>
          <div className="flex flex-col sm:flex-row gap-2">
            <Button
              variant={showArchived ? 'default' : 'outline'}
              onClick={() => setShowArchived(!showArchived)}
              size="sm"
            >
              {showArchived ? t('projects.showActiveOnly') : t('projects.showAll')}
            </Button>
            <Button
              onClick={() => handleOpenDialog()}
              data-tour="new-project-btn"
              size="sm"
            >
              <Plus className="h-4 w-4 mr-2" />
              {t('projects.newProject')}
            </Button>
          </div>
        </div>

        {/* Search and Sort Controls */}
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={t('projects.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
          <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <ArrowUpDown className="h-4 w-4 mr-2" />
              <SelectValue placeholder={t('projects.sortBy')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">{t('projects.sortByName')}</SelectItem>
              <SelectItem value="key">{t('projects.sortByKey')}</SelectItem>
              <SelectItem value="cycles">{t('projects.sortByCycles')}</SelectItem>
              <SelectItem value="recent">{t('projects.sortByRecent')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {filteredAndSortedProjects.length === 0 ? (
        <Card className="p-8">
          <EmptyState
            illustration={<EmptyProjectsIllustration />}
            title={searchTerm ? t('projects.noProjectsFound') : t('projects.noProjects')}
            description={
              searchTerm
                ? `${t('projects.noProjectsMatch')} "${searchTerm}". ${t('projects.tryDifferentSearch')}`
                : t('projects.noProjectsDescription')
            }
            action={
              !searchTerm
                ? {
                    label: t('projects.createFirstProject'),
                    onClick: () => handleOpenDialog(),
                    startIcon: <Plus className="h-4 w-4" />,
                  }
                : undefined
            }
            size="large"
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredAndSortedProjects.map((project, index) => (
            <Card
              key={project.id}
              data-tour={index === 0 ? 'project-card' : undefined}
              className={cn(
                'flex flex-col cursor-pointer transition-all hover:shadow-lg hover:scale-[1.02]',
                !project.isActive && 'opacity-70'
              )}
              style={{ borderLeftWidth: 4, borderLeftColor: project.color || '#4A90D9' }}
              onClick={() => handleCardClick(project)}
            >
              <CardContent className="flex-1 pt-6">
                <div className="flex items-center mb-4">
                  <Avatar
                    className="mr-4"
                    style={{ backgroundColor: project.color || '#4A90D9' }}
                  >
                    <AvatarFallback className="text-white font-semibold">
                      {project.projectKey.substring(0, 2)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <h3 className="font-semibold text-lg">{project.name}</h3>
                    <Badge variant="secondary" className="mt-1">
                      {project.projectKey}
                    </Badge>
                  </div>
                </div>

                {project.description && (
                  <p className="text-sm text-muted-foreground mb-4 line-clamp-2">
                    {project.description}
                  </p>
                )}

                <div className="flex flex-wrap gap-2">
                  {project.projectType === 'KANBAN' ? (
                    <Badge variant="outline" className="flex items-center gap-1 text-purple-600 border-purple-600">
                      <Kanban className="h-3 w-3" />
                      {t('projects.kanban')}
                    </Badge>
                  ) : (
                    <>
                      <Badge variant="outline" className="flex items-center gap-1">
                        <TrendingUp className="h-3 w-3" />
                        {project.cycleCount || 0} {t('projects.cycles')}
                      </Badge>
                      {project.activeCycleCount !== undefined && project.activeCycleCount > 0 && (
                        <Badge variant="outline" className="flex items-center gap-1 text-green-600 border-green-600">
                          <Play className="h-3 w-3" />
                          {project.activeCycleCount} {t('projects.cycleActive')}
                        </Badge>
                      )}
                    </>
                  )}
                  {!project.isActive && (
                    <Badge variant="secondary">{t('projects.archived')}</Badge>
                  )}
                </div>

                {project.ownerName && (
                  <p className="text-xs text-muted-foreground mt-2">
                    {t('projects.owner')}: {project.ownerName}
                  </p>
                )}
              </CardContent>

              <CardFooter className="justify-end gap-1 pt-0">
                <TooltipProvider>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleViewDetails(project);
                        }}
                        aria-label={t('projects.viewDetailsFor', { name: project.name })}
                      >
                        <Eye className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t('projects.viewDetails')}</TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleViewCycles(project);
                        }}
                        aria-label={project.projectType === 'KANBAN' 
                          ? t('projects.viewBacklogFor', { name: project.name })
                          : t('projects.viewCyclesFor', { name: project.name })}
                      >
                        {project.projectType === 'KANBAN' ? (
                          <ListTodo className="h-4 w-4" aria-hidden="true" />
                        ) : (
                          <TrendingUp className="h-4 w-4" aria-hidden="true" />
                        )}
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>
                      {project.projectType === 'KANBAN' ? t('projects.viewBacklog') : t('projects.viewCycles')}
                    </TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleOpenDialog(project);
                        }}
                        aria-label={t('projects.editProjectFor', { name: project.name })}
                      >
                        <Pencil className="h-4 w-4" aria-hidden="true" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t('common.edit')}</TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button 
                        variant="ghost" 
                        size="icon" 
                        onClick={(e) => {
                          e.stopPropagation();
                          handleToggleArchive(project);
                        }}
                        aria-label={project.isActive ? `Archive ${project.name} project` : `Restore ${project.name} project`}
                      >
                        {project.isActive ? (
                          <Archive className="h-4 w-4" aria-hidden="true" />
                        ) : (
                          <ArchiveRestore className="h-4 w-4" />
                        )}
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{project.isActive ? t('projects.archive') : t('projects.activate')}</TooltipContent>
                  </Tooltip>
                  <Tooltip>
                    <TooltipTrigger asChild>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={(e) => {
                          e.stopPropagation();
                          setDeleteDialog({ open: true, project });
                        }}
                        disabled={project.projectType === 'SHAPE_UP' && (project.cycleCount || 0) > 0}
                        className="text-destructive hover:text-destructive"
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TooltipTrigger>
                    <TooltipContent>{t('common.delete')}</TooltipContent>
                  </Tooltip>
                </TooltipProvider>
              </CardFooter>
            </Card>
          ))}
        </div>
      )}

      {/* Create/Edit Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md max-h-[90vh] flex flex-col">
          <DialogHeader>
            <DialogTitle>
              {editingProject ? t('projects.editProject') : t('projects.createNewProject')}
            </DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4 overflow-y-auto flex-1">
            <div className="space-y-2">
              <Label htmlFor="name">{t('projects.projectName')} *</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => {
                  handleNameChange(e.target.value);
                  setFieldErrors((prev) => ({ ...prev, name: '' }));
                }}
                placeholder={t('projects.namePlaceholder')}
              />
              {fieldErrors.name ? (
                <p className="text-xs text-destructive">{fieldErrors.name}</p>
              ) : (
                <p className="text-xs text-muted-foreground">{t('projects.nameDescriptive')}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label htmlFor="projectKey">{t('projects.projectKey')} *</Label>
              <Input
                id="projectKey"
                value={formData.projectKey}
                onChange={(e) => {
                  setFormData({ ...formData, projectKey: e.target.value.toUpperCase() });
                  setFieldErrors((prev) => ({ ...prev, projectKey: '' }));
                }}
                maxLength={10}
                className="uppercase"
                disabled={!!editingProject}
              />
              {fieldErrors.projectKey ? (
                <p className="text-xs text-destructive">{fieldErrors.projectKey}</p>
              ) : (
                <p className="text-xs text-muted-foreground">{t('projects.keyFormat')}</p>
              )}
            </div>
            <div className="space-y-2">
              <Label>{t('projects.projectType')} *</Label>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, projectType: 'SHAPE_UP' })}
                  className={cn(
                    'flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all',
                    formData.projectType === 'SHAPE_UP'
                      ? 'border-primary bg-primary/10'
                      : 'border-muted hover:border-muted-foreground/50'
                  )}
                >
                  <Layers className={cn(
                    'h-8 w-8',
                    formData.projectType === 'SHAPE_UP' ? 'text-primary' : 'text-muted-foreground'
                  )} />
                  <span className={cn(
                    'font-medium text-sm',
                    formData.projectType === 'SHAPE_UP' ? 'text-primary' : 'text-muted-foreground'
                  )}>
                    {t('projects.shapeUp')}
                  </span>
                  <span className="text-xs text-muted-foreground text-center">
                    {t('projects.shapeUpDescription')}
                  </span>
                </button>
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, projectType: 'KANBAN' })}
                  className={cn(
                    'flex flex-col items-center gap-2 p-4 rounded-lg border-2 transition-all',
                    formData.projectType === 'KANBAN'
                      ? 'border-primary bg-primary/10'
                      : 'border-muted hover:border-muted-foreground/50'
                  )}
                >
                  <Kanban className={cn(
                    'h-8 w-8',
                    formData.projectType === 'KANBAN' ? 'text-primary' : 'text-muted-foreground'
                  )} />
                  <span className={cn(
                    'font-medium text-sm',
                    formData.projectType === 'KANBAN' ? 'text-primary' : 'text-muted-foreground'
                  )}>
                    {t('projects.kanban')}
                  </span>
                  <span className="text-xs text-muted-foreground text-center">
                    {t('projects.kanbanDescription')}
                  </span>
                </button>
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">{t('common.description')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label>{t('projects.projectColor')}</Label>
              <div className="flex flex-wrap gap-2" role="radiogroup" aria-label={t('projects.selectColor')}>
                {PROJECT_COLORS.map((color) => (
                  <button
                    key={color}
                    type="button"
                    role="radio"
                    onClick={() => setFormData({ ...formData, color })}
                    className={cn(
                      'w-9 h-9 rounded-full transition-transform hover:scale-110 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2',
                      formData.color === color && 'ring-2 ring-offset-2 ring-foreground'
                    )}
                    style={{ backgroundColor: color }}
                    aria-label={t('projects.selectColorValue', { color })}
                    aria-checked={formData.color === color}
                  />
                ))}
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="logoUrl">{t('projects.logoUrl')}</Label>
              <Input
                id="logoUrl"
                value={formData.logoUrl || ''}
                onChange={(e) => setFormData({ ...formData, logoUrl: e.target.value })}
                placeholder={t('projects.logoUrlPlaceholder')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog} disabled={saving}>
              {t('common.cancel')}
            </Button>
            <LoadingButton
              onClick={handleSave}
              loading={saving}
              loadingText={t('projects.saving')}
            >
              {editingProject ? t('common.update') : t('common.create')}
            </LoadingButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ open, project: deleteDialog.project })}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('projects.deleteProject')}</DialogTitle>
            <DialogDescription>
              {t('projects.confirmDelete')} "{deleteDialog.project?.name}"? {t('projects.confirmDeleteDescription')}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, project: null })}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleDelete}>
              {t('common.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
