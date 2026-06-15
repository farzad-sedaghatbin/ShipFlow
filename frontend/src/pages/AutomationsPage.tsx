import { useCallback, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Zap,
  Plus,
  Trash2,
  Clock,
  Play,
  History,
  ChevronRight,
  AlertCircle,
  LayoutTemplate,
} from 'lucide-react';
import { automationService } from '../services/automationService';
import { useProject, useToast } from '../contexts';
import { usePermission } from '../hooks/usePermission';
import type {
  WorkflowAutomation,
  WorkflowAutomationTemplate,
  WorkflowAutomationExecution,
} from '../types/automation';
import { getUserFriendlyError } from '../utils/errorMessages';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { Card, CardContent } from '../components/ui/card';
import { Button } from '../components/ui/button';
import { Badge } from '../components/ui/badge';
import { Switch } from '../components/ui/switch';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Alert, AlertDescription } from '../components/ui/alert';
import ProjectRequiredDialog from '../components/ProjectRequiredDialog';
import AutomationRuleForm from '../components/workflow/AutomationRuleForm';
import AutomationTemplateGallery from '../components/workflow/AutomationTemplateGallery';
import AutomationExecutionLog from '../components/workflow/AutomationExecutionLog';

const TRIGGER_LABELS: Record<string, string> = {
  TASK_CREATED: 'Task Created',
  TASK_STATUS_CHANGED: 'Task Status Changed',
  TASK_ASSIGNED: 'Task Assigned',
  TASK_COMPLETED: 'Task Completed',
  PITCH_CREATED: 'Pitch Created',
  PITCH_STATUS_CHANGED: 'Pitch Status Changed',
  CYCLE_STARTED: 'Cycle Started',
  CYCLE_ENDED: 'Cycle Ended',
  CYCLE_STATUS_CHANGED: 'Cycle Status Changed',
  COMMENT_ADDED: 'Comment Added',
  BETTING_TABLE_LOCKED: 'Betting Table Locked',
  HILL_CHART_MOVED: 'Hill Chart Moved',
  APPETITE_EXCEEDED: 'Appetite Exceeded',
  SCOPE_CREEP_DETECTED: 'Scope Creep Detected',
};

const ACTION_LABELS: Record<string, string> = {
  NOTIFY_ASSIGNEE: 'Notify Assignee',
  NOTIFY_PROJECT_MEMBERS: 'Notify Project Members',
  SEND_WEBHOOK: 'Send Webhook',
  SEND_EMAIL: 'Send Email',
  ADD_COMMENT: 'Add Comment',
  CHANGE_TASK_STATUS: 'Change Task Status',
  CREATE_TASK: 'Create Task',
};

export default function AutomationsPage() {
  const { t, i18n } = useTranslation();
  const { currentProject } = useProject();
  const { showSuccess, showError } = useToast();
  const { hasPermissionSync } = usePermission();
  const isManager = hasPermissionSync('PITCH', 'MANAGE');

  const [automations, setAutomations] = useState<WorkflowAutomation[]>([]);
  const [templates, setTemplates] = useState<WorkflowAutomationTemplate[]>([]);
  const [executions, setExecutions] = useState<WorkflowAutomationExecution[]>([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [showTemplates, setShowTemplates] = useState(false);
  const [editingAutomation, setEditingAutomation] = useState<WorkflowAutomation | null>(null);
  const [selectedForLog, setSelectedForLog] = useState<WorkflowAutomation | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchData = useCallback(async () => {
    if (!currentProject?.id) return;
    setLoading(true);
    try {
      const [rules, tmpl, exec] = await Promise.all([
        automationService.getByProject(currentProject.id),
        automationService.getTemplates(),
        automationService.getProjectExecutionLog(currentProject.id, 50),
      ]);
      setAutomations(rules.data);
      setTemplates(tmpl.data);
      setExecutions(exec.data);
    } catch (err) {
      showError(getUserFriendlyError(err));
    } finally {
      setLoading(false);
    }
  }, [currentProject?.id, showError]);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleToggle = async (automation: WorkflowAutomation) => {
    try {
      const { data } = await automationService.toggle(automation.id);
      setAutomations(prev => prev.map(a => a.id === data.id ? data : a));
      showSuccess(data.enabled
        ? t('automation.enabledSuccess', { name: data.name })
        : t('automation.disabledSuccess', { name: data.name })
      );
    } catch (err) {
      showError(getUserFriendlyError(err));
    }
  };

  const handleDelete = async (id: number) => {
    setDeletingId(id);
    try {
      await automationService.delete(id);
      setAutomations(prev => prev.filter(a => a.id !== id));
      showSuccess(t('automation.deletedSuccess'));
    } catch (err) {
      showError(getUserFriendlyError(err));
    } finally {
      setDeletingId(null);
    }
  };

  const handleSaved = () => {
    setShowForm(false);
    setEditingAutomation(null);
    fetchData();
  };

  const handleTemplateSelect = async (template: WorkflowAutomationTemplate) => {
    if (!currentProject?.id) return;
    try {
      await automationService.createFromTemplate(template.id, currentProject.id);
      showSuccess(t('automation.createdFromTemplate', { name: template.name }));
      setShowTemplates(false);
      fetchData();
    } catch (err) {
      showError(getUserFriendlyError(err));
    }
  };

  if (!currentProject) return <ProjectRequiredDialog open featureDescription={t('automation.title')} />;

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Zap className="h-6 w-6 text-primary" />
          <div>
            <h1 className="text-2xl font-bold">{t('automation.title')}</h1>
            <p className="text-sm text-muted-foreground">{t('automation.subtitle')}</p>
          </div>
        </div>
        {isManager && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={() => setShowTemplates(true)}>
              <LayoutTemplate className="mr-2 h-4 w-4" />
              {t('automation.browseTemplates')}
            </Button>
            <Button onClick={() => { setEditingAutomation(null); setShowForm(true); }}>
              <Plus className="mr-2 h-4 w-4" />
              {t('automation.new')}
            </Button>
          </div>
        )}
      </div>

      <Tabs defaultValue="rules">
        <TabsList>
          <TabsTrigger value="rules">
            <Zap className="mr-1 h-4 w-4" />
            {t('automation.tabRules')} {automations.length > 0 && `(${automations.length})`}
          </TabsTrigger>
          <TabsTrigger value="history">
            <History className="mr-1 h-4 w-4" />
            {t('automation.tabHistory')}
          </TabsTrigger>
        </TabsList>

        {/* Rules tab */}
        <TabsContent value="rules" className="space-y-3 mt-4">
          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map(i => (
                <div key={i} className="h-24 rounded-lg bg-muted animate-pulse" />
              ))}
            </div>
          ) : automations.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-xl border-2 border-dashed py-16 text-center">
              <Zap className="mb-3 h-10 w-10 text-muted-foreground/40" />
              <p className="font-medium text-muted-foreground">{t('automation.emptyTitle')}</p>
              <p className="mt-1 text-sm text-muted-foreground">{t('automation.emptySubtitle')}</p>
              {isManager && (
                <div className="mt-4 flex gap-2">
                  <Button variant="outline" size="sm" onClick={() => setShowTemplates(true)}>
                    <LayoutTemplate className="mr-2 h-4 w-4" />
                    {t('automation.startFromTemplate')}
                  </Button>
                  <Button size="sm" onClick={() => setShowForm(true)}>
                    <Plus className="mr-2 h-4 w-4" />
                    {t('automation.createFirst')}
                  </Button>
                </div>
              )}
            </div>
          ) : (
            automations.map(automation => (
              <Card key={automation.id} className={!automation.enabled ? 'opacity-60' : ''}>
                <CardContent className="flex items-start gap-4 p-4">
                  <div className="mt-1">
                    <Zap className={`h-5 w-5 ${automation.enabled ? 'text-primary' : 'text-muted-foreground'}`} />
                  </div>
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-semibold">{automation.name}</span>
                      {automation.templateName && (
                        <Badge variant="secondary" className="text-xs">
                          {automation.templateName}
                        </Badge>
                      )}
                      {!automation.enabled && (
                        <Badge variant="outline" className="text-xs">{t('common.disabled')}</Badge>
                      )}
                    </div>
                    {automation.description && (
                      <p className="mt-0.5 text-sm text-muted-foreground line-clamp-1">
                        {automation.description}
                      </p>
                    )}
                    <div className="mt-2 flex items-center gap-3 text-xs text-muted-foreground flex-wrap">
                      <span className="flex items-center gap-1">
                        <Play className="h-3 w-3" />
                        {TRIGGER_LABELS[automation.triggerType] ?? automation.triggerType}
                      </span>
                      <ChevronRight className="h-3 w-3" />
                      <span>{ACTION_LABELS[automation.actionType] ?? automation.actionType}</span>
                      <span className="ml-auto flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {automation.executionCount} {t('automation.runs')}
                        {automation.lastTriggeredAt && (
                          <> · {formatLocalizedDate(automation.lastTriggeredAt, i18n.language)}</>
                        )}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 shrink-0">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => setSelectedForLog(automation)}
                      title={t('automation.viewLog')}
                    >
                      <History className="h-4 w-4" />
                    </Button>
                    {isManager && (
                      <>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => { setEditingAutomation(automation); setShowForm(true); }}
                        >
                          {t('common.edit')}
                        </Button>
                        <Switch
                          checked={automation.enabled}
                          onCheckedChange={() => handleToggle(automation)}
                          aria-label={t('automation.toggleEnabled')}
                        />
                        <Button
                          variant="ghost"
                          size="sm"
                          disabled={deletingId === automation.id}
                          onClick={() => handleDelete(automation.id)}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </>
                    )}
                  </div>
                </CardContent>
              </Card>
            ))
          )}
        </TabsContent>

        {/* History tab */}
        <TabsContent value="history" className="mt-4">
          {executions.length === 0 ? (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertDescription>{t('automation.noExecutions')}</AlertDescription>
            </Alert>
          ) : (
            <AutomationExecutionLog executions={executions} />
          )}
        </TabsContent>
      </Tabs>

      {/* Rule form dialog */}
      {showForm && (
        <AutomationRuleForm
          open={showForm}
          projectId={currentProject.id}
          automation={editingAutomation}
          onClose={() => { setShowForm(false); setEditingAutomation(null); }}
          onSaved={handleSaved}
        />
      )}

      {/* Template gallery dialog */}
      {showTemplates && (
        <AutomationTemplateGallery
          open={showTemplates}
          templates={templates}
          onSelect={handleTemplateSelect}
          onClose={() => setShowTemplates(false)}
        />
      )}

      {/* Per-rule execution log sheet */}
      {selectedForLog && (
        <AutomationExecutionLog
          automationId={selectedForLog.id}
          automationName={selectedForLog.name}
          onClose={() => setSelectedForLog(null)}
        />
      )}
    </div>
  );
}
