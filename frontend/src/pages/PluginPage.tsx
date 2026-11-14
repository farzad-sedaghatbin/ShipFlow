import { useParams } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { isAxiosError } from 'axios';
import { AlertCircle, Loader2 } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { PluginBlockRenderer } from '@/components/plugins/PluginBlockRenderer';
import { useProject } from '@/contexts';
import { pluginService } from '@/services/pluginService';

/** Hosts a page contributed by a UI plugin: /plugins/:pluginId/:pageKey. */
export default function PluginPage() {
  const { t } = useTranslation();
  const { pluginId = '', pageKey = '' } = useParams();
  const { currentProject } = useProject();
  const projectId = currentProject?.id ?? null;

  const { data, isLoading, error } = useQuery({
    queryKey: ['plugins', 'ui', 'page', pluginId, pageKey, projectId],
    queryFn: async () => (await pluginService.getPage(pluginId, pageKey, projectId)).data,
    retry: false,
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-16" role="status">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" aria-hidden="true" />
        <span className="sr-only">{t('plugins.page.loading')}</span>
      </div>
    );
  }

  if (error || !data) {
    const status = isAxiosError(error) ? error.response?.status : undefined;
    const messageKey =
      status === 404
        ? 'plugins.page.notFound'
        : status === 403
          ? 'plugins.page.forbidden'
          : 'plugins.page.error';
    return (
      <Alert variant="destructive" className="max-w-2xl">
        <AlertCircle className="h-4 w-4" />
        <AlertDescription>{t(messageKey)}</AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="mx-auto flex w-full max-w-4xl flex-col gap-4">
      <h1 className="text-2xl font-bold">{data.title}</h1>
      {data.blocks.map((block, i) => (
        <PluginBlockRenderer key={i} block={block} />
      ))}
    </div>
  );
}
