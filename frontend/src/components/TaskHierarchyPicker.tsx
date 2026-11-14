import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Command as CommandPrimitive } from 'cmdk';
import { ArrowRight, CornerDownRight, Loader2, Search } from 'lucide-react';
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';
import { useDebounce } from '../hooks/useDebounce';
import { taskService } from '../services/taskService';
import type { Task, TaskStatus } from '../types';

export type TaskHierarchyPickerMode = 'parent' | 'child';

interface TaskHierarchyPickerProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  /** The task whose page we are on. */
  task: Task;
  /**
   * `parent`: pick the task `task` should become a subtask of.
   * `child`: pick an existing task to add as a subtask of `task`.
   */
  mode: TaskHierarchyPickerMode;
  /** Called with the child and parent once the user confirms. Resolves true when it worked. */
  onConfirm: (child: Task, parent: Task) => Promise<boolean>;
}

const STATUS_VARIANT: Record<TaskStatus, 'secondary' | 'outline' | 'info' | 'destructive' | 'warning' | 'success'> = {
  BACKLOG: 'secondary',
  TODO: 'outline',
  IN_PROGRESS: 'info',
  BLOCKED: 'destructive',
  IN_REVIEW: 'warning',
  DONE: 'success',
  CANCELLED: 'secondary',
};

const taskKey = (t: Task) => (t.projectKey ? `${t.projectKey}-${t.id}` : `#${t.id}`);

/**
 * Jira-style "convert to subtask" / "add existing subtask" picker: a search palette (title or ID,
 * arrow keys + Enter), then a live preview of exactly what will change before anything is saved.
 * Which tasks are listed is decided by the server (same project, two levels only, no tasks that
 * already have subtasks), so what you can pick is always something that will succeed.
 */
export default function TaskHierarchyPicker({ open, onOpenChange, task, mode, onConfirm }: TaskHierarchyPickerProps) {
  const { t } = useTranslation();
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState<Task | null>(null);
  const [saving, setSaving] = useState(false);
  const debounced = useDebounce(query, 250);

  useEffect(() => {
    if (open) {
      setQuery('');
      setSelected(null);
      setSaving(false);
    }
  }, [open]);

  const { data: candidates = [], isFetching } = useQuery({
    queryKey: ['task-hierarchy-candidates', mode, task.id, debounced],
    queryFn: async () =>
      (mode === 'parent'
        ? await taskService.getParentCandidates(task.id, debounced)
        : await taskService.getSubtaskCandidates(task.id, debounced)
      ).data,
    enabled: open,
    staleTime: 0,
  });

  const child = mode === 'parent' ? task : selected;
  const parent = mode === 'parent' ? selected : task;
  const statusLabel = (s: TaskStatus) => t(`taskHierarchy.status.${s}`);

  const changes: string[] = [];
  if (child && parent) {
    if ((child.cycleId ?? null) !== (parent.cycleId ?? null)) {
      changes.push(
        t('taskHierarchy.preview.cycle', { cycle: parent.cycleName || t('taskHierarchy.productBacklog') })
      );
    }
    if (child.status === 'BACKLOG') {
      changes.push(t('taskHierarchy.preview.status', { from: statusLabel('BACKLOG'), to: statusLabel('TODO') }));
    }
  }

  const confirm = async () => {
    if (!child || !parent || saving) return;
    setSaving(true);
    const ok = await onConfirm(child, parent);
    setSaving(false);
    if (ok) onOpenChange(false);
  };

  const choose = (candidate: Task) => {
    // Enter / click on the already-selected row confirms, so the whole flow works from the keyboard.
    if (selected?.id === candidate.id) {
      void confirm();
    } else {
      setSelected(candidate);
    }
  };

  const title =
    mode === 'parent'
      ? t('taskHierarchy.pickParentTitle', { task: task.title })
      : t('taskHierarchy.pickChildTitle', { task: task.title });

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="gap-0 overflow-hidden p-0 sm:max-w-xl">
        <DialogTitle className="sr-only">{title}</DialogTitle>
        <DialogDescription className="sr-only">{t('taskHierarchy.pickerHint')}</DialogDescription>

        <div className="border-b bg-muted/30 px-4 pb-3 pt-4 pr-12">
          <p className="truncate text-sm font-medium">{title}</p>
          <p className="mt-0.5 text-xs text-muted-foreground">{t('taskHierarchy.pickerHint')}</p>
        </div>

        <CommandPrimitive shouldFilter={false} className="flex flex-col" loop>
          <div className="flex items-center gap-2 border-b px-4">
            <Search className="h-4 w-4 shrink-0 text-muted-foreground" />
            <CommandPrimitive.Input
              value={query}
              onValueChange={setQuery}
              placeholder={t('taskHierarchy.searchPlaceholder')}
              className="h-11 w-full bg-transparent text-sm outline-none placeholder:text-muted-foreground"
              autoFocus
            />
            {isFetching && <Loader2 className="h-4 w-4 shrink-0 animate-spin text-muted-foreground" />}
          </div>

          <CommandPrimitive.List className="max-h-72 overflow-y-auto p-1.5">
            {!isFetching && candidates.length === 0 && (
              <div className="px-4 py-8 text-center">
                <p className="text-sm font-medium">{t('taskHierarchy.noResults')}</p>
                {mode === 'child' && (
                  <p className="mx-auto mt-1 max-w-xs text-xs text-muted-foreground">
                    {t('taskHierarchy.noResultsChildHint')}
                  </p>
                )}
              </div>
            )}
            {candidates.map((c) => (
              <CommandPrimitive.Item
                key={c.id}
                value={String(c.id)}
                onSelect={() => choose(c)}
                className={cn(
                  'flex cursor-pointer items-center gap-3 rounded-md px-3 py-2 text-sm',
                  'data-[selected=true]:bg-accent data-[selected=true]:text-accent-foreground',
                  selected?.id === c.id && 'ring-2 ring-primary ring-offset-0'
                )}
              >
                <span className="w-20 shrink-0 truncate font-mono text-xs text-muted-foreground">{taskKey(c)}</span>
                <span className="flex-1 truncate">{c.title}</span>
                <span className="hidden max-w-[7rem] shrink-0 truncate text-xs text-muted-foreground sm:inline">
                  {c.cycleName || t('taskHierarchy.productBacklog')}
                </span>
                <Badge variant={STATUS_VARIANT[c.status]} className="shrink-0">
                  {statusLabel(c.status)}
                </Badge>
              </CommandPrimitive.Item>
            ))}
          </CommandPrimitive.List>
        </CommandPrimitive>

        <div className="border-t bg-muted/30 px-4 py-3">
          {child && parent ? (
            <div className="space-y-2" aria-live="polite">
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1 text-sm">
                <span className="font-medium">{child.title}</span>
                <ArrowRight className="h-3.5 w-3.5 text-muted-foreground rtl:rotate-180" />
                <CornerDownRight className="h-3.5 w-3.5 text-muted-foreground" />
                <span className="text-muted-foreground">{t('taskHierarchy.subtaskOf')}</span>
                <span className="font-medium">{parent.title}</span>
              </div>
              {changes.length > 0 && (
                <ul className="list-disc space-y-0.5 pl-5 text-xs text-muted-foreground">
                  {changes.map((line) => (
                    <li key={line}>{line}</li>
                  ))}
                </ul>
              )}
            </div>
          ) : (
            <p className="text-xs text-muted-foreground">{t('taskHierarchy.selectToPreview')}</p>
          )}
          <div className="mt-3 flex items-center justify-between gap-2">
            <span className="hidden text-xs text-muted-foreground sm:inline">{t('taskHierarchy.keyboardHint')}</span>
            <div className="ml-auto flex gap-2">
              <Button variant="ghost" size="sm" onClick={() => onOpenChange(false)} disabled={saving}>
                {t('common.cancel')}
              </Button>
              <Button size="sm" onClick={confirm} disabled={!child || !parent || saving}>
                {saving && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                {t('taskHierarchy.confirm')}
              </Button>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  );
}
