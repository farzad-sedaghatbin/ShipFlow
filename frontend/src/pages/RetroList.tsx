import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  Plus,
  Trash2,
  Play,
  Square,
  Eye,
  Loader2,
  Brain,
  Search,
  ArrowUpDown,
} from 'lucide-react';
import { retroService } from '../services/retroService';
import { cycleService } from '../services/cycleService';
import { useProject, useAuth, useToast } from '../contexts';
import { Retrospective, Cycle, RetroStatus } from '../types';
import EmptyState from '../components/EmptyState';

import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
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
import { Alert, AlertDescription } from '../components/ui/alert';

type BadgeVariant = 'default' | 'secondary' | 'destructive' | 'success' | 'warning' | 'info' | 'outline';

const statusVariants: Record<RetroStatus, BadgeVariant> = {
  DRAFT: 'secondary',
  OPEN: 'default',
  CLOSED: 'success',
};

const statusLabels: Record<RetroStatus, string> = {
  DRAFT: '📝 Draft',
  OPEN: '🟢 Open',
  CLOSED: '✅ Closed',
};

export default function RetroList() {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected } = useProject();
  const { user } = useAuth();
  const { showSuccess, showError } = useToast();
  const [retros, setRetros] = useState<Retrospective[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [loading, setLoading] = useState(true);
  const [retroEnabled, setRetroEnabled] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'title' | 'status' | 'cycle' | 'recent'>('recent');
  const [openDialog, setOpenDialog] = useState(false);
  const [deleteDialog, setDeleteDialog] = useState<{ open: boolean; retro: Retrospective | null }>({
    open: false,
    retro: null,
  });
  const [newRetro, setNewRetro] = useState({ title: '', notes: '', cycleId: '' });

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    if (currentProject && !isAllProjectsSelected) {
      loadData();
    }
  }, [currentProject, isAllProjectsSelected]);

  const loadData = async () => {
    if (!currentProject) return;
    setLoading(true);
    try {
      // Check if retros are enabled for this project
      const enabledRes = await retroService.isEnabled(currentProject.id);
      setRetroEnabled(enabledRes.data.enabled);

      if (enabledRes.data.enabled) {
        const [retrosRes, cyclesRes] = await Promise.all([
          retroService.getByProject(currentProject.id),
          cycleService.getByProject(currentProject.id),
        ]);
        setRetros(retrosRes.data);
        setCycles(cyclesRes.data);
      }
    } catch (error: any) {
      console.error('Failed to load retros:', error);
      if (error.response?.status === 403) {
        setRetroEnabled(false);
      }
    } finally {
      setLoading(false);
    }
  };

  const handleCreateRetro = async () => {
    if (!currentProject || !newRetro.title || !newRetro.cycleId) return;

    try {
      await retroService.create({
        title: newRetro.title,
        notes: newRetro.notes,
        cycleId: parseInt(newRetro.cycleId),
        projectId: currentProject.id,
      });
      showSuccess('Retrospective created successfully!');
      setOpenDialog(false);
      setNewRetro({ title: '', notes: '', cycleId: '' });
      loadData();
    } catch (error) {
      showError('Failed to create retrospective');
    }
  };

  const handleOpenRetro = async (id: number) => {
    try {
      await retroService.open(id);
      showSuccess('Retrospective opened!');
      loadData();
    } catch (error) {
      showError('Failed to open retrospective');
    }
  };

  const handleCloseRetro = async (id: number) => {
    try {
      await retroService.close(id);
      showSuccess('Retrospective closed!');
      loadData();
    } catch (error) {
      showError('Failed to close retrospective');
    }
  };

  const handleDeleteRetro = async () => {
    if (!deleteDialog.retro) return;
    try {
      await retroService.delete(deleteDialog.retro.id);
      showSuccess('Retrospective deleted!');
      setDeleteDialog({ open: false, retro: null });
      loadData();
    } catch (error) {
      showError('Failed to delete retrospective');
    }
  };

  const handleCycleChange = (cycleId: string) => {
    const cycle = cycles.find((c) => c.id === parseInt(cycleId));
    setNewRetro({
      ...newRetro,
      cycleId,
      title: cycle ? `${cycle.name} Retro` : '',
    });
  };

  // Check for "All Projects" first before loading check
  if (isAllProjectsSelected || !currentProject) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-6">{t('retroListPage.title')}</h1>
        <Alert variant="info">
          <AlertDescription>
            {t('retroListPage.selectProject')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!retroEnabled) {
    return (
      <div>
        <h1 className="text-2xl font-bold mb-6">{t('retroListPage.title')}</h1>
        <Alert variant="warning" className="mb-4">
          <AlertDescription>
            {t('retroListPage.retrosDisabled')}
            {isAdmin && ` ${t('pitchDetailPage.youCanEnable')}`}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  // Filter and sort retrospectives
  const filteredAndSortedRetros = retros
    .filter(retro => {
      if (!searchTerm) return true;
      const search = searchTerm.toLowerCase();
      return (
        retro.title.toLowerCase().includes(search) ||
        retro.notes?.toLowerCase().includes(search) ||
        retro.cycleName?.toLowerCase().includes(search)
      );
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'title':
          return a.title.localeCompare(b.title);
        case 'status':
          return a.status.localeCompare(b.status);
        case 'cycle':
          return (a.cycleName || '').localeCompare(b.cycleName || '');
        case 'recent':
          return (b.id || 0) - (a.id || 0);
        default:
          return 0;
      }
    });

  return (
    <div>
      {/* Header */}
      <div className="flex flex-col gap-4 mb-8">
        <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4">
          <div>
            <h1 className="text-2xl font-bold">{t('retroListPage.title')}</h1>
            <p className="text-muted-foreground">
              {t('retroListPage.noRetrosDesc').replace('Create your first retrospective', 'Reflect on cycles, capture learnings, and plan improvements')}
            </p>
          </div>
          <Button onClick={() => setOpenDialog(true)} className="w-full sm:w-auto">
            <Plus className="h-4 w-4 mr-2" />
            {t('retroListPage.createRetro')}
          </Button>
        </div>

        {/* Search and Sort Controls */}
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={t('retroListPage.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
          <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <ArrowUpDown className="h-4 w-4 mr-2" />
              <SelectValue placeholder={t('retroListPage.sortBy')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="recent">{t('retroListPage.sortRecent')}</SelectItem>
              <SelectItem value="title">{t('retroListPage.sortTitle')}</SelectItem>
              <SelectItem value="status">{t('retroListPage.sortStatus')}</SelectItem>
              <SelectItem value="cycle">{t('retroListPage.sortCycle')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Content */}
      {filteredAndSortedRetros.length === 0 ? (
        <EmptyState
          title={searchTerm ? t('retroListPage.noRetros') : t('retroListPage.noRetros')}
          description={
            searchTerm
              ? `${t('retroListPage.noRetros')} "${searchTerm}". ${t('common.tryDifferentSearch')}`
              : t('retroListPage.noRetrosDesc')
          }
          action={
            !searchTerm
              ? {
                  label: t('retroListPage.createRetro'),
                  onClick: () => setOpenDialog(true),
                }
              : undefined
          }
          icon={Brain}
        />
      ) : (
        <div className="space-y-4">
          {filteredAndSortedRetros.map((retro) => (
            <Card key={retro.id}>
              <CardContent className="pt-6">
                <div className="flex justify-between items-start">
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1 flex-wrap">
                      <h3 className="text-lg font-semibold">{retro.title}</h3>
                      <Badge variant={statusVariants[retro.status]}>
                        {statusLabels[retro.status]}
                      </Badge>
                    </div>
                    <p className="text-sm text-muted-foreground">
                      {t('retroListPage.cycle')}: {retro.cycleName} • {t('backlogPage.created')}: {formatLocalizedDate(new Date(retro.createdAt), i18n.language)}
                      {retro.closedAt && ` • ${t('backlogPage.closed')}: ${formatLocalizedDate(new Date(retro.closedAt), i18n.language)}`}
                    </p>
                    {retro.notes && (
                      <p className="text-sm mt-2">{retro.notes}</p>
                    )}
                    <p className="text-sm text-muted-foreground mt-2">
                      {retro.itemCount || 0} {t('retroBoardPage.addItem').toLowerCase()}
                    </p>
                  </div>
                  <div className="flex gap-1 ml-4 flex-shrink-0">
                    <TooltipProvider>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => navigate(`/retros/${retro.id}`)}
                            className="text-primary hover:text-primary"
                          >
                            <Eye className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>View Board</TooltipContent>
                      </Tooltip>
                    </TooltipProvider>

                    {retro.status === 'DRAFT' && (
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleOpenRetro(retro.id)}
                              className="text-success hover:text-success"
                            >
                              <Play className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Open for team</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    )}

                    {retro.status === 'OPEN' && isAdmin && (
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleCloseRetro(retro.id)}
                              className="text-warning hover:text-warning"
                            >
                              <Square className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Close retro</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    )}

                    {isAdmin && (
                      <TooltipProvider>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => setDeleteDialog({ open: true, retro })}
                              className="text-destructive hover:text-destructive"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>Delete</TooltipContent>
                        </Tooltip>
                      </TooltipProvider>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Create Dialog */}
      <Dialog open={openDialog} onOpenChange={setOpenDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('retroListPage.createRetro')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="cycle">{t('retroListPage.cycle')} *</Label>
              <Select value={newRetro.cycleId} onValueChange={handleCycleChange}>
                <SelectTrigger id="cycle">
                  <SelectValue placeholder={t('retroListPage.selectCycle')} />
                </SelectTrigger>
                <SelectContent>
                  {cycles.map((cycle) => (
                    <SelectItem key={cycle.id} value={cycle.id.toString()}>
                      {cycle.name} ({cycle.phase})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="title">{t('retroListPage.retroTitle')} *</Label>
              <Input
                id="title"
                value={newRetro.title}
                onChange={(e) => setNewRetro({ ...newRetro, title: e.target.value })}
                placeholder={t('retroListPage.enterTitle')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="notes">{t('retroListPage.notes')}</Label>
              <Textarea
                id="notes"
                value={newRetro.notes}
                onChange={(e) => setNewRetro({ ...newRetro, notes: e.target.value })}
                rows={3}
                placeholder={t('retroListPage.optionalNotes')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setOpenDialog(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={handleCreateRetro}
              disabled={!newRetro.title || !newRetro.cycleId}
            >
              {t('retroListPage.create')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={deleteDialog.open} onOpenChange={(open) => !open && setDeleteDialog({ open: false, retro: null })}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{t('retroListPage.deleteRetro')}?</DialogTitle>
            <DialogDescription>
              {t('retroListPage.confirmDelete')}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialog({ open: false, retro: null })}>
              {t('common.cancel')}
            </Button>
            <Button variant="destructive" onClick={handleDeleteRetro}>
              {t('common.delete')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
