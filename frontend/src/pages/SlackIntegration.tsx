import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Trash2, Send, RefreshCw } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Switch } from '../components/ui/switch';
import { Card, CardContent } from '../components/ui/card';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Badge } from '../components/ui/badge';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../components/ui/tabs';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  slackService,
  SlackConfiguration,
  SlackChannelConfig,
  CreateSlackConfigurationRequest,
  CreateSlackChannelConfigRequest,
} from '../services/slackService';

export default function SlackIntegrationPage() {
  const { t } = useTranslation();
  const [tabValue, setTabValue] = useState('workspace');
  const [configurations, setConfigurations] = useState<SlackConfiguration[]>([]);
  const [activeConfig, setActiveConfig] = useState<SlackConfiguration | null>(null);
  const [channelConfigs, setChannelConfigs] = useState<SlackChannelConfig[]>([]);
  const [_loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  
  // Dialog states
  const [configDialogOpen, setConfigDialogOpen] = useState(false);
  const [channelDialogOpen, setChannelDialogOpen] = useState(false);
  const [testDialogOpen, setTestDialogOpen] = useState(false);
  
  // Form states
  const [configForm, setConfigForm] = useState<CreateSlackConfigurationRequest>({
    workspaceName: '',
    webhookUrl: '',
    defaultChannel: '',
    isEnabled: true,
  });
  
  const [channelForm, setChannelForm] = useState<CreateSlackChannelConfigRequest>({
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

  const fetchConfigurations = async () => {
    try {
      setLoading(true);
      const configs = await slackService.getAllConfigurations();
      setConfigurations(configs);
      
      const active = await slackService.getActiveConfiguration();
      setActiveConfig(active);
      
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.loadFailed'));
    } finally {
      setLoading(false);
    }
  };

  const fetchChannelConfigs = async (configId: number) => {
    try {
      const channels = await slackService.getChannelConfigs(configId);
      setChannelConfigs(channels);
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.channelLoadFailed'));
    }
  };

  const handleCreateConfiguration = async () => {
    try {
      await slackService.createConfiguration(configForm);
      setSuccess(t('slackIntegration.configCreated'));
      setConfigDialogOpen(false);
      setConfigForm({
        workspaceName: '',
        webhookUrl: '',
        defaultChannel: '',
        isEnabled: true,
      });
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.createFailed'));
    }
  };

  const handleDeleteConfiguration = async (configId: number) => {
    if (!confirm(t('slackIntegration.deleteConfirm'))) {
      return;
    }
    
    try {
      await slackService.deleteConfiguration(configId);
      setSuccess(t('slackIntegration.configDeleted'));
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.deleteFailed'));
    }
  };

  const handleCreateChannelConfig = async () => {
    if (!activeConfig) return;
    
    try {
      await slackService.createChannelConfig(activeConfig.id, channelForm);
      setSuccess(t('slackIntegration.channelCreated'));
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
      setError(err.response?.data?.message || t('slackIntegration.channelCreateFailed'));
    }
  };

  const handleDeleteChannelConfig = async (channelConfigId: number) => {
    if (!confirm(t('slackIntegration.deleteChannelConfirm'))) {
      return;
    }
    
    try {
      await slackService.deleteChannelConfig(channelConfigId);
      setSuccess(t('slackIntegration.channelDeleted'));
      if (activeConfig) {
        fetchChannelConfigs(activeConfig.id);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.channelDeleteFailed'));
    }
  };

  const handleSendTestNotification = async () => {
    if (!activeConfig) return;
    
    try {
      await slackService.sendTestNotification(activeConfig.id, {
        message: testMessage,
        channel: testChannel,
      });
      setSuccess(t('slackIntegration.testSent'));
      setTestDialogOpen(false);
    } catch (err: any) {
      setError(err.response?.data?.message || t('slackIntegration.testFailed'));
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">{t('slackIntegration.title')}</h1>
        <div className="flex gap-2">
          <Button onClick={() => setConfigDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            {t('slackIntegration.workspace')}
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
        <Alert className="mb-4">
          <AlertDescription>{success}</AlertDescription>
        </Alert>
      )}

      <Tabs value={tabValue} onValueChange={setTabValue} className="mb-4">
        <TabsList>
          <TabsTrigger value="workspace">{t('slackIntegration.workspace')}</TabsTrigger>
          <TabsTrigger value="channels" disabled={!activeConfig}>
            {t('slackIntegration.channels')}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="workspace" className="mt-6">
          <Card>
            <CardContent className="pt-6">
              {configurations.length === 0 ? (
                <p className="text-muted-foreground">
                  No Slack workspace configured. Click "{t('slackIntegration.workspace')}" to get started.
                </p>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t('slackIntegration.workspaceName')}</TableHead>
                        <TableHead>{t('slackIntegration.webhookUrl')}</TableHead>
                        <TableHead>{t('slackIntegration.defaultChannel')}</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">{t('slackIntegration.actions')}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {configurations.map((config) => (
                        <TableRow key={config.id}>
                          <TableCell>{config.workspaceName}</TableCell>
                          <TableCell>
                            <code className="text-xs font-mono">
                              {config.webhookUrl.substring(0, 50)}...
                            </code>
                          </TableCell>
                          <TableCell>{config.defaultChannel || '-'}</TableCell>
                          <TableCell>
                            <Badge variant={config.isEnabled ? 'default' : 'secondary'}>
                              {config.isEnabled ? t('slackIntegration.enabled') : 'Disabled'}
                            </Badge>
                          </TableCell>
                          <TableCell className="text-right">
                            <div className="flex justify-end gap-2">
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => setTestDialogOpen(true)}
                                disabled={!config.isEnabled}
                              >
                                <Send className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => handleDeleteConfiguration(config.id)}
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
            <h2 className="text-xl font-semibold">{t('slackIntegration.notificationSettings')}</h2>
            <Button onClick={() => setChannelDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              {t('slackIntegration.addChannel')}
            </Button>
          </div>

          <Card>
            <CardContent className="pt-6">
              {channelConfigs.length === 0 ? (
                <p className="text-muted-foreground">
                  No channel configurations. Click "{t('slackIntegration.addChannel')}" to configure notification preferences for specific channels.
                </p>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>{t('slackIntegration.channelName')}</TableHead>
                        <TableHead>{t('slackIntegration.taskAssigned')}</TableHead>
                        <TableHead>{t('slackIntegration.taskCompleted')}</TableHead>
                        <TableHead>{t('slackIntegration.taskBlocked')}</TableHead>
                        <TableHead>Cycle Events</TableHead>
                        <TableHead className="text-right">{t('slackIntegration.actions')}</TableHead>
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {channelConfigs.map((channel) => (
                        <TableRow key={channel.id}>
                          <TableCell>#{channel.channelName}</TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskAssigned ? 'default' : 'secondary'}>
                              {channel.notifyTaskAssigned ? 'Yes' : 'No'}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskCompleted ? 'default' : 'secondary'}>
                              {channel.notifyTaskCompleted ? 'Yes' : 'No'}
                            </Badge>
                          </TableCell>
                          <TableCell>
                            <Badge variant={channel.notifyTaskBlocked ? 'default' : 'secondary'}>
                              {channel.notifyTaskBlocked ? 'Yes' : 'No'}
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
                              {channel.notifyCycleStarted || channel.notifyCycleCooldown ? 'Yes' : 'No'}
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

      {/* Configuration Dialog */}
      <Dialog open={configDialogOpen} onOpenChange={setConfigDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{t('slackIntegration.workspace')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="workspaceName">{t('slackIntegration.workspaceName')}</Label>
              <Input
                id="workspaceName"
                value={configForm.workspaceName}
                onChange={(e) => setConfigForm({ ...configForm, workspaceName: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="webhookUrl">{t('slackIntegration.webhookUrl')}</Label>
              <Input
                id="webhookUrl"
                value={configForm.webhookUrl}
                onChange={(e) => setConfigForm({ ...configForm, webhookUrl: e.target.value })}
                placeholder="https://hooks.slack.com/services/..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="defaultChannel">{t('slackIntegration.defaultChannel')} (optional)</Label>
              <Input
                id="defaultChannel"
                value={configForm.defaultChannel}
                onChange={(e) => setConfigForm({ ...configForm, defaultChannel: e.target.value })}
                placeholder="general"
              />
            </div>
            <div className="flex items-center space-x-2">
              <Switch
                id="isEnabled"
                checked={configForm.isEnabled}
                onCheckedChange={(checked) => setConfigForm({ ...configForm, isEnabled: checked })}
              />
              <Label htmlFor="isEnabled">{t('slackIntegration.enabled')}</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfigDialogOpen(false)}>
              {t('slackIntegration.cancel')}
            </Button>
            <Button onClick={handleCreateConfiguration}>{t('slackIntegration.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Channel Configuration Dialog */}
      <Dialog open={channelDialogOpen} onOpenChange={setChannelDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>{t('slackIntegration.notificationSettings')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="channelName">{t('slackIntegration.channelName')}</Label>
              <Input
                id="channelName"
                value={channelForm.channelName}
                onChange={(e) => setChannelForm({ ...channelForm, channelName: e.target.value })}
                placeholder="general"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="channelWebhookUrl">{t('slackIntegration.channelWebhook')}</Label>
              <Input
                id="channelWebhookUrl"
                value={channelForm.channelWebhookUrl}
                onChange={(e) =>
                  setChannelForm({ ...channelForm, channelWebhookUrl: e.target.value })
                }
                placeholder="Leave empty to use workspace webhook"
              />
            </div>

            <div>
              <h4 className="text-sm font-medium mb-3">{t('slackIntegration.notificationSettings')}</h4>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskAssigned"
                    checked={channelForm.notifyTaskAssigned}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskAssigned: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskAssigned">{t('slackIntegration.taskAssigned')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskCompleted"
                    checked={channelForm.notifyTaskCompleted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskCompleted: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskCompleted">{t('slackIntegration.taskCompleted')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyTaskBlocked"
                    checked={channelForm.notifyTaskBlocked}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyTaskBlocked: checked })
                    }
                  />
                  <Label htmlFor="notifyTaskBlocked">{t('slackIntegration.taskBlocked')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyPitchShaped"
                    checked={channelForm.notifyPitchShaped}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyPitchShaped: checked })
                    }
                  />
                  <Label htmlFor="notifyPitchShaped">{t('slackIntegration.pitchShaped')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyCycleStarted"
                    checked={channelForm.notifyCycleStarted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyCycleStarted: checked })
                    }
                  />
                  <Label htmlFor="notifyCycleStarted">{t('slackIntegration.cycleStarted')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyCycleCooldown"
                    checked={channelForm.notifyCycleCooldown}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyCycleCooldown: checked })
                    }
                  />
                  <Label htmlFor="notifyCycleCooldown">{t('slackIntegration.cycleCooldown')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifyBettingCompleted"
                    checked={channelForm.notifyBettingCompleted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifyBettingCompleted: checked })
                    }
                  />
                  <Label htmlFor="notifyBettingCompleted">{t('slackIntegration.bettingCompleted')}</Label>
                </div>
                <div className="flex items-center space-x-2">
                  <Switch
                    id="notifySprintStarted"
                    checked={channelForm.notifySprintStarted}
                    onCheckedChange={(checked) =>
                      setChannelForm({ ...channelForm, notifySprintStarted: checked })
                    }
                  />
                  <Label htmlFor="notifySprintStarted">{t('slackIntegration.sprintStarted')}</Label>
                </div>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setChannelDialogOpen(false)}>
              {t('slackIntegration.cancel')}
            </Button>
            <Button onClick={handleCreateChannelConfig}>{t('slackIntegration.save')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Test Notification Dialog */}
      <Dialog open={testDialogOpen} onOpenChange={setTestDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>{t('slackIntegration.testNotification')}</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="testMessage">{t('slackIntegration.testMessage')}</Label>
              <textarea
                id="testMessage"
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={testMessage}
                onChange={(e) => setTestMessage(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="testChannel">{t('slackIntegration.testChannel')}</Label>
              <Input
                id="testChannel"
                value={testChannel}
                onChange={(e) => setTestChannel(e.target.value)}
                placeholder="Leave empty to use default channel"
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTestDialogOpen(false)}>
              {t('slackIntegration.cancel')}
            </Button>
            <Button onClick={handleSendTestNotification}>
              <Send className="mr-2 h-4 w-4" />
              {t('slackIntegration.sendTest')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
