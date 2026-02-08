import { useState, useMemo, useEffect, useCallback } from 'react';
import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';

// ==================== TYPES ====================

/**
 * Generic row type - must have a unique identifier
 */
export interface TableRow {
  id: string | number;
  [key: string]: unknown;
}

/**
 * Column configuration with proper generics
 */
export interface TableColumn<T extends TableRow = TableRow> {
  key: keyof T & string;
  label: string;
  sortable?: boolean;
  filterable?: boolean;
  render?: (value: T[keyof T], row: T) => React.ReactNode;
}

/**
 * Props with proper generics - removes `any` types
 */
export interface TableWidgetProps<T extends TableRow = TableRow> {
  data: T[];
  columns?: TableColumn<T>[];
  title?: string;
  loading?: boolean;
  pageSize?: number;
  searchable?: boolean;
  /** Optional key extractor for row identity. Defaults to row.id */
  getRowKey?: (row: T, index: number) => string | number;
}

type SortDirection = 'asc' | 'desc' | null;

// ==================== HELPER FUNCTIONS ====================

/**
 * Generic search filter for table data
 */
function filterBySearchTerm<T extends TableRow>(
  data: T[],
  searchTerm: string,
  columns: TableColumn<T>[]
): T[] {
  if (!searchTerm.trim()) return data;
  
  const term = searchTerm.toLowerCase();
  
  return data.filter(row => {
    return columns.some(col => {
      if (col.filterable === false) return false;
      
      const value = row[col.key];
      return matchesSearch(value, term);
    });
  });
}

/**
 * Recursively check if a value matches search term
 */
function matchesSearch(value: unknown, term: string): boolean {
  if (value === null || value === undefined) return false;
  
  if (Array.isArray(value)) {
    return value.some(item => matchesSearch(item, term));
  }
  
  if (typeof value === 'object' && value !== null) {
    if (value instanceof Date) {
      return value.toLocaleDateString().toLowerCase().includes(term);
    }
    return Object.values(value).some(v => matchesSearch(v, term));
  }
  
  return String(value).toLowerCase().includes(term);
}

/**
 * Generic sort function for table data
 */
function sortData<T extends TableRow>(
  data: T[],
  sortKey: string | null,
  sortDirection: SortDirection
): T[] {
  if (!sortKey || !sortDirection) return data;
  
  return [...data].sort((a, b) => {
    const aVal = a[sortKey as keyof T];
    const bVal = b[sortKey as keyof T];
    
    if (aVal === bVal) return 0;
    if (aVal === null || aVal === undefined) return 1;
    if (bVal === null || bVal === undefined) return -1;
    
    const comparison = aVal < bVal ? -1 : 1;
    return sortDirection === 'asc' ? comparison : -comparison;
  });
}

/**
 * Paginate data with bounds checking
 */
function paginateData<T>(
  data: T[],
  page: number,
  pageSize: number
): T[] {
  const start = (page - 1) * pageSize;
  const end = start + pageSize;
  return data.slice(start, end);
}

/**
 * Clamp page number to valid range
 */
function clampPage(page: number, totalPages: number): number {
  if (totalPages <= 0) return 1;
  return Math.max(1, Math.min(page, totalPages));
}

/**
 * Render cell value safely with proper type handling
 */
function renderCellValue(value: unknown): React.ReactNode {
  if (value === null || value === undefined) {
    return '-';
  }
  
  if (Array.isArray(value)) {
    if (value.length === 0) return '-';
    if (typeof value[0] === 'object') {
      return `${value.length} items`;
    }
    return value.join(', ');
  }
  
  if (typeof value === 'object' && value !== null) {
    if (value instanceof Date) {
      return value.toLocaleDateString();
    }
    const obj = value as Record<string, unknown>;
    if ('name' in obj && typeof obj.name === 'string') return obj.name;
    if ('title' in obj && typeof obj.title === 'string') return obj.title;
    if ('id' in obj) return `ID: ${obj.id}`;
    return `{${Object.keys(obj).length} fields}`;
  }
  
  if (typeof value === 'boolean') {
    return value ? 'Yes' : 'No';
  }
  
  return String(value);
}

/**
 * Auto-generate columns from data when not provided
 */
function generateColumns<T extends TableRow>(data: T[]): TableColumn<T>[] {
  if (data.length === 0) return [];
  
  const firstRow = data[0];
  return Object.keys(firstRow).map(key => ({
    key: key as keyof T & string,
    label: key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1'),
    sortable: true,
    filterable: true,
  }));
}

// ==================== COMPONENT ====================

export default function TableWidget<T extends TableRow = TableRow>({
  data = [] as T[],
  columns = [],
  title = 'Data Table',
  loading = false,
  pageSize = 10,
  searchable = true,
  getRowKey,
}: TableWidgetProps<T>) {
  const [currentPage, setCurrentPage] = useState(1);
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Auto-generate columns if not provided
  const effectiveColumns = useMemo<TableColumn<T>[]>(() => {
    return columns.length > 0 ? columns : generateColumns(data);
  }, [columns, data]);

  // Filter data based on search term
  const filteredData = useMemo(() => {
    return filterBySearchTerm(data, searchTerm, effectiveColumns);
  }, [data, searchTerm, effectiveColumns]);

  // Sort filtered data
  const sortedData = useMemo(() => {
    return sortData(filteredData, sortKey, sortDirection);
  }, [filteredData, sortKey, sortDirection]);

  // Calculate total pages
  const totalPages = useMemo(() => {
    return Math.max(1, Math.ceil(sortedData.length / pageSize));
  }, [sortedData.length, pageSize]);

  // Clamp page when data/filters change
  useEffect(() => {
    const clampedPage = clampPage(currentPage, totalPages);
    if (clampedPage !== currentPage) {
      setCurrentPage(clampedPage);
    }
  }, [totalPages, currentPage]);

  // Paginate data
  const paginatedData = useMemo(() => {
    return paginateData(sortedData, currentPage, pageSize);
  }, [sortedData, currentPage, pageSize]);

  // Handle sort with cycling: asc -> desc -> null
  const handleSort = useCallback((key: string) => {
    if (sortKey === key) {
      if (sortDirection === 'asc') {
        setSortDirection('desc');
      } else if (sortDirection === 'desc') {
        setSortKey(null);
        setSortDirection(null);
      }
    } else {
      setSortKey(key);
      setSortDirection('asc');
    }
    setCurrentPage(1);
  }, [sortKey, sortDirection]);

  // Handle search with page reset
  const handleSearchChange = useCallback((value: string) => {
    setSearchTerm(value);
    setCurrentPage(1);
  }, []);

  // Get row key - use provided function or default to row.id
  const getKey = useCallback((row: T, index: number): string | number => {
    if (getRowKey) return getRowKey(row, index);
    return row.id ?? `row-${index}`;
  }, [getRowKey]);

  const getSortIcon = (key: string) => {
    if (sortKey !== key) return <ArrowUpDown className="h-3 w-3 ml-1" />;
    if (sortDirection === 'asc') return <ArrowUp className="h-3 w-3 ml-1" />;
    return <ArrowDown className="h-3 w-3 ml-1" />;
  };

  if (loading) {
    return (
      <div className="h-full flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary"></div>
      </div>
    );
  }

  return (
    <div className="h-full flex flex-col p-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3">
        <h3 className="font-semibold text-sm">{title}</h3>
        {searchable && (
          <Input
            type="text"
            placeholder="Search..."
            value={searchTerm}
            onChange={(e) => handleSearchChange(e.target.value)}
            className="w-48 h-8 text-sm"
          />
        )}
      </div>

      {/* Table */}
      <div className="flex-1 overflow-auto border rounded-md">
        {effectiveColumns.length === 0 || paginatedData.length === 0 ? (
          <div className="flex items-center justify-center h-32 text-sm text-muted-foreground">
            No data available
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-background sticky top-0 z-10">
              <tr className="border-b bg-muted/50">
                {effectiveColumns.map((col) => (
                  <th
                    key={col.key}
                    className="text-left p-2 font-medium bg-muted/50"
                  >
                    {col.sortable !== false ? (
                      <button
                        onClick={() => handleSort(col.key)}
                        className="flex items-center hover:text-primary transition-colors"
                      >
                        {col.label}
                        {getSortIcon(col.key)}
                      </button>
                    ) : (
                      col.label
                    )}
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {paginatedData.map((row, idx) => (
                <tr
                  key={getKey(row, idx)}
                  className="border-b hover:bg-muted/30 transition-colors"
                >
                  {effectiveColumns.map((col) => (
                    <td key={col.key} className="p-2">
                      {col.render
                        ? col.render(row[col.key], row)
                        : renderCellValue(row[col.key])}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between mt-3 text-xs">
          <div className="text-muted-foreground">
            Showing {(currentPage - 1) * pageSize + 1} to{' '}
            {Math.min(currentPage * pageSize, sortedData.length)} of{' '}
            {sortedData.length} entries
          </div>
          <div className="flex gap-1">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(1)}
              disabled={currentPage === 1}
              className="h-7 px-2"
            >
              First
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(currentPage - 1)}
              disabled={currentPage === 1}
              className="h-7 px-2"
            >
              Previous
            </Button>
            <div className="flex items-center px-2">
              Page {currentPage} of {totalPages}
            </div>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="h-7 px-2"
            >
              Next
            </Button>
            <Button
              variant="outline"
              size="sm"
              onClick={() => setCurrentPage(totalPages)}
              disabled={currentPage === totalPages}
              className="h-7 px-2"
            >
              Last
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
