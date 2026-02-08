import { useState, useMemo } from 'react';
import { ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Button } from './ui/button';
import { Input } from './ui/input';

export interface TableColumn {
  key: string;
  label: string;
  sortable?: boolean;
  filterable?: boolean;
  render?: (value: any, row: any) => React.ReactNode;
}

export interface TableWidgetProps {
  data: any[];
  columns?: TableColumn[];
  title?: string;
  loading?: boolean;
  pageSize?: number;
  searchable?: boolean;
}

type SortDirection = 'asc' | 'desc' | null;

export default function TableWidget({
  data = [],
  columns = [],
  title = 'Data Table',
  loading = false,
  pageSize = 10,
  searchable = true,
}: TableWidgetProps) {
  const [currentPage, setCurrentPage] = useState(1);
  const [sortKey, setSortKey] = useState<string | null>(null);
  const [sortDirection, setSortDirection] = useState<SortDirection>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Auto-generate columns if not provided
  const effectiveColumns: TableColumn[] = useMemo(() => {
    if (columns.length > 0) return columns;
    
    if (data.length === 0) return [];
    
    const firstRow = data[0];
    return Object.keys(firstRow).map(key => ({
      key,
      label: key.charAt(0).toUpperCase() + key.slice(1).replace(/([A-Z])/g, ' $1'),
      sortable: true,
      filterable: true,
    }));
  }, [columns, data]);

  // Filter data based on search term
  const filteredData = useMemo(() => {
    if (!searchTerm) return data;
    
    return data.filter(row => {
      return effectiveColumns.some(col => {
        const value = row[col.key];
        if (value === null || value === undefined) return false;
        
        // Handle different types for search
        if (Array.isArray(value)) {
          // Search in array items
          return value.some(item => 
            String(item).toLowerCase().includes(searchTerm.toLowerCase())
          );
        } else if (typeof value === 'object') {
          // Search in object properties
          return Object.values(value).some(v => 
            String(v).toLowerCase().includes(searchTerm.toLowerCase())
          );
        }
        
        return String(value).toLowerCase().includes(searchTerm.toLowerCase());
      });
    });
  }, [data, searchTerm, effectiveColumns]);

  // Sort data
  const sortedData = useMemo(() => {
    if (!sortKey || !sortDirection) return filteredData;
    
    return [...filteredData].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      
      if (aVal === bVal) return 0;
      if (aVal === null || aVal === undefined) return 1;
      if (bVal === null || bVal === undefined) return -1;
      
      const comparison = aVal < bVal ? -1 : 1;
      return sortDirection === 'asc' ? comparison : -comparison;
    });
  }, [filteredData, sortKey, sortDirection]);

  // Paginate data
  const paginatedData = useMemo(() => {
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    return sortedData.slice(start, end);
  }, [sortedData, currentPage, pageSize]);

  const totalPages = Math.ceil(sortedData.length / pageSize);

  const handleSort = (key: string) => {
    if (sortKey === key) {
      // Cycle through: asc -> desc -> null
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
  };

  const getSortIcon = (key: string) => {
    if (sortKey !== key) return <ArrowUpDown className="h-3 w-3 ml-1" />;
    if (sortDirection === 'asc') return <ArrowUp className="h-3 w-3 ml-1" />;
    return <ArrowDown className="h-3 w-3 ml-1" />;
  };

  // Helper function to render cell values safely
  const renderCellValue = (value: any): React.ReactNode => {
    if (value === null || value === undefined) {
      return '-';
    }
    
    // Handle arrays
    if (Array.isArray(value)) {
      if (value.length === 0) return '-';
      // If array contains objects, show count
      if (typeof value[0] === 'object') {
        return `${value.length} items`;
      }
      // For primitive arrays, join them
      return value.join(', ');
    }
    
    // Handle objects
    if (typeof value === 'object') {
      // Check if it's a Date object
      if (value instanceof Date) {
        return value.toLocaleDateString();
      }
      // For other objects, try to find a meaningful field to display
      // Common patterns: name, title, id, or show as JSON string
      if ('name' in value) return value.name;
      if ('title' in value) return value.title;
      if ('id' in value) return `ID: ${value.id}`;
      // Otherwise, show count of properties
      return `{${Object.keys(value).length} fields}`;
    }
    
    // Handle booleans
    if (typeof value === 'boolean') {
      return value ? 'Yes' : 'No';
    }
    
    // Handle numbers and strings
    return String(value);
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
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setCurrentPage(1);
            }}
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
                  key={idx}
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
