import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Eye, EyeOff, GripVertical, RotateCcw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { dashboardWidgetApi } from '../services/dashboardApi';
import { DashboardWidget } from '../types/dashboard';
import { useToast, useProject } from '../contexts';
import { resolveOrgCapabilities } from '../config/projectTypeCapabilities';

interface DashboardCustomizerProps {
  widgets: DashboardWidget[];
  onUpdate?: () => void;
}

export function DashboardCustomizer({ widgets, onUpdate }: DashboardCustomizerProps) {
  const { t } = useTranslation();
  const [saving, setSaving] = useState(false);
  const { showSuccess, showError } = useToast();
  const { orgProjectTypes } = useProject();

  // Widget prefs are per-user, not per-project, so filter by what's meaningful
  // anywhere in the org (not just the currently selected project) — a user
  // with both a Kanban and a Shape Up project shouldn't lose Hill Chart just
  // because a Kanban project happens to be selected right now.
  const { defaultWidgetTypes } = resolveOrgCapabilities(orgProjectTypes);
  const sortedWidgets = [...widgets]
    .filter((widget) => defaultWidgetTypes.includes(widget.widgetType))
    .sort((a, b) => a.displayOrder - b.displayOrder);

  const toggleVisibility = async (widget: DashboardWidget) => {
    try {
      await dashboardWidgetApi.updateWidget(widget.id, {
        isVisible: !widget.isVisible,
      });
      await onUpdate?.();
      showSuccess(t('dashboardCustomizer.widgetToggled', { state: !widget.isVisible ? t('dashboardCustomizer.shown') : t('dashboardCustomizer.hidden') }));
    } catch (error) {
      console.error('Failed to toggle widget:', error);
      showError(t('dashboardCustomizer.updateFailed'));
    }
  };

  const resetToDefaults = async () => {
    try {
      setSaving(true);
      await dashboardWidgetApi.resetToDefaults();
      await onUpdate?.();
      showSuccess(t('dashboardCustomizer.resetSuccess'));
    } catch (error) {
      console.error('Failed to reset widgets:', error);
      showError(t('dashboardCustomizer.resetFailed'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>{t('dashboardCustomizer.title')}</CardTitle>
          <Button
            variant="outline"
            size="sm"
            onClick={resetToDefaults}
            disabled={saving}
            className="gap-2"
          >
            <RotateCcw className="w-4 h-4" />
            {t('dashboardCustomizer.resetToDefaults')}
          </Button>
        </div>
        <p className="text-sm text-muted-foreground">
          {t('dashboardCustomizer.description')}
        </p>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {sortedWidgets.map((widget) => (
            <div
              key={widget.id}
              className="flex items-start gap-3 p-3 rounded-lg border bg-card hover:bg-muted/50 transition-colors"
            >
              <GripVertical className="w-4 h-4 text-muted-foreground mt-0.5 cursor-move" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <Label htmlFor={`widget-${widget.id}`} className="font-medium cursor-pointer">
                    {t(`dashboardCustomizer.widgets.${widget.widgetType}.label`, widget.widgetType)}
                  </Label>
                  {widget.isVisible ? (
                    <Eye className="w-3.5 h-3.5 text-emerald-500" />
                  ) : (
                    <EyeOff className="w-3.5 h-3.5 text-muted-foreground" />
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  {t(`dashboardCustomizer.widgets.${widget.widgetType}.description`, t('dashboardCustomizer.customWidget'))}
                </p>
              </div>
              <Switch
                id={`widget-${widget.id}`}
                checked={widget.isVisible}
                onCheckedChange={() => toggleVisibility(widget)}
              />
            </div>
          ))}
        </div>

        <div className="mt-4 p-3 rounded-lg bg-muted/50 border border-border">
          <p className="text-xs text-muted-foreground">
            <strong>{t('dashboardCustomizer.tip')}:</strong> {t('dashboardCustomizer.tipText')}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
