import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  CooldownActivityDTO,
  CooldownActivityType,
  CooldownActivityStatus,
  CooldownSummaryDTO,
  cooldownActivityService,
} from '../services/cooldownActivityService';
import { useToast } from '../contexts';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../components/ui/dropdown-menu';
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle } from '../components/ui/alert-dialog';
import CooldownActivityDialog from '../components/CooldownActivityDialog';
import { Plus, MoreVertical, Pencil, Trash2, Loader2, CheckCircle, Clock, PlayCircle, XCircle, TrendingUp, ListChecks } from 'lucide-react';
import { safeParseId } from '../utils/validation';

const getActivityTypeIcon = (type: CooldownActivityType) => {
  const icons = {
    BUG_FIX: '🐛',
    TECH_DEBT: '🔧',
    RESEARCH: '🔬',
    EXPERIMENT: '🧪',
    TRAINING: '📚',
    DOCUMENTATION: '📝',
  };
  return icons[type] || '📋';
};

const getActivityTypeBadgeColor = (type: CooldownActivityType) => {
  const colors = {
    BUG_FIX: 'bg-red-100 text-red-800 border-red-200',
    TECH_DEBT: 'bg-orange-100 text-orange-800 border-orange-200',
    RESEARCH: 'bg-purple-100 text-purple-800 border-purple-200',
    EXPERIMENT: 'bg-blue-100 text-blue-800 border-blue-200',
    TRAINING: 'bg-green-100 text-green-800 border-green-200',
    DOCUMENTATION: 'bg-slate-100 text-slate-800 border-slate-200',
  };
  return colors[type] || 'bg-gray-100 text-gray-800 border-gray-200';
};

const getStatusBadgeColor = (status: CooldownActivityStatus) => {
  const colors = {
    PLANNED: 'bg-slate-100 text-slate-700 border-slate-300',
    IN_PROGRESS: 'bg-blue-100 text-blue-700 border-blue-300',
    COMPLETED: 'bg-green-100 text-green-700 border-green-300',
    CANCELLED: 'bg-red-100 text-red-700 border-red-300',
  };
  return colors[status] || 'bg-gray-100 text-gray-700 border-gray-300';
};

const getStatusIcon = (status: CooldownActivityStatus) => {
  const icons = {
    PLANNED: <Clock className="h-4 w-4" />,
    IN_PROGRESS: <PlayCircle className="h-4 w-4" />,
    COMPLETED: <CheckCircle className="h-4 w-4" />,
    CANCELLED: <XCircle className="h-4 w-4" />,
  };
  return icons[status] || <Clock className="h-4 w-4" />;
};

export default function CooldownActivitiesPage() {
  const { t } = useTranslation();
  const { cycleId: cycleIdParam } = useParams<{ cycleId: string }>();
  const cycleId = safeParseId(cycleIdParam);
  const { showSuccess, showError } = useToast();

  const [activities, setActivities] = useState<CooldownActivityDTO[]>([]);
  const [summary, setSummary] = useState<CooldownSummaryDTO | null>(null);
  const [loading, setLoading] = useState(true);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedActivity, setSelectedActivity] = useState<CooldownActivityDTO | undefined>(undefined);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [activityToDelete, setActivityToDelete] = useState<CooldownActivityDTO | null>(null);

  const [filterType, setFilterType] = useState<string>('all');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    if (cycleId) {
      loadData();
    }
  }, [cycleId]);

  const loadData = async () => {
    if (!cycleId) return;

    setLoading(true);
    try {
      const [activitiesRes, summaryRes] = await Promise.all([
        cooldownActivityService.getActivitiesByCycle(cycleId),
        cooldownActivityService.getCycleSummary(cycleId),
      ]);
      setActivities(activitiesRes.data);
      setSummary(summaryRes.data);
    } catch (error: any) {
      showError(error.response?.data?.message || t('cooldownActivity.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = () => {
    setSelectedActivity(undefined);
    setDialogOpen(true);
  };

  const handleEdit = (activity: CooldownActivityDTO) => {
    setSelectedActivity(activity);
    setDialogOpen(true);
  };

  const handleDeleteClick = (activity: CooldownActivityDTO) => {
    setActivityToDelete(activity);
    setDeleteDialogOpen(true);
  };

  const handleDeleteConfirm = async () => {
    if (!activityToDelete) return;

    try {
      await cooldownActivityService.deleteActivity(activityToDelete.id);
      showSuccess(t('cooldownActivity.deleteSuccess'));
      setDeleteDialogOpen(false);
      setActivityToDelete(null);
      loadData();
    } catch (error: any) {
      showError(error.response?.data?.message || t('cooldownActivity.deleteError'));
    }
  };

  const filteredActivities = activities.filter((activity) => {
    if (filterType !== 'all' && activity.activityType !== filterType) return false;
    if (filterStatus !== 'all' && activity.status !== filterStatus) return false;
    return true;
  });

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!cycleId) {
    return (
      <div className="p-6">
        <Card>
          <CardContent className="p-12 text-center">
            <p className="text-muted-foreground">{t('cooldownActivity.noCycleSelected')}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">{t('cooldownActivity.title')}</h1>
          <p className="text-muted-foreground mt-1">{t('cooldownActivity.subtitle')}</p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="h-4 w-4 me-2" />
          {t('cooldownActivity.createActivity')}
        </Button>
      </div>

      {/* Summary Cards */}
      {summary && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{t('cooldownActivity.totalActivities')}</CardTitle>
              <ListChecks className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{summary.totalActivities}</div>
              <div className="text-xs text-muted-foreground mt-1">
                {t('cooldownActivity.planned')}: {summary.plannedCount} | {t('cooldownActivity.inProgress')}: {summary.inProgressCount}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{t('cooldownActivity.completionRate')}</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{summary.completionRate.toFixed(1)}%</div>
              <div className="text-xs text-muted-foreground mt-1">
                {summary.completedCount} / {summary.totalActivities} {t('cooldownActivity.completed')}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{t('cooldownActivity.estimatedHours')}</CardTitle>
              <Clock className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{summary.totalEstimatedHours.toFixed(1)}</div>
              <div className="text-xs text-muted-foreground mt-1">
                {t('cooldownActivity.hoursPlanned')}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{t('cooldownActivity.actualHours')}</CardTitle>
              <CheckCircle className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{summary.totalActualHours.toFixed(1)}</div>
              <div className="text-xs text-muted-foreground mt-1">
                {t('cooldownActivity.hoursSpent')}
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardHeader>
          <div className="flex items-center gap-4">
            <div className="flex-1">
              <label className="text-sm font-medium mb-2 block">{t('cooldownActivity.filterByType')}</label>
              <Select value={filterType} onValueChange={setFilterType}>
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">{t('cooldownActivity.allTypes')}</SelectItem>
                  <SelectItem value={CooldownActivityType.BUG_FIX}>{t('cooldownActivity.types.bugFix')}</SelectItem>
                  <SelectItem value={CooldownActivityType.TECH_DEBT}>{t('cooldownActivity.types.techDebt')}</SelectItem>
                  <SelectItem value={CooldownActivityType.RESEARCH}>{t('cooldownActivity.types.research')}</SelectItem>
                  <SelectItem value={CooldownActivityType.EXPERIMENT}>{t('cooldownActivity.types.experiment')}</SelectItem>
                  <SelectItem value={CooldownActivityType.TRAINING}>{t('cooldownActivity.types.training')}</SelectItem>
                  <SelectItem value={CooldownActivityType.DOCUMENTATION}>{t('cooldownActivity.types.documentation')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="flex-1">
              <label className="text-sm font-medium mb-2 block">{t('cooldownActivity.filterByStatus')}</label>
              <Select value={filterStatus} onValueChange={setFilterStatus}>
                <SelectTrigger className="w-48">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">{t('cooldownActivity.allStatuses')}</SelectItem>
                  <SelectItem value={CooldownActivityStatus.PLANNED}>{t('cooldownActivity.statuses.planned')}</SelectItem>
                  <SelectItem value={CooldownActivityStatus.IN_PROGRESS}>{t('cooldownActivity.statuses.inProgress')}</SelectItem>
                  <SelectItem value={CooldownActivityStatus.COMPLETED}>{t('cooldownActivity.statuses.completed')}</SelectItem>
                  <SelectItem value={CooldownActivityStatus.CANCELLED}>{t('cooldownActivity.statuses.cancelled')}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>
        </CardHeader>
      </Card>

      {/* Activities Table */}
      <Card>
        <CardHeader>
          <CardTitle>{t('cooldownActivity.activities')} ({filteredActivities.length})</CardTitle>
        </CardHeader>
        <CardContent>
          {filteredActivities.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground">{t('cooldownActivity.noActivities')}</p>
              <Button onClick={handleCreate} variant="outline" className="mt-4">
                <Plus className="h-4 w-4 me-2" />
                {t('cooldownActivity.createFirstActivity')}
              </Button>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t('cooldownActivity.title')}</TableHead>
                  <TableHead>{t('cooldownActivity.type')}</TableHead>
                  <TableHead>{t('cooldownActivity.status')}</TableHead>
                  <TableHead>{t('cooldownActivity.assignee')}</TableHead>
                  <TableHead className="text-end">{t('cooldownActivity.estimated')}</TableHead>
                  <TableHead className="text-end">{t('cooldownActivity.actual')}</TableHead>
                  <TableHead className="text-end">{t('common.actions')}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredActivities.map((activity) => (
                  <TableRow key={activity.id}>
                    <TableCell>
                      <div>
                        <div className="font-medium">{activity.title}</div>
                        {activity.description && (
                          <div className="text-sm text-muted-foreground line-clamp-1">
                            {activity.description}
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={getActivityTypeBadgeColor(activity.activityType)}>
                        {getActivityTypeIcon(activity.activityType)} {t(`cooldownActivity.types.${activity.activityType.toLowerCase()}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline" className={`${getStatusBadgeColor(activity.status)} flex items-center gap-1 w-fit`}>
                        {getStatusIcon(activity.status)}
                        {t(`cooldownActivity.statuses.${activity.status === 'IN_PROGRESS' ? 'inprogress' : activity.status.toLowerCase()}`)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      {activity.assigneeName || (
                        <span className="text-muted-foreground">{t('cooldownActivity.unassigned')}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-end">
                      {activity.estimatedHours ? `${activity.estimatedHours}h` : '-'}
                    </TableCell>
                    <TableCell className="text-end">
                      {activity.actualHours ? `${activity.actualHours}h` : '-'}
                    </TableCell>
                    <TableCell className="text-end">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button variant="ghost" size="sm">
                            <MoreVertical className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <DropdownMenuItem onClick={() => handleEdit(activity)}>
                            <Pencil className="h-4 w-4 me-2" />
                            {t('common.edit')}
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleDeleteClick(activity)}
                            className="text-red-600"
                          >
                            <Trash2 className="h-4 w-4 me-2" />
                            {t('common.delete')}
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Dialogs */}
      {cycleId && (
        <CooldownActivityDialog
          open={dialogOpen}
          onClose={() => setDialogOpen(false)}
          onSave={loadData}
          cycleId={cycleId}
          activity={selectedActivity}
        />
      )}

      <AlertDialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('cooldownActivity.deleteConfirmTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('cooldownActivity.deleteConfirmMessage', { title: activityToDelete?.title })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction onClick={handleDeleteConfirm} className="bg-red-600 hover:bg-red-700">
              {t('common.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}
