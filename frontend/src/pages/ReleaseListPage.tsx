import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  Plus,
  Pencil,
  Trash2,
  Package,
  Calendar,
  Search,
  MoreVertical,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Flag,
} from 'lucide-react';
import { releaseService } from '../services/releaseService';
import { Release, ReleaseStatus, ReleaseRiskLevel } from '../types';
import { useProject, useToast } from '../contexts';
import ProjectRequiredDialog from '../components/ProjectRequiredDialog';

import { Card, CardContent } from '../components/ui/card';
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
import { Skeleton } from '../components/ui/skeleton';

const getStatusBadgeVariant = (status: ReleaseStatus): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (status) {
    case 'RELEASED': return 'default';
    case 'IN_PROGRESS': return 'secondary';
    case 'STAGING': return 'secondary';
    case 'PLANNING': return 'outline';
    case 'CANCELLED': return 'destructive';
    default: return 'outline';
  }
};

const getRiskBadgeVariant = (risk: ReleaseRiskLevel): 'default' | 'secondary' | 'destructive' | 'outline' => {
  switch (risk) {
    case 'LOW': return 'secondary';
    case 'MEDIUM': return 'default';
    case 'HIGH': return 'destructive';
    case 'CRITICAL': return 'destructive';
    default: return 'outline';
  }
};

const getStatusIcon = (status: ReleaseStatus) => {
  switch (status) {
    case 'RELEASED': return <CheckCircle2 className="h-4 w-4 text-green-600" />;
    case 'IN_PROGRESS': return <Clock className="h-4 w-4 text-yellow-600" />;
    case 'STAGING': return <Package className="h-4 w-4 text-purple-600" />;
    case 'PLANNING': return <Calendar className="h-4 w-4 text-blue-600" />;
    default: return <Flag className="h-4 w-4" />;
  }
};

export default function ReleaseListPage() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { showSuccess, showError } = useToast();
  const [releases, setReleases] = useState<Release[]>([]);
  const [loading, setLoading] = useState(true);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; release: Release | null }>({
    open: false,
    release: null,
  });

  // Filter states
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStatus, setFilterStatus] = useState<string>('all');

  useEffect(() => {
    if (!currentProject || isAllProjectsSelected) {
      setReleases([]);
      setLoading(false);
      return;
    }
    loadReleases();
  }, [currentProject, isAllProjectsSelected]);

  const loadReleases = async () => {
    if (!currentProject) return;
    try {
      setLoading(true);
      const response = await releaseService.getByProject(currentProject.id);
      setReleases(response.data);
    } catch (error) {
      console.error('Failed to load releases:', error);
      showError(t('releases.loadError'));
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!deleteDialog.release) return;
    try {
      await releaseService.delete(deleteDialog.release.id);
      showSuccess(t('releases.deleted'));
      setDeleteDialog({ open: false, release: null });
      loadReleases();
    } catch (error) {
      showError(t('releases.deleteError'));
      console.error('Failed to delete release:', error);
    }
  };

  const filteredReleases = releases.filter((release) => {
    const matchesSearch =
      searchTerm === '' ||
      release.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      release.version.toLowerCase().includes(searchTerm.toLowerCase());

    const matchesStatus = filterStatus === 'all' || release.status === filterStatus;

    return matchesSearch && matchesStatus;
  });

  // Group releases by status
  const upcomingReleases = filteredReleases.filter(r => ['PLANNING', 'IN_PROGRESS', 'STAGING'].includes(r.status));
  const pastReleases = filteredReleases.filter(r => r.status === 'RELEASED');

  if (isAllProjectsSelected) {
    return (
      <ProjectRequiredDialog
        open={true}
        featureDescription={t('releases.selectProjectDescription')}
      />
    );
  }

  if (loading) {
    return (
      <div className="container mx-auto py-8 space-y-4">
        <div className="flex justify-between">
          <Skeleton className="h-10 w-64" />
          <Skeleton className="h-10 w-32" />
        </div>
        <div className="space-y-4">
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} className="h-24" />
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
            <Package className="h-6 w-6" />
            {t('releases.title')}
          </h1>
          <p className="text-muted-foreground">{t('releases.description')}</p>
        </div>
        <Button onClick={() => navigate('/releases-management/new')}>
          <Plus className="h-4 w-4 mr-2" />
          {t('releases.create')}
        </Button>
      </div>

      {/* Filters */}
      <div className="flex gap-4">
        <div className="relative flex-1 max-w-sm">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder={t('releases.search')}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9"
          />
        </div>
        <Select value={filterStatus} onValueChange={setFilterStatus}>
          <SelectTrigger className="w-40">
            <SelectValue placeholder={t('releases.filterStatus')} />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="all">{t('common.all')}</SelectItem>
            <SelectItem value="PLANNING">{t('releases.status.planning')}</SelectItem>
            <SelectItem value="IN_PROGRESS">{t('releases.status.inProgress')}</SelectItem>
            <SelectItem value="STAGING">{t('releases.status.staging')}</SelectItem>
            <SelectItem value="RELEASED">{t('releases.status.released')}</SelectItem>
            <SelectItem value="CANCELLED">{t('releases.status.cancelled')}</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {filteredReleases.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <Package className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
            <h3 className="text-lg font-medium">{t('releases.noReleases')}</h3>
            <p className="text-muted-foreground mb-4">{t('releases.noReleasesDescription')}</p>
            <Button onClick={() => navigate('/releases-management/new')}>
              <Plus className="h-4 w-4 mr-2" />
              {t('releases.createFirst')}
            </Button>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-8">
          {/* Upcoming Releases */}
          {upcomingReleases.length > 0 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold flex items-center gap-2">
                <Clock className="h-5 w-5" />
                {t('releases.upcoming')} ({upcomingReleases.length})
              </h2>
              <div className="space-y-3">
                {upcomingReleases.map((release) => (
                  <Card key={release.id} className="hover:shadow-md transition-shadow">
                    <CardContent className="py-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                          {getStatusIcon(release.status)}
                          <div>
                            <div className="flex items-center gap-2">
                              <Link
                                to={`/releases-management/${release.id}`}
                                className="font-semibold text-lg hover:underline"
                              >
                                {release.version}
                              </Link>
                              <Badge variant={getStatusBadgeVariant(release.status)}>
                                {t(`releases.status.${release.status.toLowerCase()}`)}
                              </Badge>
                              <Badge variant={getRiskBadgeVariant(release.riskLevel)}>
                                {release.riskLevel === 'HIGH' || release.riskLevel === 'CRITICAL' ? (
                                  <AlertTriangle className="h-3 w-3 mr-1" />
                                ) : null}
                                {release.riskLevel}
                              </Badge>
                            </div>
                            <p className="text-sm text-muted-foreground">{release.name}</p>
                          </div>
                        </div>

                        <div className="flex items-center gap-4">
                          {release.targetDate && (
                            <div className="text-sm text-muted-foreground flex items-center gap-1">
                              <Calendar className="h-4 w-4" />
                              {formatLocalizedDate(release.targetDate, i18n.language)}
                            </div>
                          )}
                          {release.cycles && release.cycles.length > 0 && (
                            <Badge variant="outline">
                              {release.cycles.length} {t('releases.cycles')}
                            </Badge>
                          )}
                          <DropdownMenu>
                            <DropdownMenuTrigger asChild>
                              <Button variant="ghost" size="icon">
                                <MoreVertical className="h-4 w-4" />
                              </Button>
                            </DropdownMenuTrigger>
                            <DropdownMenuContent align="end">
                              <DropdownMenuItem onClick={() => navigate(`/releases-management/${release.id}`)}>
                                {t('common.view')}
                              </DropdownMenuItem>
                              <DropdownMenuItem onClick={() => navigate(`/releases-management/${release.id}/edit`)}>
                                <Pencil className="h-4 w-4 mr-2" />
                                {t('common.edit')}
                              </DropdownMenuItem>
                              <DropdownMenuItem
                                className="text-destructive"
                                onClick={() => setDeleteDialog({ open: true, release })}
                              >
                                <Trash2 className="h-4 w-4 mr-2" />
                                {t('common.delete')}
                              </DropdownMenuItem>
                            </DropdownMenuContent>
                          </DropdownMenu>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}

          {/* Past Releases */}
          {pastReleases.length > 0 && (
            <div className="space-y-4">
              <h2 className="text-lg font-semibold flex items-center gap-2">
                <CheckCircle2 className="h-5 w-5 text-green-600" />
                {t('releases.past')} ({pastReleases.length})
              </h2>
              <div className="space-y-3">
                {pastReleases.map((release) => (
                  <Card key={release.id} className="hover:shadow-md transition-shadow opacity-75">
                    <CardContent className="py-4">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-4">
                          <CheckCircle2 className="h-4 w-4 text-green-600" />
                          <div>
                            <div className="flex items-center gap-2">
                              <Link
                                to={`/releases-management/${release.id}`}
                                className="font-semibold hover:underline"
                              >
                                {release.version}
                              </Link>
                              <Badge variant="default">{t('releases.status.released')}</Badge>
                            </div>
                            <p className="text-sm text-muted-foreground">{release.name}</p>
                          </div>
                        </div>

                        <div className="flex items-center gap-4">
                          {release.releaseDate && (
                            <div className="text-sm text-muted-foreground flex items-center gap-1">
                              <Calendar className="h-4 w-4" />
                              {t('releases.releasedOn')} {formatLocalizedDate(release.releaseDate, i18n.language)}
                            </div>
                          )}
                          <Button variant="ghost" size="sm" onClick={() => navigate(`/releases-management/${release.id}`)}>
                            {t('common.view')}
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Delete Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => setDeleteDialog({ ...deleteDialog, open })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('releases.deleteConfirm')}</DialogTitle>
            <DialogDescription>
              {t('releases.deleteWarning', { name: `${deleteDialog.release?.name} (${deleteDialog.release?.version})` })}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, release: null })}>
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
