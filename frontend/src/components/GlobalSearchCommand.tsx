import { useState, useEffect, useCallback, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ListTodo,
  Bug,
  FileText,
  Layers,
  Search,
  Loader2,
  ArrowRight,
  ExternalLink,
} from 'lucide-react';
import {
  CommandDialog,
  CommandInput,
  CommandList,
  CommandEmpty,
  CommandGroup,
  CommandItem,
  CommandSeparator,
} from '@/components/ui/command';
import { useProject } from '../contexts';
import { searchService } from '../services/searchService';
import { GlobalSearchResult, GlobalSearchEntityType } from '../types';

interface GlobalSearchCommandProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

const ENTITY_CONFIG: Record<GlobalSearchEntityType, {
  icon: typeof ListTodo;
  groupKey: string;
}> = {
  TASK: { icon: ListTodo, groupKey: 'globalSearch.tasks' },
  SUBTASK: { icon: ListTodo, groupKey: 'globalSearch.subtasks' },
  BUG_REPORT: { icon: Bug, groupKey: 'globalSearch.bugReports' },
  PITCH: { icon: FileText, groupKey: 'globalSearch.pitches' },
  EPIC: { icon: Layers, groupKey: 'globalSearch.epics' },
};

// Order for display groups
const ENTITY_ORDER: GlobalSearchEntityType[] = ['TASK', 'SUBTASK', 'BUG_REPORT', 'PITCH', 'EPIC'];

export default function GlobalSearchCommand({ open, onOpenChange }: GlobalSearchCommandProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { currentProject, isAllProjectsSelected } = useProject();
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<GlobalSearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  // Track whether Ctrl / Cmd is held so we can open-in-new-tab on select
  const modifierRef = useRef(false);

  // Reset state when dialog closes
  useEffect(() => {
    if (!open) {
      setQuery('');
      setResults([]);
      setLoading(false);
      modifierRef.current = false;
    }
  }, [open]);

  // Track Ctrl / Cmd key so handleSelect can open in new tab
  useEffect(() => {
    if (!open) return;
    const onKeyDown = (e: KeyboardEvent) => { if (e.ctrlKey || e.metaKey) modifierRef.current = true; };
    const onKeyUp   = (e: KeyboardEvent) => { if (!e.ctrlKey && !e.metaKey) modifierRef.current = false; };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, [open]);

  // Debounced search
  const doSearch = useCallback((q: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);

    if (q.trim().length < 2 || !currentProject?.id) {
      setResults([]);
      setLoading(false);
      return;
    }

    setLoading(true);
    debounceRef.current = setTimeout(async () => {
      try {
        const res = await searchService.globalSearch(q.trim(), currentProject.id, 20);
        setResults(res.data);
      } catch {
        setResults([]);
      } finally {
        setLoading(false);
      }
    }, 300);
  }, [currentProject?.id]);

  useEffect(() => {
    doSearch(query);
    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    };
  }, [query, doSearch]);

  const openInNewTab = useCallback((route: string) => {
    window.open(window.location.origin + route, '_blank', 'noopener,noreferrer');
  }, []);

  const handleSelect = (result: GlobalSearchResult) => {
    if (modifierRef.current) {
      openInNewTab(result.route);
      // keep dialog open so the user can keep selecting
    } else {
      onOpenChange(false);
      navigate(result.route);
    }
  };

  // Group results by entity type, preserving order
  const grouped = ENTITY_ORDER
    .map((type) => ({
      type,
      items: results.filter((r) => r.entityType === type),
    }))
    .filter((g) => g.items.length > 0);

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput
        placeholder={
          isAllProjectsSelected
            ? t('globalSearch.selectProject', 'Select a project to search')
            : t('globalSearch.placeholder', 'Search tasks, bugs, pitches, epics...')
        }
        value={query}
        onValueChange={setQuery}
        disabled={isAllProjectsSelected}
      />
      <CommandList>
        {/* Project not selected state */}
        {isAllProjectsSelected && (
          <CommandEmpty>
            <div className="flex flex-col items-center gap-2 py-4">
              <Search className="h-8 w-8 text-muted-foreground/50" />
              <p className="text-muted-foreground">
                {t('globalSearch.selectProject', 'Select a project to search')}
              </p>
            </div>
          </CommandEmpty>
        )}

        {/* Loading state */}
        {!isAllProjectsSelected && loading && query.trim().length >= 2 && (
          <div className="flex items-center justify-center py-6">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* Min chars hint */}
        {!isAllProjectsSelected && !loading && query.trim().length > 0 && query.trim().length < 2 && (
          <CommandEmpty>
            {t('globalSearch.minChars', 'Type at least 2 characters')}
          </CommandEmpty>
        )}

        {/* No results */}
        {!isAllProjectsSelected && !loading && query.trim().length >= 2 && results.length === 0 && (
          <CommandEmpty>
            {t('globalSearch.noResults', 'No results found')}
          </CommandEmpty>
        )}

        {/* Empty state (no query) */}
        {!isAllProjectsSelected && query.trim().length === 0 && (
          <CommandEmpty>
            <div className="flex flex-col items-center gap-2 py-4">
              <Search className="h-8 w-8 text-muted-foreground/50" />
              <p className="text-sm text-muted-foreground">
                {t('globalSearch.placeholder', 'Search tasks, bugs, pitches, epics...')}
              </p>
            </div>
          </CommandEmpty>
        )}

        {/* Results grouped by entity type */}
        {grouped.map((group, idx) => {
          const config = ENTITY_CONFIG[group.type];
          const Icon = config.icon;
          return (
            <div key={group.type}>
              {idx > 0 && <CommandSeparator />}
              <CommandGroup heading={t(config.groupKey)}>
                {group.items.map((item) => (
                  <CommandItem
                    key={`${item.entityType}-${item.entityId}`}
                    value={`${item.entityType}-${item.entityId}-${item.title}`}
                    onSelect={() => handleSelect(item)}
                    className="group flex items-center gap-3 cursor-pointer"
                  >
                    <Icon className="h-4 w-4 shrink-0 text-muted-foreground" />
                    <div className="flex flex-col min-w-0 flex-1">
                      <span className="truncate font-medium">{item.title}</span>
                      {item.subtitle && (
                        <span className="truncate text-xs text-muted-foreground">
                          {item.subtitle}
                        </span>
                      )}
                    </div>
                    {item.matchedBy === 'EXACT_KEY' &&
                      item.entityType === 'BUG_REPORT' &&
                      typeof item.subtitle === 'string' &&
                      item.subtitle.includes(' · ') && (
                        <span className="text-xs text-primary font-mono shrink-0">
                          {item.subtitle.split(' · ')[0]}
                        </span>
                      )}
                    {/* New-tab button — always clickable, stops propagation so it doesn't also navigate in the same tab */}
                    <button
                      type="button"
                      title={t('globalSearch.openNewTab', 'Open in new tab')}
                      aria-label={t('globalSearch.openNewTab', 'Open in new tab')}
                      className="opacity-0 group-hover:opacity-100 focus:opacity-100 transition-opacity shrink-0 rounded p-0.5 hover:text-primary"
                      onClick={(e) => { e.stopPropagation(); openInNewTab(item.route); }}
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                    </button>
                    <ArrowRight className="h-3 w-3 shrink-0 text-muted-foreground/50 group-hover:hidden" />
                  </CommandItem>
                ))}
              </CommandGroup>
            </div>
          );
        })}
        {/* Keyboard hint — only visible when there are results */}
        {grouped.length > 0 && (
          <div className="border-t px-3 py-2 flex items-center justify-end gap-1 text-xs text-muted-foreground">
            <kbd className="rounded border bg-muted px-1 font-mono">⌘</kbd>
            <span>+</span>
            <kbd className="rounded border bg-muted px-1 font-mono">↵</kbd>
            <span>{t('globalSearch.openNewTabHint', 'open in new tab')}</span>
          </div>
        )}
      </CommandList>
    </CommandDialog>
  );
}
