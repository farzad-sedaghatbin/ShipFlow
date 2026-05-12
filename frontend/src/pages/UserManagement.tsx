import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import {
  UserPlus,
  Pencil,
  Ban,
  CheckCircle,
  Search,
  User,
  Mail,
  Lock,
  KeyRound,
  Loader2,
  ShieldAlert,
  Eye,
  EyeOff,
  RefreshCw,
  Trash2,
} from 'lucide-react';
import { useToast, useAuth } from '../contexts';
import { usePermission } from '../hooks/usePermission';
import api from '../services/api';
import { User as UserType, UserRole, CreateUserRequest, Person } from '../types';
import { cn } from '../lib/utils';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Input } from '../components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import { Label } from '../components/ui/label';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { Alert, AlertDescription } from '../components/ui/alert';
import { ConfirmDialog } from '../components/ui/confirm-dialog';

const USER_ROLES: UserRole[] = ['ADMIN', 'MANAGER', 'MEMBER', 'READONLY'];

export default function UserManagement() {
  const { t, i18n } = useTranslation();
  const { showToast } = useToast();
  const { user: currentUser } = useAuth();
  const { hasPermission } = usePermission();
  const [users, setUsers] = useState<UserType[]>([]);
  const [people, setPeople] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [canManageUsers, setCanManageUsers] = useState<boolean | null>(null);

  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [formData, setFormData] = useState<CreateUserRequest>({
    username: '',
    password: '',
    email: '',
    role: 'MEMBER',
  });
  const [saving, setSaving] = useState(false);

  // Role change dialog
  const [roleDialogOpen, setRoleDialogOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<UserType | null>(null);
  const [newRole, setNewRole] = useState<UserRole>('MEMBER');

  // Reset password dialog
  const [resetPasswordDialogOpen, setResetPasswordDialogOpen] = useState(false);
  const [resetPasswordUser, setResetPasswordUser] = useState<UserType | null>(null);
  const [newPassword, setNewPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [resettingPassword, setResettingPassword] = useState(false);
  
  // Toggle active confirmation dialog
  const [toggleActiveConfirmOpen, setToggleActiveConfirmOpen] = useState(false);
  const [userToToggle, setUserToToggle] = useState<UserType | null>(null);

  // Delete confirmation dialog
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [userToDelete, setUserToDelete] = useState<UserType | null>(null);

  // Check if current user has user management permission
  useEffect(() => {
    hasPermission('USER', 'MANAGE').then(setCanManageUsers).catch(() => setCanManageUsers(false));
  }, [hasPermission]); // Include hasPermission since it's now stable

  useEffect(() => {
    const abortController = new AbortController();
    if (canManageUsers) {
      fetchUsers();
      fetchPeople();
    }
    return () => abortController.abort();
  }, [canManageUsers]);

  const fetchUsers = async () => {
    try {
      const response = await api.get<UserType[]>('/users');
      setUsers(response.data);
    } catch (error) {
      showToast(t('userManagement.loadFailed'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchPeople = async () => {
    try {
      const response = await api.get<Person[]>('/persons');
      setPeople(response.data.filter((p) => p.isActive));
    } catch (error) {
      // Silent fail
    }
  };

  const handleOpenDialog = () => {
    setFormData({
      username: '',
      password: '',
      email: '',
      role: 'MEMBER',
    });
    setDialogOpen(true);
  };

  // Show loading while checking permission
  if (canManageUsers === null) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="flex justify-center items-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </div>
    );
  }

  // Only admins can manage users
  if (!canManageUsers) {
    return (
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <Alert variant="destructive">
          <ShieldAlert className="h-4 w-4" />
          <AlertDescription>
            {t('auth.accessDenied')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setFormData({ username: '', password: '', email: '', role: 'MEMBER' });
  };

  const handleSave = async () => {
    if (!formData.username.trim()) {
      showToast(t('userManagement.usernameRequired'), 'error');
      return;
    }
    if (!formData.password.trim() || formData.password.length < 6) {
      showToast(t('userManagement.passwordMinLength'), 'error');
      return;
    }

    setSaving(true);
    try {
      await api.post('/auth/register', formData);
      showToast(t('userManagement.userCreated'), 'success');
      fetchUsers();
      handleCloseDialog();
    } catch (error) {
      // Error handled by interceptor
    } finally {
      setSaving(false);
    }
  };

  const openDeleteConfirm = (user: UserType) => {
    setUserToDelete(user);
    setDeleteConfirmOpen(true);
  };

  const handleDeleteUser = async () => {
    if (!userToDelete) return;
    try {
      await api.delete(`/users/${userToDelete.id}`);
      showToast(t('userManagement.userDeleted'), 'success');
      fetchUsers();
      setDeleteConfirmOpen(false);
      setUserToDelete(null);
    } catch {
      // Error handled by Axios interceptor
    }
  };

  const openToggleActiveConfirm = (user: UserType) => {
    setUserToToggle(user);
    setToggleActiveConfirmOpen(true);
  };

  const handleToggleActive = async () => {
    if (!userToToggle) return;
    const action = userToToggle.isActive ? 'deactivate' : 'activate';

    try {
      await api.put(`/users/${userToToggle.id}/${action}`);
      showToast(t('userManagement.userActivated', { action }), 'success');
      fetchUsers();
      setToggleActiveConfirmOpen(false);
      setUserToToggle(null);
    } catch (error) {
      // Error handled by interceptor
    }
  };

  const handleOpenRoleDialog = (user: UserType) => {
    setSelectedUser(user);
    setNewRole(user.role);
    setRoleDialogOpen(true);
  };

  const handleChangeRole = async () => {
    if (!selectedUser) return;

    try {
      await api.put(`/users/${selectedUser.id}/role?role=${newRole}`);
      showToast(t('userManagement.roleUpdated'), 'success');
      fetchUsers();
      setRoleDialogOpen(false);
    } catch (error) {
      // Error handled by interceptor
    }
  };

  const handleOpenResetPasswordDialog = (user: UserType) => {
    setResetPasswordUser(user);
    setNewPassword('');
    setShowNewPassword(false);
    setResetPasswordDialogOpen(true);
  };

  const generateRandomPassword = () => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*';
    let password = '';
    for (let i = 0; i < 12; i++) {
      password += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    setNewPassword(password);
  };

  const handleResetPassword = async () => {
    if (!resetPasswordUser || !newPassword) return;
    
    if (newPassword.length < 6) {
      showToast(t('userManagement.passwordMinLength'), 'error');
      return;
    }

    setResettingPassword(true);
    try {
      await api.put(`/users/${resetPasswordUser.id}/reset-password`, {
        newPassword,
      });
      showToast(t('userManagement.passwordReset'), 'success');
      setResetPasswordDialogOpen(false);
      setNewPassword('');
      setResetPasswordUser(null);
    } catch (error) {
      // Error handled by interceptor
    } finally {
      setResettingPassword(false);
    }
  };

  const getRoleClassName = (role: UserRole): string => {
    const classNames: Record<UserRole, string> = {
      ADMIN: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
      MANAGER: 'bg-blue-100 text-blue-800 dark:bg-blue-900/30 dark:text-blue-400',
      MEMBER: 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400',
      READONLY: 'bg-gray-100 text-gray-800 dark:bg-gray-900/30 dark:text-gray-400',
    };
    return classNames[role];
  };

  const filteredUsers = users.filter(
    (user) =>
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (user.personName && user.personName.toLowerCase().includes(searchTerm.toLowerCase())) ||
      user.role.toLowerCase().includes(searchTerm.toLowerCase())
  );

  if (!canManageUsers) {
    return (
      <div className="p-4">
        <Alert variant="destructive">
          <ShieldAlert className="h-4 w-4" />
          <AlertDescription>
            {t('userManagement.permissionDenied')}
          </AlertDescription>
        </Alert>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <h1 className="text-2xl font-bold tracking-tight">{t('userManagement.title')}</h1>
        <Button onClick={handleOpenDialog} size="sm">
          <UserPlus className="mr-2 h-4 w-4" />
          {t('userManagement.addUser')}
        </Button>
      </div>

      {/* Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t('userManagement.totalUsers')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold">{users.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t('userManagement.active')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-green-600 dark:text-green-400">
              {users.filter((u) => u.isActive).length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t('userManagement.admins')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-red-600 dark:text-red-400">
              {users.filter((u) => u.role === 'ADMIN').length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t('userManagement.members')}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
              {users.filter((u) => u.role === 'MEMBER').length}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Search */}
      <Card>
        <CardContent className="p-4">
          <div className="relative">
            <Label htmlFor="users-search" className="sr-only">{t('userManagement.searchUsers')}</Label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              id="users-search"
              type="search"
              placeholder={t('userManagement.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
              aria-label={t('userManagement.searchUsers')}
            />
          </div>
        </CardContent>
      </Card>

      {/* Users Table */}
      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('userManagement.user')}</TableHead>
                <TableHead>{t('userManagement.role')}</TableHead>
                <TableHead>{t('userManagement.linkedPerson')}</TableHead>
                <TableHead>{t('userManagement.status')}</TableHead>
                <TableHead>{t('userManagement.created')}</TableHead>
                <TableHead className="text-right">{t('userManagement.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredUsers.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Avatar className="h-9 w-9">
                        <AvatarFallback className="bg-primary/10 text-primary">
                          {user.username.charAt(0).toUpperCase()}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <p className="font-medium">{user.username}</p>
                        {user.email && (
                          <p className="text-sm text-muted-foreground">{user.email}</p>
                        )}
                      </div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge
                      className={cn(
                        'cursor-pointer hover:opacity-80 transition-opacity',
                        getRoleClassName(user.role)
                      )}
                      onClick={() => handleOpenRoleDialog(user)}
                    >
                      {user.role.replace('_', ' ')}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {user.personName || (
                      <span className="text-muted-foreground text-sm">{t('userManagement.notLinked')}</span>
                    )}
                  </TableCell>
                  <TableCell>
                    <Badge
                      variant={user.isActive ? 'default' : 'secondary'}
                      className={cn(
                        user.isActive
                          ? 'bg-green-100 text-green-800 dark:bg-green-900/30 dark:text-green-400'
                          : 'bg-gray-100 text-gray-600 dark:bg-gray-800 dark:text-gray-400'
                      )}
                    >
                      {user.isActive ? t('userManagement.active') : t('userManagement.inactive')}
                    </Badge>
                  </TableCell>
                  <TableCell className="text-sm text-muted-foreground">
                    {formatLocalizedDate(new Date(user.createdAt), i18n.language)}
                  </TableCell>
                  <TableCell className="text-right">
                    <TooltipProvider>
                      <div className="flex items-center justify-end gap-1">
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => handleOpenRoleDialog(user)}
                            >
                              <Pencil className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('userManagement.changeRole')}</TooltipContent>
                        </Tooltip>
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => handleOpenResetPasswordDialog(user)}
                            >
                              <KeyRound className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('userManagement.resetPassword')}</TooltipContent>
                        </Tooltip>
                        {user.id !== currentUser?.userId && (
                          <>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className={cn(
                                    'h-8 w-8',
                                    user.isActive
                                      ? 'text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950'
                                      : 'text-green-600 hover:text-green-700 hover:bg-green-50 dark:hover:bg-green-950'
                                  )}
                                  onClick={() => openToggleActiveConfirm(user)}
                                >
                                  {user.isActive ? (
                                    <Ban className="h-4 w-4" />
                                  ) : (
                                    <CheckCircle className="h-4 w-4" />
                                  )}
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>
                                {user.isActive ? t('userManagement.deactivate') : t('userManagement.activate')}
                              </TooltipContent>
                            </Tooltip>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-950"
                                  onClick={() => openDeleteConfirm(user)}
                                >
                                  <Trash2 className="h-4 w-4" />
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>{t('userManagement.deleteUser')}</TooltipContent>
                            </Tooltip>
                          </>
                        )}
                      </div>
                    </TooltipProvider>
                  </TableCell>
                </TableRow>
              ))}
              {filteredUsers.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="text-center py-8">
                    <p className="text-muted-foreground">
                      {searchTerm ? t('userManagement.noUsersFoundSearch') : t('userManagement.noUsersFound')}
                    </p>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {/* Add User Dialog */}
      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('userManagement.addNewUser')}</DialogTitle>
            <DialogDescription>
              {t('userManagement.createNewUser')}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="username">{t('userManagement.username')} *</Label>
              <div className="relative">
                <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="username"
                  placeholder={t('userManagement.enterUsername')}
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">{t('userManagement.password')} *</Label>
              <div className="relative">
                <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="password"
                  type="password"
                  placeholder={t('userManagement.enterPassword')}
                  value={formData.password}
                  onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                  className="pl-10"
                />
              </div>
              <p className="text-xs text-muted-foreground">{t('userManagement.minCharacters')}</p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">{t('userManagement.email')}</Label>
              <div className="relative">
                <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  id="email"
                  type="email"
                  placeholder={t('userManagement.enterEmail')}
                  value={formData.email}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="pl-10"
                />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="role">{t('userManagement.role')}</Label>
              <Select
                value={formData.role}
                onValueChange={(value) => setFormData({ ...formData, role: value as UserRole })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('userManagement.selectRole')} />
                </SelectTrigger>
                <SelectContent>
                  {USER_ROLES.map((role) => (
                    <SelectItem key={role} value={role}>
                      {role.replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="person">{t('userManagement.linkToPerson')}</Label>
              <Select
                value={formData.personId?.toString() || 'none'}
                onValueChange={(value) =>
                  setFormData({ ...formData, personId: value && value !== 'none' ? Number(value) : undefined })
                }
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('userManagement.selectPerson')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('userManagement.none')}</SelectItem>
                  {people.map((person) => (
                    <SelectItem key={person.id} value={person.id.toString()}>
                      {person.name} ({person.email})
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={handleCloseDialog}>
              {t('userManagement.cancel')}
            </Button>
            <Button onClick={handleSave} disabled={saving}>
              {saving ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t('userManagement.creating')}
                </>
              ) : (
                t('userManagement.createUser')
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Change Role Dialog */}
      <Dialog open={roleDialogOpen} onOpenChange={setRoleDialogOpen}>
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>{t('userManagement.changeRole')}</DialogTitle>
            <DialogDescription>
              {t('userManagement.changeRoleFor')} <strong>{selectedUser?.username}</strong>
            </DialogDescription>
          </DialogHeader>
          <div className="py-4">
            <div className="space-y-2">
              <Label htmlFor="newRole">{t('userManagement.newRole')}</Label>
              <Select value={newRole} onValueChange={(value) => setNewRole(value as UserRole)}>
                <SelectTrigger>
                  <SelectValue placeholder={t('userManagement.selectRole')} />
                </SelectTrigger>
                <SelectContent>
                  {USER_ROLES.map((role) => (
                    <SelectItem key={role} value={role}>
                      {role.replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setRoleDialogOpen(false)}>
              {t('userManagement.cancel')}
            </Button>
            <Button onClick={handleChangeRole}>{t('userManagement.updateRole')}</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Reset Password Dialog */}
      <Dialog open={resetPasswordDialogOpen} onOpenChange={setResetPasswordDialogOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{t('userManagement.resetPassword')}</DialogTitle>
            <DialogDescription>
              {t('userManagement.resetPasswordFor', { username: resetPasswordUser?.username })}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="newPassword">{t('userManagement.newPassword')}</Label>
              <div className="flex space-x-2">
                <div className="relative flex-1">
                  <Input
                    id="newPassword"
                    type={showNewPassword ? 'text' : 'password'}
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    placeholder={t('userManagement.enterNewPassword')}
                    className="pr-10"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="absolute right-0 top-0 h-full px-3 hover:bg-transparent"
                    onClick={() => setShowNewPassword(!showNewPassword)}
                  >
                    {showNewPassword ? (
                      <EyeOff className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Eye className="h-4 w-4 text-muted-foreground" />
                    )}
                  </Button>
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="icon"
                  onClick={generateRandomPassword}
                  title={t('userManagement.generatePassword')}
                >
                  <RefreshCw className="h-4 w-4" />
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                {t('userManagement.passwordMinLength')}
              </p>
            </div>
          </div>
          <DialogFooter>
            <Button 
              variant="outline" 
              onClick={() => setResetPasswordDialogOpen(false)}
              disabled={resettingPassword}
            >
              {t('userManagement.cancel')}
            </Button>
            <Button 
              onClick={handleResetPassword}
              disabled={!newPassword || newPassword.length < 6 || resettingPassword}
            >
              {resettingPassword ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  {t('userManagement.resetting')}
                </>
              ) : (
                t('userManagement.resetPassword')
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Toggle Active Confirmation Dialog */}
      <ConfirmDialog
        open={toggleActiveConfirmOpen}
        onOpenChange={setToggleActiveConfirmOpen}
        title={userToToggle?.isActive ? t('userManagement.deactivate') : t('userManagement.activate')}
        description={t(`userManagement.confirm${userToToggle?.isActive ? 'Deactivate' : 'Activate'}`, { username: userToToggle?.username || '' })}
        confirmLabel={t('common.confirm')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleToggleActive}
        variant={userToToggle?.isActive ? 'destructive' : 'default'}
      />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        open={deleteConfirmOpen}
        onOpenChange={setDeleteConfirmOpen}
        title={t('userManagement.deleteUser')}
        description={t('userManagement.confirmDelete', { username: userToDelete?.username || '' })}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleDeleteUser}
        variant="destructive"
      />
    </div>
  );
}
