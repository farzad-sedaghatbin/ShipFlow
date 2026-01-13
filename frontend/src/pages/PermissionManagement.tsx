import { useState, useEffect } from 'react';
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
  Edit,
  Trash2,
  RefreshCw,
} from 'lucide-react';
import { useAuth, useToast } from '../contexts';
import permissionService, { Permission, UserRole, ResourceType, PermissionType } from '../services/permissionService';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
import { Button } from '../components/ui/button';
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
import { PermissionEditDialog } from '../components/PermissionEditDialog';

type ViewMode = 'role-matrix' | 'role-details' | 'my-permissions';

export default function PermissionManagement() {
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
  
  const isAdmin = currentUser?.role === 'ADMIN';
  const roles = permissionService.getUserRoles();
  const resources = permissionService.getResourceTypes();
  const permissionTypes = permissionService.getPermissionTypes();

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    setLoading(true);
    try {
      // Load current user's permissions
      const userPerms = await permissionService.getCurrentUserPermissions();
      setMyPermissions(userPerms.permissions);

      // Load all role permissions for matrix view
      if (isAdmin) {
        const all = await permissionService.getAllPermissions();
        setAllPermissions(all);
        await loadPermissionMatrix();
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

  const handleDeletePermission = async (id: number) => {
    if (!confirm('Are you sure you want to delete this permission?')) return;
    
    try {
      await permissionService.deletePermission(id);
      showToast('Permission deleted successfully', 'success');
      loadData();
    } catch (error) {
      showToast('Failed to delete permission', 'error');
    }
  };

  const handleDialogSave = () => {
    showToast(
      editMode === 'create' ? 'Permission created successfully' : 'Permission updated successfully',
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

  if (!isAdmin && viewMode !== 'my-permissions') {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Alert className="border-amber-500">
          <ShieldCheck className="h-5 w-5 text-amber-500" />
          <AlertDescription>
            You don't have permission to manage role permissions. You can only view your own permissions.
          </AlertDescription>
        </Alert>
        <Card className="mt-6">
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              My Permissions
            </CardTitle>
            <CardDescription>
              Your current role: <Badge variant={permissionService.getRoleBadgeColor(currentUser?.role as UserRole) as any}>{currentUser?.role}</Badge>
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
            Permission Management
          </h1>
          <p className="text-muted-foreground mt-2">
            {isAdmin ? 'Manage role-based permissions across all resources' : 'View your role permissions'}
          </p>
        </div>
        {isAdmin && (
          <div className="flex gap-2">
            <Button variant="outline" onClick={loadData} disabled={loading}>
              <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              Refresh
            </Button>
            <Button onClick={handleCreatePermission}>
              <Plus className="h-4 w-4 mr-2" />
              Add Permission
            </Button>
          </div>
        )}
      </div>

      {/* Info Banner - only show for non-admin users */}
      {!isAdmin && (
        <Alert className="mb-6 border-blue-500">
          <Info className="h-5 w-5 text-blue-500" />
          <AlertDescription>
            You can view your assigned permissions below. Contact your administrator to request additional permissions.
          </AlertDescription>
        </Alert>
      )}

      {/* View Mode Tabs */}
      <Tabs value={viewMode} onValueChange={(v) => setViewMode(v as ViewMode)} className="mb-6">
        <TabsList className="grid w-full max-w-2xl grid-cols-3">
          <TabsTrigger value="role-matrix">
            <Database className="h-4 w-4 mr-2" />
            Permission Matrix
          </TabsTrigger>
          <TabsTrigger value="role-details">
            <Shield className="h-4 w-4 mr-2" />
            All Permissions
          </TabsTrigger>
          <TabsTrigger value="my-permissions">
            <Eye className="h-4 w-4 mr-2" />
            My Permissions
          </TabsTrigger>
        </TabsList>

        {/* Permission Matrix View */}
        <TabsContent value="role-matrix" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Permission Matrix</CardTitle>
              <CardDescription>
                Overview of all role permissions across resources (✓ = granted)
              </CardDescription>
            </CardHeader>
            <CardContent>
              {/* Search and Filter */}
              <div className="flex gap-4 mb-6">
                <div className="flex-1 relative">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    placeholder="Search resources..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="pl-10"
                  />
                </div>
                <Select value={filterResource} onValueChange={(v) => setFilterResource(v as ResourceType | 'ALL')}>
                  <SelectTrigger className="w-48">
                    <SelectValue placeholder="Filter by resource" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="ALL">All Resources</SelectItem>
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
                        Resource
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
                <h4 className="font-semibold mb-3">Permission Types:</h4>
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

        {/* Role Details View - All Permissions List */}
        <TabsContent value="role-details" className="mt-6">
          <Card>
            <CardHeader>
              <div className="flex justify-between items-start">
                <div>
                  <CardTitle>All Permissions</CardTitle>
                  <CardDescription>
                    Complete list of all permissions in the system ({allPermissions.length} total)
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              {loading ? (
                <div className="flex justify-center items-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              ) : (
                <div className="border rounded-lg overflow-auto">
                  <Table>
                    <TableHeader>
                      <TableRow>
                        <TableHead>Role</TableHead>
                        <TableHead>Resource</TableHead>
                        <TableHead>Permission</TableHead>
                        <TableHead>Description</TableHead>
                        {isAdmin && <TableHead className="text-right">Actions</TableHead>}
                      </TableRow>
                    </TableHeader>
                    <TableBody>
                      {allPermissions.map((perm) => (
                        <TableRow key={perm.id}>
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
                            <TableCell className="text-right">
                              <div className="flex justify-end gap-2">
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
                      ))}
                    </TableBody>
                  </Table>
                </div>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* My Permissions View */}
        <TabsContent value="my-permissions" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Eye className="h-5 w-5" />
                My Permissions
              </CardTitle>
              <CardDescription>
                Your current role: <Badge variant={permissionService.getRoleBadgeColor(currentUser?.role as UserRole) as any}>{currentUser?.role}</Badge>
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
  const resources = permissionService.getResourceTypes();
  
  const groupedByResource = resources.map(resource => ({
    resource,
    permissions: permissions.filter(p => p.resourceType === resource)
  })).filter(g => g.permissions.length > 0);

  if (permissions.length === 0) {
    return (
      <Alert>
        <AlertDescription>
          No permissions found. Contact your administrator if you believe this is an error.
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <div className="space-y-4">
      <div className="bg-primary/5 border border-primary/20 rounded-lg p-4 mb-6">
        <div className="flex items-center gap-2 mb-2">
          <ShieldCheck className="h-5 w-5 text-primary" />
          <h3 className="font-semibold">Total Permissions: {permissions.length}</h3>
        </div>
        <p className="text-sm text-muted-foreground">
          These permissions control what actions you can perform across the application.
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
