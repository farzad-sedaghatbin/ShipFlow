import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { Plus, LayoutDashboard, Star, Edit, Trash2, Copy, Sparkles, Eye, BarChart3 } from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Combobox } from '../components/ui/combobox';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Badge } from '../components/ui/badge';
import { ConfirmDialog } from '../components/ui/confirm-dialog';
import { useToast } from '../contexts';
import { customDashboardService } from '../services/customDashboardService';
import { cycleService } from '../services/cycleService';
import { pitchService } from '../services/pitchService';
import { teamService } from '../services/teamService';
import { CustomDashboard, CreateDashboardRequest } from '../types/customDashboard';
import { Cycle, Pitch, Team } from '../types';
import { getUserFriendlyError } from '../utils/errorMessages';
import LoadingButton from '../components/LoadingButton';
import { TableSkeleton } from '../components/Skeletons';

export default function DashboardManager() {
  const { t, i18n } = useTranslation();
  const [dashboards, setDashboards] = useState<CustomDashboard[]>([]);
  const [templates, setTemplates] = useState<CustomDashboard[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [teams, setTeams] = useState<Team[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateDialog, setShowCreateDialog] = useState(false);
  const [showTemplateDialog, setShowTemplateDialog] = useState(false);
  const [editingDashboard, setEditingDashboard] = useState<CustomDashboard | null>(null);
  const [savingDashboard, setSavingDashboard] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [dashboardToDelete, setDashboardToDelete] = useState<CustomDashboard | null>(null);
  const { showSuccess, showError } = useToast();
  const navigate = useNavigate();

  const [formData, setFormData] = useState<CreateDashboardRequest>({
    name: '',
    description: '',
    isDefault: false
  });

  useEffect(() => {
    loadDashboards();
    loadTemplates();
    loadScopeData();
  }, []);

  const loadScopeData = async () => {
    try {
      const [cyclesData, pitchesData, teamsData] = await Promise.all([
        cycleService.getMyCycles(),
        pitchService.getMyPitches(),
        teamService.getAll()
      ]);
      setCycles(cyclesData.data);
      setPitches(pitchesData.data);
      setTeams(teamsData.data);
    } catch (err) {
      console.error('Failed to load scope data:', err);
    }
  };

  const loadDashboards = async () => {
    try {
      setLoading(true);
      const response = await customDashboardService.getAll();
      setDashboards(response);
    } catch (err) {
      showError(getUserFriendlyError(err, 'Failed to load dashboards'));
    } finally {
      setLoading(false);
    }
  };

  const loadTemplates = async () => {
    try {
      const response = await customDashboardService.getTemplates();
      setTemplates(response);
    } catch (err) {
      console.error('Failed to load templates:', err);
    }
  };

  const openCreateDialog = () => {
    setEditingDashboard(null);
    setFormData({ name: '', description: '', isDefault: false });
    setShowCreateDialog(true);
  };

  const openEditDialog = (dashboard: CustomDashboard) => {
    setEditingDashboard(dashboard);
    setFormData({
      name: dashboard.name,
      description: dashboard.description,
      isDefault: dashboard.isDefault
    });
    setShowCreateDialog(true);
  };

  const handleSave = async () => {
    if (!formData.name.trim()) {
      showError(t('common.nameRequired'));
      return;
    }

    try {
      setSavingDashboard(true);
      if (editingDashboard) {
        await customDashboardService.update(editingDashboard.id, formData);
        showSuccess(t('common.updateSuccess'));
      } else {
        await customDashboardService.create(formData);
        showSuccess(t('common.createSuccess'));
      }
      setShowCreateDialog(false);
      loadDashboards();
    } catch (err) {
      showError(getUserFriendlyError(err, t('common.saveFailed')));
    } finally {
      setSavingDashboard(false);
    }
  };

  const openDeleteConfirm = (dashboard: CustomDashboard) => {
    if (dashboard.isDefault) {
      showError(t('common.cannotDeleteDefault'));
      return;
    }
    setDashboardToDelete(dashboard);
    setDeleteConfirmOpen(true);
  };

  const handleDelete = async () => {
    if (!dashboardToDelete) return;

    try {
      await customDashboardService.delete(dashboardToDelete.id);
      showSuccess(t('common.deleteSuccess'));
      loadDashboards();
      setDeleteConfirmOpen(false);
      setDashboardToDelete(null);
    } catch (err) {
      showError(getUserFriendlyError(err, t('common.deleteFailed')));
    }
  };

  const handleSetDefault = async (dashboard: CustomDashboard) => {
    try {
      await customDashboardService.setAsDefault(dashboard.id);
      showSuccess(t('common.setDefaultSuccess', { name: dashboard.name }));
      loadDashboards();
    } catch (err) {
      showError(getUserFriendlyError(err, t('common.setDefaultFailed')));
    }
  };

  const handleCloneTemplate = async (template: CustomDashboard) => {
    try {
      await customDashboardService.create({
        name: template.name,
        description: template.description,
        cloneFromTemplateId: template.id,
        isDefault: false
      });
      showSuccess(`Report board created from "${template.name}" template`);
      setShowTemplateDialog(false);
      loadDashboards();
    } catch (err) {
      showError(getUserFriendlyError(err, 'Failed to clone template'));
    }
  };

  // Static report boards
  const staticReports = [
    {
      id: -1,
      name: t('dashboardManager.cycleReports'),
      description: t('dashboardManager.cycleReportsDesc'),
      url: '/reports',
      isStatic: true
    }
  ];

  if (loading) {
    return (
      <div className="container mx-auto py-6 px-4">
        <div className="flex justify-between items-center mb-6">
          <h1 className="text-3xl font-bold">Reports</h1>
        </div>
        <TableSkeleton rows={3} columns={2} />
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 px-4">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">{t('dashboardManager.title')}</h1>
          <p className="text-muted-foreground mt-1">
            {t('dashboardManager.description')}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setShowTemplateDialog(true)}>
            <Sparkles className="me-2 h-4 w-4" />
            {t('dashboardManager.useTemplate')}
          </Button>
          <Button onClick={openCreateDialog}>
            <Plus className="me-2 h-4 w-4" />
            {t('dashboardManager.newReportBoard')}
          </Button>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {/* Static Built-in Reports */}
        {staticReports.map((report) => (
          <Card key={`static-${report.id}`} className="border-blue-200 bg-blue-50/30">
            <CardHeader>
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <CardTitle className="flex items-center gap-2">
                    <BarChart3 className="h-5 w-5 text-blue-600" />
                    {report.name}
                    <Badge variant="outline" className="bg-blue-100 text-blue-700 border-blue-300">
                      {t('dashboardManager.static')}
                    </Badge>
                  </CardTitle>
                  {report.description && (
                    <CardDescription className="mt-1">
                      {report.description}
                    </CardDescription>
                  )}
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <Button
                className="w-full"
                onClick={() => navigate(report.url)}
              >
                <Eye className="me-2 h-4 w-4" />
                {t('dashboardManager.openReport')}
              </Button>
            </CardContent>
          </Card>
        ))}

        {/* User's Custom Report Boards */}
        {dashboards.length === 0 ? (
          <Card className="col-span-full md:col-span-2 lg:col-span-2">
            <CardContent className="py-12 text-center">
              <LayoutDashboard className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">{t('dashboardManager.noCustomBoards')}</h3>
              <p className="text-muted-foreground mb-4">
                {t('dashboardManager.noCustomBoardsDesc')}
              </p>
              <div className="flex justify-center gap-2">
                <Button variant="outline" onClick={() => setShowTemplateDialog(true)}>
                  <Sparkles className="me-2 h-4 w-4" />
                  {t('dashboardManager.browseTemplates')}
                </Button>
                <Button onClick={openCreateDialog}>
                  <Plus className="me-2 h-4 w-4" />
                  {t('dashboardManager.createReportBoard')}
                </Button>
              </div>
            </CardContent>
          </Card>
        ) : (
          dashboards.map((dashboard) => (
            <Card key={dashboard.id} className="relative">
              <CardHeader>
                <div className="flex justify-between items-start">
                  <div className="flex-1">
                    <CardTitle className="flex items-center gap-2">
                      {dashboard.name}
                      {dashboard.isDefault && (
                        <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                      )}
                    </CardTitle>
                    {dashboard.description && (
                      <CardDescription className="mt-1">
                        {dashboard.description}
                      </CardDescription>
                    )}
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-3">
                  <div className="flex items-center gap-2 text-sm text-muted-foreground">
                    <span>Created {formatLocalizedDate(new Date(dashboard.createdAt), i18n.language)}</span>
                  </div>
                  {dashboard.templateCategory && (
                    <Badge variant="secondary">{dashboard.templateCategory}</Badge>
                  )}
                  <div className="flex flex-wrap justify-end gap-2 pt-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => navigate(`/reports/${dashboard.id}`)}
                    >
                      <Eye className="me-2 h-4 w-4" />
                      {t('dashboardManager.view')}
                    </Button>
                    {!dashboard.isDefault && (
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleSetDefault(dashboard)}
                      >
                        <Star className="me-2 h-4 w-4" />
                        {t('common.setDefault')}
                      </Button>
                    )}
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEditDialog(dashboard)}
                    >
                      <Edit className="me-2 h-4 w-4" />
                      {t('dashboardManager.edit')}
                    </Button>
                    {!dashboard.isDefault && (
                      <Button
                        variant="destructive"
                        size="sm"
                        onClick={() => openDeleteConfirm(dashboard)}
                      >
                        <Trash2 className="me-2 h-4 w-4" />
                        {t('dashboardManager.delete')}
                      </Button>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={showCreateDialog} onOpenChange={setShowCreateDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {editingDashboard ? t('dashboardManager.editDashboard') : t('dashboardManager.createDashboard')}
            </DialogTitle>
            <DialogDescription>
              {editingDashboard
                ? t('dashboardManager.updateDashboardDetails')
                : t('dashboardManager.createNewDashboard')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="name">{t('dashboardManager.dashboardName')}</Label>
              <Input
                id="name"
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                placeholder={t('dashboardManager.dashboardNamePlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">{t('dashboardManager.dashboardDescription')}</Label>
              <Textarea
                id="description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder={t('dashboardManager.dashboardDescPlaceholder')}
                rows={3}
              />
            </div>

            {/* Scope Section */}
            <div className="border-t pt-4 space-y-4">
              <div>
                <h3 className="font-medium mb-1 text-sm">{t('dashboardManager.scope')}</h3>
                <p className="text-xs text-muted-foreground">
                  {t('dashboardManager.scopeDesc')}
                </p>
              </div>

              {/* Cycle Scope */}
              <div className="space-y-2">
                <Label className="text-sm">{t('dashboardManager.cycle')}</Label>
                <Combobox
                  options={[
                    { value: 'none', label: t('dashboardManager.allCycles') },
                    ...cycles.map(cycle => ({ value: cycle.id.toString(), label: cycle.name }))
                  ]}
                  value={formData.cycleId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    cycleId: value === 'none' ? undefined : parseInt(value) 
                  })}
                  placeholder={t('dashboardManager.allCycles')}
                  searchPlaceholder="Search cycles..."
                />
              </div>

              {/* Pitch Scope */}
              <div className="space-y-2">
                <Label className="text-sm">{t('dashboardManager.pitch')}</Label>
                <Combobox
                  options={[
                    { value: 'none', label: t('dashboardManager.allPitches') },
                    ...pitches.map(pitch => ({ value: pitch.id.toString(), label: pitch.title }))
                  ]}
                  value={formData.pitchId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    pitchId: value === 'none' ? undefined : parseInt(value) 
                  })}
                  placeholder={t('dashboardManager.allPitches')}
                  searchPlaceholder="Search pitches..."
                />
              </div>

              {/* Team Scope */}
              <div className="space-y-2">
                <Label className="text-sm">{t('dashboardManager.team')}</Label>
                <Combobox
                  options={[
                    { value: 'none', label: t('dashboardManager.allTeams') },
                    ...teams.map(team => ({ value: team.id.toString(), label: team.name }))
                  ]}
                  value={formData.teamId?.toString() || 'none'}
                  onValueChange={(value) => setFormData({ 
                    ...formData, 
                    teamId: value === 'none' ? undefined : parseInt(value) 
                  })}
                  placeholder={t('dashboardManager.allTeams')}
                  searchPlaceholder="Search teams..."
                />
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowCreateDialog(false)}>
              {t('common.cancel')}
            </Button>
            <LoadingButton onClick={handleSave} loading={savingDashboard}>
              {editingDashboard ? t('common.update') : t('common.create')}
            </LoadingButton>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Template Gallery Dialog */}
      <Dialog open={showTemplateDialog} onOpenChange={setShowTemplateDialog}>
        <DialogContent className="max-w-4xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t('dashboardManager.dashboardTemplates')}</DialogTitle>
            <DialogDescription>
              {t('dashboardManager.dashboardTemplatesDesc')}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 md:grid-cols-2 py-4">
            {templates.length === 0 ? (
              <div className="col-span-2 text-center py-8 text-muted-foreground">
                {t('dashboardManager.noTemplates')}
              </div>
            ) : (
              templates.map((template) => (
                <Card key={template.id} className="cursor-pointer hover:border-primary">
                  <CardHeader>
                    <CardTitle className="text-lg">{template.name}</CardTitle>
                    <CardDescription>{template.description}</CardDescription>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-3">
                      {template.templateCategory && (
                        <Badge>{template.templateCategory}</Badge>
                      )}
                      <Button
                        className="w-full"
                        onClick={() => handleCloneTemplate(template)}
                      >
                        <Copy className="me-2 h-4 w-4" />
                        {t('dashboardManager.useThisTemplate')}
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))
            )}
          </div>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title={t('dashboardManager.deleteTitle')}
        description={t('common.confirmDelete', { name: dashboardToDelete?.name || '' })}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleDelete}
        variant="destructive"
      />
    </div>
  );
}
