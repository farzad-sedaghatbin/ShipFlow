// Pagination
// Spring Boot 3.3+ with PageSerializationMode.VIA_DTO puts totalElements/totalPages
// inside a nested `page` object. All fields except `content` and `page` are optional
// to remain compatible with both the legacy flat format and VIA_DTO format.
export interface Page<T> {
  content: T[];
  // Spring Data VIA_DTO format (Boot 3.3+ / Spring Data 3.3+)
  page?: {
    size: number;
    number: number;
    totalElements: number;
    totalPages: number;
  };
  // Legacy flat fields (Boot ≤ 3.2, kept optional for backward compat)
  pageable?: {
    pageNumber: number;
    pageSize: number;
    sort: { sorted: boolean; unsorted: boolean; empty: boolean };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages?: number;
  totalElements?: number;
  last?: boolean;
  size?: number;
  number?: number;
  sort?: { sorted: boolean; unsorted: boolean; empty: boolean };
  numberOfElements?: number;
  first?: boolean;
  empty?: boolean;
}

/** Extract totalElements from either Spring Data page format. */
export function getPageTotal<T>(p: Page<T>): number {
  return p.page?.totalElements ?? p.totalElements ?? 0;
}

/** Extract totalPages from either Spring Data page format. */
export function getPageCount<T>(p: Page<T>): number {
  return p.page?.totalPages ?? p.totalPages ?? 0;
}

// Enums
export type CyclePhase = 'SHAPING_BUILDING' | 'BETTING_COOLDOWN';
/**
 * Shape Up pitch lifecycle statuses.
 * Pre-cycle: IDEA → DRAFT → SHAPED (no cycle required)
 * In-cycle: PENDING → STARTED → ... → DONE (requires cycle)
 */
export type PitchStatus = 'IDEA' | 'DRAFT' | 'PENDING' | 'SHAPED' | 'STARTED' | 'IN_PROGRESS' | 'TESTING' | 'DONE' | 'COOLDOWN' | 'CANCELLED';
export type TeamMemberRole = 'BACKEND' | 'FRONTEND' | 'MOBILE' | 'QA' | 'DESIGNER' | 'FULLSTACK' | 'TECH_LEAD' | 'PRODUCT_MANAGER';
export type MeetingType = 'SHAPING' | 'BETTING' | 'KICKOFF' | 'STANDUP' | 'DEMO' | 'RETROSPECTIVE' | 'HILL_CHART_REVIEW';
export type TaskStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskCategory = 'PITCH_SCOPE' | 'DEBT_IMPROVEMENT';
export type DependencyType = 'BLOCKS' | 'DEPENDS_ON' | 'RELATED_TO';
export type RetroStatus = 'DRAFT' | 'OPEN' | 'CLOSED';
export type RetroColumnType = 'WENT_WELL' | 'DID_NOT_GO_WELL' | 'TRY_NEXT' | 'ACTIONS';
export type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

/** Business value priority level */
export type BusinessValue = 'HIGH' | 'MEDIUM' | 'LOW';

/** Request to batch-reorder entities by sort order */
export interface ReorderRequest {
  items: { id: number; sortOrder: number }[];
}

/**
 * Project methodology type.
 * SHAPE_UP: 6-week cycles with betting, pitches, and cooldown
 * KANBAN: Continuous flow with visual board, no cycles
 * SCRUM: Sprint-based development with backlog, story points, burndown, and velocity
 */
export type ProjectType = 'SHAPE_UP' | 'KANBAN' | 'SCRUM';

/**
 * Role a user can have within a specific project.
 * VIEWER: Read-only access
 * CONTRIBUTOR: Can create/edit items
 * MANAGER: Full project access including team management
 */
export type ProjectRole = 'VIEWER' | 'CONTRIBUTOR' | 'MANAGER';

// Project DTOs
export interface Project {
  id: number;
  name: string;
  projectKey: string;
  description?: string;
  color?: string;
  logoUrl?: string;
  ownerId?: number;
  ownerName?: string;
  isActive: boolean;
  enableRetrospectives?: boolean;
  /**
   * Project methodology type - determines available features and navigation.
   */
  projectType: ProjectType;
  createdAt: string;
  updatedAt?: string;
  cycleCount?: number;
  activeCycleCount?: number;
  /**
   * Current user's role within this project.
   * Null if user has access via ADMIN role or team membership only.
   */
  userProjectRole?: ProjectRole;
}

/**
 * A user's direct assignment to a project
 */
export interface ProjectMember {
  userId: number;
  username: string;
  email?: string;
  projectRole: ProjectRole;
  grantedAt: string;
  grantedByUsername?: string;
}

export interface CreateProjectRequest {
  name: string;
  projectKey: string;
  description?: string;
  color?: string;
  logoUrl?: string;
  ownerId?: number;
  /**
   * Project methodology type. Defaults to SHAPE_UP if not provided.
   */
  projectType?: ProjectType;
}

// Cycle DTOs
export interface Cycle {
  id: number;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  name: string;
  startDate: string;
  endDate: string;
  phase: CyclePhase;
  isActive: boolean;
  pitchCount?: number;
  /** For Scrum sprints: total number of tasks (stories) in this sprint */
  taskCount?: number;
  teamCount?: number;
  projectType?: ProjectType;
  /** Scrum: the goal statement for this sprint */
  sprintGoal?: string | null;
  // velocityActual removed — computed dynamically via VelocityService; see VelocityPoint
}

export interface CreateCycleRequest {
  projectId: number;
  name: string;
  startDate: string;
  endDate?: string;  // Optional - auto-calculated from OrganizationSettings if not provided
  phase?: CyclePhase;
  /** Scrum: the goal statement for this sprint */
  sprintGoal?: string | null;
}

export interface Team {
  id: number;
  name: string;
  isArchived?: boolean;
  assignments?: TeamAssignment[];
  // Capacity overrides
  hoursPerDayOverride?: number;
  workingDaysPerWeekOverride?: number;
  effectiveHoursPerDay?: number;
  effectiveWorkingDaysPerWeek?: number;
}

export interface CreateTeamRequest {
  name: string;
  hoursPerDayOverride?: number;
  workingDaysPerWeekOverride?: number;
}

// Person - independent entity that persists across cycles
export interface Person {
  id: number;
  name: string;
  email: string;
  skills?: string;
  avatarUrl?: string;
  isActive: boolean;
  createdAt: string;
  // Capacity override
  hoursPerDayOverride?: number;
  currentAssignments?: TeamAssignment[];
  pastAssignments?: TeamAssignment[];
}

export interface CreatePersonRequest {
  name: string;
  email: string;
  skills?: string;
  avatarUrl?: string;
  // Optional user account creation
  createUser?: boolean;
  username?: string;
  password?: string;
  userRole?: UserRole;
}

// TeamAssignment - links Person to Team with role and date range
export interface TeamAssignment {
  id: number;
  personId: number;
  personName?: string;
  teamId: number;
  teamName?: string;
  cycleId?: number;
  cycleName?: string;
  role: TeamMemberRole;
  startDate: string;
  endDate?: string;
  isActive: boolean;
  notes?: string;
  // Capacity override
  hoursPerDayOverride?: number;
  effectiveHoursPerDay?: number;
  capacitySource?: 'ORGANIZATION' | 'TEAM' | 'PERSON' | 'ASSIGNMENT';
}

export interface CreateTeamAssignmentRequest {
  personId: number;
  teamId: number;
  role: TeamMemberRole;
  startDate?: string;
  endDate?: string;
  notes?: string;
  hoursPerDayOverride?: number;
}

export interface Pitch {
  id: number;
  title: string;
  description?: string;
  /** Appetite in days - nullable for IDEA/DRAFT statuses */
  appetiteDays?: number;
  /** Cycle ID - nullable for pre-cycle statuses (IDEA, DRAFT, SHAPED) */
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  teamId?: number;
  teamName?: string;
  status: PitchStatus;
  createdAt: string;
  updatedAt: string;
  totalHoursSpent?: number;
  appetiteHours?: number;
  progressPercentage?: number;
  // Team Capacity and Budget
  teamMemberCount?: number;
  totalBudgetPersonDays?: number;
  budgetUtilizationPercent?: number;
  busiestPerson?: BusiestPerson;
  // Circuit Breaker
  isCircuitBreakerTriggered?: boolean;
  circuitBreakerReason?: string;
  circuitBreakerDate?: string;
  // Shape Up Methodology Fields
  problemStatement?: string;
  solution?: string;
  rabbitHoles?: string;
  risks?: string;
  noGos?: string;
  wireframeLinks?: string;
  // Epic and Release linking
  epicId?: number;
  epicName?: string;
  targetReleaseId?: number;
  targetReleaseName?: string;
  targetReleaseVersion?: string;
  // Prioritization
  priority?: BusinessValue;
  sortOrder?: number;
  // Roadmap dependencies
  blockingPitches?: PitchDependency[];
  blockedByPitches?: PitchDependency[];
}

export interface BusiestPerson {
  personId: number;
  personName: string;
  role?: string;
  hoursPerDay: number;
  capacitySource: string; // 'organization' | 'team' | 'person' | 'assignment'
  totalBudgetHours: number;
  hoursSpent: number;
  utilizationPercent: number;
  isOverBudget: boolean;
}

export interface CreatePitchRequest {
  title: string;
  description?: string;
  /** Appetite in days - required for SHAPED and above statuses */
  appetiteDays?: number;
  /** Cycle ID - required for PENDING and above statuses */
  cycleId?: number;
  teamId?: number;
  // Epic and Release linking (roadmap)
  epicId?: number;
  targetReleaseId?: number;
  status?: PitchStatus;
  // Shape Up Methodology Fields
  problemStatement?: string;
  solution?: string;
  rabbitHoles?: string;
  risks?: string;
  noGos?: string;
  wireframeLinks?: string;
  // Prioritization
  priority?: BusinessValue;
  sortOrder?: number;
}

/**
 * Lightweight request for creating an idea.
 * Ideas are raw concepts that don't require shaping details yet.
 */
export interface CreateIdeaRequest {
  title: string;
  description?: string;
  epicId?: number;
}

/**
 * Shape Up workflow utility functions
 */
export const PitchStatusUtils = {
  /** Pre-cycle statuses where pitches don't have a cycle assignment */
  preCycleStatuses: ['IDEA', 'DRAFT', 'SHAPED'] as const,
  
  /** In-cycle statuses where pitches are assigned to a cycle */
  inCycleStatuses: ['PENDING', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE', 'COOLDOWN', 'CANCELLED'] as const,
  
  /** Check if a pitch is in a pre-cycle status */
  isPreCycle: (status: PitchStatus): boolean => 
    ['IDEA', 'DRAFT', 'SHAPED'].includes(status),
  
  /** Check if a pitch requires shaping (IDEA or DRAFT) */
  requiresShaping: (status: PitchStatus): boolean => 
    ['IDEA', 'DRAFT'].includes(status),
  
  /** Check if a pitch is ready for the betting table */
  isReadyForBetting: (status: PitchStatus): boolean => 
    status === 'SHAPED',
  
  /** Check if a pitch is in progress (active work) */
  isInProgress: (status: PitchStatus): boolean => 
    ['STARTED', 'IN_PROGRESS', 'TESTING'].includes(status),
  
  /** Check if a pitch is complete */
  isComplete: (status: PitchStatus): boolean => 
    ['DONE', 'CANCELLED'].includes(status),

  /** Get display color for status */
  getStatusColor: (status: PitchStatus): string => {
    switch (status) {
      case 'IDEA': return '#9e9e9e'; // grey
      case 'DRAFT': return '#64b5f6'; // light blue
      case 'SHAPED': return '#4caf50'; // green
      case 'PENDING': return '#ff9800'; // orange
      case 'STARTED': return '#2196f3'; // blue
      case 'IN_PROGRESS': return '#1976d2'; // darker blue
      case 'TESTING': return '#9c27b0'; // purple
      case 'DONE': return '#388e3c'; // dark green
      case 'COOLDOWN': return '#607d8b'; // blue grey
      case 'CANCELLED': return '#f44336'; // red
      default: return '#9e9e9e';
    }
  }
};

// Response from AI pitch data extraction
export interface ExtractedPitchData {
  title?: string;
  problemStatement?: string;
  solution?: string;
  rabbitHoles?: string;
  risks?: string;
  noGos?: string;
  appetiteDays?: number;
  wireframeLinks?: string;
  extractionSuccessful: boolean;
  errorMessage?: string;
  documentId?: number; // ID of saved document
}

export interface WorkLog {
  id: number;
  personId: number;
  personName?: string;
  pitchId?: number;
  pitchTitle?: string;
  taskId?: number;
  taskTitle?: string;
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  date: string;
  hoursSpent: number;
  note?: string;
}

export interface CreateWorkLogRequest {
  personId: number;
  pitchId?: number;
  taskId?: number;
  date: string;
  hoursSpent: number;
  note?: string;
}

// For users creating work logs for themselves (no personId required)
export interface CreateWorkLogForSelfRequest {
  pitchId?: number;
  taskId?: number;
  date: string;
  hoursSpent: number;
  note?: string;
}

export interface WorkLogSummary {
  todayHours: number;
  todayCount: number;
  totalHours: number;
  totalCount: number;
}

export interface WorkLogPersonSummary {
  personId: number;
  personName: string;
  totalHours: number;
  entryCount: number;
}

export interface MeetingAction {
  id?: number;
  description: string;
  assignedToId?: number;
  assignedToName?: string;
  status: ActionStatus;
  dueDate?: string;
  notes?: string;
}

export interface Meeting {
  id: number;
  pitchId?: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  type: MeetingType;
  dateHeld: string;
  dorReady: boolean;
  dodReady: boolean;
  dorItems?: MeetingChecklistItem[];
  dodItems?: MeetingChecklistItem[];
  notes?: string;
  retrospectiveId?: number;
  retrospectiveTitle?: string;
  decisions?: string;
  attendees?: string;
  actions?: MeetingAction[];
}

export interface MeetingChecklistItem {
  id?: number;
  name: string;
  description: string;
  isRequired: boolean;
  isCompleted: boolean;
}

export interface CreateMeetingRequest {
  pitchId?: number;
  projectId?: number;
  type: MeetingType;
  dateHeld: string;
  dorReady?: boolean;
  dodReady?: boolean;
  dorItems?: MeetingChecklistItem[];
  dodItems?: MeetingChecklistItem[];
  notes?: string;
  retrospectiveId?: number;
  decisions?: string;
  attendees?: string;
  actions?: MeetingAction[];
}

export interface Evidence {
  id: number;
  pitchId: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  personId: number;
  personName?: string;
  date: string;
  description: string;
  fileUrl?: string;
}

export interface CreateEvidenceRequest {
  pitchId: number;
  personId: number;
  date: string;
  description: string;
  fileUrl?: string;
}

// Report DTOs
export interface RiskDistribution {
  lowRiskCount: number;
  mediumRiskCount: number;
  highRiskCount: number;
  criticalRiskCount: number;
  averageRiskScore: number;
  maxRiskScore: number;
  minRiskScore: number;
}

export interface EnhancedCycleReport {
  cycleId: number;
  cycleName: string;
  projectName?: string;
  startDate: string;
  endDate: string;
  
  // Pitch metrics
  totalPitches: number;
  completedPitches: number;
  inProgressPitches: number;
  notStartedPitches: number;
  
  // Hours and efficiency
  totalAppetiteHours: number;
  totalActualHours: number;
  varianceHours: number;
  variancePercentage: number;
  efficiencyPercentage: number;
  
  // Out-of-scope work (Tasks)
  totalTasks: number;
  completedTasks: number;
  totalTaskEstimateHours: number;
  totalTaskActualHours: number;
  
  // Risk distribution
  riskDistribution: RiskDistribution;
  
  // Team member statistics
  totalTeamMembers: number;
  averageHoursPerMember: number;
  maxHoursPerMember: number;
  minHoursPerMember: number;
  
  // Detailed breakdowns
  pitchReports: PitchReport[];
  memberReports: MemberWorkReport[];
  
  // Top performers and risks
  topPerformers: string[];
  overBudgetPitches: string[];
  
  // v0.5 - Insight, Not Metrics
  signals?: CycleSignalsDTO;            // Decision support signals
  narrativeSummary?: CycleSummaryDTO;   // AI-generated or template narrative
}

// v0.5 - Signal and Narrative DTOs for enhanced reports
export interface CycleSignalsDTO {
  cycleId: number | null;
  cycleName: string | null;
  projectId: number;
  projectName: string;
  analyzedAt: string;
  overallHealthScore: number;
  appetiteAccuracy: AppetiteAccuracyDTO | null;
  shapingPattern: ShapingPatternDTO | null;
  riskCorrelation: RiskCorrelationDTO | null;
  retroFollowThrough: RetroFollowThroughDTO | null;
}

export interface AppetiteAccuracyDTO {
  trend: string;
  averageVariancePercent: number;
  cyclesAnalyzed: number;
  interpretation: string;
  recommendations: string[];
}

export interface ShapingPatternDTO {
  overallQuality: string;
  overShapedCount: number;
  underShapedCount: number;
  accuratelyShapedCount: number;
  interpretation: string;
  recommendations: string[];
}

export interface RiskCorrelationDTO {
  predictiveAccuracyPercent: number;
  pitchesAnalyzed: number;
  interpretation: string;
  recommendations: string[];
}

export interface RetroFollowThroughDTO {
  followThroughRatePercent: number;
  totalActionItems: number;
  actedOnCount: number;
  pendingCount: number;
  trend: string;
  interpretation: string;
  recommendations: string[];
}

export interface CycleSummaryDTO {
  cycleId: number;
  cycleName: string;
  projectName?: string;
  startDate: string;
  endDate: string;
  phase?: string;
  
  // Narrative sections
  whatWeBet: CycleNarrativeDTO | null;
  whatShipped: CycleNarrativeDTO | null;
  whatWeCut: CycleNarrativeDTO | null;
  surprises: CycleNarrativeDTO | null;
  fullSummary: CycleNarrativeDTO | null;
  
  // Quick stats for context
  totalPitchesCommitted?: number;
  pitchesShipped?: number;
  pitchesCut?: number;
  pitchesInProgress?: number;
  averageAppetiteAccuracy?: number;
  
  // Generation metadata
  generatedAt: string;
  aiGenerated?: boolean;
  exportFormats?: string[];
}

export interface CycleNarrativeDTO {
  id: number;
  cycleId: number;
  narrativeType: 'WHAT_WE_BET' | 'WHAT_SHIPPED' | 'WHAT_WE_CUT' | 'SURPRISES' | 'FULL_SUMMARY';
  content: string;
  isAiGenerated: boolean;
  generatedAt: string;
}

export interface CycleReport {
  cycleId: number;
  cycleName: string;
  totalPitches: number;
  completedPitches: number;
  inProgressPitches: number;
  totalAppetiteHours: number;
  totalActualHours: number;
  efficiencyPercentage: number;
  
  // Out-of-scope work (Tasks not associated with pitches)
  totalTasks: number;
  completedTasks: number;
  totalTaskEstimateHours: number;
  totalTaskActualHours: number;
  
  pitchReports: PitchReport[];
  memberReports: MemberWorkReport[];
}

export interface PitchReport {
  pitchId: number;
  pitchTitle: string;
  teamName: string;
  status: PitchStatus;
  appetiteDays: number;
  appetiteHours: number;
  actualHours: number;
  varianceHours: number;
  variancePercentage: number;
  isOverBudget: boolean;
}

export interface MemberWorkReport {
  memberId: number;
  memberName: string;
  role: TeamMemberRole;
  teamName: string;
  totalHours: number;
  workDays: number;
  avgHoursPerDay: number;
  pitchWork: PitchWorkSummary[];
}

export interface PitchWorkSummary {
  pitchId: number;
  pitchTitle: string;
  hoursSpent: number;
}

// User/Auth Types
// 4-tier role model:
// ADMIN: Full system access - can manage users, settings, permissions
// MANAGER: Can manage cycles, pitches, teams, approve bets
// MEMBER: Can create/update own work, contribute to pitches/tasks
// READONLY: Read-only access across all resources
export type UserRole = 'ADMIN' | 'MANAGER' | 'MEMBER' | 'READONLY';

export interface User {
  id: number;
  username: string;
  email?: string;
  role: UserRole;
  personId?: number;
  personName?: string;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
}

export interface UserProfile {
  id: number;
  username: string;
  email?: string;
  role: UserRole;
  isActive: boolean;
  createdAt: string;
  updatedAt?: string;
  personId?: number;
  personName?: string;
  avatarUrl?: string;
  department?: string;
  bio?: string;
  skills?: string;
}

export interface UpdateProfileRequest {
  email?: string;
  avatarUrl?: string;
  bio?: string;
  skills?: string;
  department?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface CreateUserRequest {
  username: string;
  password: string;
  email?: string;
  role: UserRole;
  personId?: number;
  projectIds?: number[];
}

export interface HillChartPoint {
  id: number;
  pitchId: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  scope: string;
  description: string;
  position: number; // 0-50 uphill (figuring out), 50-100 downhill (executing)
  
  // Scope-Task Bridge fields
  linkedTaskId?: number;
  linkedTaskTitle?: string;
  autoProgressEnabled?: boolean;
  suggestedPosition?: number;
  
  createdAt: string;
  updatedAt: string;
}

export interface CreateHillChartPointRequest {
  pitchId: number;
  scope: string;
  description: string;
  position: number;
  createTaskAutomatically?: boolean; // When true, auto-creates a linked Task (default: true)
  assigneeId?: number; // Optional assignee for the auto-created task
}

export interface UpdateHillChartPointRequest {
  scope?: string;
  description?: string;
  position?: number;
}

// Notification User Mapping
export interface NotificationUserMapping {
  id: number;
  personId: number;
  providerName: string;
  externalUserId: string;
}

// Task DTOs
export interface Task {
  id: number;
  title: string;
  description?: string;
  status: TaskStatus;
  priority: TaskPriority;
  category?: TaskCategory;
  estimateHours?: number;
  actualHours?: number;
  cycleId: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  
  // Pitch and Scope relationships
  pitchId?: number;
  pitchTitle?: string;
  scopeId?: number;
  scopeName?: string;
  
  // Auto-created scope for root tasks linked to pitch (Scope-Task Bridge)
  autoCreatedScopeId?: number;
  showOnHillChart?: boolean;

  teamId?: number;
  teamName?: string;

  assigneeId?: number;
  assigneeName?: string;
  assigneeAvatarUrl?: string;
  pairAssigneeId?: number;
  pairAssigneeName?: string;
  pairAssigneeAvatarUrl?: string;
  createdById?: number;
  createdByName?: string;
  parentTaskId?: number;
  parentTaskTitle?: string;
  children?: Task[];
  dueDate?: string;
  completedAt?: string;
  createdAt: string;
  updatedAt: string;
  tags?: string;
  commentCount?: number;
  
  // Manual ordering (drag-to-reorder)
  sortOrder?: number;

  // Dependency information
  blockingTasks?: TaskDependency[];
  blockedByTasks?: TaskDependency[];
  blockedByCount?: number;
  isBlocked?: boolean;
  
  // Release linking
  targetReleaseId?: number;
  targetReleaseName?: string;
  targetReleaseVersion?: string;

  // File attachments
  attachments?: TaskAttachment[];

  // Scrum: story point estimate for this task
  storyPoints?: number | null;
}

export interface TaskAttachment {
  id: number;
  taskId: number;
  fileName: string;
  fileSize: number;
  contentType: string;
  uploadedById?: number;
  uploadedByUsername?: string;
  createdAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  cycleId: number;
  pitchId?: number;
  status?: TaskStatus;
  priority?: TaskPriority;
  category?: TaskCategory;
  estimateHours?: number;
  actualHours?: number;
  teamId?: number;
  assigneeId?: number;
  pairAssigneeId?: number;
  parentTaskId?: number;
  dueDate?: string;
  tags?: string;
  
  // Scope-Task Bridge: scope auto-creation is decided entirely backend-side
  // (root task + pitch + no existing scope). Clients cannot toggle it.
  initialHillPosition?: number; // Initial hill chart position (0-100) applied to the auto-created scope

  // Scrum: story point estimate
  storyPoints?: number | null;
}

// Scrum Chart DTOs
export interface BurndownPoint {
  date: string;
  remainingPoints: number;
  idealPoints: number;
}

export interface VelocityPoint {
  cycleId: number;
  cycleName: string;
  plannedPoints: number;
  completedPoints: number;
}

// Task Dependency Types
export interface TaskDependency {
  id: number;
  sourceTaskId: number;
  sourceTaskTitle: string;
  targetTaskId: number;
  targetTaskTitle: string;
  dependencyType: DependencyType;
  createdAt: string;
}

export interface CreateTaskDependencyRequest {
  targetTaskId: number;
  dependencyType?: DependencyType;
}

export interface TaskDependencies {
  blocking: TaskDependency[];
  blockedBy: TaskDependency[];
}

// Pitch Dependency Types
export interface PitchDependency {
  id: number;
  sourcePitchId: number;
  sourcePitchTitle: string;
  targetPitchId: number;
  targetPitchTitle: string;
  dependencyType: DependencyType;
  createdAt: string;
}

export interface PitchDependencies {
  blocking: PitchDependency[];
  blockedBy: PitchDependency[];
}

// Epic Dependency Types
export interface EpicDependency {
  id: number;
  sourceEpicId: number;
  sourceEpicName: string;
  targetEpicId: number;
  targetEpicName: string;
  dependencyType: DependencyType;
  createdAt: string;
}

export interface EpicDependencies {
  blocking: EpicDependency[];
  blockedBy: EpicDependency[];
}

export interface TaskStatistics {
  cycleId: number;
  cycleName: string;
  totalTasks: number;
  backlogTasks: number;
  todoTasks: number;
  inProgressTasks: number;
  blockedTasks: number;
  inReviewTasks: number;
  doneTasks: number;
  cancelledTasks: number;
  completionPercentage: number;
  totalEstimateHours: number;
  totalActualHours: number;
  avgTasksPerPerson: number;
}
// QA Test Management Types
export type TestCaseStatus = 'DRAFT' | 'READY' | 'APPROVED' | 'DEPRECATED' | 'ARCHIVED';
export type TestCasePriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TestCaseType = 'FUNCTIONAL' | 'INTEGRATION' | 'UNIT' | 'E2E' | 'REGRESSION' | 'SMOKE' | 'PERFORMANCE' | 'SECURITY' | 'USABILITY' | 'ACCESSIBILITY';
export type TestRunStatus = 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED' | 'BLOCKED' | 'SKIPPED';
export type BugSeverity = 'TRIVIAL' | 'MINOR' | 'MAJOR' | 'CRITICAL' | 'BLOCKER';
export type BugStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'VERIFIED' | 'CLOSED' | 'REOPENED' | 'WONT_FIX' | 'DUPLICATE';

export interface TestCase {
  id: number;
  testCaseKey: string;
  title: string;
  description?: string;
  preconditions?: string;
  steps?: string;
  expectedResult?: string;
  pitchId?: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  teamId?: number;
  teamName?: string;
  
  // Scope and Task relationships
  scopeId?: number;
  scopeName?: string;
  taskId?: number;
  taskTitle?: string;
  
  type: TestCaseType;
  priority: TestCasePriority;
  status: TestCaseStatus;
  aiGenerated?: boolean;
  tags?: string;
  tagList?: string[];
  estimatedMinutes?: number;
  createdById?: number;
  createdByName?: string;
  updatedById?: number;
  updatedByName?: string;
  createdAt: string;
  updatedAt: string;
  totalRuns?: number;
  passedRuns?: number;
  failedRuns?: number;
  passRate?: number;
}

export interface CreateTestCaseRequest {
  title: string;
  description?: string;
  preconditions?: string;
  steps?: string;
  expectedResult?: string;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  scopeId?: number;
  taskId?: number;
  type: TestCaseType;
  priority: TestCasePriority;
  status?: TestCaseStatus;
  tags?: string[];
  estimatedMinutes?: number;
  aiGenerated?: boolean;
}

export interface UpdateTestCaseRequest {
  title?: string;
  description?: string;
  preconditions?: string;
  steps?: string;
  expectedResult?: string;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  scopeId?: number;
  taskId?: number;
  type?: TestCaseType;
  priority?: TestCasePriority;
  status?: TestCaseStatus;
  tags?: string[];
  estimatedMinutes?: number;
}

export interface BugReport {
  id: number;
  bugKey: string;
  title: string;
  description: string;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  component?: string;

  // Direct project association
  projectId?: number;
  projectName?: string;
  projectKey?: string;

  pitchId?: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  teamId?: number;
  teamName?: string;
  testRunId?: number;
  
  // Task relationship
  taskId?: number;
  taskTitle?: string;
  
  severity: BugSeverity;
  status: BugStatus;
  tags?: string;
  tagList?: string[];
  attachments?: string;
  reporterId?: number;
  reporterName?: string;
  assigneeId?: number;
  assigneeName?: string;
  resolution?: string;
  resolvedAt?: string;
  createdAt: string;
  updatedAt: string;
  commentCount?: number;
  
  // Release tracking
  targetReleaseId?: number;
  targetReleaseName?: string;
  targetReleaseVersion?: string;
  fixedInReleaseId?: number;
  fixedInReleaseName?: string;
  fixedInReleaseVersion?: string;
  /** Whether the bug slipped to a different release than originally targeted */
  isSlipped?: boolean;
}

export interface CreateBugReportRequest {
  title: string;
  description: string;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  component?: string;
  projectId?: number;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  testRunId?: number;
  taskId?: number;
  severity: BugSeverity;
  status?: BugStatus;
  tags?: string[];
  attachments?: string;
  assigneeId?: number;
  targetReleaseId?: number;
}

export interface UpdateBugReportRequest {
  title?: string;
  description?: string;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  component?: string;
  projectId?: number;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  taskId?: number;
  severity?: BugSeverity;
  status?: BugStatus;
  tags?: string[];
  attachments?: string;
  assigneeId?: number;
  resolution?: string;
  targetReleaseId?: number;
  fixedInReleaseId?: number;
}

export interface TestRun {
  id: number;
  testCaseId: number;
  testCaseKey?: string;
  testCaseTitle?: string;
  cycleId?: number;
  cycleName?: string;
  pitchId?: number;
  pitchTitle?: string;
  status: TestRunStatus;
  executedById: number;
  executedByName?: string;
  executedAt: string;
  durationSeconds?: number;
  notes?: string;
  actualResult?: string;
  buildVersion?: string;
  environment?: string;
  attachments?: string;
  bugReportId?: number;
  bugReportKey?: string;
  createdAt: string;
}

export interface CreateTestRunRequest {
  testCaseId: number;
  cycleId?: number;
  pitchId?: number;
  status: TestRunStatus;
  executedAt?: string;
  durationSeconds?: number;
  notes?: string;
  actualResult?: string;
  buildVersion?: string;
  environment?: string;
  attachments?: string;
}

export interface TestCoverage {
  pitchId: number;
  pitchTitle: string;
  cycleId?: number;
  cycleName?: string;
  totalTestCases: number;
  draftTestCases: number;
  readyTestCases: number;
  approvedTestCases: number;
  totalRuns: number;
  passedRuns: number;
  failedRuns: number;
  blockedRuns: number;
  skippedRuns: number;
  testCaseCoverage: number;
  passRate: number;
  coverageScore: number;
  totalBugs: number;
  openBugs: number;
  criticalBugs: number;
  blockerBugs: number;
}

export interface QADashboard {
  cycleId: number;
  cycleName: string;
  totalTestCases: number;
  draftTestCases: number;
  readyTestCases: number;
  approvedTestCases: number;
  deprecatedTestCases: number;
  aiGeneratedTestCases: number;
  totalRuns: number;
  passedRuns: number;
  failedRuns: number;
  blockedRuns: number;
  skippedRuns: number;
  pendingRuns: number;
  totalBugs: number;
  openBugs: number;
  inProgressBugs: number;
  resolvedBugs: number;
  verifiedBugs: number;
  closedBugs: number;
  trivialBugs: number;
  minorBugs: number;
  majorBugs: number;
  criticalBugs: number;
  blockerBugs: number;
  overallPassRate: number;
  overallCoverage: number;
  pitchCoverage: TestCoverage[];
}

export interface QATestManagementStatus {
  testManagementEnabled: boolean;
  aiTestGenerationEnabled: boolean;
  totalTestCases: number;
  totalBugReports: number;
  totalTestRuns: number;
  aiGeneratedTestCases: number;
}

export interface GenerateTestCasesRequest {
  pitchId: number;
  additionalContext?: string;
  focusAreas?: string[];
  maxTestCases?: number;
  testTypes?: string[];
}

export interface TestCaseSuggestion {
  title: string;
  description?: string;
  preconditions?: string;
  steps?: string;
  expectedResult?: string;
  suggestedType?: string;
  suggestedPriority?: string;
  suggestedTags?: string[];
  confidenceScore?: number;
}

export interface GenerateTestCasesResponse {
  suggestions: TestCaseSuggestion[];
  contextUsed?: string;
  aiEnabled: boolean;
  processingTimeMs?: number;
  errorMessage?: string;
}

// === Betting Table Types ===

export interface BettingSlot {
  id: number;
  cycleId: number;
  cycleName?: string;
  teamId: number;
  teamName?: string;
  pitchId?: number;
  pitchTitle?: string;
  pitchAppetiteDays?: number;
  pitchStatus?: string;
  position: number;
  startDate: string;
  endDate: string;
  durationWeeks?: number;
  notes?: string;
  createdAt: string;
  updatedAt: string;
  canFitPitch?: boolean;
}

export interface CreateBettingSlotRequest {
  cycleId: number;
  teamId: number;
  pitchId?: number;
  position: number;
  startDate: string;
  endDate: string;
  notes?: string;
}

export interface TeamTrack {
  teamId: number;
  teamName: string;
  slots: BettingSlot[];
  totalCapacityWeeks: number;
  usedCapacityWeeks: number;
  availableCapacityWeeks: number;
}

export interface BettingTable {
  cycleId: number;
  cycleName: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  cycleStartDate: string;
  cycleEndDate: string;
  cycleDurationWeeks: number;
  isCycleActive: boolean;
  shapedPitches: Pitch[];
  teamTracks: TeamTrack[];
  totalShapedPitches: number;
  totalAssignedPitches: number;
  totalCapacityWeeks: number;
  usedCapacityWeeks: number;
}

// Retrospective DTOs
export interface Retrospective {
  id: number;
  title: string;
  notes?: string;
  status: RetroStatus;
  cycleId: number;
  cycleName?: string;
  projectId: number;
  projectName?: string;
  projectKey?: string;
  createdById?: number;
  createdByName?: string;
  createdAt: string;
  updatedAt?: string;
  closedAt?: string;
  items?: RetroItem[];
  itemCount?: number;
}

export interface RetroItem {
  id: number;
  content: string;
  columnType: RetroColumnType;
  retrospectiveId: number;
  authorId?: number;
  authorName?: string;
  isAnonymous?: boolean;
  voteCount: number;
  hasVoted: boolean;
  mergedIntoId?: number;
  mergedItemIds?: number[];
  createdAt: string;
  updatedAt?: string;
  
  // v0.5 - Action tracking fields
  actedOn?: boolean;
  actedOnNotes?: string;
  actedOnAt?: string;
  actedOnById?: number;
  actedOnByName?: string;
}

export interface CreateRetroRequest {
  title: string;
  notes?: string;
  cycleId: number;
  projectId: number;
}

export interface CreateRetroItemRequest {
  content: string;
  columnType: RetroColumnType;
  retrospectiveId: number;
  isAnonymous?: boolean;
}

export interface UpdateRetroRequest {
  title?: string;
  notes?: string;
}

export interface CycleRetroStatus {
  cycleId: number;
  cycleName: string;
  totalRetros: number;
  closedRetros: number;
  canCloseCycle: boolean;
  message: string;
}

// Audit History Types
export type RevisionType = 'CREATED' | 'MODIFIED' | 'DELETED';

/**
 * Represents a single field change in an entity revision.
 */
export interface FieldChange {
  /** The name of the field that was changed (e.g., "status", "priority", "assignee") */
  fieldName: string;
  /** The previous value of the field before the change (null for creation) */
  oldValue: string | null;
  /** The new value of the field after the change */
  newValue: string | null;
}

/**
 * Represents a single revision entry in the entity's change history.
 */
export interface EntityHistory {
  /** The revision number (sequential identifier) */
  revisionNumber: number;
  /** The timestamp when this revision was created */
  revisionDate: string;
  /** The username of the user who made this change ("system" for automated changes) */
  modifiedBy: string;
  /** The type of revision: CREATED, MODIFIED, or DELETED */
  revisionType: RevisionType;
  /** The list of individual field changes in this revision */
  changes: FieldChange[];
}

// ============================================
// Roadmap & Release Management Types
// ============================================

/** Initiative status in strategic planning */
export type InitiativeStatus = 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';

/** Epic status (similar to Initiative) */
export type EpicStatus = 'DRAFT' | 'PLANNED' | 'IN_PROGRESS' | 'COMPLETED' | 'ON_HOLD' | 'CANCELLED';

/** Release lifecycle status */
export type ReleaseStatus = 'PLANNING' | 'IN_PROGRESS' | 'STAGING' | 'RELEASED' | 'CANCELLED';

/** Risk level for releases */
export type ReleaseRiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

/**
 * Summary of an Epic within an Initiative
 */
export interface EpicSummary {
  id: number;
  name: string;
  status: EpicStatus;
  pitchCount: number;
}

/**
 * Strategic initiative - top-level group spanning multiple quarters
 */
export interface Initiative {
  id: number;
  name: string;
  description?: string;
  status: InitiativeStatus;
  color?: string;
  projectId: number;
  projectName?: string;
  ownerId?: number;
  ownerName?: string;
  targetStartDate?: string;
  targetEndDate?: string;
  createdAt: string;
  updatedAt?: string;
  // Progress metrics
  epicCount?: number;
  completedEpicCount?: number;
  totalPitchCount?: number;
  completedPitchCount?: number;
  progressPercentage?: number;
  // Nested epics
  epics?: EpicSummary[];
  // Prioritization
  priority?: BusinessValue;
}

/**
 * Request to create or update an Initiative
 */
export interface CreateInitiativeRequest {
  name: string;
  description?: string;
  status?: InitiativeStatus;
  color?: string;
  projectId: number;
  ownerId?: number;
  targetStartDate?: string;
  targetEndDate?: string;
  // Prioritization
  priority?: BusinessValue;
}

/**
 * Summary of a Pitch within an Epic
 */
export interface PitchSummary {
  id: number;
  title: string;
  status: PitchStatus;
  priority?: BusinessValue;
  sortOrder?: number;
}

/**
 * Epic - groups pitches under an initiative
 */
export interface Epic {
  id: number;
  name: string;
  description?: string;
  status: EpicStatus;
  color?: string;
  projectId: number;
  projectName?: string;
  initiativeId?: number;
  initiativeName?: string;
  targetStartDate?: string;
  targetEndDate?: string;
  createdAt: string;
  updatedAt?: string;
  // Progress metrics
  pitchCount?: number;
  completedPitchCount?: number;
  progressPercentage?: number;
  // Nested pitches
  pitches?: PitchSummary[];
  // Prioritization
  priority?: BusinessValue;
  // Roadmap dependencies (informational)
  blockingEpics?: EpicDependency[];
  blockedByEpics?: EpicDependency[];
}

/**
 * Request to create or update an Epic
 */
export interface CreateEpicRequest {
  name: string;
  description?: string;
  status?: EpicStatus;
  color?: string;
  projectId: number;
  initiativeId?: number;
  targetStartDate?: string;
  targetEndDate?: string;
  // Prioritization
  priority?: BusinessValue;
}

/**
 * Summary of a Cycle linked to a Release
 */
export interface CycleSummary {
  id: number;
  name: string;
  startDate?: string;
  endDate?: string;
}

/**
 * Release - a versioned delivery milestone
 */
export interface Release {
  id: number;
  name: string;
  version: string;
  description?: string;
  status: ReleaseStatus;
  riskLevel: ReleaseRiskLevel;
  projectId: number;
  projectName?: string;
  targetDate?: string;
  releaseDate?: string;
  releaseNotes?: string;
  createdAt: string;
  updatedAt?: string;
  // Linked cycles
  cycles?: CycleSummary[];
}

/**
 * Request to create or update a Release
 */
export interface CreateReleaseRequest {
  name: string;
  version: string;
  description?: string;
  status?: ReleaseStatus;
  riskLevel?: ReleaseRiskLevel;
  projectId: number;
  targetDate?: string;
  releaseDate?: string;
  releaseNotes?: string;
  cycleIds?: number[];
}

/**
 * Bug information within a release progress summary
 */
export interface ReleaseBugInfo {
  id: number;
  title: string;
  status: string;
  severity: string;
  targetReleaseId?: number;
  targetReleaseVersion?: string;
  fixedInReleaseId?: number;
  fixedInReleaseVersion?: string;
  isSlipped: boolean;
}

/**
 * Release progress metrics
 */
export interface ReleaseProgress {
  releaseId: number;
  name: string;
  version: string;
  status: ReleaseStatus;
  riskLevel: ReleaseRiskLevel;
  targetDate?: string;
  releaseDate?: string;
  // Pitch metrics
  totalPitches: number;
  completedPitches: number;
  inProgressPitches: number;
  pitchCompletionPercentage: number;
  // Task metrics
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  blockedTasks: number;
  taskCompletionPercentage: number;
  // Bug metrics
  totalBugs: number;
  openBugs: number;
  resolvedBugs: number;
  slippedBugs: number;
  // Slipped bug details
  slippedBugList?: ReleaseBugInfo[];
}

/**
 * Timeline representation for Gantt charts
 */
export interface TimelinePitch {
  id: number;
  title: string;
  status: string;
  startDate?: string;
  endDate?: string;
  progress: number;
  cycleId?: number;
  cycleName?: string;
}

export interface TimelineEpic {
  id: number;
  name: string;
  description?: string;
  status: string;
  color?: string;
  startDate?: string;
  endDate?: string;
  progress: number;
  quarterLabel?: string;
  pitches?: TimelinePitch[];
}

export interface TimelineInitiative {
  id: number;
  name: string;
  description?: string;
  status: string;
  color?: string;
  startDate?: string;
  endDate?: string;
  progress: number;
  quarterLabel?: string;
  epics?: TimelineEpic[];
}

export interface TimelineRelease {
  id: number;
  name: string;
  version: string;
  status: string;
  targetDate?: string;
  releaseDate?: string;
  riskLevel: string;
  progressPercentage: number;
  pitchCount?: number;
  taskCount?: number;
  bugCount?: number;
}

/**
 * Complete roadmap timeline data for Gantt view
 */
export interface RoadmapTimeline {
  projectId: number;
  startDate: string;
  endDate: string;
  initiatives: TimelineInitiative[];
  orphanEpics: TimelineEpic[];
  releases: TimelineRelease[];
}

// Bulk task operations
export type BulkAction = 'ASSIGN' | 'CHANGE_STATUS' | 'CHANGE_PRIORITY' | 'ADD_TAG' | 'DELETE';

export interface BulkTaskUpdateRequest {
  taskIds: number[];
  action: BulkAction;
  /** Action-specific payload — see BulkAction docs for expected value per action. */
  value?: string;
}

export interface BulkUpdateResult {
  successCount: number;
  failureCount: number;
  errors: string[];
}

// Global Search
export type GlobalSearchEntityType = 'TASK' | 'SUBTASK' | 'BUG_REPORT' | 'PITCH' | 'EPIC';

export interface GlobalSearchResult {
  entityType: GlobalSearchEntityType;
  entityId: number;
  title: string;
  subtitle: string;
  route: string;
  score: number;
  matchedBy: 'EXACT_KEY' | 'TRIGRAM';
}

export * from './betting-analytics';
export * from './circuit-breaker';

export interface ImportJobDTO {
  id: number;
  fileName: string;
  sourceFormat: 'JIRA_CSV' | 'LINEAR_CSV' | 'ASANA_CSV' | 'GENERIC_CSV';
  status: 'PENDING' | 'PARSING' | 'IMPORTING' | 'COMPLETED' | 'FAILED';
  totalRows: number;
  importedRows: number;
  failedRows: number;
  errorLog: string | null;
  projectId: number | null;
  projectName: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface ZephyrRowResult {
  rowNumber: number;
  zephyrKey: string;
  title: string;
  success: boolean;
  testCaseId: number | null;
  error: string | null;
}

export interface ZephyrImportReportDTO {
  importJobId: number;
  fileName: string;
  status: 'COMPLETED' | 'FAILED';
  totalRows: number;
  importedRows: number;
  failedRows: number;
  rows: ZephyrRowResult[];
  completedAt: string | null;
}

export interface LinearConnectionStatus {
  connected: boolean;
  configured: boolean;
  teamId: string | null;
  teamName: string | null;
}

export interface LinearTeam {
  id: string;
  name: string;
  key: string;
}

export interface JiraConnectionStatus {
  connected: boolean;
  configured: boolean;
  cloudId: string | null;
  cloudName: string | null;
}

export interface JiraProject {
  id: string;
  key: string;
  name: string;
  description: string | null;
}
