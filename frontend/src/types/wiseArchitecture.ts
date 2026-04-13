/**
 * Types for the Wise Architecture feature - AI-powered technical solution generator.
 * This is an experimental feature that helps generate technical solutions for pitches.
 */

/**
 * Technology stack categories
 */
export type TechStackCategory = 'MOBILE' | 'BACKEND' | 'WEB' | 'UNKNOWN';

/**
 * Supported technology stack types
 */
export type TechStackType =
  // Mobile
  | 'MOBILE_KOTLIN'
  | 'MOBILE_SWIFT'
  | 'MOBILE_REACT_NATIVE'
  | 'MOBILE_FLUTTER'
  // Backend
  | 'BACKEND_JAVA'
  | 'BACKEND_NODE'
  | 'BACKEND_PYTHON'
  | 'BACKEND_GO'
  | 'BACKEND_DOTNET'
  // Web
  | 'WEB_REACT'
  | 'WEB_ANGULAR'
  | 'WEB_VUE'
  | 'WEB_NEXTJS'
  // Unknown
  | 'UNKNOWN';

/**
 * Mapping of stack types to their categories
 */
export const STACK_CATEGORIES: Record<TechStackType, TechStackCategory> = {
  MOBILE_KOTLIN: 'MOBILE',
  MOBILE_SWIFT: 'MOBILE',
  MOBILE_REACT_NATIVE: 'MOBILE',
  MOBILE_FLUTTER: 'MOBILE',
  BACKEND_JAVA: 'BACKEND',
  BACKEND_NODE: 'BACKEND',
  BACKEND_PYTHON: 'BACKEND',
  BACKEND_GO: 'BACKEND',
  BACKEND_DOTNET: 'BACKEND',
  WEB_REACT: 'WEB',
  WEB_ANGULAR: 'WEB',
  WEB_VUE: 'WEB',
  WEB_NEXTJS: 'WEB',
  UNKNOWN: 'UNKNOWN',
};

/**
 * Human-readable display names for stack types
 */
export const STACK_DISPLAY_NAMES: Record<TechStackType, string> = {
  MOBILE_KOTLIN: 'Kotlin (Android)',
  MOBILE_SWIFT: 'Swift (iOS)',
  MOBILE_REACT_NATIVE: 'React Native',
  MOBILE_FLUTTER: 'Flutter',
  BACKEND_JAVA: 'Java (Spring)',
  BACKEND_NODE: 'Node.js',
  BACKEND_PYTHON: 'Python',
  BACKEND_GO: 'Go',
  BACKEND_DOTNET: '.NET',
  WEB_REACT: 'React',
  WEB_ANGULAR: 'Angular',
  WEB_VUE: 'Vue.js',
  WEB_NEXTJS: 'Next.js',
  UNKNOWN: 'Unknown',
};

/**
 * Detected technology stack information
 */
export interface DetectedStack {
  stackType: TechStackType;
  repositoryId: number;
  repositoryName: string;
  confidence: number;  // 0-100
  detectedFiles?: string[];
}

/**
 * Request to detect technology stacks in repositories
 */
export interface DetectStacksRequest {
  pitchId: number;
  repositoryIds: number[];
  /**
   * Optional map of repository ID to branch name.
   * If not specified for a repo, the default branch will be used.
   */
  repositoryBranches?: Record<number, string>;
}

/**
 * Response from stack detection
 */
export interface DetectStacksResponse {
  pitchId: number;
  pitchName: string;
  detectedStacks: DetectedStack[];
  repositoriesScanned: number;
  message?: string;
}

/**
 * Request to analyze and generate technical solution
 */
export interface WiseArchitectureRequest {
  pitchId: number;
  repositoryIds: number[];
  /**
   * Optional map of repository ID to branch name.
   * If not specified for a repo, the default branch will be used.
   */
  repositoryBranches?: Record<number, string>;
  selectedStacks: TechStackType[];
}

/**
 * Reusable service identified in the codebase
 */
export interface ReusableService {
  serviceName: string;
  filePath: string;
  description: string;
  howToUse: string;
  /** Specific methods to call from this service. */
  methodsToCall?: string[];
  /** Import/injection statement. */
  importStatement?: string;
}

/**
 * Recommended library/tool to use
 */
export interface RecommendedLibrary {
  name: string;
  version?: string;
  purpose: string;
  documentationUrl?: string;
  alreadyInProject: boolean;
}

/**
 * A sub-task within an implementation step with acceptance criteria.
 */
export interface SubTask {
  task: string;
  acceptanceCriteria: string;
}

/**
 * A single implementation step
 */
export interface ImplementationStep {
  stepNumber: number;
  title: string;
  description: string;
  estimatedHours: number;
  filesToCreate?: string[];
  filesToModify?: string[];
  /** Concrete sub-tasks with acceptance criteria. */
  subTasks?: SubTask[];
  /** Key method/function signatures to implement. */
  methodSignatures?: string[];
  /** Step numbers this step depends on. */
  dependsOnSteps?: number[];
}

/**
 * Component within the architecture breakdown.
 */
export interface ArchitectureComponent {
  name: string;
  responsibility: string;
  interactsWith?: string[];
}

/**
 * API contract definition for an endpoint.
 */
export interface ApiContract {
  endpoint: string;
  method: string;
  requestShape?: string;
  responseShape?: string;
  description?: string;
}

/**
 * Data model entity definition.
 */
export interface DataModel {
  entityName: string;
  fields?: string[];
  relationships?: string[];
}

/**
 * Configuration change required for the implementation.
 */
export interface ConfigChange {
  key: string;
  value: string;
  description?: string;
}

/**
 * Structured architecture detail breaking the overview into actionable sections.
 */
export interface ArchitectureDetail {
  summary: string;
  components?: ArchitectureComponent[];
  apiContracts?: ApiContract[];
  dataModel?: DataModel[];
  configChanges?: ConfigChange[];
}

/**
 * Technical solution for a specific technology stack
 */
export interface StackSolution {
  stackType: TechStackType;
  /** Kept for backward compatibility; populated from architectureDetail.summary for new solutions. */
  architectureOverview: string;
  /** Structured architecture breakdown. */
  architectureDetail?: ArchitectureDetail;
  reusableServices: ReusableService[];
  recommendedLibraries: RecommendedLibrary[];
  implementationSteps: ImplementationStep[];
  bestPractices: string[];
  estimatedHours: number;
  riskFactors?: string[];
}

/**
 * Appetite check result
 */
export interface AppetiteCheck {
  passed: boolean;
  availableHours: number;
  estimatedHours: number;
  hoursByStack?: Record<TechStackType, number>;
  message: string;
}

/**
 * Reduced scope suggestion when appetite is exceeded
 */
export interface ReducedScope {
  explanation: string;
  reducedSteps?: ImplementationStep[];
  deferredItems?: string[];
  reducedTotalHours: number;
}

/**
 * Information about which context sources were available for analysis.
 * Missing context sources result in less accurate recommendations.
 */
export interface ContextSources {
  hasCodeContext: boolean;
  hasTeamSkills: boolean;
  hasFigmaContext: boolean;
  hasRoadmapContext: boolean;
  warnings?: string[];
}

/**
 * A single agent-consumable Markdown file generated by Wise Architecture.
 * Files are designed to be read by AI coding agents (Claude Code, Cursor, Copilot Workspace)
 * to drive implementation of the pitch without additional context.
 *
 * category values: "overview" | "backend" | "mobile" | "web" | "api" | "plan"
 */
export interface GeneratedMarkdownFile {
  filename: string;      // e.g. "api-design.md"
  title: string;         // e.g. "API Design Guide"
  content: string;       // Full Markdown content
  category: string;      // UI grouping / icon selection
  stackType?: TechStackType; // null for cross-stack files
}

/**
 * Complete response from Wise Architecture analysis
 */
export interface WiseArchitectureResponse {
  sessionId: string;
  pitchId: number;
  pitchName: string;
  solutions: Record<TechStackType, StackSolution>;
  appetiteCheck: AppetiteCheck;
  totalEstimatedHours: number;
  reducedScope?: ReducedScope;
  contextSources?: ContextSources;
  generatedAt: string;
  /** Agent-consumable Markdown files generated from this analysis. */
  generatedFiles?: GeneratedMarkdownFile[];
}

/**
 * Follow-up question request
 */
export interface FollowUpQuestion {
  sessionId: string;
  question: string;
}

/**
 * Response to a follow-up question
 */
export interface FollowUpResponse {
  answer: string;
  isCodeRequest: boolean;
  copilotPrompt?: string;
}

/**
 * Wise Architecture feature status
 */
export interface WiseArchitectureStatus {
  enabled: boolean;
  aiEnabled: boolean;
}

/**
 * Conversation history item for UI display
 */
export interface ConversationItem {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  copilotPrompt?: string;
  timestamp: Date;
}

// =====================================================================
// ASYNC JOB TYPES - For large repositories that may timeout
// =====================================================================

/**
 * Job status values
 */
export type JobStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

/**
 * Job type values
 */
export type JobType = 'DETECT_STACKS' | 'ANALYZE';

/**
 * Response from starting an async job
 */
export interface JobStartResponse {
  jobId: string;
  status: JobStatus;
  message: string;
}

/**
 * Job status response with progress information
 */
export interface JobStatusResponse {
  jobId: string;
  jobType: JobType;
  status: JobStatus;
  progress: number;  // 0-100
  progressMessage?: string;
  createdAt?: string;
  completedAt?: string;
  errorMessage?: string;
}

/**
 * Progress callback for polling
 */
export type ProgressCallback = (progress: number, message?: string) => void;

// =====================================================================
// ADVICE HISTORY TYPES - For reviewing and following up on past advice
// =====================================================================

/**
 * Message type in a conversation
 */
export type AdviceMessageType = 'INITIAL_SOLUTION' | 'FOLLOW_UP';

/**
 * Single advice entry in history
 */
export interface AdviceHistoryItem {
  id: number;
  conversationId: string;
  pitchId: number;
  pitchTitle: string;
  userId: number;
  messageType: AdviceMessageType;
  userMessage?: string;
  aiResponse: string;
  techStacks: string[];
  repositoryIds: number[];
  hasFigmaContext: boolean;
  hasGitHubContext: boolean;
  hasRoadmapContext: boolean;
  processingTimeMs?: number;
  feedbackHelpful?: boolean;
  feedbackText?: string;
  feedbackAt?: string;
  createdAt: string;
}

/**
 * Conversation summary for history list
 */
export interface ConversationSummary {
  conversationId: string;
  pitchId: number;
  pitchTitle: string;
  projectName?: string;
  techStacks: string[];
  messageCount: number;
  createdAt: string;
  lastMessageAt: string;
  responsePreview: string;
}

/**
 * Paginated response for conversation history
 */
export interface ConversationHistoryPage {
  content: ConversationSummary[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

/**
 * Feedback request for advice entry
 */
export interface AdviceFeedbackRequest {
  helpful: boolean;
  feedbackText?: string;
}
