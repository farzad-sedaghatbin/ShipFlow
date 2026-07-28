import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { X, UserCheck, RefreshCw, ArrowUpDown, Tag, Trash2, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from '@/components/ui/dropdown-menu';
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog';
import { taskService } from '../services/taskService';
import { BulkAction, TaskStatus, TaskPriority, Person } from '../types';

interface Props {
  selectedIds: Set<number>;
  persons: Person[];
  onClear: () => void;
  onSuccess: () => void;
}

const STATUS_OPTIONS: { value: TaskStatus; labelKey: string }[] = [
  { value: 'BACKLOG', labelKey: 'backlogPage.statusOptions.backlog' },
  { value: 'TODO', labelKey: 'backlogPage.statusOptions.todo' },
  { value: 'IN_PROGRESS', labelKey: 'backlogPage.statusOptions.inProgress' },
  { value: 'BLOCKED', labelKey: 'backlogPage.statusOptions.blocked' },
  { value: 'IN_REVIEW', labelKey: 'backlogPage.statusOptions.inReview' },
  { value: 'DONE', labelKey: 'backlogPage.statusOptions.done' },
  { value: 'CANCELLED', labelKey: 'backlogPage.statusOptions.cancelled' },
];

const PRIORITY_OPTIONS: { value: TaskPriority; labelKey: string }[] = [
  { value: 'LOW', labelKey: 'backlogPage.priorityOptions.low' },
  { value: 'MEDIUM', labelKey: 'backlogPage.priorityOptions.medium' },
  { value: 'HIGH', labelKey: 'backlogPage.priorityOptions.high' },
  { value: 'URGENT', labelKey: 'backlogPage.priorityOptions.urgent' },
];

export default function BulkActionBar({ selectedIds, persons, onClear, onSuccess }: Props) {
  const { t } = useTranslation();
  const [busy, setBusy] = useState(false);
  const [tagInput, setTagInput] = useState('');
  const [tagOpen, setTagOpen] = useState(false);
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);

  const count = selectedIds.size;
  const ids = Array.from(selectedIds);

  async function apply(action: BulkAction, value?: string) {
    setBusy(true);
    try {
      const res = await taskService.bulkUpdate({ taskIds: ids, action, value });
      const { data } = res;
      if (data.failureCount === 0) {
        toast.success(t('bulkActions.successAll', { count: data.successCount }));
      } else {
        toast.warning(
          t('bulkActions.partialSuccess', {
            success: data.successCount,
            failed: data.failureCount,
          })
        );
      }
      onSuccess();
    } catch {
      toast.error(t('bulkActions.error'));
    } finally {
      setBusy(false);
    }
  }

  function handleTagSubmit() {
    const tag = tagInput.trim();
    if (!tag) return;
    setTagOpen(false);
    setTagInput('');
    apply('ADD_TAG', tag);
  }

  if (count === 0) return null;

  return (
    <>
      {/* Sticky bar fixed to bottom of viewport.
          Below `sm` this spans the viewport width (minus a small margin) with
          a horizontally-scrolling button row instead of the desktop
          centered/fixed-width bar — six actions + separators comfortably
          exceed 600px, which overflows a 375px (iPhone SE) screen with no
          way to reach the clipped buttons otherwise. */}
      <div
        className="fixed bottom-4 left-2 right-2 z-50 mx-auto flex max-w-full items-center gap-1 overflow-x-auto rounded-xl border bg-background px-3 py-2 shadow-2xl ring-1 ring-border sm:bottom-6 sm:left-1/2 sm:right-auto sm:w-auto sm:max-w-[calc(100vw-2rem)] sm:-translate-x-1/2 sm:gap-2 sm:px-4 sm:py-2.5"
        role="toolbar"
        aria-label={t('bulkActions.toolbar')}
      >
        {/* Count badge */}
        <Badge variant="default" className="shrink-0 tabular-nums">
          {t('bulkActions.selected', { count })}
        </Badge>

        <div className="h-5 w-px shrink-0 bg-border" />

        {/* Assign to */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              className="shrink-0 gap-1.5"
              aria-label={t('bulkActions.assignTo')}
            >
              <UserCheck className="h-3.5 w-3.5 shrink-0" />
              <span className="hidden sm:inline">{t('bulkActions.assignTo')}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="center" className="max-h-64 overflow-y-auto">
            <DropdownMenuLabel>{t('bulkActions.selectPerson')}</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onSelect={() => apply('ASSIGN', '')}>
              <span className="text-muted-foreground">{t('backlogPage.unassigned')}</span>
            </DropdownMenuItem>
            {persons.map((p) => (
              <DropdownMenuItem key={p.id} onSelect={() => apply('ASSIGN', String(p.id))}>
                {p.name}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Change status */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              className="shrink-0 gap-1.5"
              aria-label={t('bulkActions.changeStatus')}
            >
              <RefreshCw className="h-3.5 w-3.5 shrink-0" />
              <span className="hidden sm:inline">{t('bulkActions.changeStatus')}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="center">
            {STATUS_OPTIONS.map((s) => (
              <DropdownMenuItem key={s.value} onSelect={() => apply('CHANGE_STATUS', s.value)}>
                {t(s.labelKey)}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Change priority */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              className="shrink-0 gap-1.5"
              aria-label={t('bulkActions.changePriority')}
            >
              <ArrowUpDown className="h-3.5 w-3.5 shrink-0" />
              <span className="hidden sm:inline">{t('bulkActions.changePriority')}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="center">
            {PRIORITY_OPTIONS.map((p) => (
              <DropdownMenuItem key={p.value} onSelect={() => apply('CHANGE_PRIORITY', p.value)}>
                {t(p.labelKey)}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Add tag */}
        <DropdownMenu open={tagOpen} onOpenChange={setTagOpen}>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              disabled={busy}
              className="shrink-0 gap-1.5"
              aria-label={t('bulkActions.addTag')}
            >
              <Tag className="h-3.5 w-3.5 shrink-0" />
              <span className="hidden sm:inline">{t('bulkActions.addTag')}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="center" className="p-2 w-52">
            <div className="flex gap-1">
              <Input
                autoFocus
                placeholder={t('bulkActions.tagPlaceholder')}
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleTagSubmit();
                  }
                }}
                className="h-8 text-sm"
              />
              <Button size="sm" className="h-8 px-2 shrink-0" onClick={handleTagSubmit}>
                {t('common.add')}
              </Button>
            </div>
          </DropdownMenuContent>
        </DropdownMenu>

        <div className="h-5 w-px shrink-0 bg-border" />

        {/* Delete */}
        <Button
          variant="ghost"
          size="sm"
          disabled={busy}
          className="shrink-0 gap-1.5 text-destructive hover:text-destructive"
          aria-label={t('common.delete')}
          onClick={() => setDeleteConfirmOpen(true)}
        >
          <Trash2 className="h-3.5 w-3.5 shrink-0" />
          <span className="hidden sm:inline">{t('common.delete')}</span>
        </Button>

        <div className="h-5 w-px shrink-0 bg-border" />

        {/* Loading indicator or Clear */}
        {busy ? (
          <Loader2 className="h-4 w-4 shrink-0 animate-spin text-muted-foreground" />
        ) : (
          <Button
            variant="ghost"
            size="icon"
            className="h-7 w-7 shrink-0"
            onClick={onClear}
            title={t('bulkActions.clearSelection')}
          >
            <X className="h-3.5 w-3.5" />
          </Button>
        )}
      </div>

      {/* Delete confirmation dialog */}
      <AlertDialog open={deleteConfirmOpen} onOpenChange={setDeleteConfirmOpen}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('bulkActions.deleteConfirmTitle', { count })}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('bulkActions.deleteConfirmDescription', { count })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => {
                setDeleteConfirmOpen(false);
                apply('DELETE');
              }}
            >
              {t('common.delete')}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
