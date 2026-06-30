import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ScopeTabs } from '../components/knowledge/ScopeTabs';
import { SourceList } from '../components/knowledge/SourceList';
import { AddSourceDialog } from '../components/knowledge/AddSourceDialog';
import { useKnowledgeSourceEvents } from '../hooks/useKnowledgeSourceEvents';

/**
 * Knowledge Center landing page — lists user-curated knowledge sources fed into
 * the AI features (Q&A, test generation, architecture, risk).
 */
export default function KnowledgeCenter() {
  const { t } = useTranslation();
  const [openAdd, setOpenAdd] = useState(false);

  // Subscribe to SSE events so the list refreshes when ingestion finishes.
  useKnowledgeSourceEvents();

  return (
    <div className="space-y-4 p-6" data-tour="knowledge-page">
      <header className="flex items-center justify-between gap-4 flex-wrap">
        <div>
          <h1 className="text-2xl font-semibold text-foreground">
            {t('knowledgeCenter.title')}
          </h1>
          <p className="text-sm text-muted-foreground">
            {t('knowledgeCenter.subtitle')}
          </p>
        </div>
        <button
          data-tour="knowledge-add-source"
          onClick={() => setOpenAdd(true)}
          className="rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
        >
          {t('knowledgeCenter.addSource')}
        </button>
      </header>

      <ScopeTabs />
      <SourceList />

      {openAdd && <AddSourceDialog onClose={() => setOpenAdd(false)} />}
    </div>
  );
}
