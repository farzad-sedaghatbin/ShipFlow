import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Plus, Edit3, Save, X, Filter } from 'lucide-react';
import { Button } from '../components/ui/button';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '../components/ui/dialog';
import DashboardGrid from '../components/DashboardGrid';
import WidgetSelector from '../components/WidgetSelector';
import WidgetSettings from '../components/WidgetSettings';
import { customDashboardService } from '../services/customDashboardService';
import { customMetricService } from '../services/customMetricService';
import { useToast } from '../contexts';
import { CustomDashboard, DashboardWidgetConfig, AddWidgetRequest } from '../types/customDashboard';
import { CustomMetric } from '../types/metrics';
import { getUserFriendlyError } from '../utils/errorMessages';
import { Layout } from 'react-grid-layout';
import { Switch } from '../components/ui/switch';

export default function DashboardView() {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const [dashboard, setDashboard] = useState<CustomDashboard | null>(null);
  const [widgets, setWidgets] = useState<DashboardWidgetConfig[]>([]);
  const [metrics, setMetrics] = useState<CustomMetric[]>([]);
  const [loading, setLoading] = useState(true);
  const [editMode, setEditMode] = useState(false);
  const [showWidgetSelector, setShowWidgetSelector] = useState(false);
  const [showWidgetSettings, setShowWidgetSettings] = useState(false);
  const [selectedWidget, setSelectedWidget] = useState<DashboardWidgetConfig | null>(null);
  const { showSuccess, showError } = useToast();
  const saveTimeoutRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    loadDashboard();
    loadMetrics();
  }, [id]);

  useEffect(() => {
    // Cleanup timeout on unmount
    return () => {
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }
    };
  }, []);

  const loadDashboard = async () => {
    if (!id) return;

    try {
      setLoading(true);
      const dashboardData = await customDashboardService.getById(parseInt(id));
      setDashboard(dashboardData);
      
      // Load widgets
      const widgetsData = await customDashboardService.getWidgets(parseInt(id));
      setWidgets(widgetsData);
    } catch (error) {
      showError(getUserFriendlyError(error));
    } finally {
      setLoading(false);
    }
  };

  const loadMetrics = async () => {
    try {
      const metricsData = await customMetricService.getAll();
      setMetrics(metricsData);
    } catch (error) {
      console.error('Error loading metrics:', error);
    }
  };

  const handleToggleContextFilter = async () => {
    if (!id) return;

    try {
      const updatedDashboard = await customDashboardService.toggleUserContextFilter(parseInt(id));
      setDashboard(updatedDashboard);
      // Force widgets to reload with new filter context by reloading dashboard
      await loadDashboard();
      showSuccess(
        updatedDashboard.userContextFilter 
          ? t('dashboardView.personalContext')
          : t('dashboardView.organizationWide')
      );
    } catch (error) {
      showError(getUserFriendlyError(error));
    }
  };

  const handleLayoutChange = useCallback(async (layout: Layout) => {
    if (!editMode || !id) return;

    // Debounce layout saves to avoid excessive API calls during drag
    if (saveTimeoutRef.current) {
      clearTimeout(saveTimeoutRef.current);
    }

    saveTimeoutRef.current = setTimeout(async () => {
      try {
        // Update widget positions
        const updates = Array.from(layout).map(item => ({
          id: parseInt(item.i),
          positionX: item.x,
          positionY: item.y,
          width: item.w,
          height: item.h
        }));

        // Save all position updates
        for (const update of updates) {
          const widget = widgets.find(w => w.id === update.id);
          if (widget) {
            const updatedWidget = {
              ...widget,
              positionX: update.positionX,
              positionY: update.positionY,
              width: update.width,
              height: update.height
            };
            await customDashboardService.updateWidget(parseInt(id), update.id, updatedWidget);
          }
        }
      } catch (error) {
        console.error('Error saving layout:', error);
      }
    }, 1000); // Debounce for 1 second
  }, [editMode, id, widgets]);

  const handleAddWidget = async (request: AddWidgetRequest) => {
    if (!id) return;

    try {
      const newWidget = await customDashboardService.addWidget(parseInt(id), request);
      setWidgets([...widgets, newWidget]);
      showSuccess(t('dashboardView.widgetAdded'));
    } catch (error) {
      showError(getUserFriendlyError(error));
      throw error;
    }
  };

  const handleRemoveWidget = async (widgetId: number) => {
    if (!id) return;

    if (!confirm(t('dashboardView.removeWidgetConfirm'))) {
      return;
    }

    try {
      await customDashboardService.removeWidget(parseInt(id), widgetId);
      setWidgets(widgets.filter(w => w.id !== widgetId));
      showSuccess(t('dashboardView.widgetRemoved'));
    } catch (error) {
      showError(getUserFriendlyError(error));
    }
  };

  const handleConfigureWidget = (widgetId: number) => {
    const widget = widgets.find(w => w.id === widgetId);
    if (widget) {
      setSelectedWidget(widget);
      setShowWidgetSettings(true);
    }
  };

  const handleSaveWidgetSettings = async (widget: DashboardWidgetConfig) => {
    if (!id || !widget.id) return;

    try {
      const updated = await customDashboardService.updateWidget(parseInt(id), widget.id, widget);
      setWidgets(widgets.map(w => w.id === widget.id ? updated : w));
      setShowWidgetSettings(false);
      setSelectedWidget(null);
      showSuccess(t('dashboardView.widgetUpdated'));
    } catch (error) {
      showError(getUserFriendlyError(error));
    }
  };

  const handleSaveLayout = async () => {
    setEditMode(false);
    showSuccess(t('dashboardView.layoutSaved'));
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!dashboard) {
    return (
      <div className="flex items-center justify-center h-96 text-muted-foreground">
        {t('dashboardView.notFound')}
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">{dashboard.name}</h1>
          {dashboard.description && (
            <p className="text-muted-foreground mt-1">{dashboard.description}</p>
          )}
        </div>
        <div className="flex items-center gap-2">
          {/* User Context Filter Toggle */}
          <div className="flex items-center gap-2 px-3 py-2 border rounded-lg bg-card">
            <Filter className="h-4 w-4 text-muted-foreground" />
            <span className="text-sm font-medium">
              {dashboard.userContextFilter ? t('dashboardView.personal') : t('dashboardView.organizationWide')}
            </span>
            <Switch
              checked={dashboard.userContextFilter || false}
              onCheckedChange={handleToggleContextFilter}
              aria-label="Toggle user context filter"
            />
          </div>
          
          {editMode ? (
            <>
              <Button
                variant="outline"
                onClick={() => {
                  setEditMode(false);
                  loadDashboard(); // Reload to discard changes
                }}
              >
                <X className="h-4 w-4 me-2" />
                {t('common.cancel')}
              </Button>
              <Button onClick={handleSaveLayout}>
                <Save className="h-4 w-4 me-2" />
                {t('dashboardView.saveLayout')}
              </Button>
            </>
          ) : (
            <Button variant="outline" onClick={() => setEditMode(true)}>
              <Edit3 className="h-4 w-4 me-2" />
              {t('dashboardView.editDashboard')}
            </Button>
          )}
          <Button onClick={() => setShowWidgetSelector(true)}>
            <Plus className="h-4 w-4 me-2" />
            {t('dashboardView.addWidget')}
          </Button>
        </div>
      </div>

      {/* Dashboard Grid */}
      {widgets.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-96 border-2 border-dashed rounded-lg">
          <p className="text-muted-foreground mb-4">{t('dashboardView.noWidgets')}</p>
          <Button onClick={() => setShowWidgetSelector(true)}>
            <Plus className="h-4 w-4 me-2" />
            {t('dashboardView.addFirstWidget')}
          </Button>
        </div>
      ) : (
        <DashboardGrid
          widgets={widgets}
          editable={editMode}
          userContextFilter={dashboard.userContextFilter}
          onLayoutChange={handleLayoutChange}
          onWidgetRemove={handleRemoveWidget}
          onWidgetConfigure={handleConfigureWidget}
        />
      )}

      {/* Widget Selector Dialog */}
      <WidgetSelector
        open={showWidgetSelector}
        onClose={() => setShowWidgetSelector(false)}
        metrics={metrics}
        onWidgetAdd={handleAddWidget}
      />

      {/* Widget Settings Dialog */}
      <Dialog open={showWidgetSettings} onOpenChange={setShowWidgetSettings}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>{t('dashboardView.widgetSettings')}</DialogTitle>
          </DialogHeader>
          {selectedWidget && (
            <WidgetSettings
              widget={selectedWidget}
              metrics={metrics}
              onSave={handleSaveWidgetSettings}
              onCancel={() => {
                setShowWidgetSettings(false);
                setSelectedWidget(null);
              }}
            />
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
