import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useKnowledgeSources } from '../../hooks/useKnowledgeSources';
import { SourceRow } from './SourceRow';
import { SourceDetailPanel } from './SourceDetailPanel';

export function SourceList() {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useKnowledgeSources();
  const [selectedId, setSelectedId] = useState<number | null>(null);

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[0, 1, 2].map((i) => (
          <div
            key={i}
            className="h-14 rounded-md border border-border bg-card animate-pulse"
          />
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="rounded-md border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
        {t('common.error')}
      </div>
    );
  }

  const sources = data ?? [];

  if (sources.length === 0) {
    return (
      <div
        data-tour="knowledge-empty-state"
        className="rounded-md border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground"
      >
        {t('knowledgeCenter.empty')}
      </div>
    );
  }

  return (
    <>
      <div className="space-y-2" data-tour="knowledge-source-list">
        {sources.map((s) => (
          <SourceRow
            key={s.id}
            source={s}
            onSelect={() => setSelectedId(s.id)}
          />
        ))}
      </div>
      {selectedId != null && (
        <SourceDetailPanel
          sourceId={selectedId}
          onClose={() => setSelectedId(null)}
        />
      )}
    </>
  );
}
