import { describe, it, expect, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { useFilterState, createFilterConfig } from './useFilterState';
import React from 'react';

// Wrapper component for providing router context
const createWrapper = (initialEntries: string[] = ['/']) => {
  return function Wrapper({ children }: { children: React.ReactNode }) {
    return (
      <MemoryRouter initialEntries={initialEntries}>
        {children}
      </MemoryRouter>
    );
  };
};

// Type helper for filter configs to avoid literal type inference
type TestFilters = {
  search: string;
  page: number;
  status: string[];
  sortBy: string;
  showCompleted: boolean;
  assigneeIds: number[];
  tags: string[];
};

describe('useFilterState', () => {
  describe('Basic Functionality', () => {
    it('should initialize with default values when no URL params', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page' | 'status'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/']) }
      );

      expect(result.current.filters.search).toBe('');
      expect(result.current.filters.page).toBe(0);
      expect(result.current.filters.status).toEqual([]);
    });

    it('should initialize from URL search params', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/?search=test&page=2']) }
      );

      expect(result.current.filters.search).toBe('test');
      expect(result.current.filters.page).toBe(2);
    });

    it('should handle array values from URL', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'status'>>({
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/?status=ACTIVE,PENDING,DONE']) }
      );

      expect(result.current.filters.status).toEqual(['ACTIVE', 'PENDING', 'DONE']);
    });
  });

  describe('setFilter', () => {
    it('should update a single filter value', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilter('search', 'hello');
      });

      expect(result.current.filters.search).toBe('hello');
    });

    it('should not affect other filter values', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/?page=5']) }
      );

      act(() => {
        result.current.setFilter('search', 'test');
      });

      expect(result.current.filters.search).toBe('test');
      expect(result.current.filters.page).toBe(5);
    });
  });

  describe('setFilters', () => {
    it('should update multiple filter values at once', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page' | 'sortBy'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
          sortBy: { defaultValue: 'createdAt' },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilters({
          search: 'query',
          page: 2,
          sortBy: 'name',
        });
      });

      expect(result.current.filters.search).toBe('query');
      expect(result.current.filters.page).toBe(2);
      expect(result.current.filters.sortBy).toBe('name');
    });

    it('should allow partial updates', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: 'initial' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilters({ page: 3 });
      });

      expect(result.current.filters.search).toBe('initial');
      expect(result.current.filters.page).toBe(3);
    });
  });

  describe('resetFilters', () => {
    it('should reset all filters to default values', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page' | 'status'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/?search=test&page=5&status=ACTIVE']) }
      );

      // Verify initial state from URL
      expect(result.current.filters.search).toBe('test');
      expect(result.current.filters.page).toBe(5);

      act(() => {
        result.current.resetFilters();
      });

      expect(result.current.filters.search).toBe('');
      expect(result.current.filters.page).toBe(0);
      expect(result.current.filters.status).toEqual([]);
    });
  });

  describe('resetFilter', () => {
    it('should reset a single filter to its default value', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/?search=test&page=5']) }
      );

      act(() => {
        result.current.resetFilter('search');
      });

      expect(result.current.filters.search).toBe('');
      expect(result.current.filters.page).toBe(5); // Should remain unchanged
    });
  });

  describe('hasActiveFilters', () => {
    it('should return false when all filters are at default values', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page' | 'status'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/']) }
      );

      expect(result.current.hasActiveFilters).toBe(false);
    });

    it('should return true when any filter differs from default', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilter('search', 'query');
      });

      expect(result.current.hasActiveFilters).toBe(true);
    });

    it('should return true when array filter has values', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'status'>>({
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/?status=ACTIVE']) }
      );

      expect(result.current.hasActiveFilters).toBe(true);
    });
  });

  describe('activeFilterCount', () => {
    it('should return 0 when no filters are active', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
        }),
        { wrapper: createWrapper(['/']) }
      );

      expect(result.current.activeFilterCount).toBe(0);
    });

    it('should count each non-default filter', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'search' | 'page' | 'status'>>({
          search: { defaultValue: '' },
          page: { defaultValue: 0 },
          status: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilters({
          search: 'test',
          status: ['ACTIVE', 'DONE'],
        });
      });

      expect(result.current.activeFilterCount).toBe(2);
    });
  });

  describe('Custom Parsers and Serializers', () => {
    it('should use custom parser', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'showCompleted'>>({
          showCompleted: {
            defaultValue: false,
            parse: (val) => val === 'yes',
            serialize: (val) => val ? 'yes' : null,
          },
        }),
        { wrapper: createWrapper(['/?showCompleted=yes']) }
      );

      expect(result.current.filters.showCompleted).toBe(true);
    });

    it('should use custom serializer when setting values', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'showCompleted'>>({
          showCompleted: {
            defaultValue: false,
            parse: (val) => val === 'yes',
            serialize: (val) => val ? 'yes' : null,
          },
        }),
        { wrapper: createWrapper(['/']) }
      );

      act(() => {
        result.current.setFilter('showCompleted', true);
      });

      expect(result.current.filters.showCompleted).toBe(true);
    });
  });

  describe('Type Support', () => {
    it('should handle string filters', () => {
      const { result } = renderHook(
        () => useFilterState<{ name: string }>({
          name: { defaultValue: '' },
        }),
        { wrapper: createWrapper(['/?name=John']) }
      );

      expect(result.current.filters.name).toBe('John');
    });

    it('should handle number filters', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'page'> & { rowsPerPage: number }>({
          page: { defaultValue: 0 },
          rowsPerPage: { defaultValue: 10 },
        }),
        { wrapper: createWrapper(['/?page=5&rowsPerPage=25']) }
      );

      expect(result.current.filters.page).toBe(5);
      expect(result.current.filters.rowsPerPage).toBe(25);
    });

    it('should handle boolean filters', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'showCompleted'>>({
          showCompleted: { defaultValue: false },
        }),
        { wrapper: createWrapper(['/?showCompleted=true']) }
      );

      expect(result.current.filters.showCompleted).toBe(true);
    });

    it('should handle string array filters', () => {
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'tags'>>({
          tags: { defaultValue: [] },
        }),
        { wrapper: createWrapper(['/?tags=react,typescript,jest']) }
      );

      expect(result.current.filters.tags).toEqual(['react', 'typescript', 'jest']);
    });

    it('should handle number array filters', () => {
      // When the default value is an empty array the hook cannot auto-detect the
      // element type; provide an explicit parser to get number parsing.
      const { result } = renderHook(
        () => useFilterState<Pick<TestFilters, 'assigneeIds'>>({
          assigneeIds: {
            defaultValue: [] as number[],
            parse: (val) => (val ? val.split(',').filter(Boolean).map(Number) : []),
          },
        }),
        { wrapper: createWrapper(['/?assigneeIds=1,2,3']) }
      );

      expect(result.current.filters.assigneeIds).toEqual([1, 2, 3]);
    });
  });
});

describe('createFilterConfig', () => {
  it('should create config with default value', () => {
    const config = createFilterConfig('');
    expect(config.defaultValue).toBe('');
  });

  it('should allow custom options', () => {
    const customParse = vi.fn();
    const config = createFilterConfig('', { parse: customParse });
    
    expect(config.defaultValue).toBe('');
    expect(config.parse).toBe(customParse);
  });
});
