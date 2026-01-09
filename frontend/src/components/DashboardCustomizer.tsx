import { useState, useEffect } from 'react';
import { Eye, EyeOff, GripVertical, RotateCcw } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Switch } from '@/components/ui/switch';
import { Label } from '@/components/ui/label';
import { dashboardWidgetApi } from '../services/dashboardApi';
import { DashboardWidget } from '../types/dashboard';
import { useToast } from '../contexts';

const WIDGET_LABELS: Record<string, string> = {
  OVERDUE_TASKS: 'Overdue Tasks',
  BLOCKED_TASKS: 'Blocked Tasks',
  UPCOMING_DEADLINES: 'Upcoming Deadlines',
  MY_TASKS: 'My Tasks',
  TEAM_WORKLOAD: 'Team Workload',
  CYCLE_PROGRESS: 'Cycle Progress',
  RECENT_ACTIVITY: 'Recent Activity',
};

const WIDGET_DESCRIPTIONS: Record<string, string> = {
  OVERDUE_TASKS: 'Shows tasks that are past their due date',
  BLOCKED_TASKS: 'Displays all currently blocked tasks',
  UPCOMING_DEADLINES: 'Cycle deadlines in the next 2 weeks',
  MY_TASKS: 'Your assigned tasks with completion stats',
  TEAM_WORKLOAD: 'Workload distribution across teams',
  CYCLE_PROGRESS: 'Progress tracking for active cycles',
  RECENT_ACTIVITY: 'Latest activity feed from the system',
};

interface DashboardCustomizerProps {
  onUpdate?: () => void;
}

export function DashboardCustomizer({ onUpdate }: DashboardCustomizerProps) {
  const [widgets, setWidgets] = useState<DashboardWidget[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { showSuccess, showError } = useToast();

  useEffect(() => {
    loadWidgets();
  }, []);

  const loadWidgets = async () => {
    try {
      setLoading(true);
      const data = await dashboardWidgetApi.getAllWidgets();
      setWidgets(data.sort((a, b) => a.displayOrder - b.displayOrder));
    } catch (error) {
      console.error('Failed to load widgets:', error);
      showError('Failed to load widget configuration');
    } finally {
      setLoading(false);
    }
  };

  const toggleVisibility = async (widget: DashboardWidget) => {
    try {
      const updated = await dashboardWidgetApi.updateWidget(widget.id, {
        isVisible: !widget.isVisible,
      });
      setWidgets((prev) =>
        prev.map((w) => (w.id === widget.id ? updated : w))
      );
      onUpdate?.();
      showSuccess(`Widget ${updated.isVisible ? 'shown' : 'hidden'}`);
    } catch (error) {
      console.error('Failed to toggle widget:', error);
      showError('Failed to update widget');
    }
  };

  const resetToDefaults = async () => {
    try {
      setSaving(true);
      const defaultWidgets = await dashboardWidgetApi.resetToDefaults();
      setWidgets(defaultWidgets.sort((a, b) => a.displayOrder - b.displayOrder));
      onUpdate?.();
      showSuccess('Widgets reset to default configuration');
    } catch (error) {
      console.error('Failed to reset widgets:', error);
      showError('Failed to reset widgets');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle>Widget Configuration</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="text-sm text-muted-foreground">Loading...</div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle>Widget Configuration</CardTitle>
          <Button
            variant="outline"
            size="sm"
            onClick={resetToDefaults}
            disabled={saving}
            className="gap-2"
          >
            <RotateCcw className="w-4 h-4" />
            Reset to Defaults
          </Button>
        </div>
        <p className="text-sm text-muted-foreground">
          Toggle visibility of widgets on your dashboard
        </p>
      </CardHeader>
      <CardContent>
        <div className="space-y-3">
          {widgets.map((widget) => (
            <div
              key={widget.id}
              className="flex items-start gap-3 p-3 rounded-lg border bg-card hover:bg-muted/50 transition-colors"
            >
              <GripVertical className="w-4 h-4 text-muted-foreground mt-0.5 cursor-move" />
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                  <Label htmlFor={`widget-${widget.id}`} className="font-medium cursor-pointer">
                    {WIDGET_LABELS[widget.widgetType] || widget.widgetType}
                  </Label>
                  {widget.isVisible ? (
                    <Eye className="w-3.5 h-3.5 text-emerald-500" />
                  ) : (
                    <EyeOff className="w-3.5 h-3.5 text-muted-foreground" />
                  )}
                </div>
                <p className="text-xs text-muted-foreground">
                  {WIDGET_DESCRIPTIONS[widget.widgetType] || 'Custom widget'}
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
            <strong>Tip:</strong> Hidden widgets won't appear on your dashboard but can be re-enabled
            anytime. Changes are saved automatically.
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
