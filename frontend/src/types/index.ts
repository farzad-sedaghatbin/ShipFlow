// Pagination
export interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      sorted: boolean;
      unsorted: boolean;
      empty: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort: {
    sorted: boolean;
    unsorted: boolean;
    empty: boolean;
  };
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

// Enums
export type CyclePhase = 'SHAPING' | 'BETTING' | 'BUILD' | 'COOLDOWN';
export type PitchStatus = 'PENDING' | 'SHAPED' | 'STARTED' | 'IN_PROGRESS' | 'TESTING' | 'DONE' | 'COOLDOWN' | 'CANCELLED';
export type TeamMemberRole = 'BACKEND' | 'FRONTEND' | 'QA' | 'DESIGNER' | 'FULLSTACK' | 'TECH_LEAD' | 'PRODUCT_MANAGER';
export type MeetingType = 'SHAPING' | 'BETTING' | 'KICKOFF' | 'STANDUP' | 'DEMO' | 'RETROSPECTIVE' | 'HILL_CHART_REVIEW';
export type TaskStatus = 'BACKLOG' | 'TODO' | 'IN_PROGRESS' | 'BLOCKED' | 'IN_REVIEW' | 'DONE' | 'CANCELLED';
export type TaskPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TaskCategory = 'PITCH_SCOPE' | 'DEBT_IMPROVEMENT';
export type RetroStatus = 'DRAFT' | 'OPEN' | 'CLOSED';
export type RetroColumnType = 'WENT_WELL' | 'DID_NOT_GO_WELL' | 'TRY_NEXT' | 'ACTIONS';
export type ActionStatus = 'OPEN' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';

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
  createdAt: string;
  updatedAt?: string;
  cycleCount?: number;
  activeCycleCount?: number;
}

export interface CreateProjectRequest {
  name: string;
  projectKey: string;
  description?: string;
  color?: string;
  logoUrl?: string;
  ownerId?: number;
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
  teamCount?: number;
}

export interface CreateCycleRequest {
  projectId: number;
  name: string;
  startDate: string;
  endDate: string;
  phase?: CyclePhase;
}

export interface Team {
  id: number;
  name: string;
  cycleId?: number;
  cycleName?: string;
  projectId?: number;
  projectName?: string;
  projectKey?: string;
  assignments?: TeamAssignment[];
}

export interface CreateTeamRequest {
  name: string;
  cycleId?: number;
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
  currentAssignments?: TeamAssignment[];
  pastAssignments?: TeamAssignment[];
}

export interface CreatePersonRequest {
  name: string;
  email: string;
  skills?: string;
  avatarUrl?: string;
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
}

export interface CreateTeamAssignmentRequest {
  personId: number;
  teamId: number;
  role: TeamMemberRole;
  startDate?: string;
  endDate?: string;
  notes?: string;
}

export interface Pitch {
  id: number;
  title: string;
  description?: string;
  appetiteDays: number;
  cycleId: number;
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
  // Shape Up Methodology Fields
  problemStatement?: string;
  solution?: string;
  rabbitHoles?: string;
  risks?: string;
  noGos?: string;
  wireframeLinks?: string;
}

export interface CreatePitchRequest {
  title: string;
  description?: string;
  appetiteDays: number;
  cycleId: number;
  teamId?: number;
  status?: PitchStatus;
  // Shape Up Methodology Fields
  problemStatement?: string;
  solution?: string;
  rabbitHoles?: string;
  risks?: string;
  noGos?: string;
  wireframeLinks?: string;
}

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
  notes?: string;
  retrospectiveId?: number;
  retrospectiveTitle?: string;
  decisions?: string;
  attendees?: string;
  actions?: MeetingAction[];
}

export interface CreateMeetingRequest {
  pitchId?: number;
  type: MeetingType;
  dateHeld: string;
  dorReady?: boolean;
  dodReady?: boolean;
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
export type UserRole = 'ADMIN' | 'PROJECT_MANAGER' | 'PRODUCT' | 'DEVELOPER' | 'QA';

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
  createdAt: string;
  updatedAt: string;
}

export interface CreateHillChartPointRequest {
  pitchId: number;
  scope: string;
  description: string;
  position: number;
}

export interface UpdateHillChartPointRequest {
  scope?: string;
  description?: string;
  position?: number;
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
}

export interface CreateTaskRequest {
  title: string;
  description?: string;
  cycleId: number;
  status?: TaskStatus;
  priority?: TaskPriority;
  category?: TaskCategory;
  estimateHours?: number;
  actualHours?: number;
  assigneeId?: number;
  pairAssigneeId?: number;
  parentTaskId?: number;
  dueDate?: string;
  tags?: string;
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
  pitchId?: number;
  pitchTitle?: string;
  cycleId?: number;
  cycleName?: string;
  teamId?: number;
  teamName?: string;
  testRunId?: number;
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
}

export interface CreateBugReportRequest {
  title: string;
  description: string;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  testRunId?: number;
  severity: BugSeverity;
  status?: BugStatus;
  tags?: string[];
  attachments?: string;
  assigneeId?: number;
}

export interface UpdateBugReportRequest {
  title?: string;
  description?: string;
  stepsToReproduce?: string;
  expectedBehavior?: string;
  actualBehavior?: string;
  environment?: string;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  severity?: BugSeverity;
  status?: BugStatus;
  tags?: string[];
  attachments?: string;
  assigneeId?: number;
  resolution?: string;
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
  voteCount: number;
  hasVoted: boolean;
  mergedIntoId?: number;
  mergedItemIds?: number[];
  createdAt: string;
  updatedAt?: string;
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