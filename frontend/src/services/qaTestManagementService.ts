import api from './api';
import {
  TestCase,
  TestCaseType,
  TestCasePriority,
  CreateTestCaseRequest,
  UpdateTestCaseRequest,
  BugReport,
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
    priorities?: TestCasePriority[]
  ) => 
    api.get<TestCase[]>('/qa/test-cases/filter', {
      params: {
        cycleId,
        pitchId,
        statuses: statuses?.join(','),
        types: types?.join(','),
        priorities: priorities?.join(','),
      },
    }),

  createTestCase: (request: CreateTestCaseRequest) => 
    api.post<TestCase>('/qa/test-cases', request),

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
    sortOrder: string = 'desc'
  ) => {
    const params: any = { page, size, sortBy, sortOrder };
    if (projectId !== undefined) params.projectId = projectId;
    if (cycleId !== undefined) params.cycleId = cycleId;
    if (pitchId !== undefined) params.pitchId = pitchId;
    if (statuses && statuses.length > 0) params.statuses = statuses.join(',');
    if (severities && severities.length > 0) params.severities = severities.join(',');
    if (assigneeIds && assigneeIds.length > 0) params.assigneeIds = assigneeIds.join(',');
    if (exclude !== undefined) params.exclude = exclude;
    
    return api.get<Page<BugReport>>('/qa/bug-reports/filter', { params });
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

  updateTestRunStatus: (id: number, status: TestRunStatus, notes?: string) => 
    api.patch<TestRun>(`/qa/test-runs/${id}/status`, null, {
      params: { status, notes },
    }),

  deleteTestRun: (id: number) => 
    api.delete<{ message: string }>(`/qa/test-runs/${id}`),

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
};

export default qaTestManagementService;
