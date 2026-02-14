import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  Plus,
  Pencil,
  Trash2,
  Layers,
  Calendar,
  FileText,
  Search,
  MoreVertical,
  Target,
} from 'lucide-react';
import { epicService } from '../services/epicService';
import { Epic, EpicStatus } from '../types';
import { useProject, useToast } from '../contexts';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { Progress } from '../components/ui/progress';
import { Skeleton } from '../components/ui/skeleton';

const getStatusBadgeVariant = (status: EpicStatus): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'COMPLETED': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'PLANNED': return 'outline';
    case 'ON_HOLD': return 'destructive';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function EpicListPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [epics, setEpics] = useState<Epic[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; epic: Epic | null }>({
    open: false,
    epic: null,
  });
  
  // Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');
  const [filterInitiative, setFilterInitiative] = useState<string>('all');

  useEffect(() => {
    if (!currentProject || isAllProjectsSelected) {
      setEpics([]);
      setLoading(false);
      return;
    }
    loadEpics();
  }, [currentProject, isAllProjectsSelected]);

  const loadEpics = async () => {
    if (!currentProject) return;
    try {
      setLoading(true);
      const response = await epicService.getByProject(currentProject.id);
      setEpics(response.data);
    } catch (error) {
      console.error('Failed to load epics:', error);
      showError(t('epics.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.epic) return;
    try {
      await epicService.delete(deleteDialog.epic.id);
      showSuccess(t('epics.deleted'));
      setDeleteDialog({ open: false, epic: null });
      loadEpics();
    } catch (error) {
      showError(t('epics.deleteError'));
      console.error('Failed to delete epic:', error);
    }
  };

  const uniqueInitiatives = [...new Set(epics.filter(e => e.initiativeName).map(e => e.initiativeName!))];

  const filteredEpics = epics.filter((epic) => {
    const matchesSearch =
      searchTerm === '' ||
      epic.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (epic.description && epic.description.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesStatus = filterStatus === 'all' || epic.status === filterStatus;
    const matchesInitiative = 
      filterInitiative === 'all' ||
      (filterInitiative === 'orphan' && !epic.initiativeId) ||
      epic.initiativeName === filterInitiative;
    
    return matchesSearch && matchesStatus && matchesInitiative;
  });

  if (isAllProjectsSelected) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="py-12 text-center">
            <Layers className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('epics.selectProject')}</h3>
            <p className="text-muted-foreground">{t('epics.selectProjectDescription')}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="container mx-auto py-8 space-y-4">
        <div className="flex justify-between">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-48" />
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Layers className="h-6 w-6" />
            {t('epics.title')}
          </h1>
          <p className="text-muted-foreground">{t('epics.description')}</p>
        </div>
        <Button onClick={() => navigate('/epics/new')}>
          <Plus className="h-4 w-4 mr-2" />
          {t('epics.create')}
        </Button>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder={t('epics.search')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder={t('epics.filterStatus')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('common.all')}</SelectItem>
            <SelectItem value="DRAFT">{t('epics.status.draft')}</SelectItem>
            <SelectItem value="PLANNED">{t('epics.status.planned')}</SelectItem>
            <SelectItem value="IN_PROGRESS">{t('epics.status.inProgress')}</SelectItem>
            <SelectItem value="COMPLETED">{t('epics.status.completed')}</SelectItem>
            <SelectItem value="ON_HOLD">{t('epics.status.onHold')}</SelectItem>
          </SelectContent>
        </Select>
        <Select value={filterInitiative} onValueChange={setFilterInitiative}>
          <SelectTrigger className="w-48">
            <SelectValue placeholder={t('epics.filterInitiative')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('common.all')}</SelectItem>
            <SelectItem value="orphan">{t('epics.noInitiative')}</SelectItem>
            {uniqueInitiatives.map((name) => (
              <SelectItem key={name} value={name}>{name}</SelectItem>
            ))}
          </SelectContent>
        </Select>
      </div>

      {/* Epic Cards */}
      {filteredEpics.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Layers className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('epics.noEpics')}</h3>
            <p className="text-muted-foreground mb-4">{t('epics.noEpicsDescription')}</p>
            <Button onClick={() => navigate('/epics/new')}>
              <Plus className="h-4 w-4 mr-2" />
              {t('epics.createFirst')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredEpics.map((epic) => (
            <Card key={epic.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-4 h-4 rounded-full"
                      style={{ backgroundColor: epic.color || '#6366f1' }}
                    />
                    <CardTitle className="text-lg">
                      <Link to={`/epics/${epic.id}`} className="hover:underline">
                        {epic.name}
                      </Link>
                    </CardTitle>
                  </div>
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem onClick={() => navigate(`/epics/${epic.id}/edit`)}>
                        <Pencil className="h-4 w-4 mr-2" />
                        {t('common.edit')}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-destructive"
                        onClick={() => setDeleteDialog({ open: true, epic })}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {t('common.delete')}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
                <div className="flex items-center gap-2">
                  <Badge variant={getStatusBadgeVariant(epic.status)}>
                    {t(`epics.status.${epic.status.toLowerCase()}`)}
                  </Badge>
                  {epic.initiativeName && (
                    <Badge variant="outline" className="text-xs">
                      <Target className="h-3 w-3 mr-1" />
                      {epic.initiativeName}
                    </Badge>
                  )}
                </div>
              </CardHeader>
              <CardContent>
                {epic.description && (
                  <CardDescription className="line-clamp-2 mb-4">
                    {epic.description}
                  </CardDescription>
                )}
                
                <div className="space-y-3">
                  {/* Date range */}
                  {(epic.targetStartDate || epic.targetEndDate) && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Calendar className="h-4 w-4" />
                      <span>
                        {epic.targetStartDate && formatLocalizedDate(epic.targetStartDate, i18n.language)}
                        {epic.targetStartDate && epic.targetEndDate && ' - '}
                        {epic.targetEndDate && formatLocalizedDate(epic.targetEndDate, i18n.language)}
                      </span>
                    </div>
                  )}
                  
                  {/* Pitch count */}
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <FileText className="h-4 w-4" />
                    <span>
                      {epic.pitchCount || 0} {t('epics.pitches')}
                      {epic.completedPitchCount !== undefined && (
                        <span className="text-green-600 ml-1">
                          ({epic.completedPitchCount} {t('epics.completed')})
                        </span>
                      )}
                    </span>
                  </div>
                  
                  {/* Progress */}
                  {epic.progressPercentage !== undefined && (
                    <div className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span>{t('epics.progress')}</span>
                        <span>{Math.round(epic.progressPercentage)}%</span>
                      </div>
                      <Progress value={epic.progressPercentage} className="h-2" />
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Delete Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ ...deleteDialog, open })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('epics.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('epics.deleteWarning', { name: deleteDialog.epic?.name })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, epic: null })}>
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
