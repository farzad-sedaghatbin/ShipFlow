# P2 Refactoring Recommendations (v0.5.2)

This document outlines recommended refactoring for large files identified in the codebase. These are P2 (lower priority) improvements that enhance maintainability without blocking the release.

## Overview

**P2 Goal**: Decompose large monolithic files into smaller, focused modules to improve:
- Code readability and maintainability
- Testing granularity
- Team collaboration (reduce merge conflicts)
- Performance (tree-shaking, lazy loading)

## Frontend Refactoring Candidates

### 1. BacklogPage.tsx (2,013 lines) - HIGHEST PRIORITY

**Current State**: Single monolithic component with:
- 60+ state variables
- 20+ event handlers
- Massive JSX render section (1,200+ lines)
- Mixed concerns (data fetching, filtering, UI, dialogs)

**Recommended Decomposition**:

#### Phase 1: Extract Constants
- ✅ **COMPLETED**: Created `constants/backlogConstants.ts`
  - Status options, priority options
  - Badge variant helper functions

#### Phase 2: Extract Custom Hooks
- ✅ **COMPLETED**: `hooks/useBacklogData.ts` - Data fetching logic
  - Tasks, cycles, persons, pitches, scopes
  - Active timer management
  - Statistics loading (React Query patterns)
  
- ✅ **COMPLETED**: `hooks/useBacklogFilters.ts` - Filtering and sorting logic (26 tests)
  - Status, priority, assignee filters
  - Dependency filters
  - Pagination state
  
- ✅ **COMPLETED**: `hooks/useBacklogForm.ts` - Dialog form management (22 tests)
  - Form state and validation
  - Create/edit task logic

#### Phase 3: Extract Components
- ✅ **COMPLETED**: `components/backlog/BacklogHeader.tsx` - Page header with cycle selector
- ✅ **COMPLETED**: `components/backlog/BacklogStatistics.tsx` - Statistics cards
- ✅ **COMPLETED**: `components/backlog/BacklogFilters.tsx` - Filter controls
- ✅ **COMPLETED**: `components/backlog/BacklogTaskTable.tsx` - List view table (586 lines)
- ✅ **COMPLETED**: `components/backlog/BacklogTaskDialog.tsx` - Create/edit dialog (289 lines)
- ✅ **COMPLETED**: `components/backlog/BacklogDeleteDialog.tsx` - Delete confirmation
- `components/backlog/BacklogViewDialog.tsx` - Task detail viewer (remaining)

#### Expected Result:
- BacklogPage.tsx reduced from 2,013 → ~300 lines (when integrated)
- ✅ 6 reusable components created
- ✅ 3 custom hooks with 48 unit tests
- ✅ Better test coverage achieved

---

### 2. OrganizationSettings.tsx (1,510 lines)

**Current State**: Single settings page with multiple tabs/sections

**Recommended Decomposition**:
- `components/settings/GeneralSettingsTab.tsx`
- `components/settings/TeamSettingsTab.tsx`
- `components/settings/IntegrationSettingsTab.tsx`
- `components/settings/SecuritySettingsTab.tsx`
- `hooks/useOrganizationSettings.ts` - Data fetching and updates

**Expected Result**: 1,510 → ~200 lines

---

### 3. PitchDetail.tsx (1,314 lines)

**Current State**: Single pitch detail page with complex UI

**Recommended Decomposition**:
- `components/pitch/PitchHeader.tsx`
- `components/pitch/PitchScopeSection.tsx`
- `components/pitch/PitchHillChart.tsx`
- `components/pitch/PitchComments.tsx`
- `components/pitch/PitchAttachments.tsx`
- `hooks/usePitchDetail.ts` - Data fetching

**Expected Result**: 1,314 → ~250 lines

---

### 4. MeetingList.tsx (1,182 lines)

**Current State**: Meeting list with calendar integration

**Recommended Decomposition**:
- `components/meeting/MeetingCalendar.tsx`
- `components/meeting/MeetingListView.tsx`
- `components/meeting/MeetingDialog.tsx`
- `hooks/useMeetings.ts` - Data fetching with React Query

**Expected Result**: 1,182 → ~200 lines

---

## Backend Refactoring Candidates

### 1. QAService.java (1,215 lines) - DECOMPOSITION COMPLETE

**Current State**: Already uses sub-services (good!) but main `askQuestion()` method is very long (~400 lines)

**Recommended Decomposition**:

The service already delegates to:
- ✅ `RAGEvaluator` - Quality evaluation
- ✅ `DocumentReranker` - Result re-ranking
- ✅ `ContextWindowManager` - Token management
- ✅ `ConversationManager` - Chat history
- ✅ `SecurityDocumentFilter` - Access control
- ✅ `QueryDecomposer` - Complex query handling
- ✅ `FeedbackLearningService` - User feedback
- ✅ `LLMCacheService` - Response caching
- ✅ `PromptCompressor` - Prompt optimization
- ✅ `ContentGuardrails` - Safety filtering

**Additional Services Created (v0.5.2)**:

- ✅ **COMPLETED**: `QuestionProcessingService` - Question preparation and decomposition (31 tests)
  - Handle query decomposition
  - Extract context from questions
  - Detect ambiguous queries
  - Search term extraction
  
- ✅ **COMPLETED**: `EmbeddingSearchService` - Vector search orchestration
  - Embedding generation with retry
  - Vector store search with fallback
  - Context filtering and boosting
  - Recency boost for recent documents
  
- ✅ **COMPLETED**: `AnswerGenerationService` - LLM interaction (22 tests)
  - Prompt construction
  - Rate limit handling
  - Response caching integration
  - Confidence scoring

**Implementation Strategy**:
```java
// Before: 400-line method
public QAResponse askQuestion(request, userId) {
  // ... 400 lines ...
}

// After: Orchestrator pattern
public QAResponse askQuestion(request, userId) {
  // 1. Process question
  ProcessedQuestion processed = questionProcessor.process(request);
  
  // 2. Search vector store
  SearchResults results = embeddingSearch.search(processed);
  
  // 3. Generate answer
  return answerGenerator.generate(processed, results);
}
```

**Expected Result**: 1,215 → ~600 lines (50% reduction)

---

### 2. RiskAnalysisService.java (1,064 lines)

**Current State**: Risk analysis with multiple algorithms

**Recommended Decomposition**:
- `RiskCalculationService` - Core risk calculations
- `RiskReportingService` - Report generation
- `RiskMitigationService` - Mitigation suggestions
- `RiskHistoryService` - Historical tracking

**Expected Result**: 1,064 → ~300 lines

---

### 3. PitchHealthService.java (981 lines)

**Current State**: Health metrics for pitches

**Recommended Decomposition**:
- `HealthMetricsCalculator` - Metric calculations
- `HealthScoreAggregator` - Score aggregation
- `HealthReportGenerator` - Report generation

**Expected Result**: 981 → ~350 lines

---

### 4. CycleSignalService.java (937 lines)

**Current State**: Cycle progress signals

**Recommended Decomposition**:
- `SignalDetector` - Detect cycle signals
- `SignalAnalyzer` - Analyze patterns
- `SignalNotifier` - Send notifications

**Expected Result**: 937 → ~300 lines

---

## Implementation Priority

### High Priority (v0.5.2) - COMPLETED
1. ✅ **BacklogPage.tsx constants** - COMPLETED
2. ✅ **BacklogPage.tsx hooks** - COMPLETED (3 hooks, 48 tests)
3. ✅ **BacklogPage.tsx components** - COMPLETED (6 components)
4. ✅ **QAService.java orchestration** - COMPLETED (3 services, 53 tests)

### Medium Priority (v0.6.0)
5. OrganizationSettings.tsx decomposition
6. PitchDetail.tsx decomposition
7. RiskAnalysisService.java decomposition

### Low Priority (Future)
8. MeetingList.tsx decomposition
9. PitchHealthService.java decomposition
10. CycleSignalService.java decomposition

---

## Benefits of This Refactoring

### Developer Experience
- **Faster navigation**: Jump to specific features instead of scrolling through 2,000-line files
- **Easier code review**: Review 50-line components instead of 500-line diffs
- **Reduced merge conflicts**: Multiple developers can work on different components
- **Better IDE performance**: Smaller files = faster autocomplete and error checking

### Testing
- **Unit testing**: Test individual components/services in isolation
- **Mocking**: Easier to mock dependencies
- **Coverage**: Better test coverage visibility

### Performance
- **Tree-shaking**: Unused components can be eliminated in production bundles
- **Code splitting**: Lazy load large features (especially OrganizationSettings)
- **Compilation**: Faster TypeScript/Vite builds

### Maintainability
- **Single Responsibility**: Each component/service has one clear purpose
- **Reusability**: Components can be reused across pages
- **Discoverability**: New team members can find code faster

---

## How to Implement

### Step 1: Create Feature Branch
```bash
git checkout -b feature/v0.5.3-decompose-backlog
```

### Step 2: Extract Constants (✅ Done)
```typescript
// Already created: constants/backlogConstants.ts
```

### Step 3: Extract Custom Hooks
```typescript
// Example: hooks/useBacklogData.ts
import { useQuery } from '@tanstack/react-query';

export function useBacklogData(cycleId: number | null) {
  const tasks = useQuery({ ... });
  const statistics = useQuery({ ... });
  // ... return all data
}
```

### Step 4: Extract Components
```tsx
// Example: components/backlog/BacklogStatistics.tsx
export function BacklogStatistics({ statistics }: Props) {
  return <div>...</div>;
}
```

### Step 5: Refactor Main Component
```tsx
// BacklogPage.tsx - Now much smaller!
export default function BacklogPage() {
  const data = useBacklogData(cycleId);
  const filters = useBacklogFilters();
  
  return (
    <>
      <BacklogHeader {...} />
      <BacklogStatistics {...} />
      <BacklogFilters {...} />
      <BacklogTaskTable {...} />
    </>
  );
}
```

### Step 6: Test Thoroughly
- Unit tests for new components
- Integration tests for BacklogPage
- Manual testing of all features

### Step 7: Merge & Deploy
```bash
git push origin feature/v0.5.3-decompose-backlog
# Create PR, review, merge
```

---

## Success Metrics

Track these metrics before and after refactoring:

- **File LOC**: Should reduce by 70%+
- **Component complexity**: Cyclomatic complexity < 10
- **Test coverage**: Should increase by 20%+
- **Build time**: Should decrease by 10%+
- **Bundle size**: Should decrease or stay same (tree-shaking)
- **Developer velocity**: Measure PR review time

---

## Notes

This is a **living document**. Update it as refactoring progresses. Mark items as ✅ COMPLETED when done.

**Related Documents**:
- [DEVELOPMENT_WORKFLOW.md](./DEVELOPMENT_WORKFLOW.md) - Development process
- [CONTRIBUTING.md](./CONTRIBUTING.md) - Contribution guidelines
- [API_CONTRACT_GENERATION.md](./API_CONTRACT_GENERATION.md) - API type generation

**Created**: 2026-02-09  
**Last Updated**: 2026-02-09  
**Status**: High Priority Complete (Phases 1-3 for BacklogPage, QAService decomposition)
