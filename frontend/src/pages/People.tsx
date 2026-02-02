import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { 
  Plus, 
  Pencil, 
  Trash2, 
  Search, 
  User, 
  Mail, 
  Award, 
  History, 
  ClipboardList, 
  Clock, 
  FileText,
  Loader2,
  Lock,
  UserPlus
} from 'lucide-react';
import { useToast } from '../contexts';
import api from '../services/api';
import { workLogService } from '../services/workLogService';
import { Person, CreatePersonRequest, TeamAssignment, WorkLog, UserRole } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyPeopleIllustration, EmptyWorkLogsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../components/ui/avatar';
import { Checkbox } from '../components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '../components/ui/tooltip';
import { ScrollArea } from '../components/ui/scroll-area';

const USER_ROLES: UserRole[] = ['ADMIN', 'MANAGER', 'MEMBER', 'READONLY'];
const MIN_PASSWORD_LENGTH = 6;

export default function People() {
  const { t, i18n } = useTranslation();
  const { showToast } = useToast();
  const [people, setPeople] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  
  // Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingPerson, setEditingPerson] = useState<Person | null>(null);
  const [formData, setFormData] = useState<CreatePersonRequest>({
    name: '',
    email: '',
    skills: '',
    avatarUrl: '',
    createUser: false,
    username: '',
    password: '',
    userRole: 'MEMBER',
  });
  const [saving, setSaving] = useState(false);
  
  // History dialog
  const [historyDialogOpen, setHistoryDialogOpen] = useState(false);
  const [selectedPerson, setSelectedPerson] = useState<Person | null>(null);
  const [assignments, setAssignments] = useState<TeamAssignment[]>([]);
  const [loadingAssignments, setLoadingAssignments] = useState(false);
  
  // Work logs activity dialog
  const [activityDialogOpen, setActivityDialogOpen] = useState(false);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [loadingWorkLogs, setLoadingWorkLogs] = useState(false);

  useEffect(() => {
    const abortController = new AbortController();
    fetchPeople();
    return () => abortController.abort();
  }, []);

  const fetchPeople = async () => {
    try {
      const response = await api.get<Person[]>('/persons');
      setPeople(response.data);
    } catch (error) {
      showToast(t('people.loadFailed'), 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchAssignments = async (personId: number) => {
    setLoadingAssignments(true);
    try {
      const response = await api.get<TeamAssignment[]>(`/persons/${personId}/assignments`);
      setAssignments(response.data);
    } catch (error) {
      showToast(t('peopleManagement.failedToLoadAssignments'), 'error');
    } finally {
      setLoadingAssignments(false);
    }
  };

  const handleOpenDialog = (person?: Person) => {
    if (person) {
      setEditingPerson(person);
      setFormData({
        name: person.name,
        email: person.email,
        skills: person.skills || '',
        avatarUrl: person.avatarUrl || '',
        createUser: false,
        username: '',
        password: '',
        userRole: 'MEMBER',
      });
    } else {
      setEditingPerson(null);
      setFormData({ 
        name: '', 
        email: '', 
        skills: '', 
        avatarUrl: '',
        createUser: false,
        username: '',
        password: '',
        userRole: 'MEMBER',
      });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingPerson(null);
    setFormData({ 
      name: '', 
      email: '', 
      skills: '', 
      avatarUrl: '',
      createUser: false,
      username: '',
      password: '',
      userRole: 'MEMBER',
    });
  };

  const handleSave = async () => {
    if (!formData.name.trim()) {
      showToast(t('peopleManagement.nameRequired'), 'error');
      return;
    }
    if (!formData.email.trim()) {
      showToast(t('peopleManagement.emailRequired'), 'error');
      return;
    }

    // Validate user creation fields if creating user
    if (formData.createUser && !editingPerson) {
      if (!formData.password || !formData.password.trim() || formData.password.length < MIN_PASSWORD_LENGTH) {
        showToast(t('userManagement.passwordMinLength'), 'error');
        return;
      }
      if (!formData.userRole) {
        showToast(t('userManagement.roleRequired'), 'error');
        return;
      }
    }

    setSaving(true);
    try {
      if (editingPerson) {
        await api.put(`/persons/${editingPerson.id}`, formData);
        showToast(t('peopleManagement.personUpdated'), 'success');
      } else {
        await api.post('/persons', formData);
        if (formData.createUser) {
          showToast(t('peopleManagement.personAndUserCreated'), 'success');
        } else {
          showToast(t('peopleManagement.personCreated'), 'success');
        }
      }
      fetchPeople();
      handleCloseDialog();
    } catch (error) {
      // Error handled by interceptor
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (person: Person) => {
    if (!confirm(t('peopleManagement.confirmDeactivate') + ' ' + person.name + '?')) {
      return;
    }

    try {
      await api.delete(`/persons/${person.id}`);
      showToast(t('peopleManagement.personDeactivated'), 'success');
      fetchPeople();
    } catch (error) {
      // Error handled by interceptor
    }
  };

  const handleViewHistory = (person: Person) => {
    setSelectedPerson(person);
    fetchAssignments(person.id);
    setHistoryDialogOpen(true);
  };

  const handleViewActivity = (person: Person) => {
    setSelectedPerson(person);
    fetchWorkLogs(person.id);
    setActivityDialogOpen(true);
  };

  const fetchWorkLogs = async (personId: number) => {
    setLoadingWorkLogs(true);
    try {
      const response = await workLogService.getByPersonId(personId);
      // Sort by date descending (most recent first)
      const sortedLogs = response.data.sort((a, b) => 
        new Date(b.date).getTime() - new Date(a.date).getTime()
      );
      setWorkLogs(sortedLogs);
    } catch (error) {
      showToast(t('peopleManagement.failedToLoadWorkLogs'), 'error');
      setWorkLogs([]);
    } finally {
      setLoadingWorkLogs(false);
    }
  };

  // Handle email change and automatically set username when creating user
  const handleEmailChange = (email: string) => {
    setFormData(prev => ({
      ...prev,
      email,
      // If creating user is enabled, automatically set username to email
      username: prev.createUser ? email : prev.username
    }));
  };

  // Handle create user checkbox change
  const handleCreateUserChange = (createUser: boolean) => {
    setFormData(prev => ({
      ...prev,
      createUser,
      // If enabling user creation, set username to current email
      username: createUser ? prev.email : '',
      // Reset password when toggling
      password: createUser ? prev.password : ''
    }));
  };

  const filteredPeople = people.filter(
    (person) =>
      person.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      person.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (person.skills && person.skills.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  const getRoleBadgeVariant = (role: string): 'default' | 'secondary' | 'destructive' | 'outline' => {
    const variants: Record<string, 'default' | 'secondary' | 'destructive' | 'outline'> = {
      BACKEND: 'default',
      FRONTEND: 'secondary',
      FULLSTACK: 'default',
      QA: 'outline',
      DESIGNER: 'secondary',
      TECH_LEAD: 'destructive',
      PRODUCT_MANAGER: 'destructive',
    };
    return variants[role] || 'outline';
  };

  const getRoleClassName = (role: string) => {
    const classes: Record<string, string> = {
      BACKEND: 'bg-blue-500 hover:bg-blue-600',
      FRONTEND: 'bg-purple-500 hover:bg-purple-600 text-white',
      FULLSTACK: 'bg-green-500 hover:bg-green-600 text-white',
      QA: 'bg-amber-500 hover:bg-amber-600 text-white',
      DESIGNER: 'bg-pink-500 hover:bg-pink-600 text-white',
      TECH_LEAD: 'bg-red-500 hover:bg-red-600',
      PRODUCT_MANAGER: 'bg-red-500 hover:bg-red-600',
    };
    return classes[role] || '';
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <TooltipProvider>
      <div>
        <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4 mb-6">
          <h1 className="text-3xl font-bold tracking-tight">{t('peopleManagement.title')}</h1>
          <Button onClick={() => handleOpenDialog()} className="w-full sm:w-auto">
            <Plus className="h-4 w-4 mr-2" />
            {t('peopleManagement.addPerson')}
          </Button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">{t('peopleManagement.totalPeople')}</p>
              <p className="text-3xl font-bold">{people.length}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">{t('peopleManagement.active')}</p>
              <p className="text-3xl font-bold text-green-600">
                {people.filter((p) => p.isActive).length}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">{t('peopleManagement.inactive')}</p>
              <p className="text-3xl font-bold text-muted-foreground">
                {people.filter((p) => !p.isActive).length}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Search */}
        <div className="bg-card rounded-lg border p-4 mb-4">
          <div className="relative">
            <Label htmlFor="people-search" className="sr-only">{t('peopleManagement.searchPlaceholder')}</Label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              id="people-search"
              type="search"
              className="pl-10"
              placeholder={t('peopleManagement.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              aria-label={t('peopleManagement.searchPlaceholder')}
            />
          </div>
        </div>

        {/* People Table */}
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t('peopleManagement.person')}</TableHead>
                <TableHead>{t('peopleManagement.email')}</TableHead>
                <TableHead>{t('peopleManagement.skills')}</TableHead>
                <TableHead>{t('peopleManagement.status')}</TableHead>
                <TableHead>{t('peopleManagement.joined')}</TableHead>
                <TableHead className="text-right">{t('peopleManagement.actions')}</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filteredPeople.map((person) => (
                <TableRow 
                  key={person.id} 
                  className="cursor-pointer hover:bg-muted/50"
                  onClick={() => handleViewActivity(person)}
                >
                  <TableCell>
                    <div className="flex items-center gap-3">
                      <Avatar>
                        <AvatarImage src={person.avatarUrl} alt={person.name} />
                        <AvatarFallback>{person.name.charAt(0)}</AvatarFallback>
                      </Avatar>
                      <span className="font-medium">{person.name}</span>
                    </div>
                  </TableCell>
                  <TableCell>{person.email}</TableCell>
                  <TableCell>
                    <div className="flex gap-1 flex-wrap">
                      {person.skills?.split(',').slice(0, 3).map((skill, i) => (
                        <Badge key={i} variant="outline" className="text-xs">
                          {skill.trim()}
                        </Badge>
                      ))}
                      {person.skills && person.skills.split(',').length > 3 && (
                        <Badge variant="secondary" className="text-xs">
                          +{person.skills.split(',').length - 3}
                        </Badge>
                      )}
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={person.isActive ? 'default' : 'secondary'} className={cn(
                      person.isActive && 'bg-green-500 hover:bg-green-600'
                    )}>
                      {person.isActive ? t('peopleManagement.active') : t('peopleManagement.inactive')}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {formatLocalizedDate(new Date(person.createdAt), i18n.language)}
                  </TableCell>
                  <TableCell className="text-right" onClick={(e) => e.stopPropagation()}>
                    <div className="flex justify-end gap-1">
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="h-8 w-8 text-primary"
                            onClick={() => handleViewActivity(person)}
                          >
                            <ClipboardList className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('peopleManagement.viewActivity')}</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="h-8 w-8"
                            onClick={() => handleViewHistory(person)}
                          >
                            <History className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('peopleManagement.teamHistory')}</TooltipContent>
                      </Tooltip>
                      <Tooltip>
                        <TooltipTrigger asChild>
                          <Button 
                            variant="ghost" 
                            size="icon" 
                            className="h-8 w-8"
                            onClick={() => handleOpenDialog(person)}
                          >
                            <Pencil className="h-4 w-4" />
                          </Button>
                        </TooltipTrigger>
                        <TooltipContent>{t('peopleManagement.edit')}</TooltipContent>
                      </Tooltip>
                      {person.isActive && (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button 
                              variant="ghost" 
                              size="icon" 
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={() => handleDelete(person)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>{t('peopleManagement.deactivate')}</TooltipContent>
                        </Tooltip>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
              {filteredPeople.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} className="border-0">
                    {people.length === 0 ? (
                      <EmptyState
                        illustration={<EmptyPeopleIllustration />}
                        title={t('peopleManagement.noPeopleYet')}
                        description={t('peopleManagement.noPeopleDescription')}
                        action={{
                          label: t('peopleManagement.addFirstPerson'),
                          onClick: () => handleOpenDialog(),
                          startIcon: <Plus className="h-4 w-4" />,
                        }}
                        size="medium"
                      />
                    ) : (
                      <EmptyState
                        title={t('peopleManagement.noMatches')}
                        description={t('peopleManagement.noMatchesDescription', { searchTerm })}
                        size="small"
                        compact
                      />
                    )}
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </div>

        {/* Add/Edit Dialog */}
        <Dialog open={dialogOpen} onOpenChange={(open) => !open && handleCloseDialog()}>
          <DialogContent className="sm:max-w-md">
            <DialogHeader>
              <DialogTitle>
                {editingPerson ? t('peopleManagement.editPerson') : t('peopleManagement.addNewPerson')}
              </DialogTitle>
              <DialogDescription>
                {editingPerson ? t('peopleManagement.updatePerson') : t('peopleManagement.addNewPerson')}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="name">{t('peopleManagement.name')} *</Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="name"
                    className="pl-10"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder={t('peopleManagement.enterName')}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">{t('peopleManagement.email')} *</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    className="pl-10"
                    value={formData.email}
                    onChange={(e) => handleEmailChange(e.target.value)}
                    placeholder={t('peopleManagement.enterEmail')}
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="skills">{t('peopleManagement.skillsLabel')}</Label>
                <div className="relative">
                  <Award className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="skills"
                    className="pl-10"
                    value={formData.skills}
                    onChange={(e) => setFormData({ ...formData, skills: e.target.value })}
                    placeholder={t('peopleManagement.skillsPlaceholder')}
                  />
                </div>
                <p className="text-xs text-muted-foreground">{t('peopleManagement.skillsHint')}</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="avatarUrl">{t('peopleManagement.avatarUrl')}</Label>
                <Input
                  id="avatarUrl"
                  value={formData.avatarUrl}
                  onChange={(e) => setFormData({ ...formData, avatarUrl: e.target.value })}
                  placeholder={t('peopleManagement.avatarPlaceholder')}
                />
              </div>
              
              {/* User Account Creation Section */}
              {!editingPerson && (
                <>
                  <div className="border-t pt-4">
                    <div className="flex items-center space-x-2">
                      <Checkbox
                        id="createUser"
                        checked={formData.createUser}
                        onCheckedChange={handleCreateUserChange}
                      />
                      <Label htmlFor="createUser" className="flex items-center gap-2">
                        <UserPlus className="h-4 w-4" />
                        {t('peopleManagement.createUserAccount')}
                      </Label>
                    </div>
                    <p className="text-xs text-muted-foreground mt-1">
                      {t('peopleManagement.createUserAccountDesc')}
                    </p>
                  </div>
                  
                  {formData.createUser && (
                    <div className="space-y-4 pl-6 border-l-2 border-muted">
                      <div className="space-y-2">
                        <Label htmlFor="username">{t('userManagement.username')}</Label>
                        <div className="relative">
                          <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="username"
                            className="pl-10"
                            value={formData.username}
                            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                            placeholder={t('userManagement.enterUsername')}
                            aria-describedby="username-help"
                            disabled
                          />
                        </div>
                        <p id="username-help" className="text-xs text-muted-foreground">
                          {t('peopleManagement.usernameAutoSet')}
                        </p>
                      </div>
                      
                      <div className="space-y-2">
                        <Label htmlFor="password">{t('userManagement.password')} *</Label>
                        <div className="relative">
                          <Lock className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                          <Input
                            id="password"
                            type="password"
                            className="pl-10"
                            value={formData.password}
                            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                            placeholder={t('userManagement.enterPassword')}
                          />
                        </div>
                        <p className="text-xs text-muted-foreground">
                          {t('userManagement.minCharacters')}
                        </p>
                      </div>
                      
                      <div className="space-y-2">
                        <Label htmlFor="userRole">{t('userManagement.role')}</Label>
                        <Select
                          value={formData.userRole}
                          onValueChange={(value) => setFormData({ ...formData, userRole: value as UserRole })}
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
                    </div>
                  )}
                </>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={handleCloseDialog}>
                {t('peopleManagement.cancel')}
              </Button>
              <Button onClick={handleSave} disabled={saving}>
                {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                {saving ? t('peopleManagement.saving') : editingPerson ? t('peopleManagement.update') : t('peopleManagement.create')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Assignment History Dialog */}
        <Dialog open={historyDialogOpen} onOpenChange={(open) => !open && setHistoryDialogOpen(false)}>
          <DialogContent className="sm:max-w-3xl">
            <DialogHeader>
              <div className="flex items-center gap-3">
                <Avatar className="h-12 w-12">
                  <AvatarImage src={selectedPerson?.avatarUrl} alt={selectedPerson?.name} />
                  <AvatarFallback>{selectedPerson?.name.charAt(0)}</AvatarFallback>
                </Avatar>
                <div>
                  <DialogTitle>{selectedPerson?.name}</DialogTitle>
                  <DialogDescription>{t('peopleManagement.teamAssignmentHistory')}</DialogDescription>
                </div>
              </div>
            </DialogHeader>
            <div className="py-4">
              {loadingAssignments ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              ) : assignments.length === 0 ? (
                <p className="text-center text-muted-foreground py-8">
                  {t('peopleManagement.noAssignments')}
                </p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>{t('peopleManagement.team')}</TableHead>
                      <TableHead>{t('peopleManagement.cycle')}</TableHead>
                      <TableHead>{t('peopleManagement.role')}</TableHead>
                      <TableHead>{t('peopleManagement.period')}</TableHead>
                      <TableHead>{t('peopleManagement.assignmentStatus')}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {assignments.map((assignment) => (
                      <TableRow key={assignment.id}>
                        <TableCell>{assignment.teamName}</TableCell>
                        <TableCell>{assignment.cycleName || '-'}</TableCell>
                        <TableCell>
                          <Badge 
                            variant={getRoleBadgeVariant(assignment.role)}
                            className={getRoleClassName(assignment.role)}
                          >
                            {assignment.role.replace('_', ' ')}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {formatLocalizedDate(new Date(assignment.startDate), i18n.language)}
                          {assignment.endDate && ` - ${formatLocalizedDate(new Date(assignment.endDate), i18n.language)}`}
                        </TableCell>
                        <TableCell>
                          <Badge 
                            variant={assignment.isActive ? 'default' : 'secondary'}
                            className={cn(assignment.isActive && 'bg-green-500 hover:bg-green-600')}
                          >
                            {assignment.isActive ? t('peopleManagement.assignmentActive') : t('peopleManagement.assignmentPast')}
                          </Badge>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setHistoryDialogOpen(false)}>
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Work Logs Activity Dialog */}
        <Dialog open={activityDialogOpen} onOpenChange={(open) => !open && setActivityDialogOpen(false)}>
          <DialogContent className="sm:max-w-3xl">
            <DialogHeader>
              <div className="flex items-center gap-3">
                <Avatar className="h-12 w-12 bg-primary">
                  <AvatarImage src={selectedPerson?.avatarUrl} alt={selectedPerson?.name} />
                  <AvatarFallback className="bg-primary text-primary-foreground">
                    {selectedPerson?.name.charAt(0)}
                  </AvatarFallback>
                </Avatar>
                <div>
                  <DialogTitle>{selectedPerson?.name}</DialogTitle>
                  <DialogDescription>{t('peopleManagement.workActivityTitle')}</DialogDescription>
                </div>
              </div>
            </DialogHeader>
            <div className="py-4">
              {loadingWorkLogs ? (
                <div className="flex justify-center py-8">
                  <Loader2 className="h-8 w-8 animate-spin text-primary" />
                </div>
              ) : workLogs.length === 0 ? (
                <div className="py-8">
                  <EmptyState
                    illustration={<EmptyWorkLogsIllustration />}
                    title={t('peopleManagement.noWorkLogs')}
                    description={t('peopleManagement.noWorkLogsDesc', { name: selectedPerson?.name })}
                    size="small"
                    compact
                  />
                </div>
              ) : (
                <>
                  {/* Summary Stats */}
                  <div className="grid grid-cols-3 gap-4 mb-6">
                    <Card className="border">
                      <CardContent className="text-center py-3">
                        <p className="text-2xl font-bold text-primary">
                          {workLogs.reduce((sum, log) => sum + log.hoursSpent, 0).toFixed(1)}
                        </p>
                        <p className="text-xs text-muted-foreground">{t('peopleManagement.totalHours')}</p>
                      </CardContent>
                    </Card>
                    <Card className="border">
                      <CardContent className="text-center py-3">
                        <p className="text-2xl font-bold text-purple-600">
                          {workLogs.length}
                        </p>
                        <p className="text-xs text-muted-foreground">{t('peopleManagement.logEntries')}</p>
                      </CardContent>
                    </Card>
                    <Card className="border">
                      <CardContent className="text-center py-3">
                        <p className="text-2xl font-bold text-green-600">
                          {new Set(workLogs.map(log => log.pitchId)).size}
                        </p>
                        <p className="text-xs text-muted-foreground">{t('peopleManagement.pitchesWorked')}</p>
                      </CardContent>
                    </Card>
                  </div>

                  {/* Work Logs Table */}
                  <ScrollArea className="h-[400px]">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>{t('peopleManagement.date')}</TableHead>
                          <TableHead>{t('peopleManagement.pitch')}</TableHead>
                          <TableHead>{t('peopleManagement.project')}</TableHead>
                          <TableHead className="text-right">{t('peopleManagement.hours')}</TableHead>
                          <TableHead>{t('peopleManagement.notes')}</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {workLogs.slice(0, 50).map((log) => (
                          <TableRow key={log.id}>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <Clock className="h-4 w-4 text-muted-foreground" />
                                {formatLocalizedDate(new Date(log.date), i18n.language)}
                              </div>
                            </TableCell>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <FileText className="h-4 w-4 text-primary" />
                                <span className="font-medium text-sm">
                                  {log.pitchTitle || `Pitch #${log.pitchId}`}
                                </span>
                              </div>
                            </TableCell>
                            <TableCell>
                              {log.projectKey ? (
                                <Badge variant="outline" className="text-xs">
                                  {log.projectKey}
                                </Badge>
                              ) : (
                                <span className="text-sm text-muted-foreground">
                                  {log.projectName || '-'}
                                </span>
                              )}
                            </TableCell>
                            <TableCell className="text-right">
                              <Badge 
                                variant={log.hoursSpent >= 4 ? 'default' : 'secondary'}
                                className={cn(log.hoursSpent >= 4 && 'bg-green-500 hover:bg-green-600')}
                              >
                                {log.hoursSpent}h
                              </Badge>
                            </TableCell>
                            <TableCell>
                              <span 
                                className="text-sm text-muted-foreground max-w-[200px] truncate block"
                                title={log.note}
                              >
                                {log.note || '-'}
                              </span>
                            </TableCell>
                          </TableRow>
                        ))}
                      </TableBody>
                    </Table>
                  </ScrollArea>
                  {workLogs.length > 50 && (
                    <p className="text-xs text-muted-foreground mt-2">
                      {t('peopleManagement.showingFirst', { count: workLogs.length })}
                    </p>
                  )}
                </>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setActivityDialogOpen(false)}>
                {t('peopleManagement.close')}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </TooltipProvider>
  );
}
