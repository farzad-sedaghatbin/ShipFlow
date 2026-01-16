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
  // i18n ready
  if (false) console.log(t('slackIntegration.title'));
  
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
      setError(err.response?.data?.message || 'Failed to fetch Slack configurations');
    } finally {
      setLoading(false);
    }
  };

  const fetchChannelConfigs = async (configId: number) => {
    try {
      const channels = await slackService.getChannelConfigs(configId);
      setChannelConfigs(channels);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch channel configurations');
    }
  };

  const handleCreateConfiguration = async () => {
    try {
      await slackService.createConfiguration(configForm);
      setSuccess('Slack workspace configured successfully');
      setConfigDialogOpen(false);
      setConfigForm({
        workspaceName: '',
        webhookUrl: '',
        defaultChannel: '',
        isEnabled: true,
      });
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create Slack configuration');
    }
  };

  const handleDeleteConfiguration = async (configId: number) => {
    if (!confirm('Are you sure you want to delete this Slack configuration?')) {
      return;
    }
    
    try {
      await slackService.deleteConfiguration(configId);
      setSuccess('Slack configuration deleted successfully');
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete Slack configuration');
    }
  };

  const handleCreateChannelConfig = async () => {
    if (!activeConfig) return;
    
    try {
      await slackService.createChannelConfig(activeConfig.id, channelForm);
      setSuccess('Channel configuration saved successfully');
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
      setError(err.response?.data?.message || 'Failed to create channel configuration');
    }
  };

  const handleDeleteChannelConfig = async (channelConfigId: number) => {
    if (!confirm('Are you sure you want to delete this channel configuration?')) {
      return;
    }
    
    try {
      await slackService.deleteChannelConfig(channelConfigId);
      setSuccess('Channel configuration deleted successfully');
      if (activeConfig) {
        fetchChannelConfigs(activeConfig.id);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete channel configuration');
    }
  };

  const handleSendTestNotification = async () => {
    if (!activeConfig) return;
    
    try {
      await slackService.sendTestNotification(activeConfig.id, {
        message: testMessage,
        channel: testChannel,
      });
      setSuccess('Test notification sent successfully');
      setTestDialogOpen(false);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to send test notification');
    }
  };

  return (
    <div className="p-6">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold">Slack Integration</h1>
        <div className="flex gap-2">
          <Button onClick={() => setConfigDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Configure Workspace
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
          <TabsTrigger value="workspace">Workspace Configuration</TabsTrigger>
          <TabsTrigger value="channels" disabled={!activeConfig}>
            Channel Notifications
          </TabsTrigger>
        </TabsList>

        <TabsContent value="workspace" className="mt-6">
          <Card>
            <CardContent className="pt-6">
              {configurations.length === 0 ? (
                <p className="text-muted-foreground">
                  No Slack workspace configured. Click "Configure Workspace" to get started.
                </p>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Workspace Name</TableHead>
                        <TableHead>Webhook URL</TableHead>
                        <TableHead>Default Channel</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
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
                              {config.isEnabled ? 'Enabled' : 'Disabled'}
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
            <h2 className="text-xl font-semibold">Channel Notification Settings</h2>
            <Button onClick={() => setChannelDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add Channel
            </Button>
          </div>

          <Card>
            <CardContent className="pt-6">
              {channelConfigs.length === 0 ? (
                <p className="text-muted-foreground">
                  No channel configurations. Click "Add Channel" to configure notification preferences for specific channels.
                </p>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Channel Name</TableHead>
                        <TableHead>Task Assigned</TableHead>
                        <TableHead>Task Completed</TableHead>
                        <TableHead>Task Blocked</TableHead>
                        <TableHead>Cycle Events</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
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
            <DialogTitle>Configure Slack Workspace</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="workspaceName">Workspace Name</Label>
              <Input
                id="workspaceName"
                value={configForm.workspaceName}
                onChange={(e) => setConfigForm({ ...configForm, workspaceName: e.target.value })}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="webhookUrl">Webhook URL</Label>
              <Input
                id="webhookUrl"
                value={configForm.webhookUrl}
                onChange={(e) => setConfigForm({ ...configForm, webhookUrl: e.target.value })}
                placeholder="https://hooks.slack.com/services/..."
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="defaultChannel">Default Channel (optional)</Label>
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
              <Label htmlFor="isEnabled">Enable Slack Integration</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setConfigDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleCreateConfiguration}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Channel Configuration Dialog */}
      <Dialog open={channelDialogOpen} onOpenChange={setChannelDialogOpen}>
        <DialogContent className="sm:max-w-[600px]">
          <DialogHeader>
            <DialogTitle>Configure Channel Notifications</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="channelName">Channel Name</Label>
              <Input
                id="channelName"
                value={channelForm.channelName}
                onChange={(e) => setChannelForm({ ...channelForm, channelName: e.target.value })}
                placeholder="general"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="channelWebhookUrl">Channel-Specific Webhook URL (optional)</Label>
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
              <h4 className="text-sm font-medium mb-3">Notification Preferences</h4>
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
              Cancel
            </Button>
            <Button onClick={handleCreateChannelConfig}>Save</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Test Notification Dialog */}
      <Dialog open={testDialogOpen} onOpenChange={setTestDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Send Test Notification</DialogTitle>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="testMessage">Message</Label>
              <textarea
                id="testMessage"
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                value={testMessage}
                onChange={(e) => setTestMessage(e.target.value)}
                rows={3}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="testChannel">Channel (optional)</Label>
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
              Cancel
            </Button>
            <Button onClick={handleSendTestNotification}>
              <Send className="mr-2 h-4 w-4" />
              Send Test
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
