import { describe, it, expect } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useBacklogFilters } from './useBacklogFilters';

describe('useBacklogFilters', () => {
  describe('Initialization', () => {
    it('should initialize with default values', () => {
      const { result } = renderHook(() => useBacklogFilters());

      expect(result.current.statusFilter).toEqual([]);
      expect(result.current.priorityFilter).toEqual([]);
      expect(result.current.assigneeFilter).toEqual([]);
      expect(result.current.dependencyFilter).toBe('all');
      expect(result.current.excludeMode).toBe(false);
      expect(result.current.activeCategory).toBe('PITCH_SCOPE');
      expect(result.current.tabValue).toBe('all');
      expect(result.current.viewMode).toBe('list');
      expect(result.current.page).toBe(0);
      expect(result.current.rowsPerPage).toBe(25);
      expect(result.current.sortBy).toBe('createdAt');
      expect(result.current.sortOrder).toBe('desc');
      expect(result.current.hasActiveFilters).toBe(false);
    });

    it('should initialize with custom category', () => {
      const { result } = renderHook(() => useBacklogFilters('DEBT_IMPROVEMENT'));

      expect(result.current.activeCategory).toBe('DEBT_IMPROVEMENT');
    });
  });

  describe('Status Filter', () => {
    it('should toggle status filter on', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleStatusFilter('TODO');
      });

      expect(result.current.statusFilter).toEqual(['TODO']);
      expect(result.current.hasActiveFilters).toBe(true);
      expect(result.current.page).toBe(0);
    });

    it('should toggle status filter off', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleStatusFilter('TODO');
      });
      act(() => {
        result.current.toggleStatusFilter('TODO');
      });

      expect(result.current.statusFilter).toEqual([]);
      expect(result.current.hasActiveFilters).toBe(false);
    });

    it('should support multiple status filters', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleStatusFilter('TODO');
      });
      act(() => {
        result.current.toggleStatusFilter('IN_PROGRESS');
      });

      expect(result.current.statusFilter).toEqual(['TODO', 'IN_PROGRESS']);
    });

    it('should set status filter directly', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setStatusFilter(['DONE', 'BLOCKED']);
      });

      expect(result.current.statusFilter).toEqual(['DONE', 'BLOCKED']);
    });
  });

  describe('Priority Filter', () => {
    it('should toggle priority filter', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.togglePriorityFilter('HIGH');
      });

      expect(result.current.priorityFilter).toEqual(['HIGH']);
      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should set priority filter directly', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setPriorityFilter(['LOW', 'MEDIUM']);
      });

      expect(result.current.priorityFilter).toEqual(['LOW', 'MEDIUM']);
    });
  });

  describe('Assignee Filter', () => {
    it('should toggle assignee filter', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleAssigneeFilter(1);
      });

      expect(result.current.assigneeFilter).toEqual([1]);
      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should set assignee filter directly', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setAssigneeFilter([1, 2, 3]);
      });

      expect(result.current.assigneeFilter).toEqual([1, 2, 3]);
    });
  });

  describe('Dependency Filter', () => {
    it('should set dependency filter to blocked', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setDependencyFilter('blocked');
      });

      expect(result.current.dependencyFilter).toBe('blocked');
      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should set dependency filter to blocking', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setDependencyFilter('blocking');
      });

      expect(result.current.dependencyFilter).toBe('blocking');
    });

    it('should set dependency filter back to all', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setDependencyFilter('blocked');
      });
      act(() => {
        result.current.setDependencyFilter('all');
      });

      expect(result.current.dependencyFilter).toBe('all');
      expect(result.current.hasActiveFilters).toBe(false);
    });
  });

  describe('Clear All Filters', () => {
    it('should clear all filters', () => {
      const { result } = renderHook(() => useBacklogFilters());

      // Set various filters
      act(() => {
        result.current.toggleStatusFilter('TODO');
        result.current.togglePriorityFilter('HIGH');
        result.current.toggleAssigneeFilter(1);
        result.current.setDependencyFilter('blocked');
        result.current.setExcludeMode(true);
      });

      expect(result.current.hasActiveFilters).toBe(true);

      // Clear all
      act(() => {
        result.current.clearAllFilters();
      });

      expect(result.current.statusFilter).toEqual([]);
      expect(result.current.priorityFilter).toEqual([]);
      expect(result.current.assigneeFilter).toEqual([]);
      expect(result.current.dependencyFilter).toBe('all');
      expect(result.current.excludeMode).toBe(false);
      expect(result.current.hasActiveFilters).toBe(false);
      expect(result.current.page).toBe(0);
    });
  });

  describe('Sorting', () => {
    it('should change sort field', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.handleSort('title');
      });

      expect(result.current.sortBy).toBe('title');
      expect(result.current.sortOrder).toBe('desc');
      expect(result.current.page).toBe(0);
    });

    it('should toggle sort order when sorting same field', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.handleSort('createdAt');
      });

      // First click on already selected field should toggle to asc
      expect(result.current.sortOrder).toBe('asc');

      act(() => {
        result.current.handleSort('createdAt');
      });

      expect(result.current.sortOrder).toBe('desc');
    });
  });

  describe('Pagination', () => {
    it('should change page', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setPage(5);
      });

      expect(result.current.page).toBe(5);
    });

    it('should change rows per page and reset page', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setPage(3);
      });
      act(() => {
        result.current.setRowsPerPage(50);
      });

      expect(result.current.rowsPerPage).toBe(50);
      expect(result.current.page).toBe(0);
    });
  });

  describe('View and Tab', () => {
    it('should change view mode', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setViewMode('kanban');
      });

      expect(result.current.viewMode).toBe('kanban');
    });

    it('should change tab value', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setTabValue('my');
      });

      expect(result.current.tabValue).toBe('my');
    });
  });

  describe('Category', () => {
    it('should change category and reset page', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setPage(2);
      });
      act(() => {
        result.current.setActiveCategory('DEBT_IMPROVEMENT');
      });

      expect(result.current.activeCategory).toBe('DEBT_IMPROVEMENT');
      expect(result.current.page).toBe(0);
    });
  });

  describe('hasActiveFilters', () => {
    it('should be true when status filter is active', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleStatusFilter('TODO');
      });

      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should be true when priority filter is active', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.togglePriorityFilter('HIGH');
      });

      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should be true when assignee filter is active', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.toggleAssigneeFilter(1);
      });

      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should be true when dependency filter is not all', () => {
      const { result } = renderHook(() => useBacklogFilters());

      act(() => {
        result.current.setDependencyFilter('blocked');
      });

      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should be false when all filters are at default', () => {
      const { result } = renderHook(() => useBacklogFilters());

      expect(result.current.hasActiveFilters).toBe(false);
    });
  });
});
