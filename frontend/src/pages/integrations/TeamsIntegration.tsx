import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Send, RefreshCw, HelpCircle } from 'lucide-react';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Switch } from '../../components/ui/switch';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../components/ui/card';
import { Alert, AlertDescription } from '../../components/ui/alert';
import { Badge } from '../../components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogDescription,
} from '../../components/ui/dialog';
import {
  teamsService,
  TeamsConfiguration,
  TeamsChannelConfig,
  CreateTeamsConfigurationRequest,
  CreateTeamsChannelConfigRequest,
} from '../../services/teamsService';

export default function TeamsIntegration() {
  const { t } = useTranslation();
  // i18n ready
  if (false) console.log(t('teamsIntegration.title'));
  const [tabValue, setTabValue] = useState('workspace');
  const [configurations, setConfigurations] = useState<TeamsConfiguration[]>([]);
  const [activeConfig, setActiveConfig] = useState<TeamsConfiguration | null>(null);
  const [channelConfigs, setChannelConfigs] = useState<TeamsChannelConfig[]>([]);
  const [_loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Dialog states
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [channelDialogOpen, setChannelDialogOpen] = useState(false);
  const [testDialogOpen, setTestDialogOpen] = useState(false);
  const [helpDialogOpen, setHelpDialogOpen] = useState(false);

  // Form states
  const [configForm, setConfigForm] = useState<CreateTeamsConfigurationRequest>({
    tenantName: '',
    webhookUrl: '',
    defaultChannel: '',
    isEnabled: true,
  });

  const [channelForm, setChannelForm] = useState<CreateTeamsChannelConfigRequest>({
    channelName: '',
    channelWebhookUrl: '',
    notifyTaskAssigned: true,
    notifyTaskCompleted: true,
    notifyTaskBlocked: false,
    notifyPitchShaped: true,
    notifyCycleStarted: true,
    notifyCycleCooldown: true,
    notifyBettingCompleted: false,
    notifySprintStarted: false,
  });

  const [testMessage, setTestMessage] = useState('Test notification from ShipFlow');
  const [testChannel, setTestChannel] = useState('');

  useEffect(() => {
    fetchConfigurations();
  }, []);

  useEffect(() => {
    if (activeConfig) {
      fetchChannelConfigs(activeConfig.id);
    }
  }, [activeConfig]);

  // Auto-clear messages after 5 seconds
  useEffect(() => {
    if (success || error) {
      const timer = setTimeout(() => {
        setSuccess(null);
        setError(null);
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [success, error]);

  const fetchConfigurations = async () => {
    try {
      setLoading(true);
      const configs = await teamsService.getAllConfigurations();
      setConfigurations(configs);

      const active = await teamsService.getActiveConfiguration();
      setActiveConfig(active);

      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.fetchConfigsFailed'));
    } finally {
      setLoading(false);
    }
  };

  const fetchChannelConfigs = async (configId: number) => {
    try {
      const channels = await teamsService.getChannelConfigs(configId);
      setChannelConfigs(channels);
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.fetchChannelsFailed'));
    }
  };

  const handleCreateConfiguration = async () => {
    try {
      await teamsService.createConfiguration(configForm);
      setSuccess(t('teamsIntegration.workspaceConfigured'));
      setConfigDialogOpen(false);
      setConfigForm({
        tenantName: '',
        webhookUrl: '',
        defaultChannel: '',
        isEnabled: true,
      });
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.createConfigFailed'));
    }
  };

  const handleDeleteConfiguration = async (configId: number) => {
    if (!confirm(t('teamsIntegration.confirmDeleteConfig'))) {
      return;
    }

    try {
      await teamsService.deleteConfiguration(configId);
      setSuccess(t('teamsIntegration.configDeleted'));
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.deleteConfigFailed'));
    }
  };

  const handleCreateChannelConfig = async () => {
    if (!activeConfig) return;

    try {
      await teamsService.createChannelConfig(activeConfig.id, channelForm);
      setSuccess(t('teamsIntegration.channelConfigSaved'));
      setChannelDialogOpen(false);
      setChannelForm({
        channelName: '',
        channelWebhookUrl: '',
        notifyTaskAssigned: true,
        notifyTaskCompleted: true,
        notifyTaskBlocked: false,
        notifyPitchShaped: true,
        notifyCycleStarted: true,
        notifyCycleCooldown: true,
        notifyBettingCompleted: false,
        notifySprintStarted: false,
      });
      fetchChannelConfigs(activeConfig.id);
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.createChannelFailed'));
    }
  };

  const handleDeleteChannelConfig = async (channelConfigId: number) => {
    if (!confirm(t('teamsIntegration.confirmDeleteChannel'))) {
      return;
    }

    try {
      await teamsService.deleteChannelConfig(channelConfigId);
      setSuccess(t('teamsIntegration.channelDeleted'));
      if (activeConfig) {
        fetchChannelConfigs(activeConfig.id);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.deleteChannelFailed'));
    }
  };

  const handleSendTestNotification = async () => {
    if (!activeConfig) return;

    try {
      await teamsService.sendTestNotification(activeConfig.id, {
        message: testMessage,
        channel: testChannel,
      });
      setSuccess(t('teamsIntegration.testNotificationSent'));
      setTestDialogOpen(false);
    } catch (err: any) {
      setError(err.response?.data?.message || t('teamsIntegration.sendTestFailed'));
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-3xl font-bold">{t('teamsIntegration.title')}</h1>
          <p className="text-muted-foreground mt-1">
            {t('teamsIntegration.description')}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setHelpDialogOpen(true)}>
            <HelpCircle className="mr-2 h-4 w-4" />
            {t('teamsIntegration.setupGuide')}
          </Button>
          <Button onClick={() => setConfigDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            {t('teamsIntegration.configureTenant')}
          </Button>
          <Button variant="ghost" size="icon" onClick={fetchConfigurations}>
            <RefreshCw className="h-4 w-4" />
          </Button>
        </div>
      </div>

      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertDescription>{error}</AlertDescription>
        </Alert>
      )}

      {success && (
        <Alert className="mb-4 border-green-500 bg-green-50 text-green-700 dark:bg-green-950 dark:text-green-400">
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      <Tabs value={tabValue} onValueChange={setTabValue} className="mb-4">
        <TabsList>
          <TabsTrigger value="workspace">{t('teamsIntegration.tenantConfiguration')}</TabsTrigger>
          <TabsTrigger value="channels" disabled={!activeConfig}>
            {t('teamsIntegration.channelNotifications')}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="workspace" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('teamsIntegration.tenantConfigurationTitle')}</CardTitle>
              <CardDescription>
                {t('teamsIntegration.tenantConfigurationDescription')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {configurations.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-muted-foreground mb-4">
                    {t('teamsIntegration.noTenant')}
                  </p>
                  <Button onClick={() => setHelpDialogOpen(true)} variant="outline">
                    {t('teamsIntegration.viewSetupGuide')}
                  </Button>
                </div>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t('teamsIntegration.tenantName')}</TableHead>
                        <TableHead>{t('teamsIntegration.webhookUrl')}</TableHead>
                        <TableHead>{t('teamsIntegration.defaultChannel')}</TableHead>
                        <TableHead>{t('common.status')}</TableHead>
                        <TableHead className="text-right">{t('common.actions')}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {configurations.map((config) => (
                        <TableRow key={config.id}>
                          <TableCell className="font-medium">{config.tenantName}</TableCell>
                          <TableCell>
                            <code className="text-xs font-mono bg-muted px-2 py-1 rounded">
                              {config.webhookUrl.substring(0, 40)}...
                            </code>
                          </TableCell>
                          <TableCell>{config.defaultChannel || '-'}</TableCell>
                          <TableCell>
                            <Badge variant={config.isEnabled ? 'default' : 'secondary'}>
                              {config.isEnabled ? t('teamsIntegration.enabled') : t('teamsIntegration.disabled')}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <div className="flex justify-end gap-2">
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setTestDialogOpen(true)}
                                disabled={!config.isEnabled}
                                title={t('teamsIntegration.sendTest')}
                              >
                                <Send className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => handleDeleteConfiguration(config.id)}
                                title={t('teamsIntegration.deleteConfig')}
                              >
                                <Trash2 className="h-4 w-4 text-destructive" />
                              </Button>
                            </div>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="channels" className="mt-6">
          <div className="flex justify-between items-center mb-4">
            <div>
              <h2 className="text-xl font-semibold">{t('teamsIntegration.channelSettingsTitle')}</h2>
              <p className="text-sm text-muted-foreground">
                {t('teamsIntegration.channelSettingsDescription')}
              </p>
            </div>
            <Button onClick={() => setChannelDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              {t('teamsIntegration.addChannel')}
            </Button>
          </div>

          <Card>
            <CardContent className="pt-6">
              {channelConfigs.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
                  {t('teamsIntegration.noChannels')}
                </p>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t('teamsIntegration.channelName')}</TableHead>
                        <TableHead>{t('teamsIntegration.taskAssigned')}</TableHead>
                        <TableHead>{t('teamsIntegration.taskCompleted')}</TableHead>
                        <TableHead>{t('teamsIntegration.taskBlocked')}</TableHead>
                        <TableHead>{t('teamsIntegration.cycleEvents')}</TableHead>
                        <TableHead className="text-right">{t('common.actions')}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {channelConfigs.map((channel) => (
                        <TableRow key={channel.id}>
                          <TableCell className="font-medium">{channel.channelName}</TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskAssigned ? 'default' : 'secondary'}>
                              {channel.notifyTaskAssigned ? t('common.yes') : t('common.no')}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskCompleted ? 'default' : 'secondary'}>
                              {channel.notifyTaskCompleted ? t('common.yes') : t('common.no')}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskBlocked ? 'default' : 'secondary'}>
                              {channel.notifyTaskBlocked ? t('common.yes') : t('common.no')}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge
                              variant={
                                channel.notifyCycleStarted || channel.notifyCycleCooldown
                                  ? 'default'
                                  : 'secondary'
                              }
                            >
                              {channel.notifyCycleStarted || channel.notifyCycleCooldown ? t('common.yes') : t('common.no')}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <Button
                              variant="ghost"
                              size="icon"
                              onClick={() => handleDeleteChannelConfig(channel.id)}
                            >
                              <Trash2 className="h-4 w-4 text-destructive" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Help Dialog */}
      <Dialog open={helpDialogOpen} onOpenChange={setHelpDialogOpen}>
        <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t('teamsIntegration.setupGuideTitle')}</DialogTitle>
            <DialogDescription>
              {t('teamsIntegration.setupGuideDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <h4 className="font-semibold">{t('teamsIntegration.step1Title')}</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>{t('teamsIntegration.step1_1')}</li>
                <li>{t('teamsIntegration.step1_2')}</li>
                <li>{t('teamsIntegration.step1_3')}</li>
                <li>{t('teamsIntegration.step1_4')}</li>
                <li dangerouslySetInnerHTML={{ __html: t('teamsIntegration.webhookNameExample') }} />
                <li>{t('teamsIntegration.step1_6')}</li>
                <li>{t('teamsIntegration.step1_7')}</li>
              </ol>
            </div>

            <div className="space-y-2">
              <h4 className="font-semibold">{t('teamsIntegration.step2Title')}</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>{t('teamsIntegration.step2_1')}</li>
                <li>{t('teamsIntegration.step2_2')}</li>
                <li>{t('teamsIntegration.step2_3')}</li>
                <li>{t('teamsIntegration.step2_4')}</li>
                <li>{t('teamsIntegration.step2_5')}</li>
              </ol>
            </div>

            <div className="space-y-2">
              <h4 className="font-semibold">{t('teamsIntegration.step3Title')}</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>{t('teamsIntegration.step3_1')}</li>
                <li>{t('teamsIntegration.step3_2')}</li>
                <li>{t('teamsIntegration.step3_3')}</li>
                <li>{t('teamsIntegration.step3_4')}</li>
              </ol>
            </div>

            <div className="bg-muted p-4 rounded-lg">
              <h4 className="font-semibold mb-2">{t('teamsIntegration.multipleChannelsTip')}</h4>
              <p className="text-sm text-muted-foreground" dangerouslySetInnerHTML={{ __html: t('teamsIntegration.channelPreferences') }} />
            </div>

            <div className="bg-blue-50 dark:bg-blue-950 p-4 rounded-lg">
              <h4 className="font-semibold mb-2 text-blue-700 dark:text-blue-400">{t('teamsIntegration.supportedNotificationsTitle')}</h4>
              <ul className="text-sm text-blue-600 dark:text-blue-300 space-y-1">
                <li>• {t('teamsIntegration.notifyTaskEvents')}</li>
                <li>• {t('teamsIntegration.notifyPitchEvents')}</li>
                <li>• {t('teamsIntegration.notifyCycleEvents')}</li>
                <li>• {t('teamsIntegration.notifyBettingEvents')}</li>
                <li>• {t('teamsIntegration.notifySprintEvents')}</li>
              </ul>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setHelpDialogOpen(false)}>{t('teamsIntegration.gotIt')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Configuration Dialog */}
      <Dialog open={configDialogOpen} onOpenChange={setConfigDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{t('teamsIntegration.configureTenantTitle')}</DialogTitle>
            <DialogDescription>
              {t('teamsIntegration.configureTenantDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="tenantName">{t('teamsIntegration.tenantOrganizationName')}</Label>
              <Input
                id="tenantName"
                value={configForm.tenantName}
                onChange={(e) => setConfigForm({ ...configForm, tenantName: e.target.value })}
                placeholder={t('teamsIntegration.orgPlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="webhookUrl">{t('teamsIntegration.webhookUrl')}</Label>
              <Input
                id="webhookUrl"
                value={configForm.webhookUrl}
                onChange={(e) => setConfigForm({ ...configForm, webhookUrl: e.target.value })}
                placeholder={t('teamsIntegration.webhookPlaceholder')}
              />
              <p className="text-xs text-muted-foreground">
                {t('teamsIntegration.webhookHelp')}
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="defaultChannel">{t('teamsIntegration.defaultChannelOptional')}</Label>
              <Input
                id="defaultChannel"
                value={configForm.defaultChannel}
                onChange={(e) => setConfigForm({ ...configForm, defaultChannel: e.target.value })}
                placeholder={t('teamsIntegration.channelNamePlaceholder')}
              />
            </div>
            <div className="flex items-center space-x-2">
              <Switch
                id="isEnabled"
                checked={configForm.isEnabled}
                onCheckedChange={(checked) => setConfigForm({ ...configForm, isEnabled: checked })}
              />
              <Label htmlFor="isEnabled">{t('teamsIntegration.enableIntegration')}</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfigDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleCreateConfiguration}>{t('common.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Channel Configuration Dialog */}
      <Dialog open={channelDialogOpen} onOpenChange={setChannelDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>{t('teamsIntegration.configureChannelTitle')}</DialogTitle>
            <DialogDescription>
              {t('teamsIntegration.configureChannelDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="channelName">{t('teamsIntegration.channelName')}</Label>
              <Input
                id="channelName"
                value={channelForm.channelName}
                onChange={(e) => setChannelForm({ ...channelForm, channelName: e.target.value })}
                placeholder={t('teamsIntegration.teamNamePlaceholder')}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="channelWebhookUrl">{t('teamsIntegration.channelWebhookOptional')}</Label>
              <Input
                id="channelWebhookUrl"
                value={channelForm.channelWebhookUrl}
                onChange={(e) =>
                  setChannelForm({ ...channelForm, channelWebhookUrl: e.target.value })
                }
                placeholder={t('teamsIntegration.webhookPlaceholder')}
              />
            </div>

            <div>
              <h4 className="text-sm font-medium mb-3">{t('teamsIntegration.notificationPreferences')}</h4>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskAssigned"
                    checked={channelForm.notifyTaskAssigned}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskAssigned: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskAssigned">Task Assigned</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskCompleted"
                    checked={channelForm.notifyTaskCompleted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskCompleted: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskCompleted">Task Completed</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskBlocked"
                    checked={channelForm.notifyTaskBlocked}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskBlocked: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskBlocked">Task Blocked</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyPitchShaped"
                    checked={channelForm.notifyPitchShaped}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyPitchShaped: checked })
                    }
                  />
                  <Label htmlFor="notifyPitchShaped">Pitch Shaped</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyCycleStarted"
                    checked={channelForm.notifyCycleStarted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyCycleStarted: checked })
                    }
                  />
                  <Label htmlFor="notifyCycleStarted">Cycle Started</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyCycleCooldown"
                    checked={channelForm.notifyCycleCooldown}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyCycleCooldown: checked })
                    }
                  />
                  <Label htmlFor="notifyCycleCooldown">Cycle Cooldown</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyBettingCompleted"
                    checked={channelForm.notifyBettingCompleted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyBettingCompleted: checked })
                    }
                  />
                  <Label htmlFor="notifyBettingCompleted">Betting Completed</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifySprintStarted"
                    checked={channelForm.notifySprintStarted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifySprintStarted: checked })
                    }
                  />
                  <Label htmlFor="notifySprintStarted">Sprint Started</Label>
                </div>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setChannelDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleCreateChannelConfig}>{t('common.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Test Notification Dialog */}
      <Dialog open={testDialogOpen} onOpenChange={setTestDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{t('teamsIntegration.sendTestTitle')}</DialogTitle>
            <DialogDescription>
              {t('teamsIntegration.sendTestDescription')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="testMessage">{t('teamsIntegration.message')}</Label>
              <textarea
                id="testMessage"
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={testMessage}
                onChange={(e) => setTestMessage(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="testChannel">{t('teamsIntegration.channelOptional')}</Label>
              <Input
                id="testChannel"
                value={testChannel}
                onChange={(e) => setTestChannel(e.target.value)}
                placeholder={t('teamsIntegration.channelWebhookPlaceholder')}
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTestDialogOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={handleSendTestNotification}>
              <Send className="mr-2 h-4 w-4" />
              {t('teamsIntegration.sendTest')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
