import { useState, useEffect } from 'react';
import {
  Shield,
  ShieldCheck,
  Lock,
  Search,
  Info,
  CheckCircle,
  XCircle,
  Loader2,
  Eye,
  Database,
} from 'lucide-react';
import { useAuth, useToast } from '../contexts';
import permissionService, { Permission, UserRole, ResourceType, PermissionType } from '../services/permissionService';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Badge } from '../components/ui/badge';
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

type ViewMode = 'role-matrix' | 'role-details' | 'my-permissions';

export default function PermissionManagement() {
  const { user: currentUser } = useAuth();
  const { showToast } = useToast();
  const [loading, setLoading] = useState(true);
  const [viewMode, setViewMode] = useState<ViewMode>('role-matrix');
  
  // Role-based permissions data
  const [selectedRole, setSelectedRole] = useState<UserRole>('ADMIN');
  const [rolePermissions, setRolePermissions] = useState<Permission[]>([]);
  
  // Current user permissions
  const [myPermissions, setMyPermissions] = useState<Permission[]>([]);
  
  // Search and filter
  const [searchTerm, setSearchTerm] = useState('');
  const [filterResource, setFilterResource] = useState<ResourceType | 'ALL'>('ALL');
  
  // Permission matrix data (all roles × all resources)
  const [permissionMatrix, setPermissionMatrix] = useState<Map<string, Set<PermissionType>>>(new Map());
  
  const isAdmin = currentUser?.role === 'ADMIN';
  const roles = permissionService.getUserRoles();
  const resources = permissionService.getResourceTypes();
  const permissionTypes = permissionService.getPermissionTypes();

  useEffect(() => {
    loadData();
  }, []);

  useEffect(() => {
    if (selectedRole) {
      loadRolePermissions(selectedRole);
    }
  }, [selectedRole]);

  const loadData = async () => {
    setLoading(true);
    try {
      // Load current user's permissions
      const userPerms = await permissionService.getCurrentUserPermissions();
      setMyPermissions(userPerms.permissions);

      // Load all role permissions for matrix view
      if (isAdmin) {
        await loadPermissionMatrix();
      }
    } catch (error) {
      showToast('Failed to load permissions', 'error');
    } finally {
      setLoading(false);
    }
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

  const loadRolePermissions = async (role: UserRole) => {
    try {
      const permissions = await permissionService.getPermissionsByRole(role);
      setRolePermissions(permissions);
    } catch (error) {
      showToast(`Failed to load permissions for ${role}`, 'error');
    }
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
      <div className="mb-6">
        <h1 className="text-3xl font-bold flex items-center gap-2">
          <Shield className="h-8 w-8" />
          Permission Management
        </h1>
        <p className="text-muted-foreground mt-2">
          View and understand role-based permissions across all resources
        </p>
      </div>

      {/* Info Banner */}
      <Alert className="mb-6 border-blue-500">
        <Info className="h-5 w-5 text-blue-500" />
        <AlertDescription>
          <strong>Note:</strong> Permission management is currently read-only. Default permissions are loaded from the database migration. 
          Custom permission assignment will be available in a future update.
        </AlertDescription>
      </Alert>

      {/* View Mode Tabs */}
      <Tabs value={viewMode} onValueChange={(v) => setViewMode(v as ViewMode)} className="mb-6">
        <TabsList className="grid w-full max-w-2xl grid-cols-3">
          <TabsTrigger value="role-matrix">
            <Database className="h-4 w-4 mr-2" />
            Permission Matrix
          </TabsTrigger>
          <TabsTrigger value="role-details">
            <Shield className="h-4 w-4 mr-2" />
            Role Details
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

        {/* Role Details View */}
        <TabsContent value="role-details" className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>Role Permissions</CardTitle>
              <CardDescription>
                Detailed permissions for each role
              </CardDescription>
              <div className="mt-4">
                <Select value={selectedRole} onValueChange={(v) => setSelectedRole(v as UserRole)}>
                  <SelectTrigger className="w-64">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {roles.map(role => (
                      <SelectItem key={role} value={role}>
                        <div className="flex items-center gap-2">
                          <Badge variant={permissionService.getRoleBadgeColor(role) as any} className="text-xs">
                            {role}
                          </Badge>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </CardHeader>
            <CardContent>
              <RolePermissionsDetail 
                permissions={rolePermissions}
                resources={resources}
              />
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
function RolePermissionsDetail({ 
  permissions, 
  resources 
}: { 
  permissions: Permission[];
  resources: ResourceType[];
}) {
  const groupedByResource = resources.map(resource => ({
    resource,
    permissions: permissions.filter(p => p.resourceType === resource)
  }));

  return (
    <div className="space-y-4">
      {groupedByResource.map(({ resource, permissions: resourcePerms }) => (
        <div key={resource} className="border rounded-lg p-4">
          <div className="flex items-center justify-between mb-3">
            <h4 className="font-semibold flex items-center gap-2">
              {resourcePerms.length > 0 ? (
                <CheckCircle className="h-4 w-4 text-green-500" />
              ) : (
                <XCircle className="h-4 w-4 text-muted-foreground" />
              )}
              {permissionService.getResourceLabel(resource)}
            </h4>
            <Badge variant="outline">{resourcePerms.length} permission{resourcePerms.length !== 1 ? 's' : ''}</Badge>
          </div>
          {resourcePerms.length > 0 ? (
            <div className="flex flex-wrap gap-2">
              {resourcePerms.map(perm => (
                <Badge key={perm.id} variant="secondary">
                  {permissionService.getPermissionLabel(perm.permissionType)}
                </Badge>
              ))}
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">No permissions granted</p>
          )}
        </div>
      ))}
    </div>
  );
}

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
