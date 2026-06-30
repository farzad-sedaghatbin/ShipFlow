import { useTranslation } from 'react-i18next';
import { FileText, Link as LinkIcon, MoreVertical, Github } from 'lucide-react';
import { cn } from '@/lib/utils';
import type {
  KnowledgeProviderType,
  KnowledgeSource,
  KnowledgeSourceStatus,
} from '../../types/knowledge';

const STATUS_CLASS: Record<KnowledgeSourceStatus, string> = {
  READY: 'bg-green-100 text-green-800 dark:bg-green-900/40 dark:text-green-200',
  INGESTING: 'bg-blue-100 text-blue-800 dark:bg-blue-900/40 dark:text-blue-200',
  PENDING: 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200',
  STALE: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-200',
  FAILED: 'bg-red-100 text-red-800 dark:bg-red-900/40 dark:text-red-200',
};

function providerIcon(p: KnowledgeProviderType) {
  switch (p) {
    case 'FILE_UPLOAD':
      return FileText;
    case 'URL':
      return LinkIcon;
    case 'GITHUB':
      return Github;
    default:
      return FileText;
  }
}

function formatRelative(iso?: string | null): string {
  if (!iso) return '';
  const then = new Date(iso).getTime();
  if (Number.isNaN(then)) return '';
  const diffSec = Math.max(0, Math.round((Date.now() - then) / 1000));
  if (diffSec < 60) return `${diffSec}s ago`;
  if (diffSec < 3600) return `${Math.round(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.round(diffSec / 3600)}h ago`;
  return `${Math.round(diffSec / 86400)}d ago`;
}

interface Props {
  source: KnowledgeSource;
  onSelect: () => void;
}

export function SourceRow({ source, onSelect }: Props) {
  const { t } = useTranslation();
  const Icon = providerIcon(source.providerType);

  return (
    <button
      type="button"
      onClick={onSelect}
      data-tour="knowledge-source-row"
      className="w-full text-start flex items-center gap-3 rounded-md border border-border bg-card px-3 py-3 hover:bg-accent/40 transition-colors"
    >
      <Icon className="h-5 w-5 flex-shrink-0 text-muted-foreground" />
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2">
          <span className="font-medium truncate text-foreground">
            {source.name}
          </span>
          <span
            className={cn(
              'inline-block rounded-full px-2 py-0.5 text-xs font-medium',
              STATUS_CLASS[source.status]
            )}
          >
            {t(`knowledgeCenter.status.${source.status}`)}
          </span>
        </div>
        <div className="text-xs text-muted-foreground truncate">
          {t(`knowledgeCenter.provider.${source.providerType}`)} ·{' '}
          {source.chunkCount} {t('knowledgeCenter.chunks')}
          {source.lastIngestedAt && (
            <>
              {' · '}
              {t('knowledgeCenter.synced', {
                when: formatRelative(source.lastIngestedAt),
              })}
            </>
          )}
        </div>
      </div>
      <MoreVertical className="h-4 w-4 flex-shrink-0 text-muted-foreground" />
    </button>
  );
}
