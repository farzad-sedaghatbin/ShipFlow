import { useTranslation } from 'react-i18next';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { X, RefreshCw, Trash2 } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import { knowledgeService } from '../../services/knowledgeService';
import type {
  ChunkPreview,
  KnowledgeSource,
  KnowledgeSourceStatus,
} from '../../types/knowledge';
import { useToast } from '../../contexts';
import { useKnowledgeSources } from '../../hooks/useKnowledgeSources';

const STATUS_CLASS: Record<KnowledgeSourceStatus, string> = {
  READY: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200',
  INGESTING: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200',
  PENDING: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200',
  STALE: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200',
  FAILED: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200',
};

interface Props {
  sourceId: number;
  onClose: () => void;
}

export function SourceDetailPanel({ sourceId, onClose }: Props) {
  const { t } = useTranslation();
  const qc = useQueryClient();
  const { showToast } = useToast();

  // Pull the source from the list cache so we don't issue a redundant GET.
  const { data: sources } = useKnowledgeSources();
  const source: KnowledgeSource | undefined = sources?.find(
    (s) => s.id === sourceId
  );

  const chunksQ = useQuery<ChunkPreview[]>({
    queryKey: ['knowledge', 'chunks', sourceId],
    queryFn: async () => (await knowledgeService.chunks(sourceId)).data,
  });

  const refresh = useMutation({
    mutationFn: async () => {
      await knowledgeService.refresh(sourceId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge'] });
    },
    onError: (err) => showToast((err as Error).message, 'error'),
  });

  const remove = useMutation({
    mutationFn: async () => {
      await knowledgeService.remove(sourceId);
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['knowledge'] });
      onClose();
    },
    onError: (err) => showToast((err as Error).message, 'error'),
  });

  return (
    <>
      {/* Click-outside overlay */}
      <div
        className="fixed inset-0 z-30 bg-black/30"
        onClick={onClose}
        aria-hidden
      />
      <aside
        role="dialog"
        aria-label={t('knowledgeCenter.sourceDetail')}
        className="fixed inset-y-0 end-0 z-40 w-full max-w-md overflow-y-auto border-s border-border bg-background shadow-xl"
      >
        <header className="flex items-center justify-between border-b border-border p-4">
          <h2 className="text-lg font-semibold text-foreground truncate">
            {source?.name ?? t('knowledgeCenter.sourceDetail')}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label={t('knowledgeCenter.close')}
            className="rounded-md p-2 text-muted-foreground hover:bg-accent"
          >
            <X className="h-4 w-4" />
          </button>
        </header>

        <div className="space-y-4 p-4">
          {source && (
            <>
              <div className="flex items-center gap-2">
                <span
                  className={cn(
                    'inline-block rounded-full px-2 py-0.5 text-xs font-medium',
                    STATUS_CLASS[source.status]
                  )}
                >
                  {t(`knowledgeCenter.status.${source.status}`)}
                </span>
                <span className="text-xs text-muted-foreground">
                  {t(`knowledgeCenter.provider.${source.providerType}`)} ·{' '}
                  {source.chunkCount} {t('knowledgeCenter.chunks')}
                </span>
              </div>

              {source.description && (
                <p className="text-sm text-muted-foreground">
                  {source.description}
                </p>
              )}

              {source.lastError && (
                <div className="rounded-md border border-destructive/30 bg-destructive/10 p-3 text-xs text-destructive">
                  <div className="font-semibold mb-1">
                    {t('knowledgeCenter.lastError')}
                  </div>
                  <div className="break-words">{source.lastError}</div>
                </div>
              )}

              <div className="flex flex-wrap gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => refresh.mutate()}
                  disabled={refresh.isPending}
                >
                  <RefreshCw className="h-3.5 w-3.5 me-1.5" />
                  {t('knowledgeCenter.reIngest')}
                </Button>
                <Button
                  variant="destructive"
                  size="sm"
                  onClick={() => remove.mutate()}
                  disabled={remove.isPending}
                >
                  <Trash2 className="h-3.5 w-3.5 me-1.5" />
                  {t('knowledgeCenter.delete')}
                </Button>
              </div>

              <div>
                <h3 className="mb-2 text-sm font-semibold text-foreground">
                  {t('knowledgeCenter.chunkPreview')}
                </h3>
                {chunksQ.isLoading ? (
                  <div className="space-y-2">
                    <div className="h-12 rounded bg-accent/40 animate-pulse" />
                    <div className="h-12 rounded bg-accent/40 animate-pulse" />
                  </div>
                ) : chunksQ.data && chunksQ.data.length > 0 ? (
                  <ul className="space-y-2">
                    {chunksQ.data.map((c) => (
                      <li
                        key={c.id}
                        className="rounded-md border border-border bg-card p-3"
                      >
                        <div className="text-sm font-medium text-foreground truncate">
                          {c.title || `#${c.ordinal}`}
                        </div>
                        <div className="mt-1 text-xs text-muted-foreground line-clamp-3 whitespace-pre-wrap">
                          {c.contentPreview}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-xs text-muted-foreground">
                    {t('knowledgeCenter.noChunks')}
                  </p>
                )}
              </div>
            </>
          )}
        </div>
      </aside>
    </>
  );
}
