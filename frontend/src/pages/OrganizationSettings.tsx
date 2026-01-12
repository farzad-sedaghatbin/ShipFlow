import { useState, useEffect } from 'react';
import {
  Settings,
  Save,
  RotateCcw,
  Loader2,
  ShieldAlert,
  Calendar,
  AlertTriangle,
  Tags,
  Globe,
  Bell,
  Sparkles,
  Clock,
  Palette,
  Bug,
} from 'lucide-react';
import { useToast, useAuth } from '../contexts';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { OrganizationSettings, RiskThresholds, CategoryConfig, ColorSettings, BugStatusConfig, SeverityLevelConfig } from '../types/organizationSettings';
import { cn } from '../lib/utils';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Separator } from '../components/ui/separator';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Switch } from '../components/ui/switch';
import { Badge } from '../components/ui/badge';

const DEFAULT_RISK_THRESHOLDS: RiskThresholds = {
  lowMax: 30,
  mediumMax: 60,
  highMax: 85,
};

const DEFAULT_COLORS: ColorSettings = {
  appetiteHours: '#3B82F6',
  actualHours: '#10B981',
  overBudget: '#EF4444',
  underBudget: '#22C55E',
};

const TIME_ZONES = [
  'UTC',
  'America/New_York',
  'America/Chicago',
  'America/Denver',
  'America/Los_Angeles',
  'Europe/London',
  'Europe/Paris',
  'Asia/Tokyo',
  'Asia/Shanghai',
  'Australia/Sydney',
];

const DATE_FORMATS = [
  { value: 'MM/DD/YYYY', label: 'MM/DD/YYYY (US)' },
  { value: 'DD/MM/YYYY', label: 'DD/MM/YYYY (Europe)' },
  { value: 'YYYY-MM-DD', label: 'YYYY-MM-DD (ISO)' },
];

export default function OrganizationSettingsPage() {
  const { showToast } = useToast();
  const { user: currentUser } = useAuth();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<OrganizationSettings | null>(null);
  const [formData, setFormData] = useState<Partial<OrganizationSettings>>({});

  const isAdmin = currentUser?.role === 'ADMIN';

  useEffect(() => {
    if (isAdmin) {
      fetchSettings();
    }
  }, [isAdmin]);

  const fetchSettings = async () => {
    try {
      const response = await organizationSettingsService.getSettings();
      setSettings(response.data);
      setFormData(response.data);
    } catch (error: any) {
      if (error.response?.status === 404) {
        // Settings don't exist yet, use defaults
        const defaultSettings: Partial<OrganizationSettings> = {
          organizationName: 'My Organization',
          defaultCycleLengthWeeks: 6,
          defaultCooldownWeeks: 2,
          riskThresholds: DEFAULT_RISK_THRESHOLDS,
          timeZone: 'UTC',
          dateFormat: 'MM/DD/YYYY',
          enableNotifications: true,
          enableAIFeatures: true,
          taskCategories: [
            { name: 'PITCH_SCOPE', description: 'Work related to pitch deliverables', color: '#3B82F6', isActive: true, order: 1 },
            { name: 'DEBT_IMPROVEMENT', description: 'Technical debt and improvements', color: '#F59E0B', isActive: true, order: 2 },
          ],
          pitchCategories: [
            { name: 'FEATURE', description: 'New feature development', color: '#10B981', isActive: true, order: 1 },
            { name: 'INFRASTRUCTURE', description: 'Infrastructure and architecture', color: '#6366F1', isActive: true, order: 2 },
            { name: 'REFACTOR', description: 'Code refactoring', color: '#8B5CF6', isActive: true, order: 3 },
            { name: 'BUG_FIX', description: 'Bug fixes', color: '#EF4444', isActive: true, order: 4 },
          ],
          colors: DEFAULT_COLORS,
          bugStatuses: [
            { name: 'NEW', description: 'Newly reported', color: '#3B82F6', isActive: true, order: 1, isClosed: false },
            { name: 'IN_PROGRESS', description: 'Being worked on', color: '#F59E0B', isActive: true, order: 2, isClosed: false },
            { name: 'FIXED', description: 'Fix implemented', color: '#10B981', isActive: true, order: 3, isClosed: true },
            { name: 'VERIFIED', description: 'Fix verified', color: '#22C55E', isActive: true, order: 4, isClosed: true },
            { name: 'WONT_FIX', description: 'Will not fix', color: '#6B7280', isActive: true, order: 5, isClosed: true },
          ],
          severityLevels: [
            { name: 'CRITICAL', description: 'System down or data loss', color: '#DC2626', isActive: true, order: 1, priority: 1 },
            { name: 'HIGH', description: 'Major feature broken', color: '#F59E0B', isActive: true, order: 2, priority: 2 },
            { name: 'MEDIUM', description: 'Feature partially broken', color: '#3B82F6', isActive: true, order: 3, priority: 3 },
            { name: 'LOW', description: 'Minor issue or cosmetic', color: '#10B981', isActive: true, order: 4, priority: 4 },
          ],
        };
        setFormData(defaultSettings);
      } else {
        showToast('Failed to load organization settings', 'error');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await organizationSettingsService.updateSettings(formData);
      setSettings(response.data);
      setFormData(response.data);
      showToast('Organization settings saved successfully', 'success');
    } catch (error) {
      showToast('Failed to save settings', 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (!confirm('Are you sure you want to reset all settings to defaults? This cannot be undone.')) {
      return;
    }

    setSaving(true);
    try {
      const response = await organizationSettingsService.resetToDefaults();
      setSettings(response.data);
      setFormData(response.data);
      showToast('Settings reset to defaults', 'success');
    } catch (error) {
      showToast('Failed to reset settings', 'error');
    } finally {
      setSaving(false);
    }
  };

  const updateRiskThreshold = (field: keyof RiskThresholds, value: number) => {
    setFormData({
      ...formData,
      riskThresholds: {
        ...formData.riskThresholds!,
        [field]: value,
      },
    });
  };

  if (!isAdmin) {
    return (
      <div className="p-4">
        <Alert variant="destructive">
          <ShieldAlert className="h-4 w-4" />
          <AlertDescription>
            You do not have permission to access this page. Admin role required.
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight flex items-center gap-2">
            <Settings className="h-6 w-6" />
            Organization Settings
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            Configure organization-wide defaults and preferences
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleReset} disabled={saving} size="sm">
            <RotateCcw className="mr-2 h-4 w-4" />
            Reset to Defaults
          </Button>
          <Button onClick={handleSave} disabled={saving} size="sm">
            {saving ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Saving...
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                Save Changes
              </>
            )}
          </Button>
        </div>
      </div>

      <Tabs defaultValue="general" className="space-y-4">
        <TabsList>
          <TabsTrigger value="general">General</TabsTrigger>
          <TabsTrigger value="cycles">Cycles</TabsTrigger>
          <TabsTrigger value="risk">Risk Thresholds</TabsTrigger>
          <TabsTrigger value="colors">Colors</TabsTrigger>
          <TabsTrigger value="bugs">Bug Config</TabsTrigger>
          <TabsTrigger value="categories">Categories</TabsTrigger>
          <TabsTrigger value="features">Features</TabsTrigger>
        </TabsList>

        {/* General Settings */}
        <TabsContent value="general" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Globe className="h-5 w-5" />
                General Information
              </CardTitle>
              <CardDescription>Basic organization information and preferences</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="orgName">Organization Name</Label>
                <Input
                  id="orgName"
                  value={formData.organizationName || ''}
                  onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
                  placeholder="My Organization"
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="timezone">Time Zone</Label>
                  <select
                    id="timezone"
                    value={formData.timeZone || 'UTC'}
                    onChange={(e) => setFormData({ ...formData, timeZone: e.target.value })}
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {TIME_ZONES.map((tz) => (
                      <option key={tz} value={tz}>
                        {tz}
                      </option>
                    ))}
                  </select>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="dateFormat">Date Format</Label>
                  <select
                    id="dateFormat"
                    value={formData.dateFormat || 'MM/DD/YYYY'}
                    onChange={(e) => setFormData({ ...formData, dateFormat: e.target.value })}
                    className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
                  >
                    {DATE_FORMATS.map((fmt) => (
                      <option key={fmt.value} value={fmt.value}>
                        {fmt.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Cycle Settings */}
        <TabsContent value="cycles" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Calendar className="h-5 w-5" />
                Cycle Configuration
              </CardTitle>
              <CardDescription>Default cycle and cooldown duration settings</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="cycleLength">Default Cycle Length (weeks)</Label>
                  <Input
                    id="cycleLength"
                    type="number"
                    min="1"
                    max="12"
                    value={formData.defaultCycleLengthWeeks || 6}
                    onChange={(e) =>
                      setFormData({ ...formData, defaultCycleLengthWeeks: parseInt(e.target.value) })
                    }
                  />
                  <p className="text-xs text-muted-foreground">
                    Standard ShapeUp cycle is 6 weeks
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="cooldownLength">Default Cooldown (weeks)</Label>
                  <Input
                    id="cooldownLength"
                    type="number"
                    min="0"
                    max="4"
                    value={formData.defaultCooldownWeeks || 2}
                    onChange={(e) =>
                      setFormData({ ...formData, defaultCooldownWeeks: parseInt(e.target.value) })
                    }
                  />
                  <p className="text-xs text-muted-foreground">
                    Cooldown period between cycles (typically 2 weeks)
                  </p>
                </div>
              </div>

              <Alert>
                <Clock className="h-4 w-4" />
                <AlertDescription>
                  These defaults will be applied when creating new cycles. Existing cycles will not be affected.
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Risk Thresholds */}
        <TabsContent value="risk" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5" />
                Risk Thresholds
              </CardTitle>
              <CardDescription>
                Configure score ranges for risk levels (0-100 scale)
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 rounded-lg bg-green-50 dark:bg-green-950/20 border border-green-200 dark:border-green-900">
                  <div>
                    <div className="font-medium text-green-900 dark:text-green-100">LOW Risk</div>
                    <div className="text-sm text-green-700 dark:text-green-300">
                      Score: 0 - {formData.riskThresholds?.lowMax || 30}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lowMax" className="text-xs">Max Score</Label>
                    <Input
                      id="lowMax"
                      type="number"
                      min="0"
                      max="99"
                      value={formData.riskThresholds?.lowMax || 30}
                      onChange={(e) => updateRiskThreshold('lowMax', parseInt(e.target.value))}
                      className="w-20"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between p-4 rounded-lg bg-yellow-50 dark:bg-yellow-950/20 border border-yellow-200 dark:border-yellow-900">
                  <div>
                    <div className="font-medium text-yellow-900 dark:text-yellow-100">MEDIUM Risk</div>
                    <div className="text-sm text-yellow-700 dark:text-yellow-300">
                      Score: {(formData.riskThresholds?.lowMax || 30) + 1} - {formData.riskThresholds?.mediumMax || 60}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="mediumMax" className="text-xs">Max Score</Label>
                    <Input
                      id="mediumMax"
                      type="number"
                      min="0"
                      max="99"
                      value={formData.riskThresholds?.mediumMax || 60}
                      onChange={(e) => updateRiskThreshold('mediumMax', parseInt(e.target.value))}
                      className="w-20"
                    />
                  </div>
                </div>

                <div className="flex items-center justify-between p-4 rounded-lg bg-orange-50 dark:bg-orange-950/20 border border-orange-200 dark:border-orange-900">
                  <div>
                    <div className="font-medium text-orange-900 dark:text-orange-100">HIGH Risk</div>
                    <div className="text-sm text-orange-700 dark:text-orange-300">
                      Score: {(formData.riskThresholds?.mediumMax || 60) + 1} - {formData.riskThresholds?.highMax || 85}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="highMax" className="text-xs">Max Score</Label>
                    <Input
                      id="highMax"
                      type="number"
                      min="0"
                      max="99"
                      value={formData.riskThresholds?.highMax || 85}
                      onChange={(e) => updateRiskThreshold('highMax', parseInt(e.target.value))}
                      className="w-20"
                    />
                  </div>
                </div>

                <div className="p-4 rounded-lg bg-red-50 dark:bg-red-950/20 border border-red-200 dark:border-red-900">
                  <div className="font-medium text-red-900 dark:text-red-100">CRITICAL Risk</div>
                  <div className="text-sm text-red-700 dark:text-red-300">
                    Score: {(formData.riskThresholds?.highMax || 85) + 1} - 100
                  </div>
                </div>
              </div>

              <Alert>
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  Changes to risk thresholds will affect how existing pitches are categorized.
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Colors */}
        <TabsContent value="colors" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Palette className="h-5 w-5" />
                Color Configuration
              </CardTitle>
              <CardDescription>
                Customize colors for appetite/actual hours visualization
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="appetiteColor">Appetite Hours Color</Label>
                  <div className="flex gap-2 items-center">
                    <Input
                      id="appetiteColor"
                      type="color"
                      value={formData.colors?.appetiteHours || DEFAULT_COLORS.appetiteHours}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          colors: { ...formData.colors!, appetiteHours: e.target.value },
                        })
                      }
                      className="w-20 h-10"
                    />
                    <span className="text-sm text-muted-foreground">
                      {formData.colors?.appetiteHours || DEFAULT_COLORS.appetiteHours}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">Color for planned/budget hours</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="actualColor">Actual Hours Color</Label>
                  <div className="flex gap-2 items-center">
                    <Input
                      id="actualColor"
                      type="color"
                      value={formData.colors?.actualHours || DEFAULT_COLORS.actualHours}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          colors: { ...formData.colors!, actualHours: e.target.value },
                        })
                      }
                      className="w-20 h-10"
                    />
                    <span className="text-sm text-muted-foreground">
                      {formData.colors?.actualHours || DEFAULT_COLORS.actualHours}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">Color for logged/actual hours</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="overBudgetColor">Over Budget Color</Label>
                  <div className="flex gap-2 items-center">
                    <Input
                      id="overBudgetColor"
                      type="color"
                      value={formData.colors?.overBudget || DEFAULT_COLORS.overBudget}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          colors: { ...formData.colors!, overBudget: e.target.value },
                        })
                      }
                      className="w-20 h-10"
                    />
                    <span className="text-sm text-muted-foreground">
                      {formData.colors?.overBudget || DEFAULT_COLORS.overBudget}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">Warning color when over budget</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="underBudgetColor">Under Budget Color</Label>
                  <div className="flex gap-2 items-center">
                    <Input
                      id="underBudgetColor"
                      type="color"
                      value={formData.colors?.underBudget || DEFAULT_COLORS.underBudget}
                      onChange={(e) =>
                        setFormData({
                          ...formData,
                          colors: { ...formData.colors!, underBudget: e.target.value },
                        })
                      }
                      className="w-20 h-10"
                    />
                    <span className="text-sm text-muted-foreground">
                      {formData.colors?.underBudget || DEFAULT_COLORS.underBudget}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">Success color when under budget</p>
                </div>
              </div>

              <Alert>
                <Palette className="h-4 w-4" />
                <AlertDescription>
                  Colors will be applied to charts, progress bars, and hour tracking displays.
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Bug Configuration */}
        <TabsContent value="bugs" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Bug className="h-5 w-5" />
                Bug Tracking Configuration
              </CardTitle>
              <CardDescription>Configure bug statuses and severity levels</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="font-semibold mb-3">Bug Statuses</h3>
                <div className="space-y-2">
                  {formData.bugStatuses?.map((status, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                      <Input
                        type="color"
                        value={status.color}
                        onChange={(e) => {
                          const updated = [...(formData.bugStatuses || [])];
                          updated[index] = { ...updated[index], color: e.target.value };
                          setFormData({ ...formData, bugStatuses: updated });
                        }}
                        className="w-12 h-10 p-1 cursor-pointer"
                      />
                      <div className="flex-1 space-y-1">
                        <Input
                          value={status.name}
                          onChange={(e) => {
                            const updated = [...(formData.bugStatuses || [])];
                            updated[index] = { ...updated[index], name: e.target.value };
                            setFormData({ ...formData, bugStatuses: updated });
                          }}
                          className="font-medium"
                          placeholder="Status name"
                        />
                        <Input
                          value={status.description}
                          onChange={(e) => {
                            const updated = [...(formData.bugStatuses || [])];
                            updated[index] = { ...updated[index], description: e.target.value };
                            setFormData({ ...formData, bugStatuses: updated });
                          }}
                          className="text-sm"
                          placeholder="Description"
                        />
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1">
                          <Switch
                            checked={status.isActive}
                            onCheckedChange={(checked) => {
                              const updated = [...(formData.bugStatuses || [])];
                              updated[index] = { ...updated[index], isActive: checked };
                              setFormData({ ...formData, bugStatuses: updated });
                            }}
                          />
                          <Label className="text-xs">Active</Label>
                        </div>
                        <div className="flex items-center gap-1">
                          <Switch
                            checked={status.isClosed}
                            onCheckedChange={(checked) => {
                              const updated = [...(formData.bugStatuses || [])];
                              updated[index] = { ...updated[index], isClosed: checked };
                              setFormData({ ...formData, bugStatuses: updated });
                            }}
                          />
                          <Label className="text-xs">Closed</Label>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              <div>
                <h3 className="font-semibold mb-3">Severity Levels</h3>
                <div className="space-y-2">
                  {formData.severityLevels?.map((severity, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                      <Input
                        type="color"
                        value={severity.color}
                        onChange={(e) => {
                          const updated = [...(formData.severityLevels || [])];
                          updated[index] = { ...updated[index], color: e.target.value };
                          setFormData({ ...formData, severityLevels: updated });
                        }}
                        className="w-12 h-10 p-1 cursor-pointer"
                      />
                      <div className="flex-1 space-y-1">
                        <Input
                          value={severity.name}
                          onChange={(e) => {
                            const updated = [...(formData.severityLevels || [])];
                            updated[index] = { ...updated[index], name: e.target.value };
                            setFormData({ ...formData, severityLevels: updated });
                          }}
                          className="font-medium"
                          placeholder="Severity name"
                        />
                        <Input
                          value={severity.description}
                          onChange={(e) => {
                            const updated = [...(formData.severityLevels || [])];
                            updated[index] = { ...updated[index], description: e.target.value };
                            setFormData({ ...formData, severityLevels: updated });
                          }}
                          className="text-sm"
                          placeholder="Description"
                        />
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1">
                          <Label className="text-xs">Priority:</Label>
                          <Input
                            type="number"
                            min="1"
                            max="10"
                            value={severity.priority}
                            onChange={(e) => {
                              const updated = [...(formData.severityLevels || [])];
                              updated[index] = { ...updated[index], priority: parseInt(e.target.value) };
                              setFormData({ ...formData, severityLevels: updated });
                            }}
                            className="w-16"
                          />
                        </div>
                        <div className="flex items-center gap-1">
                          <Switch
                            checked={severity.isActive}
                            onCheckedChange={(checked) => {
                              const updated = [...(formData.severityLevels || [])];
                              updated[index] = { ...updated[index], isActive: checked };
                              setFormData({ ...formData, severityLevels: updated });
                            }}
                          />
                          <Label className="text-xs">Active</Label>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Categories */}
        <TabsContent value="categories" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Tags className="h-5 w-5" />
                Task & Pitch Categories
              </CardTitle>
              <CardDescription>Manage custom categories for tasks and pitches</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="font-semibold mb-3">Task Categories</h3>
                <div className="space-y-2">
                  {formData.taskCategories?.map((category, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                      <div
                        className="w-4 h-4 rounded"
                        style={{ backgroundColor: category.color }}
                      />
                      <div className="flex-1">
                        <div className="font-medium">{category.name}</div>
                        <div className="text-xs text-muted-foreground">{category.description}</div>
                      </div>
                      <Badge variant={category.isActive ? 'default' : 'secondary'}>
                        {category.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              <div>
                <h3 className="font-semibold mb-3">Pitch Categories</h3>
                <div className="space-y-2">
                  {formData.pitchCategories?.map((category, index) => (
                    <div key={index} className="flex items-center gap-3 p-3 rounded-lg border">
                      <div
                        className="w-4 h-4 rounded"
                        style={{ backgroundColor: category.color }}
                      />
                      <div className="flex-1">
                        <div className="font-medium">{category.name}</div>
                        <div className="text-xs text-muted-foreground">{category.description}</div>
                      </div>
                      <Badge variant={category.isActive ? 'default' : 'secondary'}>
                        {category.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Features */}
        <TabsContent value="features" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Sparkles className="h-5 w-5" />
                Feature Toggles
              </CardTitle>
              <CardDescription>Enable or disable organization-wide features</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-muted-foreground" />
                    <Label htmlFor="ai-features">AI-Powered Features</Label>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    Enable AI for risk analysis, Q&A, and intelligent suggestions
                  </p>
                </div>
                <Switch
                  id="ai-features"
                  checked={formData.enableAIFeatures ?? true}
                  onCheckedChange={(checked) =>
                    setFormData({ ...formData, enableAIFeatures: checked })
                  }
                />
              </div>
            </CardContent>
          </Card>

          {settings && (
            <Card>
              <CardHeader>
                <CardTitle className="text-sm">Last Updated</CardTitle>
              </CardHeader>
              <CardContent>
                <p className="text-sm text-muted-foreground">
                  {new Date(settings.updatedAt).toLocaleString()} by {settings.updatedBy}
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
}
