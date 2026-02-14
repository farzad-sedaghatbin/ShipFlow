import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  Plus,
  Pencil,
  Trash2,
  Target,
  Calendar,
  Layers,
  Search,
  MoreVertical,
} from 'lucide-react';
import { initiativeService } from '../services/initiativeService';
import { Initiative, InitiativeStatus } from '../types';
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

const getStatusBadgeVariant = (status: InitiativeStatus): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'COMPLETED': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'PLANNED': return 'outline';
    case 'ON_HOLD': return 'destructive';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

export default function InitiativeListPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [initiatives, setInitiatives] = useState<Initiative[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; initiative: Initiative | null }>({
    open: false,
    initiative: null,
  });
  
  // Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    if (!currentProject || isAllProjectsSelected) {
      setInitiatives([]);
      setLoading(false);
      return;
    }
    loadInitiatives();
  }, [currentProject, isAllProjectsSelected]);

  const loadInitiatives = async () => {
    if (!currentProject) return;
    try {
      setLoading(true);
      const response = await initiativeService.getByProject(currentProject.id);
      setInitiatives(response.data);
    } catch (error) {
      console.error('Failed to load initiatives:', error);
      showError(t('initiatives.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.initiative) return;
    try {
      await initiativeService.delete(deleteDialog.initiative.id);
      showSuccess(t('initiatives.deleted'));
      setDeleteDialog({ open: false, initiative: null });
      loadInitiatives();
    } catch (error) {
      showError(t('initiatives.deleteError'));
      console.error('Failed to delete initiative:', error);
    }
  };

  const filteredInitiatives = initiatives.filter((initiative) => {
    const matchesSearch =
      searchTerm === '' ||
      initiative.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (initiative.description && initiative.description.toLowerCase().includes(searchTerm.toLowerCase()));
    
    const matchesStatus = filterStatus === 'all' || initiative.status === filterStatus;
    
    return matchesSearch && matchesStatus;
  });

  if (isAllProjectsSelected) {
    return (
      <div className="container mx-auto py-8">
        <Card>
          <CardContent className="py-12 text-center">
            <Target className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('initiatives.selectProject')}</h3>
            <p className="text-muted-foreground">{t('initiatives.selectProjectDescription')}</p>
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
            <Target className="h-6 w-6" />
            {t('initiatives.title')}
          </h1>
          <p className="text-muted-foreground">{t('initiatives.description')}</p>
        </div>
        <Button onClick={() => navigate('/initiatives/new')}>
          <Plus className="h-4 w-4 mr-2" />
          {t('initiatives.create')}
        </Button>
      </div>

      {/* Filters */}
      <div className="flex gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder={t('initiatives.search')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder={t('initiatives.filterStatus')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('common.all')}</SelectItem>
            <SelectItem value="DRAFT">{t('initiatives.status.draft')}</SelectItem>
            <SelectItem value="PLANNED">{t('initiatives.status.planned')}</SelectItem>
            <SelectItem value="IN_PROGRESS">{t('initiatives.status.inProgress')}</SelectItem>
            <SelectItem value="COMPLETED">{t('initiatives.status.completed')}</SelectItem>
            <SelectItem value="ON_HOLD">{t('initiatives.status.onHold')}</SelectItem>
            <SelectItem value="CANCELLED">{t('initiatives.status.cancelled')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {/* Initiative Cards */}
      {filteredInitiatives.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Target className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('initiatives.noInitiatives')}</h3>
            <p className="text-muted-foreground mb-4">{t('initiatives.noInitiativesDescription')}</p>
            <Button onClick={() => navigate('/initiatives/new')}>
              <Plus className="h-4 w-4 mr-2" />
              {t('initiatives.createFirst')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {filteredInitiatives.map((initiative) => (
            <Card key={initiative.id} className="hover:shadow-md transition-shadow">
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-4 h-4 rounded-full"
                      style={{ backgroundColor: initiative.color || '#6366f1' }}
                    />
                    <CardTitle className="text-lg">
                      <Link
                        to={`/initiatives/${initiative.id}`}
                        className="hover:underline"
                      >
                        {initiative.name}
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
                      <DropdownMenuItem onClick={() => navigate(`/initiatives/${initiative.id}/edit`)}>
                        <Pencil className="h-4 w-4 mr-2" />
                        {t('common.edit')}
                      </DropdownMenuItem>
                      <DropdownMenuItem
                        className="text-destructive"
                        onClick={() => setDeleteDialog({ open: true, initiative })}
                      >
                        <Trash2 className="h-4 w-4 mr-2" />
                        {t('common.delete')}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
                <Badge variant={getStatusBadgeVariant(initiative.status)}>
                  {t(`initiatives.status.${initiative.status.toLowerCase()}`)}
                </Badge>
              </CardHeader>
              <CardContent>
                {initiative.description && (
                  <CardDescription className="line-clamp-2 mb-4">
                    {initiative.description}
                  </CardDescription>
                )}
                
                <div className="space-y-3">
                  {/* Date range */}
                  {(initiative.targetStartDate || initiative.targetEndDate) && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                      <Calendar className="h-4 w-4" />
                      <span>
                        {initiative.targetStartDate && formatLocalizedDate(initiative.targetStartDate, i18n.language)}
                        {initiative.targetStartDate && initiative.targetEndDate && ' - '}
                        {initiative.targetEndDate && formatLocalizedDate(initiative.targetEndDate, i18n.language)}
                      </span>
                    </div>
                  )}
                  
                  {/* Epic count */}
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <Layers className="h-4 w-4" />
                    <span>
                      {initiative.epicCount || 0} {t('initiatives.epics')}
                      {initiative.completedEpicCount !== undefined && (
                        <span className="text-green-600 ml-1">
                          ({initiative.completedEpicCount} {t('initiatives.completed')})
                        </span>
                      )}
                    </span>
                  </div>
                  
                  {/* Progress */}
                  {initiative.progressPercentage !== undefined && (
                    <div className="space-y-1">
                      <div className="flex justify-between text-sm">
                        <span>{t('initiatives.progress')}</span>
                        <span>{Math.round(initiative.progressPercentage)}%</span>
                      </div>
                      <Progress value={initiative.progressPercentage} className="h-2" />
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
            <DialogTitle>{t('initiatives.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('initiatives.deleteWarning', { name: deleteDialog.initiative?.name })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, initiative: null })}>
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
