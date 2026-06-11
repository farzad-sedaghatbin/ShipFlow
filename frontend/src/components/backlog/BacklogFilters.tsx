import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Search } from 'lucide-react';
import { Button } from '../ui/button';
import { Checkbox } from '../ui/checkbox';
import { Input } from '../ui/input';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/dropdown-menu';
import { TabsList, TabsTrigger } from '../ui/tabs';
import { TaskStatus, TaskPriority, Person } from '../../types';
import { STATUS_OPTIONS, PRIORITY_OPTIONS } from '../../constants/backlogConstants';

export interface BacklogFiltersProps {
  onTabChange: (tab: 'all' | 'my') => void;
  statusFilter: TaskStatus[];
  onStatusFilterChange: (status: TaskStatus) => void;
  priorityFilter: TaskPriority[];
  onPriorityFilterChange: (priority: TaskPriority) => void;
  dependencyFilter: 'all' | 'blocked' | 'blocking';
  onDependencyFilterChange: (filter: 'all' | 'blocked' | 'blocking') => void;
  searchQuery: string;
  onSearchQueryChange: (query: string) => void;
  persons: Person[];
  assigneeFilter: number[];
  onAssigneeFilterChange: (personId: number) => void;
  hasActiveFilters: boolean;
  onClearFilters: () => void;
}

export function BacklogFilters({
  onTabChange,
  statusFilter,
  onStatusFilterChange,
  priorityFilter,
  onPriorityFilterChange,
  dependencyFilter,
  onDependencyFilterChange,
  searchQuery,
  onSearchQueryChange,
  persons,
  assigneeFilter,
  onAssigneeFilterChange,
  hasActiveFilters,
  onClearFilters,
}: BacklogFiltersProps) {
  const { t } = useTranslation();
  const [statusDropdownOpen, setStatusDropdownOpen] = useState(false);
  const [priorityDropdownOpen, setPriorityDropdownOpen] = useState(false);
  const [dependencyDropdownOpen, setDependencyDropdownOpen] = useState(false);
  const [assigneeDropdownOpen, setAssigneeDropdownOpen] = useState(false);

  return (
    <div className="flex flex-col gap-3 mb-4">
      <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <TabsList>
          <TabsTrigger value="all" onClick={() => onTabChange('all')}>
            {t('backlogPage.allTasks')}
          </TabsTrigger>
          <TabsTrigger value="my" onClick={() => onTabChange('my')}>
            {t('backlogPage.myTasks')}
          </TabsTrigger>
        </TabsList>

        {/* Filters */}
        <div className="flex items-center gap-2 flex-wrap">
        {/* Status Filter */}
        <DropdownMenu open={statusDropdownOpen} onOpenChange={setStatusDropdownOpen}>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              {t('backlogPage.filters.status')} {statusFilter.length > 0 && `(${statusFilter.length})`}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            {STATUS_OPTIONS.map((status) => (
              <DropdownMenuItem
                key={status.value}
                onSelect={(e) => {
                  e.preventDefault();
                  onStatusFilterChange(status.value);
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
        <DropdownMenu open={priorityDropdownOpen} onOpenChange={setPriorityDropdownOpen}>
          <DropdownMenuTrigger asChild>
            <Button variant="outline" size="sm">
              {t('backlogPage.filters.priority')} {priorityFilter.length > 0 && `(${priorityFilter.length})`}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            {PRIORITY_OPTIONS.map((priority) => (
              <DropdownMenuItem
                key={priority.value}
                onSelect={(e) => {
                  e.preventDefault();
                  onPriorityFilterChange(priority.value);
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
        <DropdownMenu open={dependencyDropdownOpen} onOpenChange={setDependencyDropdownOpen}>
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

        {/* Assignee Filter */}
        {persons.length > 0 && (
          <DropdownMenu open={assigneeDropdownOpen} onOpenChange={setAssigneeDropdownOpen}>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm">
                {t('backlogPage.filters.assignee')} {assigneeFilter.length > 0 && `(${assigneeFilter.length})`}
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48 max-h-60 overflow-y-auto">
              {persons.filter(p => p.isActive).map((person) => (
                <DropdownMenuItem
                  key={person.id}
                  onSelect={(e) => {
                    e.preventDefault();
                    onAssigneeFilterChange(person.id);
                  }}
                >
                  <Checkbox
                    checked={assigneeFilter.includes(person.id)}
                    className="mr-2"
                  />
                  {person.name}
                </DropdownMenuItem>
              ))}
            </DropdownMenuContent>
          </DropdownMenu>
        )}

        {hasActiveFilters && (
          <Button variant="ghost" size="sm" onClick={onClearFilters}>
            {t('backlogPage.filters.clearFilters')}
          </Button>
        )}
      </div>
    </div>

    {/* Search */}
    <div className="relative max-w-sm">
      <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground pointer-events-none" />
      <Input
        placeholder={t('backlogPage.filters.searchPlaceholder')}
        value={searchQuery}
        onChange={(e) => onSearchQueryChange(e.target.value)}
        className="pl-8"
      />
    </div>
  </div>
  );
}
