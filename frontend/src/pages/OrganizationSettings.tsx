import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
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
  Palette,
  Bug,
  Mail,
  Shield,
} from 'lucide-react';
import { useToast } from '../contexts';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { OrganizationSettings, RiskThresholds, ColorSettings } from '../types/organizationSettings';
import { usePermission } from '../hooks/usePermission';
import { Button } from '../components/ui/button';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { ConfirmDialog } from '../components/ui/confirm-dialog';
import {
  GeneralSettingsTab,
  CycleSettingsTab,
  RiskSettingsTab,
  WeightsSettingsTab,
  ColorsSettingsTab,
  BugSettingsTab,
  CategoriesSettingsTab,
  MeetingsSettingsTab,
  FeaturesSettingsTab,
  EmailSettingsTab,
  SsoSettingsTab,
} from '../components/organizationSettings';

const DEFAULT_RISK_THRESHOLDS: RiskThresholds = { lowMax: 30, mediumMax: 60, highMax: 85 };
const DEFAULT_COLORS: ColorSettings = {
  appetiteHours: '#3B82F6',
  actualHours: '#10B981',
  overBudget: '#EF4444',
  underBudget: '#22C55E',
};

interface FormData extends Partial<OrganizationSettings> {}

export default function OrganizationSettingsPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<OrganizationSettings | null>(null);
  const [formData, setFormData] = useState<FormData>({});
  const { hasPermission } = usePermission();
  const [canManageSettings, setCanManageSettings] = useState<boolean | null>(null);
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);

  const fetchSettings = useCallback(async () => {
    try {
      const response = await organizationSettingsService.getSettings();
      setSettings(response.data);
      setFormData(response.data);
    } catch (error: any) {
      if (error.response?.status === 404) {
        const defaultSettings: Partial<OrganizationSettings> = {
          organizationName: t('organizationSettings.defaults.organizationName'),
          defaultCycleLengthWeeks: 6,
          defaultCooldownWeeks: 2,
          riskThresholds: DEFAULT_RISK_THRESHOLDS,
          timeZone: 'UTC',
          dateFormat: 'MM/DD/YYYY',
          enableNotifications: true,
          enableAIFeatures: true,
          enableWiseArchitecture: false,
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
  }, [t, showToast]);

  // Fix: include hasPermission in deps to avoid stale closures
  useEffect(() => {
    hasPermission('SYSTEM', 'MANAGE').then(setCanManageSettings).catch(() => setCanManageSettings(false));
  }, [hasPermission]);

  useEffect(() => {
    if (canManageSettings) fetchSettings();
  }, [canManageSettings, fetchSettings]);

  const handleSave = async () => {
    setSaving(true);
    try {
      const response = await organizationSettingsService.updateSettings(formData);
      setSettings(response.data);
      setFormData(response.data);
      showToast(t('organizationSettings.updateSuccess'), 'success');
    } catch {
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
    } catch {
      showToast(t('organizationSettings.resetFailed'), 'error');
    } finally {
      setSaving(false);
    }
  };

  // Fix: separate permission-check spinner from settings-load spinner
  // so that canManageSettings=false reaches the alert instead of staying on spinner
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
          <AlertDescription>{t('organizationSettings.noPermission')}</AlertDescription>
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
          <TabsTrigger value="general"><Globe className="h-4 w-4 mr-1" />{t('organizationSettings.general')}</TabsTrigger>
          <TabsTrigger value="cycles"><Calendar className="h-4 w-4 mr-1" />{t('organizationSettings.cycles')}</TabsTrigger>
          <TabsTrigger value="risk"><AlertTriangle className="h-4 w-4 mr-1" />{t('organizationSettings.risk')}</TabsTrigger>
          <TabsTrigger value="weights">{t('organizationSettings.weights')}</TabsTrigger>
          <TabsTrigger value="colors"><Palette className="h-4 w-4 mr-1" />{t('organizationSettings.colors')}</TabsTrigger>
          <TabsTrigger value="bugs"><Bug className="h-4 w-4 mr-1" />{t('organizationSettings.bugs')}</TabsTrigger>
          <TabsTrigger value="categories"><Tags className="h-4 w-4 mr-1" />{t('organizationSettings.categories')}</TabsTrigger>
          <TabsTrigger value="meetings"><Calendar className="h-4 w-4 mr-1" />{t('organizationSettings.meetingTypes')}</TabsTrigger>
          <TabsTrigger value="features"><Sparkles className="h-4 w-4 mr-1" />{t('organizationSettings.features')}</TabsTrigger>
          <TabsTrigger value="email"><Mail className="h-4 w-4 mr-1" />{t('emailSettings.title')}</TabsTrigger>
          <TabsTrigger value="sso"><Shield className="h-4 w-4 mr-1" />{t('sso.tabLabel')}</TabsTrigger>
        </TabsList>

        <TabsContent value="general" className="space-y-4">
          <GeneralSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="cycles" className="space-y-4">
          <CycleSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="risk" className="space-y-4">
          <RiskSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="weights" className="space-y-4">
          <WeightsSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="colors" className="space-y-4">
          <ColorsSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="bugs" className="space-y-4">
          <BugSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="categories" className="space-y-4">
          <CategoriesSettingsTab formData={formData} />
        </TabsContent>

        <TabsContent value="meetings" className="space-y-4">
          <MeetingsSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="features" className="space-y-4">
          <FeaturesSettingsTab formData={formData} setFormData={setFormData} settings={settings} />
        </TabsContent>

        <TabsContent value="email" className="space-y-4">
          <EmailSettingsTab formData={formData} setFormData={setFormData} />
        </TabsContent>

        <TabsContent value="sso" className="space-y-4">
          <SsoSettingsTab />
        </TabsContent>
      </Tabs>

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
    </div>
  );
}
