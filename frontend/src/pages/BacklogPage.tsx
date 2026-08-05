import { useRef } from 'react';
import { FileText, Wrench, Info } from 'lucide-react';
import { Tabs, TabsContent } from '@/components/ui/tabs';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { TaskCategory, Task } from '../types';
import TimerWidget, { TimerWidgetHandle } from '../components/TimerWidget';
import KanbanBoard from '../components/KanbanBoard';
import { BacklogSkeleton } from '../components/Skeletons';
import { useBacklogPage } from '../hooks/useBacklogPage';
import {
  BacklogHeader,
  BacklogFilters,
  BacklogStatistics,
  BacklogTaskTable,
  BacklogTaskDialog,
  BacklogViewDialog,
  BacklogDeleteDialog,
  GanttView,
  CalendarView,
} from '../components/backlog';
import { useTranslation } from 'react-i18next';

export default function BacklogPage() {
  const { t } = useTranslation();
  const bp = useBacklogPage();
  const timerWidgetRef = useRef<TimerWidgetHandle>(null);

  const handleStartTimerOrFocus = (task: Task) => {
    if (bp.activeTimerTaskId === task.id) {
      // Timer already running for this task — scroll to and expand the widget
      timerWidgetRef.current?.focusAndExpand();
    } else {
      bp.handleStartTimer(task);
    }
  };

  if (bp.loading || bp.isSwitchingProject) {
    return <BacklogSkeleton />;
  }

  const kanbanProps = {
    tasks: bp.tasks,
    onStatusChange: bp.handleQuickStatusChange,
    // Fix: use handleViewTask so subtasks are loaded and view history is tracked
    onViewTask: (task: Task) => bp.handleViewTask(task),
    onEditTask: bp.handleOpenDialog,
    onDeleteTask: (taskId: number) => bp.setDeleteDialog({ open: true, taskId }),
    onAddSubtask: bp.handleAddSubTask,
    onStartTimer: handleStartTimerOrFocus,
    loading: bp.tasksLoading,
    // Fix: wire column visibility controls
    visibleColumns: bp.visibleColumns,
    onToggleColumn: bp.handleToggleColumn,
  };

  const taskTableProps = {
    tasks: bp.tasks,
    totalElements: bp.totalElements,
    tasksLoading: bp.tasksLoading,
    selectedCycle: bp.selectedCycle,
    page: bp.page,
    rowsPerPage: bp.rowsPerPage,
    sortBy: bp.sortBy,
    sortOrder: bp.sortOrder,
    activeTimerTaskId: bp.activeTimerTaskId,
    categoryTitle: bp.categoryTitle,
    persons: bp.persons,
    selectedTaskIds: bp.selectedTaskIds,
    onSelectedTaskIdsChange: bp.setSelectedTaskIds,
    onBulkSuccess: bp.loadTasks,
    onSort: bp.handleSort,
    onChangePage: bp.setPage,
    onChangeRowsPerPage: (val: string) => { bp.setRowsPerPage(parseInt(val, 10)); bp.setPage(0); },
    onViewTask: bp.handleViewTask,
    onEditTask: bp.handleOpenDialog,
    onDeleteTask: (taskId: number) => bp.setDeleteDialog({ open: true, taskId }),
    onAddSubTask: bp.handleAddSubTask,
    onStartTimer: handleStartTimerOrFocus,
    onQuickStatusChange: bp.handleQuickStatusChange,
    onQuickPriorityChange: bp.handleQuickPriorityChange,
    onQuickAssigneeChange: bp.handleQuickAssigneeChange,
    onOpenDialog: () => bp.handleOpenDialog(),
    onReorder: bp.handleReorder,
  };

  return (
    <div className="space-y-6" data-tour="backlog-board">
      <TimerWidget ref={timerWidgetRef} onTimerStopped={bp.handleTimerStopped} />

      <BacklogHeader
        categoryDescription={bp.categoryDescription}
        cycles={bp.cycles}
        currentProject={bp.currentProject}
        isKanbanProject={bp.isKanbanProject}
        selectedCycle={bp.selectedCycle}
        viewMode={bp.viewMode}
        exportLoading={bp.exportLoading}
        canSkipCycle={bp.canSkipCycleForDebtImprovement}
        onCycleChange={bp.setSelectedCycle}
        onViewModeChange={bp.setViewMode}
        onNewTask={() => bp.handleOpenDialog()}
        onExportCsv={bp.handleExportCsv}
      />

      {/* "All Projects" view can't create tasks (they belong to one project),
          and the disabled button alone reads as broken — surface why inline. */}
      {!bp.currentProject && (
        <Alert>
          <Info className="h-4 w-4" />
          <AlertDescription>{t('backlogPage.allProjectsHint')}</AlertDescription>
        </Alert>
      )}

      {/* Feature Tasks / Debt & Improvements is a Shape Up-specific distinction
          (pitch-scoped bet work vs. opportunistic filler) - Kanban has no Pitch
          concept, so this split isn't meaningful there. loadTasks() in
          useBacklogPage fetches every task for Kanban regardless of category. */}
      {!bp.isKanbanProject && (
        <Tabs value={bp.activeCategory} onValueChange={(v) => bp.handleCategoryChange(v as TaskCategory)} className="w-full">
          <div className="flex items-center gap-2">
            <button
              className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${bp.activeCategory === 'PITCH_SCOPE' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent'}`}
              onClick={() => bp.handleCategoryChange('PITCH_SCOPE')}
            >
              <FileText className="h-4 w-4" />
              {t('backlogPage.pitchTasks')}
            </button>
            <button
              className={`flex items-center gap-2 px-3 py-1.5 rounded-md text-sm font-medium transition-colors ${bp.activeCategory === 'DEBT_IMPROVEMENT' ? 'bg-primary text-primary-foreground' : 'text-muted-foreground hover:bg-accent'}`}
              onClick={() => bp.handleCategoryChange('DEBT_IMPROVEMENT')}
            >
              <Wrench className="h-4 w-4" />
              {t('backlogPage.debtImprovements')}
            </button>
          </div>
        </Tabs>
      )}

      {bp.statistics && <BacklogStatistics statistics={bp.statistics} />}

      <Tabs value={bp.tabValue} onValueChange={bp.setTabValue} className="w-full">
        <BacklogFilters
          onTabChange={(tab) => bp.setTabValue(tab)}
          statusFilter={bp.statusFilter}
          onStatusFilterChange={bp.handleToggleStatusFilter}
          priorityFilter={bp.priorityFilter}
          onPriorityFilterChange={bp.handleTogglePriorityFilter}
          dependencyFilter={bp.dependencyFilter}
          onDependencyFilterChange={bp.setDependencyFilter}
          searchQuery={bp.searchQuery}
          onSearchQueryChange={bp.setSearchQuery}
          persons={bp.persons}
          assigneeFilter={bp.assigneeFilter}
          onAssigneeFilterChange={bp.handleToggleAssigneeFilter}
          hasActiveFilters={bp.hasActiveFilters}
          onClearFilters={bp.handleClearFilters}
        />

        <TabsContent value="all" className="mt-0">
          {bp.viewMode === 'calendar' ? (
            <CalendarView tasks={bp.tasks} onViewTask={bp.handleViewTask} />
          ) : bp.viewMode === 'gantt' ? (
            <GanttView
              tasks={bp.tasks}
              cycles={bp.cycles}
              selectedCycle={bp.selectedCycle}
              onViewTask={bp.handleViewTask}
            />
          ) : bp.viewMode === 'kanban' ? (
            <KanbanBoard {...kanbanProps} />
          ) : (
            <BacklogTaskTable {...taskTableProps} />
          )}
        </TabsContent>
        <TabsContent value="my" className="mt-0">
          {bp.viewMode === 'calendar' ? (
            <CalendarView tasks={bp.tasks} onViewTask={bp.handleViewTask} />
          ) : bp.viewMode === 'gantt' ? (
            <GanttView
              tasks={bp.tasks}
              cycles={bp.cycles}
              selectedCycle={bp.selectedCycle}
              onViewTask={bp.handleViewTask}
            />
          ) : bp.viewMode === 'kanban' ? (
            <KanbanBoard {...kanbanProps} />
          ) : (
            <BacklogTaskTable {...taskTableProps} />
          )}
        </TabsContent>
      </Tabs>

      <BacklogTaskDialog
        open={bp.dialogOpen}
        editingTask={bp.editingTask}
        formData={bp.formData}
        dueDate={bp.dueDate}
        fieldErrors={bp.fieldErrors}
        saving={bp.saving}
        persons={bp.persons}
        teams={bp.teams}
        pitches={bp.pitches}
        activeCategory={bp.activeCategory}
        isKanbanProject={bp.isKanbanProject}
        onOpenChange={bp.setDialogOpen}
        onFormDataChange={bp.setFormData}
        onDueDateChange={bp.setDueDate}
        onPitchChange={bp.handlePitchChange}
        onSave={bp.handleSaveTask}
        onClose={bp.handleCloseDialog}
      />

      <BacklogViewDialog
        open={bp.viewDialog.open}
        task={bp.viewDialog.task}
        subtasks={bp.subtasks}
        viewHistory={bp.viewHistory}
        onClose={bp.handleCloseViewDialog}
        onBack={bp.handleViewBack}
        onViewTask={bp.handleViewTask}
        onEditTask={bp.handleOpenDialog}
        onReloadTasks={bp.loadTasks}
        setViewDialogTask={(task) => bp.setViewDialog({ open: true, task })}
      />

      <BacklogDeleteDialog
        open={bp.deleteDialog.open}
        onOpenChange={(open) => bp.setDeleteDialog({ ...bp.deleteDialog, open })}
        onCancel={() => bp.setDeleteDialog({ open: false, taskId: null })}
        onConfirm={bp.handleDeleteTask}
      />
    </div>
  );
}
