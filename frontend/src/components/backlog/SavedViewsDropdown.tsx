import { useState, MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';
import { Bookmark, Star, Trash2, Check, Loader2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Input } from '../ui/input';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { savedViewService, SavedViewFilters, SavedView } from '../../services/savedViewService';

interface Props {
  projectId: number;
  currentFilters: SavedViewFilters;
  onApplyView: (filters: SavedViewFilters) => void;
}

function filtersEqual(a: SavedViewFilters, b: SavedViewFilters): boolean {
  const normalize = (f: SavedViewFilters) => ({
    statusFilter: f.statusFilter ?? [],
    priorityFilter: f.priorityFilter ?? [],
    assigneeFilter: f.assigneeFilter ?? [],
    activeCategory: f.activeCategory ?? '',
    dependencyFilter: f.dependencyFilter ?? 'all',
    sortBy: f.sortBy ?? 'createdAt',
    sortOrder: f.sortOrder ?? 'desc',
    searchQuery: f.searchQuery ?? '',
  });
  const na = normalize(a);
  const nb = normalize(b);
  return (
    JSON.stringify(na.statusFilter.slice().sort()) === JSON.stringify(nb.statusFilter.slice().sort()) &&
    JSON.stringify(na.priorityFilter.slice().sort()) === JSON.stringify(nb.priorityFilter.slice().sort()) &&
    JSON.stringify(na.assigneeFilter.slice().sort()) === JSON.stringify(nb.assigneeFilter.slice().sort()) &&
    na.activeCategory === nb.activeCategory &&
    na.dependencyFilter === nb.dependencyFilter &&
    na.sortBy === nb.sortBy &&
    na.sortOrder === nb.sortOrder &&
    na.searchQuery === nb.searchQuery
  );
}

function hasActiveFilters(filters: SavedViewFilters): boolean {
  return (
    (filters.statusFilter?.length ?? 0) > 0 ||
    (filters.priorityFilter?.length ?? 0) > 0 ||
    (filters.assigneeFilter?.length ?? 0) > 0 ||
    (filters.dependencyFilter !== undefined && filters.dependencyFilter !== 'all') ||
    (filters.searchQuery !== undefined && filters.searchQuery.trim() !== '')
  );
}

export function SavedViewsDropdown({ projectId, currentFilters, onApplyView }: Props) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [open, setOpen] = useState(false);
  const [savingName, setSavingName] = useState('');
  const [showNameInput, setShowNameInput] = useState(false);

  const queryKey = ['saved-views', projectId] as const;

  const { data: views = [], isLoading } = useQuery<SavedView[]>({
    queryKey,
    queryFn: () => savedViewService.getSavedViews(projectId).then((r) => r.data),
    staleTime: 30_000,
    enabled: projectId > 0,
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey });

  const createMutation = useMutation({
    mutationFn: (name: string) =>
      savedViewService.createSavedView(projectId, name, currentFilters),
    onSuccess: () => {
      invalidate();
      setSavingName('');
      setShowNameInput(false);
      toast.success(t('savedViews.viewSaved'));
    },
    onError: () => {
      toast.error(t('common.error', 'An error occurred'));
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => savedViewService.deleteSavedView(projectId, id),
    onSuccess: () => {
      invalidate();
      toast.success(t('savedViews.viewDeleted'));
    },
    onError: () => {
      toast.error(t('common.error', 'An error occurred'));
    },
  });

  const setDefaultMutation = useMutation({
    mutationFn: (id: number) => savedViewService.setDefault(projectId, id),
    onSuccess: () => {
      invalidate();
    },
    onError: () => {
      toast.error(t('common.error', 'An error occurred'));
    },
  });

  const activeView = views.find((v) => filtersEqual(v.filters, currentFilters));

  const handleSave = () => {
    const name = savingName.trim();
    if (!name) return;
    createMutation.mutate(name);
  };

  const handleDelete = (view: SavedView, e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    if (window.confirm(t('savedViews.confirmDelete', { name: view.name }))) {
      deleteMutation.mutate(view.id);
    }
  };

  const handleSetDefault = (view: SavedView, e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    setDefaultMutation.mutate(view.id);
  };

  const triggerLabel = activeView ? activeView.name : t('savedViews.title');

  return (
    <DropdownMenu open={open} onOpenChange={setOpen}>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" size="sm" className="gap-1.5">
          <Bookmark className="h-4 w-4" />
          {activeView && <Check className="h-3 w-3 text-primary" />}
          <span className="max-w-[120px] truncate">{triggerLabel}</span>
        </Button>
      </DropdownMenuTrigger>

      <DropdownMenuContent align="end" className="w-64">
        <DropdownMenuLabel>{t('savedViews.title')}</DropdownMenuLabel>
        <DropdownMenuSeparator />

        {isLoading && (
          <div className="flex items-center justify-center py-3">
            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
          </div>
        )}

        {!isLoading && views.length === 0 && (
          <div className="px-2 py-3 text-sm text-muted-foreground text-center">
            {t('savedViews.noViews')}
          </div>
        )}

        {views.map((view) => (
          <DropdownMenuItem
            key={view.id}
            className="flex items-center justify-between cursor-pointer group"
            onSelect={(e) => {
              e.preventDefault();
              onApplyView(view.filters);
              setOpen(false);
            }}
          >
            <span className="flex items-center gap-1.5 truncate flex-1 mr-1">
              {filtersEqual(view.filters, currentFilters) && (
                <Check className="h-3 w-3 text-primary shrink-0" />
              )}
              <span className="truncate">{view.name}</span>
            </span>
            <span className="flex items-center gap-1 shrink-0">
              <button
                className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 rounded hover:bg-muted"
                title={t('savedViews.setDefault')}
                aria-label={t('savedViews.setDefault')}
                onClick={(e) => handleSetDefault(view, e)}
              >
                <Star
                  aria-hidden="true"
                  className={`h-3.5 w-3.5 ${view.isDefault ? 'fill-yellow-400 text-yellow-400' : 'text-muted-foreground'}`}
                />
              </button>
              <button
                className="opacity-0 group-hover:opacity-100 transition-opacity p-0.5 rounded hover:bg-destructive/10 hover:text-destructive"
                title={t('savedViews.deleteView')}
                aria-label={t('savedViews.deleteView')}
                onClick={(e) => handleDelete(view, e)}
              >
                <Trash2 aria-hidden="true" className="h-3.5 w-3.5" />
              </button>
            </span>
          </DropdownMenuItem>
        ))}

        <DropdownMenuSeparator />

        {showNameInput ? (
          <div
            className="px-2 py-2 flex items-center gap-1"
            onClick={(e) => e.stopPropagation()}
            onKeyDown={(e) => e.stopPropagation()}
          >
            <Input
              autoFocus
              value={savingName}
              onChange={(e) => setSavingName(e.target.value)}
              placeholder={t('savedViews.enterName', 'View name…')}
              className="h-7 text-sm flex-1"
              onKeyDown={(e) => {
                if (e.key === 'Enter') handleSave();
                if (e.key === 'Escape') setShowNameInput(false);
              }}
            />
            <Button
              size="sm"
              className="h-7 px-2"
              disabled={!savingName.trim() || createMutation.isPending}
              onClick={handleSave}
            >
              {createMutation.isPending ? (
                <Loader2 className="h-3 w-3 animate-spin" />
              ) : (
                t('savedViews.saveView')
              )}
            </Button>
          </div>
        ) : (
          <DropdownMenuItem
            onSelect={(e) => {
              e.preventDefault();
              setShowNameInput(true);
            }}
          >
            <Bookmark className="h-4 w-4 mr-2" />
            {t('savedViews.saveCurrentFilters', 'Save current filters')}
          </DropdownMenuItem>
        )}

        {hasActiveFilters(currentFilters) && (
          <DropdownMenuItem
            onSelect={() => {
              onApplyView({
                statusFilter: [],
                priorityFilter: [],
                assigneeFilter: [],
                activeCategory: currentFilters.activeCategory,
                dependencyFilter: 'all',
                sortBy: 'createdAt',
                sortOrder: 'desc',
                searchQuery: '',
              });
              setOpen(false);
            }}
          >
            {t('savedViews.clearFilters', 'Clear filters')}
          </DropdownMenuItem>
        )}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
