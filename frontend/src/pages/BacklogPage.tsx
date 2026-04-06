import { FileText, Wrench } from 'lucide-react';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { TaskCategory } from '../types';
import TimerWidget from '../components/TimerWidget';
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
} from '../components/backlog';
import { useTranslation } from 'react-i18next';

export default function BacklogPage() {
  const { t } = useTranslation();
  const bp = useBacklogPage();

  if (bp.loading || bp.isSwitchingProject) {
    return <BacklogSkeleton />;
  }

  const kanbanProps = {
    tasks: bp.tasks,
    onStatusChange: bp.handleQuickStatusChange,
    onViewTask: (task: any) => bp.setViewDialog({ open: true, task }),
    onEditTask: bp.handleOpenDialog,
    onDeleteTask: (taskId: number) => bp.setDeleteDialog({ open: true, taskId }),
    onAddSubtask: bp.handleAddSubTask,
    onStartTimer: bp.handleStartTimer,
    loading: bp.tasksLoading,
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
    onStartTimer: bp.handleStartTimer,
    onQuickStatusChange: bp.handleQuickStatusChange,
    onQuickPriorityChange: bp.handleQuickPriorityChange,
    onOpenDialog: () => bp.handleOpenDialog(),
  };

  return (
    <div className="space-y-6">
      <TimerWidget onTimerStopped={bp.handleTimerStopped} />

      <BacklogHeader
        categoryDescription={bp.categoryDescription}
        cycles={bp.cycles}
        currentProject={bp.currentProject}
        isKanbanProject={bp.isKanbanProject}
        selectedCycle={bp.selectedCycle}
        viewMode={bp.viewMode}
        exportLoading={bp.exportLoading}
        onCycleChange={bp.setSelectedCycle}
        onViewModeChange={bp.setViewMode}
        onNewTask={() => bp.handleOpenDialog()}
        onExportCsv={bp.handleExportCsv}
      />

      <Tabs value={bp.activeCategory} onValueChange={(v) => bp.handleCategoryChange(v as TaskCategory)} className="w-full">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="PITCH_SCOPE" className="flex items-center gap-2">
            <FileText className="h-4 w-4" />
            {bp.isKanbanProject ? t('backlogPage.featureTasks') : t('backlogPage.pitchTasks')}
          </TabsTrigger>
          <TabsTrigger value="DEBT_IMPROVEMENT" className="flex items-center gap-2">
            <Wrench className="h-4 w-4" />
            {t('backlogPage.debtImprovements')}
          </TabsTrigger>
        </TabsList>
      </Tabs>

      {bp.statistics && <BacklogStatistics statistics={bp.statistics} />}

      <Tabs value={bp.tabValue} onValueChange={bp.setTabValue} className="w-full">
        <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between mb-4">
          <TabsList>
            <TabsTrigger value="all">{t('backlogPage.allTasks')}</TabsTrigger>
            <TabsTrigger value="my">{t('backlogPage.myTasks')}</TabsTrigger>
          </TabsList>
          <BacklogFilters
            statusFilter={bp.statusFilter}
            priorityFilter={bp.priorityFilter}
            assigneeFilter={bp.assigneeFilter}
            dependencyFilter={bp.dependencyFilter}
            statusDropdownOpen={bp.statusDropdownOpen}
            priorityDropdownOpen={bp.priorityDropdownOpen}
            dependencyDropdownOpen={bp.dependencyDropdownOpen}
            onStatusDropdownOpenChange={bp.setStatusDropdownOpen}
            onPriorityDropdownOpenChange={bp.setPriorityDropdownOpen}
            onDependencyDropdownOpenChange={bp.setDependencyDropdownOpen}
            onStatusFilterChange={bp.setStatusFilter}
            onPriorityFilterChange={bp.setPriorityFilter}
            onDependencyFilterChange={bp.setDependencyFilter}
            onClearFilters={bp.handleClearFilters}
          />
        </div>

        <TabsContent value="all" className="mt-0">
          {bp.viewMode === 'kanban' ? <KanbanBoard {...kanbanProps} /> : <BacklogTaskTable {...taskTableProps} />}
        </TabsContent>
        <TabsContent value="my" className="mt-0">
          {bp.viewMode === 'kanban' ? <KanbanBoard {...kanbanProps} /> : <BacklogTaskTable {...taskTableProps} />}
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
