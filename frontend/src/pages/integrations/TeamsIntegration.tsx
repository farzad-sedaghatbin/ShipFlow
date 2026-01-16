import { useState, useEffect } from 'react';
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
      setError(err.response?.data?.message || 'Failed to fetch Teams configurations');
    } finally {
      setLoading(false);
    }
  };

  const fetchChannelConfigs = async (configId: number) => {
    try {
      const channels = await teamsService.getChannelConfigs(configId);
      setChannelConfigs(channels);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to fetch channel configurations');
    }
  };

  const handleCreateConfiguration = async () => {
    try {
      await teamsService.createConfiguration(configForm);
      setSuccess('Microsoft Teams workspace configured successfully');
      setConfigDialogOpen(false);
      setConfigForm({
        tenantName: '',
        webhookUrl: '',
        defaultChannel: '',
        isEnabled: true,
      });
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to create Teams configuration');
    }
  };

  const handleDeleteConfiguration = async (configId: number) => {
    if (!confirm('Are you sure you want to delete this Teams configuration?')) {
      return;
    }
    
    try {
      await teamsService.deleteConfiguration(configId);
      setSuccess('Teams configuration deleted successfully');
      fetchConfigurations();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to delete Teams configuration');
    }
  };

  const handleCreateChannelConfig = async () => {
    if (!activeConfig) return;
    
    try {
      await teamsService.createChannelConfig(activeConfig.id, channelForm);
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
      await teamsService.deleteChannelConfig(channelConfigId);
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
      await teamsService.sendTestNotification(activeConfig.id, {
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
        <div>
          <h1 className="text-3xl font-bold">Microsoft Teams Integration</h1>
          <p className="text-muted-foreground mt-1">
            Connect ShipFlow to Microsoft Teams for real-time notifications
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setHelpDialogOpen(true)}>
            <HelpCircle className="mr-2 h-4 w-4" />
            Setup Guide
          </Button>
          <Button onClick={() => setConfigDialogOpen(true)}>
            <Plus className="mr-2 h-4 w-4" />
            Configure Tenant
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
          <TabsTrigger value="workspace">Tenant Configuration</TabsTrigger>
          <TabsTrigger value="channels" disabled={!activeConfig}>
            Channel Notifications
          </TabsTrigger>
        </TabsList>

        <TabsContent value="workspace" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Teams Tenant Configuration</CardTitle>
              <CardDescription>
                Configure your Microsoft Teams tenant with an incoming webhook URL
              </CardDescription>
            </CardHeader>
            <CardContent>
              {configurations.length === 0 ? (
                <div className="text-center py-8">
                  <p className="text-muted-foreground mb-4">
                    No Microsoft Teams tenant configured. Click "Configure Tenant" to get started.
                  </p>
                  <Button onClick={() => setHelpDialogOpen(true)} variant="outline">
                    View Setup Guide
                  </Button>
                </div>
              ) : (
                <div className="rounded-md border">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Tenant Name</TableHead>
                        <TableHead>Webhook URL</TableHead>
                        <TableHead>Default Channel</TableHead>
                        <TableHead>Status</TableHead>
                        <TableHead className="text-right">Actions</TableHead>
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
                                title="Send test notification"
                              >
                                <Send className="h-4 w-4" />
                              </Button>
                              <Button
                                variant="ghost"
                                size="icon"
                                onClick={() => handleDeleteConfiguration(config.id)}
                                title="Delete configuration"
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
              <h2 className="text-xl font-semibold">Channel Notification Settings</h2>
              <p className="text-sm text-muted-foreground">
                Configure which notifications are sent to specific Teams channels
              </p>
            </div>
            <Button onClick={() => setChannelDialogOpen(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Add Channel
            </Button>
          </div>

          <Card>
            <CardContent className="pt-6">
              {channelConfigs.length === 0 ? (
                <p className="text-muted-foreground text-center py-8">
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
                          <TableCell className="font-medium">{channel.channelName}</TableCell>
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

      {/* Help Dialog */}
      <Dialog open={helpDialogOpen} onOpenChange={setHelpDialogOpen}>
        <DialogContent className="sm:max-w-[600px] max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>Microsoft Teams Integration Setup Guide</DialogTitle>
            <DialogDescription>
              Follow these steps to connect ShipFlow to Microsoft Teams
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <h4 className="font-semibold">Step 1: Create an Incoming Webhook</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>Open Microsoft Teams and go to the channel where you want notifications</li>
                <li>Click on the <strong>•••</strong> (More options) next to the channel name</li>
                <li>Select <strong>Connectors</strong> (or <strong>Manage connectors</strong>)</li>
                <li>Search for <strong>Incoming Webhook</strong> and click <strong>Configure</strong></li>
                <li>Give your webhook a name (e.g., "ShipFlow") and optionally upload an icon</li>
                <li>Click <strong>Create</strong></li>
                <li>Copy the webhook URL that is generated</li>
              </ol>
            </div>

            <div className="space-y-2">
              <h4 className="font-semibold">Step 2: Configure in ShipFlow</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>Click <strong>Configure Tenant</strong> above</li>
                <li>Enter your organization/tenant name</li>
                <li>Paste the webhook URL you copied from Teams</li>
                <li>Optionally set a default channel name</li>
                <li>Click <strong>Save</strong></li>
              </ol>
            </div>

            <div className="space-y-2">
              <h4 className="font-semibold">Step 3: Test the Integration</h4>
              <ol className="list-decimal list-inside space-y-1 text-sm text-muted-foreground ml-2">
                <li>Click the <strong>Send</strong> icon next to your configuration</li>
                <li>Enter a test message</li>
                <li>Click <strong>Send Test</strong></li>
                <li>Check your Teams channel for the notification</li>
              </ol>
            </div>

            <div className="bg-muted p-4 rounded-lg">
              <h4 className="font-semibold mb-2">💡 Tip: Multiple Channels</h4>
              <p className="text-sm text-muted-foreground">
                You can create webhooks for multiple channels and configure different notification
                preferences for each. Go to the "Channel Notifications" tab to add more channels.
              </p>
            </div>

            <div className="bg-blue-50 dark:bg-blue-950 p-4 rounded-lg">
              <h4 className="font-semibold mb-2 text-blue-700 dark:text-blue-400">📋 Supported Notifications</h4>
              <ul className="text-sm text-blue-600 dark:text-blue-300 space-y-1">
                <li>• Task assigned, completed, or blocked</li>
                <li>• Pitch shaped</li>
                <li>• Cycle started or entered cooldown</li>
                <li>• Betting completed</li>
                <li>• Sprint started</li>
              </ul>
            </div>
          </div>
          <DialogFooter>
            <Button onClick={() => setHelpDialogOpen(false)}>Got it!</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Configuration Dialog */}
      <Dialog open={configDialogOpen} onOpenChange={setConfigDialogOpen}>
        <DialogContent className="sm:max-w-[500px]">
          <DialogHeader>
            <DialogTitle>Configure Microsoft Teams</DialogTitle>
            <DialogDescription>
              Enter your Teams tenant details and incoming webhook URL
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="tenantName">Tenant/Organization Name</Label>
              <Input
                id="tenantName"
                value={configForm.tenantName}
                onChange={(e) => setConfigForm({ ...configForm, tenantName: e.target.value })}
                placeholder="My Organization"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="webhookUrl">Webhook URL</Label>
              <Input
                id="webhookUrl"
                value={configForm.webhookUrl}
                onChange={(e) => setConfigForm({ ...configForm, webhookUrl: e.target.value })}
                placeholder="https://outlook.office.com/webhook/..."
              />
              <p className="text-xs text-muted-foreground">
                Get this from Teams: Channel → Connectors → Incoming Webhook
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="defaultChannel">Default Channel (optional)</Label>
              <Input
                id="defaultChannel"
                value={configForm.defaultChannel}
                onChange={(e) => setConfigForm({ ...configForm, defaultChannel: e.target.value })}
                placeholder="General"
              />
            </div>
            <div className="flex items-center space-x-2">
              <Switch
                id="isEnabled"
                checked={configForm.isEnabled}
                onCheckedChange={(checked) => setConfigForm({ ...configForm, isEnabled: checked })}
              />
              <Label htmlFor="isEnabled">Enable Teams Integration</Label>
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
            <DialogDescription>
              Choose which notifications to send to this Teams channel
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="channelName">Channel Name</Label>
              <Input
                id="channelName"
                value={channelForm.channelName}
                onChange={(e) => setChannelForm({ ...channelForm, channelName: e.target.value })}
                placeholder="General"
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
                placeholder="Leave empty to use tenant webhook"
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
            <DialogDescription>
              Send a test message to verify your Teams integration is working
            </DialogDescription>
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
