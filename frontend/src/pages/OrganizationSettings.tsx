import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import {
  Settings,
  Save,
  RotateCcw,
  Loader2,
  ShieldAlert,
  Calendar,
  CalendarClock,
  AlertTriangle,
  Tags,
  Globe,
  Sparkles,
  Palette,
  Bug,
  Mail,
  Shield,
  Users,
  Upload,
  Scale,
  Puzzle,
  Sliders,
} from 'lucide-react';
import { cn } from '../lib/utils';
import { useToast } from '../contexts';
import { organizationSettingsService } from '../services/organizationSettingsService';
import { OrganizationSettings, RiskThresholds, ColorSettings } from '../types/organizationSettings';
import { usePermission } from '../hooks/usePermission';
import { Button } from '../components/ui/button';
import { Alert, AlertDescription } from '../components/ui/alert';
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
  ScimSettingsTab,
  PluginsSettingsTab,
  CustomFieldsSettingsTab,
} from '../components/organizationSettings';

const DEFAULT_RISK_THRESHOLDS: RiskThresholds = { lowMax: 30, mediumMax: 60, highMax: 85 };
const DEFAULT_COLORS: ColorSettings = {
  appetiteHours: '#3B82F6',
  actualHours: '#10B981',
  overBudget: '#EF4444',
  underBudget: '#22C55E',
};

interface FormData extends Partial<OrganizationSettings> {}

type SectionId =
  | 'general'
  | 'cycles'
  | 'categories'
  | 'meetings'
  | 'colors'
  | 'bugs'
  | 'risk'
  | 'weights'
  | 'email'
  | 'features'
  | 'plugins'
  | 'sso'
  | 'scim'
  | 'import'
  | 'customFields';

interface SidebarItem {
  id: SectionId;
  labelKey: string;
  icon: React.ElementType;
}

interface SidebarGroup {
  headerKey: string;
  items: SidebarItem[];
}

const SIDEBAR_GROUPS: SidebarGroup[] = [
  {
    headerKey: 'organizationSettings.sectionGeneral',
    items: [
      { id: 'general', labelKey: 'organizationSettings.general', icon: Globe },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionWorkManagement',
    items: [
      { id: 'cycles', labelKey: 'organizationSettings.cycles', icon: Calendar },
      { id: 'categories', labelKey: 'organizationSettings.categories', icon: Tags },
      { id: 'meetings', labelKey: 'organizationSettings.meetingTypes', icon: CalendarClock },
      { id: 'customFields', labelKey: 'customFields.navLabel', icon: Sliders },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionAppearance',
    items: [
      { id: 'colors', labelKey: 'organizationSettings.colors', icon: Palette },
      { id: 'bugs', labelKey: 'organizationSettings.bugs', icon: Bug },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionQualityRisk',
    items: [
      { id: 'risk', labelKey: 'organizationSettings.risk', icon: AlertTriangle },
      { id: 'weights', labelKey: 'organizationSettings.weights', icon: Scale },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionNotifications',
    items: [
      { id: 'email', labelKey: 'emailSettings.title', icon: Mail },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionFeatures',
    items: [
      { id: 'features', labelKey: 'organizationSettings.features', icon: Sparkles },
      { id: 'plugins', labelKey: 'pluginSettings.navLabel', icon: Puzzle },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionEnterprise',
    items: [
      { id: 'sso', labelKey: 'sso.tabLabel', icon: Shield },
      { id: 'scim', labelKey: 'scim.title', icon: Users },
    ],
  },
  {
    headerKey: 'organizationSettings.sectionData',
    items: [
      { id: 'import', labelKey: 'organizationSettings.importData', icon: Upload },
    ],
  },
];

export default function OrganizationSettingsPage() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [settings, setSettings] = useState<OrganizationSettings | null>(null);
  const [formData, setFormData] = useState<FormData>({});
  const { hasPermission } = usePermission();
  const [canManageSettings, setCanManageSettings] = useState<boolean | null>(null);
  const [resetConfirmOpen, setResetConfirmOpen] = useState(false);

  const initialSection = (searchParams.get('tab') as SectionId | null) ?? 'general';
  const [activeSection, setActiveSection] = useState<SectionId>(initialSection);

  const handleSectionChange = (section: SectionId) => {
    setActiveSection(section);
    setSearchParams({ tab: section }, { replace: true });
  };

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

  const renderContent = () => {
    switch (activeSection) {
      case 'general':
        return <GeneralSettingsTab formData={formData} setFormData={setFormData} />;
      case 'cycles':
        return <CycleSettingsTab formData={formData} setFormData={setFormData} />;
      case 'categories':
        return <CategoriesSettingsTab formData={formData} />;
      case 'meetings':
        return <MeetingsSettingsTab formData={formData} setFormData={setFormData} />;
      case 'colors':
        return <ColorsSettingsTab formData={formData} setFormData={setFormData} />;
      case 'bugs':
        return <BugSettingsTab formData={formData} setFormData={setFormData} />;
      case 'risk':
        return <RiskSettingsTab formData={formData} setFormData={setFormData} />;
      case 'weights':
        return <WeightsSettingsTab formData={formData} setFormData={setFormData} />;
      case 'email':
        return <EmailSettingsTab formData={formData} setFormData={setFormData} />;
      case 'features':
        return <FeaturesSettingsTab formData={formData} setFormData={setFormData} settings={settings} />;
      case 'customFields':
        return <CustomFieldsSettingsTab />;
      case 'plugins':
        return <PluginsSettingsTab />;
      case 'sso':
        return <SsoSettingsTab />;
      case 'scim':
        return <ScimSettingsTab formData={formData} setFormData={setFormData} />;
      case 'import':
        return (
          <div className="space-y-4">
            <div>
              <h3 className="text-lg font-semibold">{t('organizationSettings.importData')}</h3>
              <p className="text-sm text-muted-foreground mt-1">{t('organizationSettings.importDataDesc')}</p>
            </div>
            <Button variant="outline" className="gap-2" onClick={() => navigate('/import')}>
              <Upload className="h-4 w-4" />
              {t('organizationSettings.goToImport')}
            </Button>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="space-y-6">
      {/* Header — sticky so Save is always reachable while scrolled into a tab */}
      <div className="sticky top-0 z-10 bg-background pb-4 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
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

      {/* Mobile section selector */}
      <div className="md:hidden">
        <select
          value={activeSection}
          onChange={(e) => handleSectionChange(e.target.value as SectionId)}
          className="w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2"
        >
          {SIDEBAR_GROUPS.map((group) =>
            group.items.map((item) => (
              <option key={item.id} value={item.id}>
                {t(item.labelKey)}
              </option>
            ))
          )}
        </select>
      </div>

      {/* Two-column layout */}
      <div className="flex gap-6 items-start">
        {/* Left sidebar nav */}
        <nav className="hidden md:block w-52 shrink-0 space-y-1">
          {SIDEBAR_GROUPS.map((group) => (
            <div key={group.headerKey}>
              <p className="px-3 py-2 mt-4 first:mt-0 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {t(group.headerKey)}
              </p>
              {group.items.map((item) => {
                const Icon = item.icon;
                const isActive = activeSection === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    onClick={() => handleSectionChange(item.id)}
                    className={cn(
                      'flex w-full items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors',
                      isActive
                        ? 'bg-accent text-accent-foreground font-medium'
                        : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground'
                    )}
                  >
                    <Icon className="h-4 w-4 shrink-0" />
                    <span className="truncate">{t(item.labelKey)}</span>
                  </button>
                );
              })}
            </div>
          ))}
        </nav>

        {/* Right content panel */}
        <div className="flex-1 min-w-0 rounded-lg border bg-card p-6">
          {renderContent()}
        </div>
      </div>

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
