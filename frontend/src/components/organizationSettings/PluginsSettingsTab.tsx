import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Puzzle, RefreshCw, AlertCircle } from 'lucide-react';
import { Badge } from '../ui/badge';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Label } from '../ui/label';
import { Separator } from '../ui/separator';
import { Switch } from '../ui/switch';
import { pluginService, PluginDTO } from '../../services/pluginService';
import { useToast } from '../../contexts';

const TYPE_LABELS: Record<string, string> = {
  risk: 'pluginSettings.typeRisk',
  report: 'pluginSettings.typeReport',
  integration: 'pluginSettings.typeIntegration',
};

const TYPE_COLORS: Record<string, string> = {
  risk: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  report: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  integration: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
};

export function PluginsSettingsTab() {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const [plugins, setPlugins] = useState<PluginDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const res = await pluginService.listPlugins();
      setPlugins(res.data);
    } catch {
      showToast(t('pluginSettings.loadFailed'), 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, []);

  const handleToggle = async (pluginId: string, enabled: boolean) => {
    setToggling(pluginId);
    try {
      const res = await pluginService.togglePlugin(pluginId, enabled);
      setPlugins((prev) => prev.map((p) => (p.pluginId === pluginId ? res.data : p)));
      showToast(
        enabled ? t('pluginSettings.enabledSuccess') : t('pluginSettings.disabledSuccess'),
        'success',
      );
    } catch {
      showToast(t('pluginSettings.toggleFailed'), 'error');
    } finally {
      setToggling(null);
    }
  };

  const grouped = plugins.reduce<Record<string, PluginDTO[]>>((acc, p) => {
    (acc[p.type] ??= []).push(p);
    return acc;
  }, {});

  const typeOrder = ['risk', 'report', 'integration'];

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Puzzle className="h-5 w-5" />
            {t('pluginSettings.title')}
          </CardTitle>
          <CardDescription>{t('pluginSettings.description')}</CardDescription>
        </CardHeader>

        <CardContent>
          {loading ? (
            <div className="flex items-center gap-2 text-muted-foreground py-4">
              <RefreshCw className="h-4 w-4 animate-spin" />
              <span className="text-sm">{t('common.loading')}</span>
            </div>
          ) : plugins.length === 0 ? (
            <div className="flex flex-col items-center gap-2 py-8 text-center text-muted-foreground">
              <AlertCircle className="h-8 w-8" />
              <p className="text-sm">{t('pluginSettings.noPlugins')}</p>
              <p className="text-xs">{t('pluginSettings.noPluginsHint')}</p>
            </div>
          ) : (
            <div className="space-y-6">
              {typeOrder
                .filter((type) => grouped[type]?.length)
                .map((type, idx) => (
                  <div key={type}>
                    {idx > 0 && <Separator className="mb-6" />}
                    <div className="mb-3">
                      <span
                        className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TYPE_COLORS[type]}`}
                      >
                        {t(TYPE_LABELS[type])}
                      </span>
                    </div>
                    <div className="space-y-4">
                      {grouped[type].map((plugin) => (
                        <div key={plugin.pluginId} className="flex items-center justify-between">
                          <div className="space-y-0.5">
                            <div className="flex items-center gap-2">
                              <Label>{plugin.displayName}</Label>
                              <Badge variant="outline" className="text-xs font-mono">
                                {plugin.pluginId}
                              </Badge>
                            </div>
                          </div>
                          <Switch
                            checked={plugin.enabled}
                            disabled={toggling === plugin.pluginId}
                            onCheckedChange={(checked) => handleToggle(plugin.pluginId, checked)}
                          />
                        </div>
                      ))}
                    </div>
                  </div>
                ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-sm">{t('pluginSettings.sdkTitle')}</CardTitle>
          <CardDescription className="text-xs">{t('pluginSettings.sdkDesc')}</CardDescription>
        </CardHeader>
        <CardContent>
          <pre className="rounded-md bg-muted px-3 py-2 text-xs font-mono overflow-x-auto">
            {`mvn archetype:generate \\
  -DarchetypeGroupId=com.github.farzadsedaghatbin.shipflow \\
  -DarchetypeArtifactId=shipflow-plugin-archetype \\
  -DarchetypeVersion=1.0.0`}
          </pre>
        </CardContent>
      </Card>
    </div>
  );
}
