import api from './api';
import {
  TestCase,
  TestCaseType,
  TestCasePriority,
  CreateTestCaseRequest,
  UpdateTestCaseRequest,
  BugReport,
  BugStats,
  CreateBugReportRequest,
  UpdateBugReportRequest,
  TestRun,
  CreateTestRunRequest,
  TestCoverage,
  QADashboard,
  QATestManagementStatus,
  GenerateTestCasesRequest,
  GenerateTestCasesResponse,
  TestCaseStatus,
  TestRunStatus,
  BugStatus,
  BugSeverity,
  Page,
  EntityHistory,
} from '../types';

/**
 * Service for QA Test Management features
 */
export const qaTestManagementService = {
  // ========== Status ==========
  getStatus: () => 
    api.get<QATestManagementStatus>('/qa/test-management/status'),

  // ========== Test Cases ==========
  getAllTestCases: () => 
    api.get<TestCase[]>('/qa/test-cases'),

  getTestCaseById: (id: number) => 
    api.get<TestCase>(`/qa/test-cases/${id}`),

  getTestCaseByKey: (key: string) => 
    api.get<TestCase>(`/qa/test-cases/key/${key}`),

  getTestCasesByPitch: (pitchId: number) => 
    api.get<TestCase[]>(`/qa/test-cases/pitch/${pitchId}`),

  getTestCasesByCycle: (cycleId: number) => 
    api.get<TestCase[]>(`/qa/test-cases/cycle/${cycleId}`),

  getTestCasesByStatus: (status: TestCaseStatus) => 
    api.get<TestCase[]>(`/qa/test-cases/status/${status}`),

  getTestCasesWithFilters: (
    cycleId?: number,
    pitchId?: number,
    statuses?: TestCaseStatus[],
    types?: TestCaseType[],
    priorities?: TestCasePriority[],
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortOrder: string = 'desc',
    createdById?: number,
    aiGenerated?: boolean,
    projectId?: number
  ) =>
    api.get<Page<TestCase>>('/qa/test-cases/filter', {
      params: {
        projectId,
        cycleId,
        pitchId,
        statuses: statuses?.join(','),
        types: types?.join(','),
        priorities: priorities?.join(','),
        page,
        size,
        sortBy,
        sortOrder,
        createdById,
        aiGenerated,
      },
    }),

  createTestCase: (request: CreateTestCaseRequest) =>
    api.post<TestCase>('/qa/test-cases', request),

  createTestCasesBulk: (requests: CreateTestCaseRequest[]) =>
    api.post<TestCase[]>('/qa/test-cases/bulk', { testCases: requests }),

  updateTestCase: (id: number, request: UpdateTestCaseRequest) => 
    api.put<TestCase>(`/qa/test-cases/${id}`, request),

  deleteTestCase: (id: number) => 
    api.delete<{ message: string }>(`/qa/test-cases/${id}`),

  // ========== Bug Reports ==========
  getAllBugReports: (page: number = 0, size: number = 10, sortBy: string = 'createdAt', sortOrder: string = 'desc') => 
    api.get<Page<BugReport>>('/qa/bug-reports', {
      params: { page, size, sortBy, sortOrder },
    }),

  getBugReportsWithFilters: (
    projectId?: number,
    cycleId?: number,
    pitchId?: number,
    statuses?: BugStatus[],
    severities?: BugSeverity[],
    assigneeIds?: number[],
    exclude?: boolean,
    page: number = 0,
    size: number = 10,
    sortBy: string = 'createdAt',
    sortOrder: string = 'desc',
    search?: string
  ) => {
    const params: any = { page, size, sortBy, sortOrder };
    if (projectId !== undefined) params.projectId = projectId;
    if (cycleId !== undefined) params.cycleId = cycleId;
    if (pitchId !== undefined) params.pitchId = pitchId;
    if (statuses && statuses.length > 0) params.statuses = statuses.join(',');
    if (severities && severities.length > 0) params.severities = severities.join(',');
    if (assigneeIds && assigneeIds.length > 0) params.assigneeIds = assigneeIds.join(',');
    if (exclude !== undefined) params.exclude = exclude;
    if (search && search.trim()) params.search = search.trim();

    return api.get<Page<BugReport>>('/qa/bug-reports/filter', { params });
  },

  getBugStats: (
    projectId?: number,
    cycleId?: number,
    pitchId?: number,
    statuses?: BugStatus[],
    severities?: BugSeverity[],
    assigneeIds?: number[],
    exclude?: boolean,
    search?: string
  ) => {
    const params: any = {};
    if (projectId !== undefined) params.projectId = projectId;
    if (cycleId !== undefined) params.cycleId = cycleId;
    if (pitchId !== undefined) params.pitchId = pitchId;
    if (statuses && statuses.length > 0) params.statuses = statuses.join(',');
    if (severities && severities.length > 0) params.severities = severities.join(',');
    if (assigneeIds && assigneeIds.length > 0) params.assigneeIds = assigneeIds.join(',');
    if (exclude !== undefined) params.exclude = exclude;
    if (search && search.trim()) params.search = search.trim();
    return api.get<BugStats>('/qa/bug-reports/stats', { params });
  },

  getBugReportById: (id: number) =>
    api.get<BugReport>(`/qa/bug-reports/${id}`),

  getBugReportByKey: (key: string) => 
    api.get<BugReport>(`/qa/bug-reports/key/${key}`),

  getBugReportsByPitch: (pitchId: number) => 
    api.get<BugReport[]>(`/qa/bug-reports/pitch/${pitchId}`),

  getBugReportsByCycle: (cycleId: number) => 
    api.get<BugReport[]>(`/qa/bug-reports/cycle/${cycleId}`),

  getOpenBugReports: () => 
    api.get<BugReport[]>('/qa/bug-reports/open'),

  getMyAssignedBugs: () => 
    api.get<BugReport[]>('/qa/bug-reports/my-assigned'),

  getMyReportedBugs: () => 
    api.get<BugReport[]>('/qa/bug-reports/my-reported'),

  createBugReport: (request: CreateBugReportRequest) => 
    api.post<BugReport>('/qa/bug-reports', request),

  updateBugReport: (id: number, request: UpdateBugReportRequest) =>
    api.put<BugReport>(`/qa/bug-reports/${id}`, request),

  /** Lightweight PATCH to assign/unassign the QA tester for a bug. Pass null to unassign. */
  updateBugQaAssignee: (id: number, qaAssigneeId: number | null) =>
    api.patch<BugReport>(`/qa/bug-reports/${id}/qa-assignee`, { qaAssigneeId }),

  deleteBugReport: (id: number) => 
    api.delete<{ message: string }>(`/qa/bug-reports/${id}`),

  // ========== Test Runs ==========
  getAllTestRuns: () => 
    api.get<TestRun[]>('/qa/test-runs'),

  getTestRunById: (id: number) => 
    api.get<TestRun>(`/qa/test-runs/${id}`),

  getTestRunsByTestCase: (testCaseId: number) => 
    api.get<TestRun[]>(`/qa/test-runs/test-case/${testCaseId}`),

  getTestRunsByPitch: (pitchId: number) => 
    api.get<TestRun[]>(`/qa/test-runs/pitch/${pitchId}`),

  getTestRunsByCycle: (cycleId: number) => 
    api.get<TestRun[]>(`/qa/test-runs/cycle/${cycleId}`),

  getLatestTestRun: (testCaseId: number) => 
    api.get<TestRun>(`/qa/test-runs/latest/${testCaseId}`),

  createTestRun: (request: CreateTestRunRequest) =>
    api.post<TestRun>('/qa/test-runs', request),

  /** Record the same execution result against many test cases at once (Zephyr-style bulk run). */
  recordTestRunsBulk: (request: {
    testCaseIds: number[];
    status: TestRunStatus;
    environment?: string;
    buildVersion?: string;
    notes?: string;
    cycleId?: number;
    pitchId?: number;
  }) => api.post<TestRun[]>('/qa/test-runs/bulk', request),

  /** Update the status of many test runs at once (Zephyr-style bulk update). */
  updateTestRunStatusBulk: (request: { testRunIds: number[]; status: TestRunStatus; notes?: string }) =>
    api.patch<TestRun[]>('/qa/test-runs/bulk/status', request),

  updateTestRunStatus: (id: number, status: TestRunStatus, notes?: string) =>
    api.patch<TestRun>(`/qa/test-runs/${id}/status`, null, {
      params: { status, notes },
    }),

  deleteTestRun: (id: number) =>
    api.delete<{ message: string }>(`/qa/test-runs/${id}`),

  /** Link an existing bug report (defect) to a test run. A run can have several linked defects. */
  linkDefectToRun: (runId: number, bugReportId: number) =>
    api.patch<TestRun>(`/qa/test-runs/${runId}/defects/${bugReportId}`),

  /** Unlink a bug report (defect) from a test run. */
  unlinkDefectFromRun: (runId: number, bugReportId: number) =>
    api.delete<TestRun>(`/qa/test-runs/${runId}/defects/${bugReportId}`),

  // ========== Coverage & Dashboard ==========
  getTestCoverageByPitch: (pitchId: number) => 
    api.get<TestCoverage>(`/qa/coverage/pitch/${pitchId}`),

  getQADashboardByCycle: (cycleId: number) => 
    api.get<QADashboard>(`/qa/dashboard/cycle/${cycleId}`),

  // ========== AI Test Generation ==========
  generateTestCases: (request: GenerateTestCasesRequest) => 
    api.post<GenerateTestCasesResponse>('/qa/generate-test-cases', request),

  // ========== History (Audit Trail) ==========
  getBugReportHistory: (bugId: number, page?: number, size?: number) =>
    api.get<Page<EntityHistory>>(`/qa/bug-reports/${bugId}/history`, {
      params: {
        page: page ?? 0,
        size: size ?? 20,
      },
    }),

  getTestCaseHistory: (testCaseId: number, page?: number, size?: number) =>
    api.get<Page<EntityHistory>>(`/qa/test-cases/${testCaseId}/history`, {
      params: {
        page: page ?? 0,
        size: size ?? 20,
      },
    }),

  moveBugToProject: (id: number, projectId: number) =>
    api.patch<BugReport>(`/qa/bug-reports/${id}/move-to-project/${projectId}`),
};

export default qaTestManagementService;
