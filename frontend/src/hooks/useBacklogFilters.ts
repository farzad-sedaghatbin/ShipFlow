import { useState, useCallback } from 'react';
import { TaskStatus, TaskPriority, TaskCategory } from '../types';

export interface BacklogFilterState {
  statusFilter: TaskStatus[];
  priorityFilter: TaskPriority[];
  assigneeFilter: number[];
  dependencyFilter: 'all' | 'blocked' | 'blocking';
  excludeMode: boolean;
  activeCategory: TaskCategory;
  tabValue: 'all' | 'my';
  viewMode: 'list' | 'kanban';
  page: number;
  rowsPerPage: number;
  sortBy: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title';
  sortOrder: 'asc' | 'desc';
}

export interface BacklogFilterActions {
  setStatusFilter: (filter: TaskStatus[]) => void;
  toggleStatusFilter: (status: TaskStatus) => void;
  setPriorityFilter: (filter: TaskPriority[]) => void;
  togglePriorityFilter: (priority: TaskPriority) => void;
  setAssigneeFilter: (filter: number[]) => void;
  toggleAssigneeFilter: (assigneeId: number) => void;
  setDependencyFilter: (filter: 'all' | 'blocked' | 'blocking') => void;
  setExcludeMode: (exclude: boolean) => void;
  setActiveCategory: (category: TaskCategory) => void;
  setTabValue: (tab: 'all' | 'my') => void;
  setViewMode: (mode: 'list' | 'kanban') => void;
  setPage: (page: number) => void;
  setRowsPerPage: (size: number) => void;
  handleSort: (field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => void;
  clearAllFilters: () => void;
  hasActiveFilters: boolean;
}

export interface UseBacklogFiltersReturn extends BacklogFilterState, BacklogFilterActions {}

const DEFAULT_FILTER_STATE: BacklogFilterState = {
  statusFilter: [],
  priorityFilter: [],
  assigneeFilter: [],
  dependencyFilter: 'all',
  excludeMode: false,
  activeCategory: 'PITCH_SCOPE',
  tabValue: 'all',
  viewMode: 'list',
  page: 0,
  rowsPerPage: 25,
  sortBy: 'createdAt',
  sortOrder: 'desc',
};

/**
 * Hook to manage backlog filter state
 */
export function useBacklogFilters(initialCategory?: TaskCategory): UseBacklogFiltersReturn {
  const [statusFilter, setStatusFilter] = useState<TaskStatus[]>(DEFAULT_FILTER_STATE.statusFilter);
  const [priorityFilter, setPriorityFilter] = useState<TaskPriority[]>(DEFAULT_FILTER_STATE.priorityFilter);
  const [assigneeFilter, setAssigneeFilter] = useState<number[]>(DEFAULT_FILTER_STATE.assigneeFilter);
  const [dependencyFilter, setDependencyFilter] = useState<'all' | 'blocked' | 'blocking'>(DEFAULT_FILTER_STATE.dependencyFilter);
  const [excludeMode, setExcludeMode] = useState(DEFAULT_FILTER_STATE.excludeMode);
  const [activeCategory, setActiveCategory] = useState<TaskCategory>(initialCategory || DEFAULT_FILTER_STATE.activeCategory);
  const [tabValue, setTabValue] = useState<'all' | 'my'>(DEFAULT_FILTER_STATE.tabValue);
  const [viewMode, setViewMode] = useState<'list' | 'kanban'>(DEFAULT_FILTER_STATE.viewMode);
  const [page, setPage] = useState(DEFAULT_FILTER_STATE.page);
  const [rowsPerPage, setRowsPerPage] = useState(DEFAULT_FILTER_STATE.rowsPerPage);
  const [sortBy, setSortBy] = useState<'createdAt' | 'priority' | 'status' | 'dueDate' | 'title'>(DEFAULT_FILTER_STATE.sortBy);
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>(DEFAULT_FILTER_STATE.sortOrder);

  const toggleStatusFilter = useCallback((status: TaskStatus) => {
    setStatusFilter((prev) =>
      prev.includes(status) ? prev.filter((s) => s !== status) : [...prev, status]
    );
    setPage(0);
  }, []);

  const togglePriorityFilter = useCallback((priority: TaskPriority) => {
    setPriorityFilter((prev) =>
      prev.includes(priority) ? prev.filter((p) => p !== priority) : [...prev, priority]
    );
    setPage(0);
  }, []);

  const toggleAssigneeFilter = useCallback((assigneeId: number) => {
    setAssigneeFilter((prev) =>
      prev.includes(assigneeId) ? prev.filter((a) => a !== assigneeId) : [...prev, assigneeId]
    );
    setPage(0);
  }, []);

  const handleSort = useCallback((field: 'createdAt' | 'priority' | 'status' | 'dueDate' | 'title') => {
    if (sortBy === field) {
      setSortOrder((prev) => (prev === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(field);
      setSortOrder('desc');
    }
    setPage(0);
  }, [sortBy]);

  const handleCategoryChange = useCallback((category: TaskCategory) => {
    setActiveCategory(category);
    setPage(0);
  }, []);

  const handleRowsPerPageChange = useCallback((size: number) => {
    setRowsPerPage(size);
    setPage(0);
  }, []);

  const clearAllFilters = useCallback(() => {
    setStatusFilter([]);
    setPriorityFilter([]);
    setAssigneeFilter([]);
    setDependencyFilter('all');
    setExcludeMode(false);
    setPage(0);
  }, []);

  const hasActiveFilters = 
    statusFilter.length > 0 ||
    priorityFilter.length > 0 ||
    assigneeFilter.length > 0 ||
    dependencyFilter !== 'all';

  return {
    // State
    statusFilter,
    priorityFilter,
    assigneeFilter,
    dependencyFilter,
    excludeMode,
    activeCategory,
    tabValue,
    viewMode,
    page,
    rowsPerPage,
    sortBy,
    sortOrder,
    // Actions
    setStatusFilter: useCallback((filter: TaskStatus[]) => { setStatusFilter(filter); setPage(0); }, []),
    toggleStatusFilter,
    setPriorityFilter: useCallback((filter: TaskPriority[]) => { setPriorityFilter(filter); setPage(0); }, []),
    togglePriorityFilter,
    setAssigneeFilter: useCallback((filter: number[]) => { setAssigneeFilter(filter); setPage(0); }, []),
    toggleAssigneeFilter,
    setDependencyFilter: useCallback((filter: 'all' | 'blocked' | 'blocking') => { setDependencyFilter(filter); setPage(0); }, []),
    setExcludeMode,
    setActiveCategory: handleCategoryChange,
    setTabValue,
    setViewMode,
    setPage,
    setRowsPerPage: handleRowsPerPageChange,
    handleSort,
    clearAllFilters,
    hasActiveFilters,
  };
}
