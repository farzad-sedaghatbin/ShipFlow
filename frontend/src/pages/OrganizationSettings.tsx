import { useState, useEffect, useCallback, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDateTime } from '../utils/dateLocalization';
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
  Sparkles,
  Clock,
  Palette,
  Bug,
  Plus,
  Trash2,
} from 'lucide-react';
import { useToast } from '../contexts';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { OrganizationSettings, RiskThresholds, ColorSettings } from '../types/organizationSettings';
import { usePermission } from '../hooks/usePermission';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Separator } from '../components/ui/separator';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { Switch } from '../components/ui/switch';
import { Badge } from '../components/ui/badge';
import { ConfirmDialog } from '../components/ui/confirm-dialog';

const DEFAULT_RISK_THRESHOLDS: RiskThresholds = {
  lowMax: 30,
  mediumMax: 60,
  highMax: 85,
};

const DEFAULT_RISK_WEIGHTS = {
  budgetWeight: 25,
  bugsWeight: 30,
  scopeWeight: 25,
  timeWeight: 20,
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
  const { t, i18n } = useTranslation();
  const { showToast } = useToast();
  const meetingTypesEndRef = useRef<HTMLDivElement>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<OrganizationSettings | null>(null);
  const [formData, setFormData] = useState<Partial<OrganizationSettings>>({});

  const { hasPermission } = usePermission();
  const [canManageSettings, setCanManageSettings] = useState<boolean | null>(null);
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);
  
  const [deleteMeetingTypeConfirm, setDeleteMeetingTypeConfirm] = useState<{
    open: boolean;
    typeIndex: number | null;
  }>({ open: false, typeIndex: null });
  
  const [deleteItemConfirm, setDeleteItemConfirm] = useState<{
    open: boolean;
    typeIndex: number | null;
    itemIndex: number | null;
    listType: 'dor' | 'dod' | null;
  }>({ open: false, typeIndex: null, itemIndex: null, listType: null });

  const handleConfirmDeleteMeetingType = () => {
    if (deleteMeetingTypeConfirm.typeIndex === null) return;
    const newMeetingTypes = formData.meetingTypes?.filter((_, i) => i !== deleteMeetingTypeConfirm.typeIndex) || [];
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
    showToast(t('organizationSettings.meetingTypeDeleted'), 'success');
    setDeleteMeetingTypeConfirm({ open: false, typeIndex: null });
  };

  const handleConfirmDeleteItem = () => {
    const { typeIndex, itemIndex, listType } = deleteItemConfirm;
    if (typeIndex === null || itemIndex === null || !listType) return;
    
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    
    if (listType === 'dor') {
      const newDorItems = [...(meetingType.dorItems || [])];
      newDorItems[itemIndex] = { ...newDorItems[itemIndex], isDeleted: true };
      newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
    } else {
      const newDodItems = [...(meetingType.dodItems || [])];
      newDodItems[itemIndex] = { ...newDodItems[itemIndex], isDeleted: true };
      newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
    }
    
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
    showToast(t('organizationSettings.dorDodItemDeleted'), 'success');
    setDeleteItemConfirm({ open: false, typeIndex: null, itemIndex: null, listType: null });
  };

  const fetchSettings = useCallback(async () => {
    try {
      const response = await organizationSettingsService.getSettings();
      setSettings(response.data);
      setFormData(response.data);
    } catch (error: any) {
      if (error.response?.status === 404) {
        // Settings don't exist yet, use defaults
        const defaultSettings: Partial<OrganizationSettings> = {
          organizationName: t('organizationSettings.defaults.organizationName'),
          defaultCycleLengthWeeks: 6,
          defaultCooldownWeeks: 2,
          riskThresholds: DEFAULT_RISK_THRESHOLDS,
          timeZone: 'UTC',
          dateFormat: 'MM/DD/YYYY',
          enableNotifications: true,
          enableAIFeatures: true,
          taskCategories: [
            { name: 'PITCH_SCOPE', description: t('organizationSettings.defaults.taskCategory.pitchScope'), color: '#3B82F6', isActive: true, order: 1 },
            { name: 'DEBT_IMPROVEMENT', description: t('organizationSettings.defaults.taskCategory.debtImprovement'), color: '#F59E0B', isActive: true, order: 2 },
          ],
          pitchCategories: [
            { name: 'FEATURE', description: t('organizationSettings.defaults.pitchCategory.feature'), color: '#10B981', isActive: true, order: 1 },
            { name: 'INFRASTRUCTURE', description: t('organizationSettings.defaults.pitchCategory.infrastructure'), color: '#6366F1', isActive: true, order: 2 },
            { name: 'REFACTOR', description: t('organizationSettings.defaults.pitchCategory.refactor'), color: '#8B5CF6', isActive: true, order: 3 },
            { name: 'BUG_FIX', description: t('organizationSettings.defaults.pitchCategory.bugFix'), color: '#EF4444', isActive: true, order: 4 },
          ],
          colors: DEFAULT_COLORS,
          bugStatuses: [
            { name: 'NEW', description: t('organizationSettings.defaults.bugStatus.new'), color: '#3B82F6', isActive: true, order: 1, isClosed: false },
            { name: 'IN_PROGRESS', description: t('organizationSettings.defaults.bugStatus.inProgress'), color: '#F59E0B', isActive: true, order: 2, isClosed: false },
            { name: 'FIXED', description: t('organizationSettings.defaults.bugStatus.fixed'), color: '#10B981', isActive: true, order: 3, isClosed: true },
            { name: 'VERIFIED', description: t('organizationSettings.defaults.bugStatus.verified'), color: '#22C55E', isActive: true, order: 4, isClosed: true },
            { name: 'WONT_FIX', description: t('organizationSettings.defaults.bugStatus.wontFix'), color: '#6B7280', isActive: true, order: 5, isClosed: true },
          ],
          severityLevels: [
            { name: 'CRITICAL', description: t('organizationSettings.defaults.severity.critical'), color: '#DC2626', isActive: true, order: 1, priority: 1 },
            { name: 'HIGH', description: t('organizationSettings.defaults.severity.high'), color: '#F59E0B', isActive: true, order: 2, priority: 2 },
            { name: 'MEDIUM', description: t('organizationSettings.defaults.severity.medium'), color: '#3B82F6', isActive: true, order: 3, priority: 3 },
            { name: 'LOW', description: t('organizationSettings.defaults.severity.low'), color: '#10B981', isActive: true, order: 4, priority: 4 },
          ],
          defaultHoursPerDay: 8.0,
          defaultWorkingDaysPerWeek: 5,
        };
        setFormData(defaultSettings);
      } else {
        showToast(t('organizationSettings.loadFailed'), 'error');
      }
    } finally {
      setLoading(false);
    }
  }, [t, showToast, setLoading, setSettings, setFormData]);

  // Check permission on mount
  useEffect(() => {
    hasPermission('SYSTEM', 'MANAGE').then(setCanManageSettings).catch(() => setCanManageSettings(false));
  }, []); // Empty dependency array - run once on mount

  useEffect(() => {
    if (canManageSettings) {
      fetchSettings();
    }
  }, [canManageSettings, fetchSettings]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await organizationSettingsService.updateSettings(formData);
      setSettings(response.data);
      setFormData(response.data);
      showToast(t('organizationSettings.updateSuccess'), 'success');
    } catch (error) {
      showToast(t('organizationSettings.updateFailed'), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    setSaving(true);
    try {
      const response = await organizationSettingsService.resetToDefaults();
      setSettings(response.data);
      setFormData(response.data);
      showToast(t('organizationSettings.resetSuccess'), 'success');
      setResetConfirmOpen(false);
    } catch (error) {
      showToast(t('organizationSettings.resetFailed'), 'error');
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

  const updateRiskWeight = (weightField: string, value: number) => {
    const currentWeights = formData.riskWeights || DEFAULT_RISK_WEIGHTS;
    setFormData({
      ...formData,
      riskWeights: {
        ...currentWeights,
        [weightField]: value,
      },
    });
  };

  const applyRiskProfile = async (profileName: string) => {
    try {
      const response = await organizationSettingsService.getRiskProfiles();
      // Backend returns { profiles: [...] }
      const profiles = response.data.profiles;
      
      if (!profiles || !Array.isArray(profiles)) {
        console.error('Invalid response format:', response);
        showToast(t('organizationSettings.failedToLoadProfiles'), 'error');
        return;
      }
      
      const selectedProfile = profiles.find((p) => p.name === profileName);
      
      if (!selectedProfile) {
        console.error('Profile not found:', profileName, 'Available:', profiles.map(p => p.name));
        showToast(t('organizationSettings.profileNotFound', { profile: profileName }), 'error');
        return;
      }
      
      setFormData({
        ...formData,
        riskWeights: {
          budgetWeight: selectedProfile.budgetWeight,
          bugsWeight: selectedProfile.bugsWeight,
          scopeWeight: selectedProfile.scopeWeight,
          timeWeight: selectedProfile.timeWeight,
        },
      });
      showToast(t('organizationSettings.profileApplied', { profile: selectedProfile.displayName }), 'success');
    } catch (error) {
      console.error('Error applying risk profile:', error);
      showToast(t('organizationSettings.failedToApplyProfile'), 'error');
    }
  };

  const getRiskWeightsSum = () => {
    const weights = formData.riskWeights || DEFAULT_RISK_WEIGHTS;
    return weights.budgetWeight + weights.bugsWeight + weights.scopeWeight + weights.timeWeight;
  };

  const isRiskWeightsValid = () => {
    return getRiskWeightsSum() === 100;
  };

  // Show loading while checking permission
  if (canManageSettings === null) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!canManageSettings) {
    return (
      <div className="p-4">
        <Alert variant="destructive">
          <ShieldAlert className="h-4 w-4" />
          <AlertDescription>
            {t('organizationSettings.noPermission')}
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
            {t('organizationSettings.title')}
          </h1>
          <p className="text-sm text-muted-foreground mt-1">
            {t('organizationSettings.subtitle')}
          </p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={() => setResetConfirmOpen(true)} disabled={saving} size="sm">
            <RotateCcw className="mr-2 h-4 w-4" />
            {t('organizationSettings.resetToDefaults')}
          </Button>
          <Button onClick={handleSave} disabled={saving} size="sm">
            {saving ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                {t('organizationSettings.saving')}
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                {t('organizationSettings.saveChanges')}
              </>
            )}
          </Button>
        </div>
      </div>

      <Tabs defaultValue="general" className="space-y-4">
        <TabsList className="flex flex-wrap">
          <TabsTrigger value="general">{t('organizationSettings.general')}</TabsTrigger>
          <TabsTrigger value="cycles">{t('organizationSettings.cycles')}</TabsTrigger>
          <TabsTrigger value="risk">{t('organizationSettings.risk')}</TabsTrigger>
          <TabsTrigger value="weights">{t('organizationSettings.weights')}</TabsTrigger>
          <TabsTrigger value="colors">{t('organizationSettings.colors')}</TabsTrigger>
          <TabsTrigger value="bugs">{t('organizationSettings.bugs')}</TabsTrigger>
          <TabsTrigger value="categories">{t('organizationSettings.categories')}</TabsTrigger>
          <TabsTrigger value="meetings">{t('organizationSettings.meetingTypes')}</TabsTrigger>
          <TabsTrigger value="features">{t('organizationSettings.features')}</TabsTrigger>
        </TabsList>

        {/* General Settings */}
        <TabsContent value="general" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Globe className="h-5 w-5" />
                {t('organizationSettings.generalInfo')}
              </CardTitle>
              <CardDescription>{t('organizationSettings.generalDesc')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="orgName">{t('organizationSettings.organizationName')}</Label>
                <Input
                  id="orgName"
                  value={formData.organizationName || ''}
                  onChange={(e) => setFormData({ ...formData, organizationName: e.target.value })}
                  placeholder={t('organizationSettings.organizationNamePlaceholder')}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="timezone">{t('organizationSettings.timeZone')}</Label>
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
                  <Label htmlFor="dateFormat">{t('organizationSettings.dateFormat')}</Label>
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
                {t('organizationSettings.cycleConfiguration')}
              </CardTitle>
              <CardDescription>{t('organizationSettings.cycleDesc')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="cycleLength">{t('organizationSettings.defaultCycleLength')}</Label>
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
                    {t('organizationSettings.defaultCycleLengthNote')}
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="cooldownLength">{t('organizationSettings.defaultCooldown')}</Label>
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
                    {t('organizationSettings.defaultCooldownNote')}
                  </p>
                </div>
              </div>

              <Alert>
                <Clock className="h-4 w-4" />
                <AlertDescription>
                  {t('organizationSettings.cycleDefaultsNote')}
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>

          {/* Capacity Configuration */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Clock className="h-5 w-5" />
                {t('organizationSettings.capacityConfiguration', 'Capacity Configuration')}
              </CardTitle>
              <CardDescription>
                {t('organizationSettings.capacityDesc', 'Configure default work hours and working days. Teams and individuals can override these values.')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="hoursPerDay">{t('organizationSettings.defaultHoursPerDay', 'Default Hours Per Day')}</Label>
                  <Input
                    id="hoursPerDay"
                    type="number"
                    min="1"
                    max="24"
                    step="0.5"
                    value={formData.defaultHoursPerDay || 8}
                    onChange={(e) =>
                      setFormData({ ...formData, defaultHoursPerDay: parseFloat(e.target.value) })
                    }
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.defaultHoursPerDayNote', 'Standard working hours per day. Teams and individuals can override this setting.')}
                  </p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="workingDaysPerWeek">{t('organizationSettings.defaultWorkingDaysPerWeek', 'Default Working Days Per Week')}</Label>
                  <Input
                    id="workingDaysPerWeek"
                    type="number"
                    min="1"
                    max="7"
                    value={formData.defaultWorkingDaysPerWeek || 5}
                    onChange={(e) =>
                      setFormData({ ...formData, defaultWorkingDaysPerWeek: parseInt(e.target.value) })
                    }
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.defaultWorkingDaysPerWeekNote', 'Used to convert weeks to days for appetite calculations. E.g., 2 weeks = 10 working days (if 5 days/week).')}
                  </p>
                </div>
              </div>

              <Alert>
                <Sparkles className="h-4 w-4" />
                <AlertDescription>
                  {t('organizationSettings.capacityInheritanceNote', 'Capacity inheritance: Organization → Team → Person → Team Assignment. More specific settings override general ones.')}
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
                {t('organizationSettings.riskThresholds')}
              </CardTitle>
              <CardDescription>
                {t('organizationSettings.riskThresholdsDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 rounded-lg bg-green-50 dark:bg-green-950/20 border border-green-200 dark:border-green-900">
                  <div>
                    <div className="font-medium text-green-900 dark:text-green-100">{t('organizationSettings.lowRisk')}</div>
                    <div className="text-sm text-green-700 dark:text-green-300">
                      {t('organizationSettings.lowRiskRange', { max: formData.riskThresholds?.lowMax || 30 })}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="lowMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
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
                    <div className="font-medium text-yellow-900 dark:text-yellow-100">{t('organizationSettings.mediumRisk')}</div>
                    <div className="text-sm text-yellow-700 dark:text-yellow-300">
                      {t('organizationSettings.mediumRiskRange', { min: (formData.riskThresholds?.lowMax || 30) + 1, max: formData.riskThresholds?.mediumMax || 60 })}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="mediumMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
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
                    <div className="font-medium text-orange-900 dark:text-orange-100">{t('organizationSettings.highRisk')}</div>
                    <div className="text-sm text-orange-700 dark:text-orange-300">
                      {t('organizationSettings.highRiskRange', { min: (formData.riskThresholds?.mediumMax || 60) + 1, max: formData.riskThresholds?.highMax || 85 })}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="highMax" className="text-xs">{t('organizationSettings.maxScore')}</Label>
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
                  <div className="font-medium text-red-900 dark:text-red-100">{t('organizationSettings.criticalRisk')}</div>
                  <div className="text-sm text-red-700 dark:text-red-300">
                    {t('organizationSettings.criticalRiskRange', { min: (formData.riskThresholds?.highMax || 85) + 1 })}
                  </div>
                </div>
              </div>

              <Alert>
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  {t('organizationSettings.thresholdsChangeNote')}
                </AlertDescription>
              </Alert>
            </CardContent>
          </Card>
        </TabsContent>

        {/* Risk Weights */}
        <TabsContent value="weights" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <ShieldAlert className="h-5 w-5" />
                {t('organizationSettings.riskWeights')}
              </CardTitle>
              <CardDescription>
                {t('organizationSettings.riskWeightsDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Preset Profiles */}
              <div className="space-y-3">
                <Label className="text-sm font-semibold">{t('organizationSettings.quickProfiles')}</Label>
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-2">
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => applyRiskProfile('balanced')}
                    className="justify-start"
                  >
                    ⚖️ {t('organizationSettings.balanced')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => applyRiskProfile('conservative')}
                    className="justify-start"
                  >
                    🛡️ {t('organizationSettings.conservative')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => applyRiskProfile('aggressive')}
                    className="justify-start"
                  >
                    🚀 {t('organizationSettings.aggressive')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => applyRiskProfile('quality_focused')}
                    className="justify-start"
                  >
                    🎯 {t('organizationSettings.qualityFocus')}
                  </Button>
                  <Button
                    type="button"
                    variant="outline"
                    size="sm"
                    onClick={() => applyRiskProfile('time_critical')}
                    className="justify-start"
                  >
                    ⏱️ {t('organizationSettings.timeCritical')}
                  </Button>
                </div>
                <p className="text-xs text-muted-foreground">
                  {t('organizationSettings.profilesNote')}
                </p>
              </div>

              <Separator />

              {/* Weight Sliders */}
              <div className="space-y-6">
                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="budgetWeight" className="font-medium">💰 {t('organizationSettings.budgetWeight')}</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        id="budgetWeight"
                        type="number"
                        min="0"
                        max="100"
                        value={formData.riskWeights?.budgetWeight || 25}
                        onChange={(e) => updateRiskWeight('budgetWeight', parseInt(e.target.value) || 0)}
                        className="w-16 text-center"
                      />
                      <span className="text-sm text-muted-foreground">%</span>
                    </div>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    value={formData.riskWeights?.budgetWeight || 25}
                    onChange={(e) => updateRiskWeight('budgetWeight', parseInt(e.target.value))}
                    className="w-full"
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.budgetWeightDesc')}
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="bugsWeight" className="font-medium">🐛 {t('organizationSettings.bugsWeight')}</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        id="bugsWeight"
                        type="number"
                        min="0"
                        max="100"
                        value={formData.riskWeights?.bugsWeight || 30}
                        onChange={(e) => updateRiskWeight('bugsWeight', parseInt(e.target.value) || 0)}
                        className="w-16 text-center"
                      />
                      <span className="text-sm text-muted-foreground">%</span>
                    </div>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    value={formData.riskWeights?.bugsWeight || 30}
                    onChange={(e) => updateRiskWeight('bugsWeight', parseInt(e.target.value))}
                    className="w-full"
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.bugsWeightDesc')}
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="scopeWeight" className="font-medium">📊 {t('organizationSettings.scopeWeight')}</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        id="scopeWeight"
                        type="number"
                        min="0"
                        max="100"
                        value={formData.riskWeights?.scopeWeight || 25}
                        onChange={(e) => updateRiskWeight('scopeWeight', parseInt(e.target.value) || 0)}
                        className="w-16 text-center"
                      />
                      <span className="text-sm text-muted-foreground">%</span>
                    </div>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    value={formData.riskWeights?.scopeWeight || 25}
                    onChange={(e) => updateRiskWeight('scopeWeight', parseInt(e.target.value))}
                    className="w-full"
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.scopeWeightDesc')}
                  </p>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <Label htmlFor="timeWeight" className="font-medium">⏰ {t('organizationSettings.timeWeight')}</Label>
                    <div className="flex items-center gap-2">
                      <Input
                        id="timeWeight"
                        type="number"
                        min="0"
                        max="100"
                        value={formData.riskWeights?.timeWeight || 20}
                        onChange={(e) => updateRiskWeight('timeWeight', parseInt(e.target.value) || 0)}
                        className="w-16 text-center"
                      />
                      <span className="text-sm text-muted-foreground">%</span>
                    </div>
                  </div>
                  <input
                    type="range"
                    min="0"
                    max="100"
                    step="5"
                    value={formData.riskWeights?.timeWeight || 20}
                    onChange={(e) => updateRiskWeight('timeWeight', parseInt(e.target.value))}
                    className="w-full"
                  />
                  <p className="text-xs text-muted-foreground">
                    {t('organizationSettings.timeWeightDesc')}
                  </p>
                </div>
              </div>

              {/* Validation Alert */}
              <Alert variant={isRiskWeightsValid() ? 'default' : 'destructive'}>
                <AlertTriangle className="h-4 w-4" />
                <AlertDescription>
                  <div className="flex items-center justify-between">
                    <span>{t('organizationSettings.totalWeight')}: <strong>{getRiskWeightsSum()}%</strong></span>
                    {isRiskWeightsValid() ? (
                      <Badge variant="default" className="bg-green-500">✓ {t('organizationSettings.valid')}</Badge>
                    ) : (
                      <Badge variant="destructive">{t('organizationSettings.mustEqual100')}</Badge>
                    )}
                  </div>
                </AlertDescription>
              </Alert>

              <Alert>
                <ShieldAlert className="h-4 w-4" />
                <AlertDescription>
                  {t('organizationSettings.weightsChangeNote')}
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
                {t('organizationSettings.colorConfiguration')}
              </CardTitle>
              <CardDescription>
                {t('organizationSettings.colorsDesc')}
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-2">
                  <Label htmlFor="appetiteColor">{t('organizationSettings.appetiteHoursColor')}</Label>
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
                  <p className="text-xs text-muted-foreground">{t('organizationSettings.appetiteHoursColorDesc')}</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="actualColor">{t('organizationSettings.actualHoursColor')}</Label>
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
                  <p className="text-xs text-muted-foreground">{t('organizationSettings.actualHoursColorDesc')}</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="overBudgetColor">{t('organizationSettings.overBudgetColor')}</Label>
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
                  <p className="text-xs text-muted-foreground">{t('organizationSettings.overBudgetColorDesc')}</p>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="underBudgetColor">{t('organizationSettings.underBudgetColor')}</Label>
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
                  <p className="text-xs text-muted-foreground">{t('organizationSettings.underBudgetColorDesc')}</p>
                </div>
              </div>

              <Alert>
                <Palette className="h-4 w-4" />
                <AlertDescription>
                  {t('organizationSettings.colorsApplyNote')}
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
                {t('organizationSettings.bugTracking')}
              </CardTitle>
              <CardDescription>{t('organizationSettings.bugTrackingDesc')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="font-semibold mb-3">{t('organizationSettings.bugStatuses')}</h3>
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
                          placeholder={t('organizationSettings.statusName')}
                        />
                        <Input
                          value={status.description}
                          onChange={(e) => {
                            const updated = [...(formData.bugStatuses || [])];
                            updated[index] = { ...updated[index], description: e.target.value };
                            setFormData({ ...formData, bugStatuses: updated });
                          }}
                          className="text-sm"
                          placeholder={t('organizationSettings.description')}
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
                          <Label className="text-xs">{t('organizationSettings.active')}</Label>
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
                          <Label className="text-xs">{t('organizationSettings.closed')}</Label>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <Separator />

              <div>
                <h3 className="font-semibold mb-3">{t('organizationSettings.severityLevels')}</h3>
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
                          placeholder={t('organizationSettings.severityName')}
                        />
                        <Input
                          value={severity.description}
                          onChange={(e) => {
                            const updated = [...(formData.severityLevels || [])];
                            updated[index] = { ...updated[index], description: e.target.value };
                            setFormData({ ...formData, severityLevels: updated });
                          }}
                          className="text-sm"
                          placeholder={t('organizationSettings.description')}
                        />
                      </div>
                      <div className="flex items-center gap-2">
                        <div className="flex items-center gap-1">
                          <Label className="text-xs">{t('organizationSettings.priority')}:</Label>
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
                          <Label className="text-xs">{t('organizationSettings.active')}</Label>
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
                {t('organizationSettings.taskPitchCategories')}
              </CardTitle>
              <CardDescription>{t('organizationSettings.categoriesDesc')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div>
                <h3 className="font-semibold mb-3">{t('organizationSettings.taskCategories')}</h3>
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
                <h3 className="font-semibold mb-3">{t('organizationSettings.pitchCategories')}</h3>
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

        {/* Meeting Types */}
        <TabsContent value="meetings" className="space-y-4">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <div>
                <CardTitle className="flex items-center gap-2">
                  <Calendar className="h-5 w-5" />
                  {t('organizationSettings.meetingTypesConfig')}
                </CardTitle>
                <CardDescription>{t('organizationSettings.meetingTypesDesc')}</CardDescription>
              </div>
              <Button
                type="button"
                variant="outline"
                onClick={() => {
                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                  const uniqueId =
                    typeof crypto !== 'undefined' && 'randomUUID' in crypto
                      ? crypto.randomUUID()
                      : `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
                  const newName = `CUSTOM_${uniqueId}`;
                  newMeetingTypes.push({
                    name: newName,
                    displayName: t('organizationSettings.newMeetingType'),
                    description: '',
                    color: '#6b7280',
                    isActive: true,
                    order: newMeetingTypes.length + 1,
                    dorItems: [],
                    dodItems: [],
                  });
                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                  showToast(t('organizationSettings.meetingTypeAdded'), 'success');
                  // Scroll to new item after render
                  setTimeout(() => {
                    meetingTypesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
                  }, 100);
                }}
              >
                <Plus className="h-4 w-4 mr-2" />
                {t('organizationSettings.addMeetingType')}
              </Button>
            </CardHeader>
            <CardContent className="space-y-6">
              {formData.meetingTypes?.map((meetingType, typeIndex) => (
                <div key={typeIndex} className="border rounded-lg p-4 space-y-4">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div
                        className="w-4 h-4 rounded"
                        style={{ backgroundColor: meetingType.color }}
                      />
                      <div>
                        <Input
                          value={meetingType.displayName}
                          onChange={(e) => {
                            const newMeetingTypes = [...(formData.meetingTypes || [])];
                            newMeetingTypes[typeIndex] = { ...meetingType, displayName: e.target.value };
                            setFormData({ ...formData, meetingTypes: newMeetingTypes });
                          }}
                          className="font-semibold h-8"
                        />
                        <p className="text-xs text-muted-foreground mt-1">{meetingType.name}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <input
                        type="color"
                        value={meetingType.color}
                        onChange={(e) => {
                          const newMeetingTypes = [...(formData.meetingTypes || [])];
                          newMeetingTypes[typeIndex] = { ...meetingType, color: e.target.value };
                          setFormData({ ...formData, meetingTypes: newMeetingTypes });
                        }}
                        className="w-8 h-8 rounded cursor-pointer"
                      />
                      <div className="flex items-center gap-1">
                        <Switch
                          checked={meetingType.isActive}
                          onCheckedChange={(checked) => {
                            const newMeetingTypes = [...(formData.meetingTypes || [])];
                            newMeetingTypes[typeIndex] = { ...meetingType, isActive: checked };
                            setFormData({ ...formData, meetingTypes: newMeetingTypes });
                          }}
                        />
                        <Label className="text-xs">{t('organizationSettings.active')}</Label>
                      </div>
                      <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="text-destructive hover:text-destructive"
                        onClick={() => {
                          setDeleteMeetingTypeConfirm({ open: true, typeIndex });
                        }}
                        title={t('organizationSettings.deleteMeetingType')}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>

                  <Input
                    value={meetingType.description}
                    onChange={(e) => {
                      const newMeetingTypes = [...(formData.meetingTypes || [])];
                      newMeetingTypes[typeIndex] = { ...meetingType, description: e.target.value };
                      setFormData({ ...formData, meetingTypes: newMeetingTypes });
                    }}
                    placeholder={t('organizationSettings.meetingTypeDescPlaceholder')}
                    className="text-sm"
                  />

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    {/* DOR Items */}
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label className="text-sm font-medium">{t('organizationSettings.dorItems')}</Label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            const newMeetingTypes = [...(formData.meetingTypes || [])];
                            const newDorItems = [...(meetingType.dorItems || [])];
                            newDorItems.push({
                              name: '',
                              description: '',
                              isRequired: false,
                              order: newDorItems.length + 1,
                            });
                            newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
                            setFormData({ ...formData, meetingTypes: newMeetingTypes });
                          }}
                        >
                          + {t('organizationSettings.addItem')}
                        </Button>
                      </div>
                      <div className="space-y-2 max-h-64 overflow-y-auto">
                        {meetingType.dorItems?.filter(item => !item.isDeleted).map((item, itemIndex) => {
                          const actualIndex = meetingType.dorItems?.findIndex(d => d === item) ?? itemIndex;
                          return (
                          <div key={actualIndex} className="flex items-start gap-2 p-2 border rounded bg-muted/30">
                            <div className="flex-1 space-y-1">
                              <Input
                                value={item.name}
                                onChange={(e) => {
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDorItems = [...(meetingType.dorItems || [])];
                                  newDorItems[actualIndex] = { ...item, name: e.target.value };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                                placeholder={t('organizationSettings.itemName')}
                                className="h-7 text-sm"
                              />
                              <Input
                                value={item.description}
                                onChange={(e) => {
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDorItems = [...(meetingType.dorItems || [])];
                                  newDorItems[actualIndex] = { ...item, description: e.target.value };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                                placeholder={t('organizationSettings.itemDescription')}
                                className="h-7 text-xs"
                              />
                            </div>
                            <div className="flex flex-col items-center gap-1">
                              <div className="flex items-center gap-1">
                                <Switch
                                  checked={item.isRequired}
                                  onCheckedChange={(checked) => {
                                    const newMeetingTypes = [...(formData.meetingTypes || [])];
                                    const newDorItems = [...(meetingType.dorItems || [])];
                                    newDorItems[actualIndex] = { ...item, isRequired: checked };
                                    newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
                                    setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                  }}
                                />
                                <span className="text-xs">{t('organizationSettings.required')}</span>
                              </div>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon-sm"
                                onClick={() => {
                                  const confirmed = window.confirm(
                                    t('organizationSettings.confirmDeleteDorDodItem'),
                                  );
                                  if (!confirmed) return;
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDorItems = [...(meetingType.dorItems || [])];
                                  // Soft delete - mark as deleted instead of removing
                                  newDorItems[actualIndex] = { ...item, isDeleted: true };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                              >
                                ✕
                              </Button>
                            </div>
                          </div>
                        );
                        })}
                      </div>
                    </div>

                    {/* DOD Items */}
                    <div className="space-y-2">
                      <div className="flex items-center justify-between">
                        <Label className="text-sm font-medium">{t('organizationSettings.dodItems')}</Label>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          onClick={() => {
                            const newMeetingTypes = [...(formData.meetingTypes || [])];
                            const newDodItems = [...(meetingType.dodItems || [])];
                            newDodItems.push({
                              name: '',
                              description: '',
                              isRequired: false,
                              order: newDodItems.length + 1,
                            });
                            newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
                            setFormData({ ...formData, meetingTypes: newMeetingTypes });
                          }}
                        >
                          + {t('organizationSettings.addItem')}
                        </Button>
                      </div>
                      <div className="space-y-2 max-h-64 overflow-y-auto">
                        {meetingType.dodItems?.filter(item => !item.isDeleted).map((item, itemIndex) => {
                          const actualIndex = meetingType.dodItems?.findIndex(d => d === item) ?? itemIndex;
                          return (
                          <div key={actualIndex} className="flex items-start gap-2 p-2 border rounded bg-muted/30">
                            <div className="flex-1 space-y-1">
                              <Input
                                value={item.name}
                                onChange={(e) => {
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDodItems = [...(meetingType.dodItems || [])];
                                  newDodItems[actualIndex] = { ...item, name: e.target.value };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                                placeholder={t('organizationSettings.itemName')}
                                className="h-7 text-sm"
                              />
                              <Input
                                value={item.description}
                                onChange={(e) => {
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDodItems = [...(meetingType.dodItems || [])];
                                  newDodItems[actualIndex] = { ...item, description: e.target.value };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                                placeholder={t('organizationSettings.itemDescription')}
                                className="h-7 text-xs"
                              />
                            </div>
                            <div className="flex flex-col items-center gap-1">
                              <div className="flex items-center gap-1">
                                <Switch
                                  checked={item.isRequired}
                                  onCheckedChange={(checked) => {
                                    const newMeetingTypes = [...(formData.meetingTypes || [])];
                                    const newDodItems = [...(meetingType.dodItems || [])];
                                    newDodItems[actualIndex] = { ...item, isRequired: checked };
                                    newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
                                    setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                  }}
                                />
                                <span className="text-xs">{t('organizationSettings.required')}</span>
                              </div>
                              <Button
                                type="button"
                                variant="ghost"
                                size="icon-sm"
                                onClick={() => {
                                  const confirmed = window.confirm(
                                    t('organizationSettings.confirmDeleteDorDodItem'),
                                  );
                                  if (!confirmed) return;
                                  const newMeetingTypes = [...(formData.meetingTypes || [])];
                                  const newDodItems = [...(meetingType.dodItems || [])];
                                  // Soft delete - mark as deleted instead of removing
                                  newDodItems[actualIndex] = { ...item, isDeleted: true };
                                  newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
                                  setFormData({ ...formData, meetingTypes: newMeetingTypes });
                                }}
                              >
                                ✕
                              </Button>
                            </div>
                          </div>
                        );
                        })}
                      </div>
                    </div>
                  </div>
                </div>
              ))}
              <div ref={meetingTypesEndRef} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* Features */}
        <TabsContent value="features" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Sparkles className="h-5 w-5" />
                {t('organizationSettings.featureToggles')}
              </CardTitle>
              <CardDescription>{t('organizationSettings.featuresDesc')}</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <div className="flex items-center gap-2">
                    <Sparkles className="h-4 w-4 text-muted-foreground" />
                    <Label htmlFor="ai-features">{t('organizationSettings.aiFeatures')}</Label>
                  </div>
                  <p className="text-sm text-muted-foreground">
                    {t('organizationSettings.aiFeaturesDesc')}
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
              <CardContent className="pt-6">
                <p className="text-sm text-muted-foreground">
                  {t('organizationSettings.lastUpdated', { 
                    date: formatLocalizedDateTime(new Date(settings.updatedAt), i18n.language) 
                  })} by {settings.updatedBy}
                </p>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>

      {/* Reset Confirmation Dialog */}
      <ConfirmDialog
        open={resetConfirmOpen}
        onOpenChange={setResetConfirmOpen}
        title={t('organizationSettings.resetTitle')}
        description={t('organizationSettings.confirmReset')}
        confirmLabel={t('common.reset')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleReset}
        variant="destructive"
        loading={saving}
      />

      {/* Delete Meeting Type Confirmation */}
      <ConfirmDialog
        open={deleteMeetingTypeConfirm.open}
        onOpenChange={(open) => setDeleteMeetingTypeConfirm({ open, typeIndex: null })}
        title={t('organizationSettings.deleteMeetingTypeTitle')}
        description={t('organizationSettings.confirmDeleteMeetingType')}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleConfirmDeleteMeetingType}
        variant="destructive"
      />

      {/* Delete DOD/DOR Item Confirmation */}
      <ConfirmDialog
        open={deleteItemConfirm.open}
        onOpenChange={(open) => setDeleteItemConfirm({ open, typeIndex: null, itemIndex: null, listType: null })}
        title={t('organizationSettings.deleteItemTitle')}
        description={t('organizationSettings.confirmDeleteItem')}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleConfirmDeleteItem}
        variant="destructive"
      />
    </div>
  );
}
