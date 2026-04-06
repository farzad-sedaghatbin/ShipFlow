import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Checkbox } from '@/components/ui/checkbox';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { TaskStatus, TaskPriority } from '../../types';
import { statusOptions, priorityOptions } from './backlogTypes';

interface BacklogFiltersProps {
  statusFilter: TaskStatus[];
  priorityFilter: TaskPriority[];
  assigneeFilter: number[];
  dependencyFilter: 'all' | 'blocked' | 'blocking';
  statusDropdownOpen: boolean;
  priorityDropdownOpen: boolean;
  dependencyDropdownOpen: boolean;
  onStatusDropdownOpenChange: (open: boolean) => void;
  onPriorityDropdownOpenChange: (open: boolean) => void;
  onDependencyDropdownOpenChange: (open: boolean) => void;
  onStatusFilterChange: (filter: TaskStatus[]) => void;
  onPriorityFilterChange: (filter: TaskPriority[]) => void;
  onDependencyFilterChange: (filter: 'all' | 'blocked' | 'blocking') => void;
  onClearFilters: () => void;
}

export function BacklogFilters({
  statusFilter,
  priorityFilter,
  assigneeFilter,
  dependencyFilter,
  statusDropdownOpen,
  priorityDropdownOpen,
  dependencyDropdownOpen,
  onStatusDropdownOpenChange,
  onPriorityDropdownOpenChange,
  onDependencyDropdownOpenChange,
  onStatusFilterChange,
  onPriorityFilterChange,
  onDependencyFilterChange,
  onClearFilters,
}: BacklogFiltersProps) {
  const { t } = useTranslation();

  return (
    <div className="flex items-center gap-2 flex-wrap">
      {/* Status Filter */}
      <DropdownMenu open={statusDropdownOpen} onOpenChange={onStatusDropdownOpenChange}>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm">
            {t('backlogPage.filters.status')} {statusFilter.length > 0 && `(${statusFilter.length})`}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          {statusOptions.map((status) => (
            <DropdownMenuItem
              key={status.value}
              onSelect={(e) => {
                e.preventDefault();
                if (statusFilter.includes(status.value)) {
                  onStatusFilterChange(statusFilter.filter(s => s !== status.value));
                } else {
                  onStatusFilterChange([...statusFilter, status.value]);
                }
              }}
            >
              <Checkbox
                checked={statusFilter.includes(status.value)}
                className="mr-2"
              />
              {t(status.labelKey)}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Priority Filter */}
      <DropdownMenu open={priorityDropdownOpen} onOpenChange={onPriorityDropdownOpenChange}>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm">
            {t('backlogPage.filters.priority')} {priorityFilter.length > 0 && `(${priorityFilter.length})`}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          {priorityOptions.map((priority) => (
            <DropdownMenuItem
              key={priority.value}
              onSelect={(e) => {
                e.preventDefault();
                if (priorityFilter.includes(priority.value)) {
                  onPriorityFilterChange(priorityFilter.filter(p => p !== priority.value));
                } else {
                  onPriorityFilterChange([...priorityFilter, priority.value]);
                }
              }}
            >
              <Checkbox
                checked={priorityFilter.includes(priority.value)}
                className="mr-2"
              />
              {t(priority.labelKey)}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>

      {/* Dependency Filter */}
      <DropdownMenu open={dependencyDropdownOpen} onOpenChange={onDependencyDropdownOpenChange}>
        <DropdownMenuTrigger asChild>
          <Button variant="outline" size="sm">
            {t('backlogPage.filters.dependencies')} {dependencyFilter !== 'all' && `(${dependencyFilter})`}
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-48">
          <DropdownMenuItem onSelect={() => onDependencyFilterChange('all')}>
            <Checkbox checked={dependencyFilter === 'all'} className="mr-2" />
            {t('backlogPage.filters.allTasks')}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => onDependencyFilterChange('blocked')}>
            <Checkbox checked={dependencyFilter === 'blocked'} className="mr-2" />
            {t('backlogPage.filters.blockedTasks')}
          </DropdownMenuItem>
          <DropdownMenuItem onSelect={() => onDependencyFilterChange('blocking')}>
            <Checkbox checked={dependencyFilter === 'blocking'} className="mr-2" />
            {t('backlogPage.filters.blockingTasks')}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>

      {(statusFilter.length > 0 || priorityFilter.length > 0 || assigneeFilter.length > 0 || dependencyFilter !== 'all') && (
        <Button variant="ghost" size="sm" onClick={onClearFilters}>
          {t('backlogPage.filters.clearFilters')}
        </Button>
      )}
    </div>
  );
}
