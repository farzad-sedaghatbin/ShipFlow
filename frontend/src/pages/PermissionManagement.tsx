import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Shield,
  ShieldCheck,
  Lock,
  Search,
  Info,
  Loader2,
  Eye,
  Database,
  Plus,
  RefreshCw,
} from 'lucide-react';
import { useAuth, useToast } from '../contexts';
import permissionService, { Permission, UserRole, ResourceType, PermissionType } from '../services/permissionService';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
import { usePermission } from '../hooks/usePermission';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';
import { ConfirmDialog } from '../components/ui/confirm-dialog';
import { PermissionEditDialog } from '../components/PermissionEditDialog';
import { AllPermissionsView } from '../components/AllPermissionsView';

type ViewMode = 'role-matrix' | 'role-details' | 'my-permissions';

export default function PermissionManagement() {
  const { t } = useTranslation();
  const { user: currentUser } = useAuth();
  const { showToast } = useToast();
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>('role-matrix');
  
  // Role-based permissions data
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  
  // Current user permissions
  const [myPermissions, setMyPermissions] = useState<Permission[]>([]);
  
  // Search and filter
  const [searchTerm, setSearchTerm] = useState('');
  const [filterResource, setFilterResource] = useState<ResourceType | 'ALL'>('ALL');
  
  // Permission matrix data (all roles × all resources)
  const [permissionMatrix, setPermissionMatrix] = useState<Map<string, Set<PermissionType>>>(new Map());
  
  // Edit dialog
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editMode, setEditMode] = useState<'create' | 'edit'>('create');
  const [selectedPermission, setSelectedPermission] = useState<Permission | undefined>();
  
  // Enhanced All Permissions tab filters and pagination
  const [permissionSearch, setPermissionSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | 'ALL'>('ALL');
  const [resourceTypeFilter, setResourceTypeFilter] = useState<ResourceType | 'ALL'>('ALL');
  const [permTypeFilter, setPermTypeFilter] = useState<PermissionType | 'ALL'>('ALL');
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);
  const [selectedPermIds, setSelectedPermIds] = useState<Set<number>>(new Set());
  const [groupBy, setGroupBy] = useState<'none' | 'role' | 'resource'>('role');
  
  // Delete confirmation dialogs
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [permissionToDelete, setPermissionToDelete] = useState<number | null>(null);
  const [bulkDeleteConfirmOpen, setBulkDeleteConfirmOpen] = useState(false);
  
  const { hasPermission } = usePermission();
  const [canManagePermissions, setCanManagePermissions] = useState(false);
  const roles = permissionService.getUserRoles();
  const resources = permissionService.getResourceTypes();
  const permissionTypes = permissionService.getPermissionTypes();

  // Pre-load the SYSTEM:MANAGE permission
  useEffect(() => {
    const checkSystemManagePermission = async () => {
      try {
        const hasSystemManage = await hasPermission('SYSTEM', 'MANAGE');
        setCanManagePermissions(hasSystemManage);
      } catch (error) {
        console.error('Failed to check SYSTEM:MANAGE permission:', error);
        setCanManagePermissions(false);
      }
    };

    if (currentUser) {
      checkSystemManagePermission();
    }
  }, [hasPermission, currentUser]);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      // Load current user's permissions
      const userPerms = await permissionService.getCurrentUserPermissions();
      setMyPermissions(userPerms.permissions);

      if (canManagePermissions) {
        // Admins see all permissions
        const all = await permissionService.getAllPermissions();
        setAllPermissions(all);
        await loadPermissionMatrix();
      } else {
        // Non-admins only see their role's permissions
        const rolePerms = await permissionService.getPermissionsByRole(currentUser!.role as UserRole);
        setAllPermissions(rolePerms);
        // Set filter to current user's role by default
        setRoleFilter(currentUser!.role as UserRole);
      }
    } catch (error) {
      showToast('Failed to load permissions', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleCreatePermission = () => {
    setEditMode('create');
    setSelectedPermission(undefined);
    setEditDialogOpen(true);
  };

  const handleEditPermission = (permission: Permission) => {
    setEditMode('edit');
    setSelectedPermission(permission);
    setEditDialogOpen(true);
  };

  const openDeleteConfirm = (id: number) => {
    setPermissionToDelete(id);
    setDeleteConfirmOpen(true);
  };

  const handleDeletePermission = async () => {
    if (permissionToDelete === null) return;
    
    try {
      await permissionService.deletePermission(permissionToDelete);
      showToast(t('permissions.toast.deleteSuccess'), 'success');
      loadData();
      setDeleteConfirmOpen(false);
      setPermissionToDelete(null);
    } catch (error) {
      showToast(t('permissions.toast.deleteFailed'), 'error');
    }
  };

  const openBulkDeleteConfirm = () => {
    if (selectedPermIds.size === 0) return;
    setBulkDeleteConfirmOpen(true);
  };

  const handleBulkDelete = async () => {
    try {
      await permissionService.deleteBulkPermissions(Array.from(selectedPermIds));
      showToast(t('permissions.toast.bulkDeleteSuccess', { count: selectedPermIds.size }), 'success');
      setSelectedPermIds(new Set());
      loadData();
      setBulkDeleteConfirmOpen(false);
    } catch (error) {
      showToast(t('permissions.toast.bulkDeleteFailed'), 'error');
    }
  };

  const handleDialogSave = () => {
    showToast(
      t(editMode === 'create' ? 'permissions.toast.createSuccess' : 'permissions.toast.updateSuccess'),
      'success'
    );
    loadData();
  };

  const loadPermissionMatrix = async () => {
    const matrix = new Map<string, Set<PermissionType>>();
    
    for (const role of roles) {
      const permissions = await permissionService.getPermissionsByRole(role);
      permissions.forEach((perm) => {
        const key = `${role}:${perm.resourceType}`;
        if (!matrix.has(key)) {
          matrix.set(key, new Set());
        }
        matrix.get(key)!.add(perm.permissionType);
      });
    }
    
    setPermissionMatrix(matrix);
  };

  const filteredResources = resources.filter(resource => {
    if (filterResource !== 'ALL' && resource !== filterResource) return false;
    if (!searchTerm) return true;
    return permissionService.getResourceLabel(resource).toLowerCase().includes(searchTerm.toLowerCase());
  });

  if (!canManagePermissions && viewMode !== 'my-permissions') {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Alert className="border-amber-500">
          <ShieldCheck className="h-5 w-5 text-amber-500" />
          <AlertDescription>
            {t('permissions.noAccess')}
          </AlertDescription>
        </Alert>
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              {t('permissions.myPermissionsTitle')}
            </CardTitle>
            <CardDescription>
              {t('permissions.currentRole')}: <Badge variant={permissionService.getRoleBadgeColor(currentUser?.role as UserRole) as any}>{currentUser?.role}</Badge>
            </CardDescription>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex justify-center items-center py-12">
                <Loader2 className="h-8 w-8 animate-spin text-primary" />
              </div>
            ) : (
              <MyPermissionsView permissions={myPermissions} />
            )}
          </CardContent>
        </Card>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex justify-center items-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Header */}
      <div className="mb-6 flex justify-between items-start">
        <div>
          <h1 className="text-3xl font-bold flex items-center gap-2">
            <Shield className="h-8 w-8" />
            {t('permissions.title')}
          </h1>
          <p className="text-muted-foreground mt-2">
            {canManagePermissions ? t('permissions.subtitle') : t('permissions.viewOnly')}
          </p>
        </div>
        {canManagePermissions && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={loadData} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              {t('permissions.refresh')}
            </Button>
            <Button onClick={handleCreatePermission}>
              <Plus className="h-4 w-4 mr-2" />
              {t('permissions.addPermission')}
            </Button>
          </div>
        )}
      </div>

      {/* Info Banner - only show for non-admin users */}
      {!canManagePermissions && (
        <Alert className="mb-6 border-blue-500">
          <Info className="h-5 w-5 text-blue-500" />
          <AlertDescription>
            {t('permissions.viewInfo')}
          </AlertDescription>
        </Alert>
      )}

      {/* View Mode Tabs */}
      <Tabs value={viewMode} onValueChange={(v) => setViewMode(v as ViewMode)} className="mb-6">
        <TabsList className="grid w-full max-w-2xl grid-cols-3">
          <TabsTrigger value="role-matrix">
            <Database className="h-4 w-4 mr-2" />
            {t('permissions.tabs.matrix')}
          </TabsTrigger>
          <TabsTrigger value="role-details">
            <Shield className="h-4 w-4 mr-2" />
            {t('permissions.tabs.all')}
          </TabsTrigger>
          <TabsTrigger value="my-permissions">
            <Eye className="h-4 w-4 mr-2" />
            {t('permissions.tabs.my')}
          </TabsTrigger>
        </TabsList>

        {/* Permission Matrix View */}
        <TabsContent value="role-matrix" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>{t('permissions.matrix.title')}</CardTitle>
              <CardDescription>
                {t('permissions.matrix.description')}
              </CardDescription>
            </CardHeader>
            <CardContent>
              {/* Search and Filter */}
              <div className="flex gap-4 mb-6">
                <div className="flex-1 relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder={t('permissions.matrix.searchPlaceholder')}
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Select value={filterResource} onValueChange={(v) => setFilterResource(v as ResourceType | 'ALL')}>
                  <SelectTrigger className="w-48">
                    <SelectValue placeholder={t('permissions.matrix.filterPlaceholder')} />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">{t('permissions.matrix.allResources')}</SelectItem>
                    {resources.map(resource => (
                      <SelectItem key={resource} value={resource}>
                        {permissionService.getResourceLabel(resource)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Matrix Table */}
              <div className="border rounded-lg overflow-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="sticky left-0 bg-background z-10 min-w-[200px]">
                        {t('permissions.matrix.table.resource')}
                      </TableHead>
                      {roles.map(role => (
                        <TableHead key={role} className="text-center min-w-[120px]">
                          <Badge variant={permissionService.getRoleBadgeColor(role) as any}>
                            {role}
                          </Badge>
                        </TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredResources.map(resource => (
                      <TableRow key={resource}>
                        <TableCell className="sticky left-0 bg-background z-10 font-medium">
                          {permissionService.getResourceLabel(resource)}
                        </TableCell>
                        {roles.map(role => (
                          <TableCell key={`${role}-${resource}`} className="text-center">
                            <PermissionCell 
                              permissions={Array.from(permissionMatrix.get(`${role}:${resource}`) || [])}
                            />
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {/* Legend */}
              <div className="mt-6 p-4 bg-muted/50 rounded-lg">
                <h4 className="font-semibold mb-3">{t('permissions.matrix.legend')}:</h4>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                  {permissionTypes.map(type => (
                    <div key={type} className="flex items-center gap-2">
                      <Badge variant="outline" className="text-xs">
                        {type.charAt(0)}
                      </Badge>
                      <span className="text-sm">{permissionService.getPermissionLabel(type)}</span>
                    </div>
                  ))}
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        {/* All Permissions View - Enhanced with Filters, Pagination, Grouping */}
        <TabsContent value="role-details" className="mt-6">
          <AllPermissionsView
            allPermissions={allPermissions}
            loading={loading}
            isAdmin={canManagePermissions}
            currentUserRole={currentUser?.role as UserRole}
            permissionSearch={permissionSearch}
            setPermissionSearch={setPermissionSearch}
            roleFilter={roleFilter}
            setRoleFilter={setRoleFilter}
            resourceTypeFilter={resourceTypeFilter}
            setResourceTypeFilter={setResourceTypeFilter}
            permTypeFilter={permTypeFilter}
            setPermTypeFilter={setPermTypeFilter}
            currentPage={currentPage}
            setCurrentPage={setCurrentPage}
            pageSize={pageSize}
            setPageSize={setPageSize}
            selectedPermIds={selectedPermIds}
            setSelectedPermIds={setSelectedPermIds}
            groupBy={groupBy}
            setGroupBy={setGroupBy}
            handleEditPermission={handleEditPermission}
            handleDeletePermission={openDeleteConfirm}
            handleBulkDelete={openBulkDeleteConfirm}
          />
        </TabsContent>

        {/* My Permissions View */}
        <TabsContent value="my-permissions" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Eye className="h-5 w-5" />
                {t('permissions.myPermissionsTitle')}
              </CardTitle>
              <CardDescription>
                {t('permissions.currentRole')}: <Badge variant={permissionService.getRoleBadgeColor(currentUser?.role as UserRole) as any}>{currentUser?.role}</Badge>
              </CardDescription>
            </CardHeader>
            <CardContent>
              <MyPermissionsView permissions={myPermissions} />
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Edit Dialog */}
      <PermissionEditDialog
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
        onSave={handleDialogSave}
        mode={editMode}
        permission={selectedPermission}
      />

      {/* Delete Permission Confirmation Dialog */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title={t('permissions.deleteTitle')}
        description={t('permissions.confirmDelete')}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleDeletePermission}
        variant="destructive"
      />

      {/* Bulk Delete Confirmation Dialog */}
      <ConfirmDialog
        open={bulkDeleteConfirmOpen}
        onOpenChange={setBulkDeleteConfirmOpen}
        title={t('permissions.bulkDeleteTitle')}
        description={t('permissions.confirmBulkDelete', { count: selectedPermIds.size })}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleBulkDelete}
        variant="destructive"
      />
    </div>
  );
}

// Permission Cell Component (shows abbreviated permission types)
function PermissionCell({ permissions }: { permissions: PermissionType[] }) {
  if (permissions.length === 0) {
    return <span className="text-muted-foreground">-</span>;
  }

  const abbreviations = permissions.map(p => p.charAt(0)).join('');
  
  return (
    <TooltipProvider>
      <Tooltip>
        <TooltipTrigger asChild>
          <Badge variant="outline" className="cursor-help font-mono text-xs">
            {abbreviations}
          </Badge>
        </TooltipTrigger>
        <TooltipContent>
          <div className="text-sm">
            {permissions.map(p => permissionService.getPermissionLabel(p)).join(', ')}
          </div>
        </TooltipContent>
      </Tooltip>
    </TooltipProvider>
  );
}

// Role Permissions Detail Component
// My Permissions View Component
function MyPermissionsView({ permissions }: { permissions: Permission[] }) {
  const { t } = useTranslation();
  const resources = permissionService.getResourceTypes();
  
  const groupedByResource = resources.map(resource => ({
    resource,
    permissions: permissions.filter(p => p.resourceType === resource)
  })).filter(g => g.permissions.length > 0);

  if (permissions.length === 0) {
    return (
      <Alert>
        <AlertDescription>
          {t('permissions.myPermissions.empty')}
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-4">
      <div className="bg-primary/5 border border-primary/20 rounded-lg p-4 mb-6">
        <div className="flex items-center gap-2 mb-2">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <h3 className="font-semibold">{t('permissions.myPermissions.totalCount', { count: permissions.length })}</h3>
        </div>
        <p className="text-sm text-muted-foreground">
          {t('permissions.myPermissions.description')}
        </p>
      </div>

      <div className="grid gap-4 md:grid-cols-2">
        {groupedByResource.map(({ resource, permissions: resourcePerms }) => (
          <Card key={resource}>
            <CardHeader className="pb-3">
              <CardTitle className="text-base flex items-center gap-2">
                <Lock className="h-4 w-4" />
                {permissionService.getResourceLabel(resource)}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2">
                {resourcePerms.map(perm => (
                  <Badge key={perm.id} variant="default">
                    {permissionService.getPermissionLabel(perm.permissionType)}
                  </Badge>
                ))}
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
