import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { formatLocalizedDate } from '../utils/dateLocalization';
import { Plus, Trash2, Pencil, UserPlus, History, Clock, ClipboardList, Loader2, Search, ArrowUpDown } from 'lucide-react';
import { teamService } from '../services/teamService';
import { personService } from '../services/personService';
import { cycleService } from '../services/cycleService';
import { workLogService } from '../services/workLogService';
import { Team, Person, Cycle, CreateTeamRequest, CreateTeamAssignmentRequest, TeamMemberRole, TeamAssignment, WorkLog } from '../types';
import EmptyState from '../components/EmptyState';
import { EmptyTeamsIllustration, EmptyWorkLogsIllustration } from '../components/illustrations';
import { useProject, useToast } from '../contexts';
import { QAFloatingButton } from '../components/QAFloatingButton';
import { getUserFriendlyError } from '../utils/errorMessages';
import { cn } from '../lib/utils';
import { TeamsSkeleton } from '../components/Skeletons';

import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { Badge } from '../components/ui/badge';
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '../components/ui/accordion';
import { Avatar, AvatarFallback } from '../components/ui/avatar';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '../components/ui/table';
import { ScrollArea } from '../components/ui/scroll-area';
import { Separator } from '../components/ui/separator';

const roles: TeamMemberRole[] = ['BACKEND', 'FRONTEND', 'QA', 'DESIGNER', 'FULLSTACK', 'TECH_LEAD', 'PRODUCT_MANAGER'];

export default function Teams() {
  const { t, i18n } = useTranslation();
  const { currentProject, isAllProjectsSelected, isSwitchingProject, notifyProjectSwitchComplete } = useProject();
  const { showToast } = useToast();
  const [teams, setTeams] = useState<Team[]>([]);
  const [cycles, setCycles] = useState<Cycle[]>([]);
  const [persons, setPersons] = useState<Person[]>([]);
  const [loading, setLoading] = useState(true);
  const [, setSaving] = useState(false);
  const [, setFieldErrors] = useState<Record<string, string>>({});
  const [searchTerm, setSearchTerm] = useState('');
  const [sortBy, setSortBy] = useState<'name' | 'members' | 'cycle'>('name');

  const [teamDialog, setTeamDialog] = useState(false);
  const [assignmentDialog, setAssignmentDialog] = useState(false);
  const [editTeamId, setEditTeamId] = useState<number | null>(null);
  const [editAssignmentId, setEditAssignmentId] = useState<number | null>(null);

  const [teamForm, setTeamForm] = useState<CreateTeamRequest>({ name: '', cycleId: undefined });
  const [assignmentForm, setAssignmentForm] = useState<CreateTeamAssignmentRequest>({ personId: 0, role: 'BACKEND', teamId: 0 });
  const [selectedPersonId, setSelectedPersonId] = useState<string>('');
  
  // Work activity dialog states
  const [activityDialogOpen, setActivityDialogOpen] = useState(false);
  const [activityPerson, setActivityPerson] = useState<{ id: number; name: string } | null>(null);
  const [workLogs, setWorkLogs] = useState<WorkLog[]>([]);
  const [loadingWorkLogs, setLoadingWorkLogs] = useState(false);

  useEffect(() => {
    const abortController = new AbortController();
    loadData();
    return () => abortController.abort();
  }, [currentProject, isAllProjectsSelected]);

  const loadData = async () => {
    try {
      setLoading(true);
      
      let cyclesPromise;
      if (isAllProjectsSelected) {
        cyclesPromise = cycleService.getMyActiveCycles();
      } else if (currentProject) {
        cyclesPromise = cycleService.getActiveByProject(currentProject.id);
      } else {
        cyclesPromise = Promise.resolve({ data: [] });
      }
      
      const [teamsRes, cyclesRes, personsData] = await Promise.all([
        teamService.getAll(),
        cyclesPromise,
        personService.getAll(true),
      ]);
      
      const cycles = cyclesRes.data;
      setCycles(cycles);
      
      // Filter teams based on cycles (which are already project-filtered)
      const cycleIds = new Set(cycles.map((c: Cycle) => c.id));
      const filteredTeams = teamsRes.data.filter((t: Team) => 
        !t.cycleId || cycleIds.has(t.cycleId)
      );
      
      setTeams(filteredTeams);
      setPersons(personsData);
    } catch (error) {
      console.error(t('teams.loadFailed'), error);
    } finally {
      setLoading(false);
      notifyProjectSwitchComplete();
    }
  };

  const handleOpenTeamDialog = (team?: Team) => {
    if (team) {
      setEditTeamId(team.id);
      setTeamForm({ name: team.name, cycleId: team.cycleId });
    } else {
      setEditTeamId(null);
      setTeamForm({ name: '', cycleId: undefined });
    }
    setFieldErrors({});
    setTeamDialog(true);
  };

  // Validate team form
  const validateTeamForm = (): boolean => {
    const errors: Record<string, string> = {};

    if (!teamForm.name.trim()) {
      errors.name = t('validation.required');
    } else if (teamForm.name.trim().length < 2) {
      errors.name = t('validation.minLength', { min: 2 });
    }

    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSaveTeam = async () => {
    if (!validateTeamForm()) {
      return;
    }

    try {
      setSaving(true);
      if (editTeamId) {
        await teamService.update(editTeamId, teamForm);
        showToast(t('teams.updateSuccess'), 'success');
      } else {
        await teamService.create(teamForm);
        showToast(t('teams.createSuccess'), 'success');
      }
      setTeamDialog(false);
      loadData();
    } catch (error) {
      showToast(getUserFriendlyError(error, t('teams.failedToSave')), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleDeleteTeam = async (id: number) => {
    try {
      await teamService.delete(id);
      showToast(t('teams.deleteSuccess'), 'success');
      loadData();
    } catch (error) {
      showToast(getUserFriendlyError(error, t('teams.failedToDelete')), 'error');
    }
  };

  const handleOpenAssignmentDialog = (teamId: number, assignment?: TeamAssignment) => {
    if (assignment) {
      setEditAssignmentId(assignment.id);
      setSelectedPersonId(assignment.personId.toString());
      setAssignmentForm({ personId: assignment.personId, role: assignment.role, teamId: assignment.teamId });
    } else {
      setEditAssignmentId(null);
      setSelectedPersonId('');
      setAssignmentForm({ personId: 0, role: 'BACKEND', teamId });
    }
    setAssignmentDialog(true);
  };

  const handleSaveAssignment = async () => {
    try {
      setSaving(true);
      if (editAssignmentId) {
        await personService.updateAssignment(editAssignmentId, assignmentForm);
        showToast(t('teams.updateAssignmentSuccess'), 'success');
      } else {
        await personService.assignToTeam(assignmentForm);
        showToast(t('teams.assignSuccess'), 'success');
      }
      setAssignmentDialog(false);
      loadData();
    } catch (error) {
      showToast(getUserFriendlyError(error, t('teams.failedToSaveAssignment')), 'error');
    } finally {
      setSaving(false);
    }
  };

  const handleEndAssignment = async (assignmentId: number) => {
    try {
      await personService.endAssignment(assignmentId);
      showToast(t('teams.removeSuccess'), 'success');
      loadData();
    } catch (error) {
      showToast(t('teams.failedToEndAssignment'), 'error');
    }
  };

  const handleViewActivity = async (personId: number, personName: string) => {
    setActivityPerson({ id: personId, name: personName });
    setActivityDialogOpen(true);
    setLoadingWorkLogs(true);
    try {
      const response = await workLogService.getByPersonId(personId);
      // Sort by date descending (most recent first)
      const sortedLogs = response.data.sort((a, b) => 
        new Date(b.date).getTime() - new Date(a.date).getTime()
      );
      setWorkLogs(sortedLogs);
    } catch (error) {
      showToast(t('teams.failedToLoadWorkLogs'), 'error');
      setWorkLogs([]);
    } finally {
      setLoadingWorkLogs(false);
    }
  };

  const getRoleClassName = (role: TeamMemberRole): string => {
    const classNames: Record<TeamMemberRole, string> = {
      BACKEND: 'bg-blue-500/10 text-blue-700 dark:text-blue-400 border-blue-500/20',
      FRONTEND: 'bg-purple-500/10 text-purple-700 dark:text-purple-400 border-purple-500/20',
      QA: 'bg-amber-500/10 text-amber-700 dark:text-amber-400 border-amber-500/20',
      DESIGNER: 'bg-green-500/10 text-green-700 dark:text-green-400 border-green-500/20',
      FULLSTACK: 'bg-cyan-500/10 text-cyan-700 dark:text-cyan-400 border-cyan-500/20',
      TECH_LEAD: 'bg-red-500/10 text-red-700 dark:text-red-400 border-red-500/20',
      PRODUCT_MANAGER: 'bg-gray-500/10 text-gray-700 dark:text-gray-400 border-gray-500/20',
    };
    return classNames[role] || '';
  };

  // Filter and sort teams
  const filteredAndSortedTeams = teams
    .filter(team => {
      if (!searchTerm) return true;
      const search = searchTerm.toLowerCase();
      return (
        team.name.toLowerCase().includes(search) ||
        team.cycleName?.toLowerCase().includes(search) ||
        team.assignments?.some(assignment =>
          assignment.personName?.toLowerCase().includes(search)
        )
      );
    })
    .sort((a, b) => {
      switch (sortBy) {
        case 'name':
          return a.name.localeCompare(b.name);
        case 'members':
          return (b.assignments?.length || 0) - (a.assignments?.length || 0);
        case 'cycle':
          return (a.cycleName || '').localeCompare(b.cycleName || '');
        default:
          return 0;
      }
    });

  if (loading || isSwitchingProject) {
    return <TeamsSkeleton />;
  }

  return (
    <div>
      <div className="flex flex-col gap-4 mb-6">
        <div className="flex flex-col sm:flex-row justify-between items-stretch sm:items-center gap-4">
          <div>
            <h1 className="text-3xl font-bold tracking-tight">{t('teams.title')}</h1>
            <p className="text-sm text-muted-foreground">
              {isAllProjectsSelected ? t('dashboard.showingAllProjects') : currentProject?.name} • {teams.length} {teams.length !== 1 ? t('teams.teams') : t('teams.team')}
            </p>
          </div>
          <Button onClick={() => handleOpenTeamDialog()} className="w-full sm:w-auto">
            <Plus className="h-4 w-4 mr-2" />
            {t('teams.newTeam')}
          </Button>
        </div>

        {/* Search and Sort Controls */}
        <div className="flex flex-col sm:flex-row gap-2">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder={t('teams.searchPlaceholder')}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10"
            />
          </div>
          <Select value={sortBy} onValueChange={(value: any) => setSortBy(value)}>
            <SelectTrigger className="w-full sm:w-[200px]">
              <ArrowUpDown className="h-4 w-4 mr-2" />
              <SelectValue placeholder={t('teams.sortBy')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">{t('teams.sortByName')}</SelectItem>
              <SelectItem value="members">{t('teams.sortByMembers')}</SelectItem>
              <SelectItem value="cycle">{t('teams.sortByCycle')}</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mb-6">
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">{t('teams.totalTeams')}</p>
            <p className="text-4xl font-bold">{teams.length}</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-sm text-muted-foreground mb-1">{t('teams.totalMembers')}</p>
            <p className="text-4xl font-bold">
              {teams.reduce((sum, t) => sum + (t.assignments?.length || 0), 0)}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Teams List */}
      {filteredAndSortedTeams.length === 0 ? (
        <Card>
          <CardContent className="py-8">
            <EmptyState
              illustration={<EmptyTeamsIllustration />}
              title={searchTerm ? t('teams.noTeamsFound') : t('teams.noTeams')}
              description={
                searchTerm
                  ? t('teams.tryDifferentSearch')
                  : t('teams.noTeamsDescription')
              }
              action={
                !searchTerm
                  ? {
                      label: t('teams.createTeam'),
                      onClick: () => handleOpenTeamDialog(),
                      startIcon: <Plus className="h-4 w-4" />,
                    }
                  : undefined
              }
              size="medium"
            />
          </CardContent>
        </Card>
      ) : (
        <Accordion type="multiple" defaultValue={filteredAndSortedTeams.map(t => t.id.toString())} className="space-y-2">
          {filteredAndSortedTeams.map((team) => (
            <AccordionItem key={team.id} value={team.id.toString()} className="border rounded-lg px-4">
              <AccordionTrigger className="hover:no-underline py-4">
                <div className="flex items-center gap-3 flex-1 pr-4">
                  <span className="text-lg font-semibold">{team.name}</span>
                  <Badge variant="outline" className="font-normal">
                    {team.assignments?.length || 0} {t('teams.membersCount')}
                  </Badge>
                  <div className="flex-1" />
                  <div
                    role="button"
                    tabIndex={0}
                    className="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-8 w-8"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleOpenTeamDialog(team);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        e.stopPropagation();
                        handleOpenTeamDialog(team);
                      }
                    }}
                    aria-label={t('teams.editTeam')}
                  >
                    <Pencil className="h-4 w-4" aria-hidden="true" />
                  </div>
                  <div
                    role="button"
                    tabIndex={0}
                    className="inline-flex items-center justify-center rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 hover:bg-accent hover:text-accent-foreground h-8 w-8 text-destructive hover:text-destructive"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDeleteTeam(team.id);
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' || e.key === ' ') {
                        e.preventDefault();
                        e.stopPropagation();
                        handleDeleteTeam(team.id);
                      }
                    }}
                    aria-label={t('teams.deleteTeam')}
                  >
                    <Trash2 className="h-4 w-4" aria-hidden="true" />
                  </div>
                </div>
              </AccordionTrigger>
              <AccordionContent>
                <div className="flex justify-end mb-4">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleOpenAssignmentDialog(team.id)}
                  >
                    <Plus className="h-4 w-4 mr-2" />
                    {t('teams.addMember')}
                  </Button>
                </div>
                {!team.assignments || team.assignments.length === 0 ? (
                  <EmptyState
                    icon={UserPlus}
                    title={t('teams.noMembersYet')}
                    description={t('teams.addMembersToStart')}
                    size="small"
                    compact
                  />
                ) : (
                  <div className="space-y-1">
                    {team.assignments.map((assignment, index) => (
                      <div key={assignment.id}>
                        <div
                          className="flex items-center justify-between p-3 rounded-lg cursor-pointer hover:bg-muted/50 transition-colors"
                          onClick={() => handleViewActivity(assignment.personId, assignment.personName || 'Unknown')}
                        >
                          <div className="flex items-center gap-3">
                            <Avatar className="h-7 w-7">
                              <AvatarFallback className="text-xs">
                                {(assignment.personName || 'U').charAt(0)}
                              </AvatarFallback>
                            </Avatar>
                            <div>
                              <p className="text-sm font-medium">{assignment.personName}</p>
                              <Badge 
                                variant="outline" 
                                className={cn('mt-1 text-xs', getRoleClassName(assignment.role))}
                              >
                                {assignment.role.replace('_', ' ')}
                              </Badge>
                            </div>
                          </div>
                          <div className="flex items-center gap-1">
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleViewActivity(assignment.personId, assignment.personName || 'Unknown');
                              }}
                              title={t('teams.viewActivity')}
                            >
                              <History className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleOpenAssignmentDialog(team.id, assignment);
                              }}
                              aria-label={t('teams.editAssignment')}
                            >
                              <Pencil className="h-4 w-4" aria-hidden="true" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-destructive hover:text-destructive"
                              onClick={(e) => {
                                e.stopPropagation();
                                handleEndAssignment(assignment.id);
                              }}
                              aria-label={t('teams.removeMember')}
                            >
                              <Trash2 className="h-4 w-4" aria-hidden="true" />
                            </Button>
                          </div>
                        </div>
                        {index < team.assignments!.length - 1 && <Separator />}
                      </div>
                    ))}
                  </div>
                )}
              </AccordionContent>
            </AccordionItem>
          ))}
        </Accordion>
      )}

      {/* Team Dialog */}
      <Dialog open={teamDialog} onOpenChange={setTeamDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editTeamId ? t('teams.editTeam') : t('teams.newTeam')}</DialogTitle>
            <DialogDescription>
              {editTeamId ? t('teams.updateTeamDetails') : t('teams.createNewTeam')}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="team-name">{t('teams.teamName')}</Label>
              <Input
                id="team-name"
                value={teamForm.name}
                onChange={(e) => setTeamForm({ ...teamForm, name: e.target.value })}
                placeholder={t('teams.enterTeamName')}
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="cycle">{t('teams.cycleOptional')}</Label>
              <Select
                value={teamForm.cycleId?.toString() || 'none'}
                onValueChange={(value) => setTeamForm({ ...teamForm, cycleId: value === 'none' ? undefined : parseInt(value) })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('teams.selectACycle')} />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">{t('teams.none')}</SelectItem>
                  {cycles.map((c) => (
                    <SelectItem key={c.id} value={c.id.toString()}>
                      {c.name}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setTeamDialog(false)}>
              {t('teams.cancel')}
            </Button>
            <Button onClick={handleSaveTeam} disabled={!teamForm.name}>
              {editTeamId ? t('teams.update') : t('common.create')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Assignment Dialog */}
      <Dialog open={assignmentDialog} onOpenChange={setAssignmentDialog}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>{editAssignmentId ? t('teams.editAssignment') : t('teams.addTeamMember')}</DialogTitle>
            <DialogDescription>
              {editAssignmentId ? t('teams.updateMemberAssignment') : t('teams.addPersonToTeam')}
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="person">{t('teams.person')}</Label>
              <Select
                value={selectedPersonId}
                onValueChange={(value) => {
                  setSelectedPersonId(value);
                  setAssignmentForm({ ...assignmentForm, personId: parseInt(value) });
                }}
                disabled={!!editAssignmentId}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('teams.selectAPerson')} />
                </SelectTrigger>
                <SelectContent>
                  {persons.map((person) => (
                    <SelectItem key={person.id} value={person.id.toString()}>
                      {person.name} {person.email ? `(${person.email})` : t('teams.noEmail')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
            <div className="grid gap-2">
              <Label htmlFor="role">{t('teams.role')}</Label>
              <Select
                value={assignmentForm.role}
                onValueChange={(value) => setAssignmentForm({ ...assignmentForm, role: value as TeamMemberRole })}
              >
                <SelectTrigger>
                  <SelectValue placeholder={t('teams.selectARole')} />
                </SelectTrigger>
                <SelectContent>
                  {roles.map((role) => (
                    <SelectItem key={role} value={role}>
                      {role.replace('_', ' ')}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setAssignmentDialog(false)}>
              {t('teams.cancel')}
            </Button>
            <Button onClick={handleSaveAssignment} disabled={!assignmentForm.personId}>
              {editAssignmentId ? t('teams.update') : t('teams.add')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Work Activity Dialog */}
      <Dialog open={activityDialogOpen} onOpenChange={setActivityDialogOpen}>
        <DialogContent className="sm:max-w-3xl max-h-[90vh]">
          <DialogHeader>
            <div className="flex items-center gap-3">
              <Avatar className="h-10 w-10">
                <AvatarFallback className="bg-primary text-primary-foreground">
                  {activityPerson?.name.charAt(0)}
                </AvatarFallback>
              </Avatar>
              <div>
                <DialogTitle>{activityPerson?.name}</DialogTitle>
                <DialogDescription>{t('teams.recentWorkActivity')}</DialogDescription>
              </div>
            </div>
          </DialogHeader>
          
          {loadingWorkLogs ? (
            <div className="flex justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            </div>
          ) : workLogs.length === 0 ? (
            <div className="py-8">
              <EmptyState
                illustration={<EmptyWorkLogsIllustration />}
                title={t('teams.noWorkLogs')}
                description={t('teams.hasntLoggedWork', { name: activityPerson?.name })}
                size="small"
                compact
              />
            </div>
          ) : (
            <>
              {/* Summary Stats */}
              <div className="grid grid-cols-3 gap-3 mb-4">
                <Card className="border">
                  <CardContent className="py-3 text-center">
                    <p className="text-2xl font-bold text-primary">
                      {workLogs.reduce((sum, log) => sum + log.hoursSpent, 0).toFixed(1)}
                    </p>
                    <p className="text-xs text-muted-foreground">{t('teams.totalHours')}</p>
                  </CardContent>
                </Card>
                <Card className="border">
                  <CardContent className="py-3 text-center">
                    <p className="text-2xl font-bold text-purple-600 dark:text-purple-400">
                      {workLogs.length}
                    </p>
                    <p className="text-xs text-muted-foreground">{t('teams.logEntries')}</p>
                  </CardContent>
                </Card>
                <Card className="border">
                  <CardContent className="py-3 text-center">
                    <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                      {new Set(workLogs.map(log => log.pitchId)).size}
                    </p>
                    <p className="text-xs text-muted-foreground">{t('teams.pitchesWorked')}</p>
                  </CardContent>
                </Card>
              </div>

              {/* Work Logs Table */}
              <ScrollArea className="h-[400px] rounded-md border">
                <Table>
                  <TableHeader className="sticky top-0 bg-background">
                    <TableRow>
                      <TableHead>{t('teams.date')}</TableHead>
                      <TableHead>{t('teams.pitch')}</TableHead>
                      <TableHead>{t('teams.project')}</TableHead>
                      <TableHead className="text-right">{t('teams.hours')}</TableHead>
                      <TableHead>{t('teams.notes')}</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {workLogs.slice(0, 50).map((log) => (
                      <TableRow key={log.id}>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <Clock className="h-4 w-4 text-muted-foreground" />
                            <span className="text-sm">{formatLocalizedDate(new Date(log.date), i18n.language)}</span>
                          </div>
                        </TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <ClipboardList className="h-4 w-4 text-primary" />
                            <span className="text-sm font-medium">
                              {log.pitchTitle || `Pitch #${log.pitchId}`}
                            </span>
                          </div>
                        </TableCell>
                        <TableCell>
                          {log.projectKey ? (
                            <Badge variant="outline">{log.projectKey}</Badge>
                          ) : (
                            <span className="text-sm text-muted-foreground">
                              {log.projectName || '-'}
                            </span>
                          )}
                        </TableCell>
                        <TableCell className="text-right">
                          <Badge 
                            variant={log.hoursSpent >= 4 ? 'default' : 'secondary'}
                            className={log.hoursSpent >= 4 ? 'bg-green-500/10 text-green-700 dark:text-green-400' : ''}
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
                  {t('teams.showingFirstEntries', { count: workLogs.length })}
                </p>
              )}
            </>
          )}
          
          <DialogFooter>
            <Button variant="outline" onClick={() => setActivityDialogOpen(false)}>
              {t('teams.close')}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <QAFloatingButton contextType="team" />
    </div>
  );
}
