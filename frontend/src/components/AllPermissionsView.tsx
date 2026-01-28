import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  Edit,
  Trash2,
  Loader2,
  X,
  Info,
} from 'lucide-react';
import permissionService, { Permission, UserRole, ResourceType, PermissionType } from '../services/permissionService';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from './ui/card';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from './ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Checkbox } from './ui/checkbox';
import { Alert, AlertDescription } from './ui/alert';

interface AllPermissionsViewProps {
  allPermissions: Permission[];
  loading: boolean;
  isAdmin: boolean;
  currentUserRole?: UserRole;
  permissionSearch: string;
  setPermissionSearch: (value: string) => void;
  roleFilter: UserRole | 'ALL';
  setRoleFilter: (value: UserRole | 'ALL') => void;
  resourceTypeFilter: ResourceType | 'ALL';
  setResourceTypeFilter: (value: ResourceType | 'ALL') => void;
  permTypeFilter: PermissionType | 'ALL';
  setPermTypeFilter: (value: PermissionType | 'ALL') => void;
  currentPage: number;
  setCurrentPage: (value: number) => void;
  pageSize: number;
  setPageSize: (value: number) => void;
  selectedPermIds: Set<number>;
  setSelectedPermIds: (value: Set<number>) => void;
  groupBy: 'none' | 'role' | 'resource';
  setGroupBy: (value: 'none' | 'role' | 'resource') => void;
  handleEditPermission: (perm: Permission) => void;
  handleDeletePermission: (id: number) => void;
  handleBulkDelete: () => void;
}

export function AllPermissionsView({
  allPermissions,
  loading,
  isAdmin,
  currentUserRole,
  permissionSearch,
  setPermissionSearch,
  roleFilter,
  setRoleFilter,
  resourceTypeFilter,
  setResourceTypeFilter,
  permTypeFilter,
  setPermTypeFilter,
  currentPage,
  setCurrentPage,
  pageSize,
  setPageSize,
  selectedPermIds,
  setSelectedPermIds,
  groupBy,
  setGroupBy,
  handleEditPermission,
  handleDeletePermission,
  handleBulkDelete,
}: AllPermissionsViewProps) {
  const { t } = useTranslation();
  
  const roles = permissionService.getUserRoles();
  const resources = permissionService.getResourceTypes();
  const permTypes = permissionService.getPermissionTypes();

  // Apply filters
  const filteredPermissions = useMemo(() => {
    return allPermissions.filter(perm => {
      const matchesSearch = permissionSearch === '' || 
        perm.role.toLowerCase().includes(permissionSearch.toLowerCase()) ||
        perm.resourceType.toLowerCase().includes(permissionSearch.toLowerCase()) ||
        perm.permissionType.toLowerCase().includes(permissionSearch.toLowerCase()) ||
        ((perm as any).description || '').toLowerCase().includes(permissionSearch.toLowerCase());
      
      const matchesRole = roleFilter === 'ALL' || perm.role === roleFilter;
      const matchesResource = resourceTypeFilter === 'ALL' || perm.resourceType === resourceTypeFilter;
      const matchesPermType = permTypeFilter === 'ALL' || perm.permissionType === permTypeFilter;
      
      return matchesSearch && matchesRole && matchesResource && matchesPermType;
    });
  }, [allPermissions, permissionSearch, roleFilter, resourceTypeFilter, permTypeFilter]);

  // Group permissions if needed
  const groupedPermissions = useMemo(() => {
    if (groupBy === 'none') {
      return { 'All': filteredPermissions };
    }
    
    const groups: Record<string, Permission[]> = {};
    filteredPermissions.forEach(perm => {
      const key = groupBy === 'role' ? perm.role : permissionService.getResourceLabel(perm.resourceType);
      if (!groups[key]) groups[key] = [];
      groups[key].push(perm);
    });
    
    return groups;
  }, [filteredPermissions, groupBy]);

  // Pagination
  const totalPages = Math.ceil(filteredPermissions.length / pageSize);
  const paginatedPermissions = useMemo(() => {
    if (groupBy !== 'none') return groupedPermissions; // Don't paginate grouped view
    
    const start = (currentPage - 1) * pageSize;
    const end = start + pageSize;
    return { 'All': filteredPermissions.slice(start, end) };
  }, [filteredPermissions, currentPage, pageSize, groupBy, groupedPermissions]);

  const hasActiveFilters = permissionSearch || roleFilter !== 'ALL' || resourceTypeFilter !== 'ALL' || permTypeFilter !== 'ALL';

  const clearFilters = () => {
    setPermissionSearch('');
    setRoleFilter('ALL');
    setResourceTypeFilter('ALL');
    setPermTypeFilter('ALL');
  };

  const toggleSelectAll = () => {
    if (selectedPermIds.size === filteredPermissions.length) {
      setSelectedPermIds(new Set());
    } else {
      setSelectedPermIds(new Set(filteredPermissions.map(p => p.id)));
    }
  };

  const toggleSelect = (id: number) => {
    const newSet = new Set(selectedPermIds);
    if (newSet.has(id)) {
      newSet.delete(id);
    } else {
      newSet.add(id);
    }
    setSelectedPermIds(newSet);
  };

  return (
    <Card>
      <CardHeader>
        <div className="flex justify-between items-start">
          <div>
            <CardTitle>{isAdmin ? t('permissions.allPermissions.title') : t('permissions.allPermissions.roleTitle', { role: currentUserRole })}</CardTitle>
            <CardDescription>
              {t('permissions.allPermissions.permissionCount', { filtered: filteredPermissions.length, total: allPermissions.length })}
              {selectedPermIds.size > 0 && t('permissions.allPermissions.selected', { count: selectedPermIds.size })}
            </CardDescription>
          </div>
          {isAdmin && selectedPermIds.size > 0 && (
            <Button
              variant="destructive"
              size="sm"
              onClick={handleBulkDelete}
            >
              <Trash2 className="h-4 w-4 mr-2" />
              {t('permissions.allPermissions.deleteCount', { count: selectedPermIds.size })}
            </Button>
          )}
        </div>
      </CardHeader>
      <CardContent>
        {loading ? (
          <div className="flex justify-center items-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-primary" />
          </div>
        ) : (
          <div className="space-y-4">
            {/* Info banner for non-admin users */}
            {!isAdmin && currentUserRole && (
              <Alert className="border-blue-500">
                <Info className="h-4 w-4 text-blue-500" />
                <AlertDescription>
                  {t('permissions.allPermissions.roleInfo', { role: currentUserRole })}
                </AlertDescription>
              </Alert>
            )}

            {/* Filters and Search */}
            <div className="space-y-3">
              <div className="flex items-center gap-2">
                <div className="flex-1 relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder={t('permissions.allPermissions.searchPlaceholder')}
                    value={permissionSearch}
                    onChange={(e) => setPermissionSearch(e.target.value)}
                    className="pl-10"
                  />
                </div>
                {hasActiveFilters && (
                  <Button variant="ghost" size="sm" onClick={clearFilters}>
                    <X className="h-4 w-4 mr-1" />
                    {t('permissions.allPermissions.clear')}
                  </Button>
                )}
              </div>
              
              <div className="flex flex-wrap gap-2">
                {/* Only show role filter for admins */}
                {isAdmin && (
                  <Select value={roleFilter} onValueChange={(v) => setRoleFilter(v as UserRole | 'ALL')}>
                    <SelectTrigger className="w-40">
                      <Filter className="h-4 w-4 mr-2" />
                      <SelectValue placeholder={t('permissions.allPermissions.role')} />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">{t('permissions.allPermissions.allRoles')}</SelectItem>
                      {roles.map(role => (
                        <SelectItem key={role} value={role}>{role}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                )}

                <Select value={resourceTypeFilter} onValueChange={(v) => setResourceTypeFilter(v as ResourceType | 'ALL')}>
                  <SelectTrigger className="w-48">
                    <Filter className="h-4 w-4 mr-2" />
                    <SelectValue placeholder={t('permissions.allPermissions.resource')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">{t('permissions.allPermissions.allResources')}</SelectItem>
                    {resources.map(res => (
                      <SelectItem key={res} value={res}>
                        {permissionService.getResourceLabel(res)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Select value={permTypeFilter} onValueChange={(v) => setPermTypeFilter(v as PermissionType | 'ALL')}>
                  <SelectTrigger className="w-40">
                    <Filter className="h-4 w-4 mr-2" />
                    <SelectValue placeholder={t('permissions.allPermissions.type')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">{t('permissions.allPermissions.allTypes')}</SelectItem>
                    {permTypes.map(type => (
                      <SelectItem key={type} value={type}>
                        {permissionService.getPermissionLabel(type)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Select value={groupBy} onValueChange={(v) => setGroupBy(v as 'none' | 'role' | 'resource')}>
                  <SelectTrigger className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">{t('permissions.allPermissions.noGrouping')}</SelectItem>
                    <SelectItem value="role">{t('permissions.allPermissions.groupByRole')}</SelectItem>
                    <SelectItem value="resource">{t('permissions.allPermissions.groupByResource')}</SelectItem>
                  </SelectContent>
                </Select>

                <Select value={pageSize.toString()} onValueChange={(v) => {
                  setPageSize(Number(v));
                  setCurrentPage(1);
                }}>
                  <SelectTrigger className="w-32">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="10">{t('permissions.allPermissions.perPage', { count: 10 })}</SelectItem>
                    <SelectItem value="20">{t('permissions.allPermissions.perPage', { count: 20 })}</SelectItem>
                    <SelectItem value="50">{t('permissions.allPermissions.perPage', { count: 50 })}</SelectItem>
                    <SelectItem value="100">{t('permissions.allPermissions.perPage', { count: 100 })}</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Permissions Table */}
            {Object.entries(paginatedPermissions).map(([groupName, perms]) => (
              <div key={groupName} className="space-y-2">
                {groupBy !== 'none' && (
                  <h3 className="font-semibold text-sm px-2 py-1 bg-muted rounded">
                    {groupName} ({perms.length})
                  </h3>
                )}
                <div className="border rounded-lg overflow-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        {isAdmin && (
                          <TableHead className="w-12">
                            {groupBy === 'none' && (
                              <Checkbox
                                checked={selectedPermIds.size === filteredPermissions.length && filteredPermissions.length > 0}
                                onCheckedChange={toggleSelectAll}
                              />
                            )}
                          </TableHead>
                        )}
                        <TableHead>{t('permissions.allPermissions.table.role')}</TableHead>
                        <TableHead>{t('permissions.allPermissions.table.resource')}</TableHead>
                        <TableHead>{t('permissions.allPermissions.table.permission')}</TableHead>
                        <TableHead>{t('permissions.allPermissions.table.description')}</TableHead>
                        {isAdmin && <TableHead className="text-right w-32">{t('permissions.allPermissions.table.actions')}</TableHead>}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {perms.length === 0 ? (
                        <TableRow>
                          <TableCell colSpan={isAdmin ? 6 : 4} className="text-center py-8 text-muted-foreground">
                            {t('permissions.allPermissions.table.empty')}
                          </TableCell>
                        </TableRow>
                      ) : (
                        perms.map((perm) => (
                          <TableRow key={perm.id}>
                            {isAdmin && (
                              <TableCell>
                                <Checkbox
                                  checked={selectedPermIds.has(perm.id)}
                                  onCheckedChange={() => toggleSelect(perm.id)}
                                />
                              </TableCell>
                            )}
                            <TableCell>
                              <Badge variant={permissionService.getRoleBadgeColor(perm.role) as any}>
                                {perm.role}
                              </Badge>
                            </TableCell>
                            <TableCell>
                              {permissionService.getResourceLabel(perm.resourceType)}
                            </TableCell>
                            <TableCell>
                              <Badge variant="outline">
                                {permissionService.getPermissionLabel(perm.permissionType)}
                              </Badge>
                            </TableCell>
                            <TableCell className="text-sm text-muted-foreground">
                              {(perm as any).description || '-'}
                            </TableCell>
                            {isAdmin && (
                              <TableCell>
                                <div className="flex justify-end gap-1">
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleEditPermission(perm)}
                                  >
                                    <Edit className="h-4 w-4" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    onClick={() => handleDeletePermission(perm.id)}
                                    className="text-red-600 hover:text-red-700 hover:bg-red-50"
                                  >
                                    <Trash2 className="h-4 w-4" />
                                  </Button>
                                </div>
                              </TableCell>
                            )}
                          </TableRow>
                        ))
                      )}
                    </TableBody>
                  </Table>
                </div>
              </div>
            ))}

            {/* Pagination */}
            {groupBy === 'none' && totalPages > 1 && (
              <div className="flex items-center justify-between px-2">
                <div className="text-sm text-muted-foreground">
                  {t('permissions.allPermissions.pagination.showing', {
                    from: ((currentPage - 1) * pageSize) + 1,
                    to: Math.min(currentPage * pageSize, filteredPermissions.length),
                    total: filteredPermissions.length
                  })}
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <span className="text-sm">
                    {t('permissions.allPermissions.pagination.page', { current: currentPage, total: totalPages })}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setCurrentPage(currentPage + 1)}
                    disabled={currentPage === totalPages}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
