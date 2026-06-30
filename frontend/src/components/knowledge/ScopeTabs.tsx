import { useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { cn } from '@/lib/utils';

type Scope = 'org' | 'team' | 'project';

/**
 * Scope selector for the Knowledge Center.
 *
 * For now only "Org-wide" is exposed; team and project tabs will be wired once
 * lightweight user-membership lookup hooks are available. The backend list
 * endpoints already support scope=team&teamId / scope=project&projectId.
 */
export function ScopeTabs() {
  const [params, setParams] = useSearchParams();
  const { t } = useTranslation();
  const scope = (params.get('scope') ?? 'org') as Scope;

  const select = (s: Scope, extra?: Record<string, string>) => {
    const next = new URLSearchParams({ scope: s, ...(extra ?? {}) });
    setParams(next, { replace: true });
  };

  const tabClass = (active: boolean) =>
    cn(
      'px-3 py-1.5 text-sm rounded-md border transition-colors',
      active
        ? 'bg-primary/10 border-primary/40 text-primary'
        : 'bg-background border-border text-muted-foreground hover:bg-accent hover:text-accent-foreground'
    );

  return (
    <div className="flex items-center gap-2 flex-wrap">
      <button
        type="button"
        data-tour="knowledge-scope-org"
        className={tabClass(scope === 'org')}
        onClick={() => select('org')}
      >
        {t('knowledgeCenter.scope.org')}
      </button>
      {/*
        TODO(knowledge-center): wire team/project tabs once a hook returning
        the current user's team/project memberships exists. Backend endpoints
        for scope=team/project already accept teamId/projectId query params.
      */}
    </div>
  );
}
