import { useState, useEffect } from 'react';
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
  Loader2 
} from 'lucide-react';
import { useToast } from '../contexts';
import api from '../services/api';
import { workLogService } from '../services/workLogService';
import { Person, CreatePersonRequest, TeamAssignment, WorkLog } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyPeopleIllustration, EmptyWorkLogsIllustration } from '../components/illustrations';
import { cn } from '../lib/utils';

import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import { Avatar, AvatarFallback, AvatarImage } from '../components/ui/avatar';
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

export default function People() {
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
      showToast('Failed to load people', 'error');
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
      showToast('Failed to load assignments', 'error');
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
      });
    } else {
      setEditingPerson(null);
      setFormData({ name: '', email: '', skills: '', avatarUrl: '' });
    }
    setDialogOpen(true);
  };

  const handleCloseDialog = () => {
    setDialogOpen(false);
    setEditingPerson(null);
    setFormData({ name: '', email: '', skills: '', avatarUrl: '' });
  };

  const handleSave = async () => {
    if (!formData.name.trim()) {
      showToast('Name is required', 'error');
      return;
    }
    if (!formData.email.trim()) {
      showToast('Email is required', 'error');
      return;
    }

    setSaving(true);
    try {
      if (editingPerson) {
        await api.put(`/persons/${editingPerson.id}`, formData);
        showToast('Person updated successfully', 'success');
      } else {
        await api.post('/persons', formData);
        showToast('Person created successfully', 'success');
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
    if (!confirm(`Are you sure you want to deactivate ${person.name}?`)) {
      return;
    }

    try {
      await api.delete(`/persons/${person.id}`);
      showToast('Person deactivated successfully', 'success');
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
      showToast('Failed to load work logs', 'error');
      setWorkLogs([]);
    } finally {
      setLoadingWorkLogs(false);
    }
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
          <h1 className="text-3xl font-bold tracking-tight">People Management</h1>
          <Button onClick={() => handleOpenDialog()} className="w-full sm:w-auto">
            <Plus className="h-4 w-4 mr-2" />
            Add Person
          </Button>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">Total People</p>
              <p className="text-3xl font-bold">{people.length}</p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">Active</p>
              <p className="text-3xl font-bold text-green-600">
                {people.filter((p) => p.isActive).length}
              </p>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <p className="text-sm text-muted-foreground mb-1">Inactive</p>
              <p className="text-3xl font-bold text-muted-foreground">
                {people.filter((p) => !p.isActive).length}
              </p>
            </CardContent>
          </Card>
        </div>

        {/* Search */}
        <div className="bg-card rounded-lg border p-4 mb-4">
          <div className="relative">
            <Label htmlFor="people-search" className="sr-only">Search people by name, email, or skills</Label>
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" aria-hidden="true" />
            <Input
              id="people-search"
              type="search"
              className="pl-10"
              placeholder="Search by name, email, or skills..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              aria-label="Search people by name, email, or skills"
            />
          </div>
        </div>

        {/* People Table */}
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Person</TableHead>
                <TableHead>Email</TableHead>
                <TableHead>Skills</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Joined</TableHead>
                <TableHead className="text-right">Actions</TableHead>
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
                      {person.isActive ? 'Active' : 'Inactive'}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    {new Date(person.createdAt).toLocaleDateString()}
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
                        <TooltipContent>View Activity</TooltipContent>
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
                        <TooltipContent>Team History</TooltipContent>
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
                        <TooltipContent>Edit</TooltipContent>
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
                          <TooltipContent>Deactivate</TooltipContent>
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
                        title="No people yet"
                        description="Add team members to assign them to teams and track their work"
                        action={{
                          label: 'Add First Person',
                          onClick: () => handleOpenDialog(),
                          startIcon: <Plus className="h-4 w-4" />,
                        }}
                        size="medium"
                      />
                    ) : (
                      <EmptyState
                        title="No matches found"
                        description={`No people found matching "${searchTerm}"`}
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
                {editingPerson ? 'Edit Person' : 'Add New Person'}
              </DialogTitle>
              <DialogDescription>
                {editingPerson ? 'Update person details' : 'Add a new team member'}
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="name">Name *</Label>
                <div className="relative">
                  <User className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="name"
                    className="pl-10"
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="Enter name"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="email">Email *</Label>
                <div className="relative">
                  <Mail className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="email"
                    type="email"
                    className="pl-10"
                    value={formData.email}
                    onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                    placeholder="Enter email"
                  />
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="skills">Skills</Label>
                <div className="relative">
                  <Award className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="skills"
                    className="pl-10"
                    value={formData.skills}
                    onChange={(e) => setFormData({ ...formData, skills: e.target.value })}
                    placeholder="e.g., Java, React, TypeScript"
                  />
                </div>
                <p className="text-xs text-muted-foreground">Comma-separated list of skills</p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="avatarUrl">Avatar URL</Label>
                <Input
                  id="avatarUrl"
                  value={formData.avatarUrl}
                  onChange={(e) => setFormData({ ...formData, avatarUrl: e.target.value })}
                  placeholder="https://example.com/avatar.jpg"
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={handleCloseDialog}>
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={saving}>
                {saving && <Loader2 className="h-4 w-4 mr-2 animate-spin" />}
                {saving ? 'Saving...' : editingPerson ? 'Update' : 'Create'}
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
                  <DialogDescription>Team Assignment History</DialogDescription>
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
                  No team assignments found
                </p>
              ) : (
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead>Team</TableHead>
                      <TableHead>Cycle</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Period</TableHead>
                      <TableHead>Status</TableHead>
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
                          {new Date(assignment.startDate).toLocaleDateString()}
                          {assignment.endDate && ` - ${new Date(assignment.endDate).toLocaleDateString()}`}
                        </TableCell>
                        <TableCell>
                          <Badge 
                            variant={assignment.isActive ? 'default' : 'secondary'}
                            className={cn(assignment.isActive && 'bg-green-500 hover:bg-green-600')}
                          >
                            {assignment.isActive ? 'Active' : 'Past'}
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
                  <DialogDescription>Recent Work Activity</DialogDescription>
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
                    title="No work logs yet"
                    description={`${selectedPerson?.name} hasn't logged any work activity`}
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
                        <p className="text-xs text-muted-foreground">Total Hours</p>
                      </CardContent>
                    </Card>
                    <Card className="border">
                      <CardContent className="text-center py-3">
                        <p className="text-2xl font-bold text-purple-600">
                          {workLogs.length}
                        </p>
                        <p className="text-xs text-muted-foreground">Log Entries</p>
                      </CardContent>
                    </Card>
                    <Card className="border">
                      <CardContent className="text-center py-3">
                        <p className="text-2xl font-bold text-green-600">
                          {new Set(workLogs.map(log => log.pitchId)).size}
                        </p>
                        <p className="text-xs text-muted-foreground">Pitches Worked</p>
                      </CardContent>
                    </Card>
                  </div>

                  {/* Work Logs Table */}
                  <ScrollArea className="h-[400px]">
                    <Table>
                      <TableHeader>
                        <TableRow>
                          <TableHead>Date</TableHead>
                          <TableHead>Pitch</TableHead>
                          <TableHead>Project</TableHead>
                          <TableHead className="text-right">Hours</TableHead>
                          <TableHead>Notes</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {workLogs.slice(0, 50).map((log) => (
                          <TableRow key={log.id}>
                            <TableCell>
                              <div className="flex items-center gap-2">
                                <Clock className="h-4 w-4 text-muted-foreground" />
                                {new Date(log.date).toLocaleDateString()}
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
                      Showing first 50 of {workLogs.length} entries
                    </p>
                  )}
                </>
              )}
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setActivityDialogOpen(false)}>
                Close
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </TooltipProvider>
  );
}
