import { useTranslation } from 'react-i18next';
import { Plus, List, Kanban, GanttChartSquare, CalendarDays, Download, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { Cycle, Project } from '../../types';
import { ViewMode } from './backlogTypes';

export interface BacklogHeaderProps {
  categoryDescription: string;
  cycles: Cycle[];
  currentProject: Project | null;
  isKanbanProject: boolean;
  selectedCycle: number | 'all';
  viewMode: ViewMode;
  exportLoading?: boolean;
  onCycleChange: (value: number | 'all') => void;
  onViewModeChange: (mode: ViewMode) => void;
  onNewTask: () => void;
  onExportCsv?: () => void;
}

export function BacklogHeader({
  categoryDescription,
  cycles,
  currentProject,
  isKanbanProject,
  selectedCycle,
  viewMode,
  exportLoading = false,
  onCycleChange,
  onViewModeChange,
  onNewTask,
  onExportCsv,
}: BacklogHeaderProps) {
  const { t } = useTranslation();

  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{t('backlogPage.title')}</h1>
        <p className="text-muted-foreground">{categoryDescription}</p>
      </div>
      <div className="flex items-center gap-2">
        {/* Cycle Selector - Hidden for Kanban projects */}
        {!isKanbanProject && (
          <Select
            value={selectedCycle === 'all' ? 'all' : selectedCycle?.toString() || ''}
            onValueChange={(value) => onCycleChange(value === 'all' ? 'all' : Number(value))}
          >
            <SelectTrigger className="w-[250px]">
              <SelectValue placeholder={t('backlogPage.selectCycle')} />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">{t('backlogPage.allCycles')}</SelectItem>

              {/* Current Project Cycles */}
              {currentProject && cycles.filter(c => c.projectId === currentProject.id).length > 0 && (
                <>
                  <div className="px-2 py-1.5 text-xs font-semibold text-muted-foreground">
                    {currentProject.name}
                  </div>
                  {cycles
                    .filter(c => c.projectId === currentProject.id)
                    .map((cycle) => (
                      <SelectItem key={cycle.id} value={cycle.id.toString()}>
                        {cycle.name}
                      </SelectItem>
                    ))}
                </>
              )}

              {/* Other Projects Cycles */}
              {cycles.filter(c => !currentProject || c.projectId !== currentProject.id).length > 0 && (
                <>
                  <div className="px-2 py-1.5 text-xs font-semibold text-muted-foreground">
                    {t('backlogPage.otherProjects')}
                  </div>
                  {cycles
                    .filter(c => !currentProject || c.projectId !== currentProject.id)
                    .map((cycle) => (
                      <SelectItem key={cycle.id} value={cycle.id.toString()}>
                        {cycle.name}
                      </SelectItem>
                    ))}
                </>
              )}
            </SelectContent>
          </Select>
        )}

        {/* View Mode Toggle */}
        <div className="flex items-center border rounded-md">
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant={viewMode === 'list' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => onViewModeChange('list')}
                  className="rounded-r-none border-r"
                >
                  <List className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewMode.list')}</TooltipContent>
            </Tooltip>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant={viewMode === 'kanban' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => onViewModeChange('kanban')}
                  className="rounded-none border-x"
                >
                  <Kanban className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewMode.kanban')}</TooltipContent>
            </Tooltip>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant={viewMode === 'gantt' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => onViewModeChange('gantt')}
                  className="rounded-none border-x"
                >
                  <GanttChartSquare className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewMode.gantt')}</TooltipContent>
            </Tooltip>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant={viewMode === 'calendar' ? 'default' : 'ghost'}
                  size="sm"
                  onClick={() => onViewModeChange('calendar')}
                  className="rounded-l-none"
                >
                  <CalendarDays className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewMode.calendar')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>

        {/* Export CSV Button */}
        {onExportCsv && (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={onExportCsv}
                  disabled={exportLoading || !currentProject}
                  aria-label={t('backlogPage.exportCsv')}
                >
                  {exportLoading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4" />
                  )}
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.exportCsv')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        )}

        {/* New Task Button */}
        {isKanbanProject ? (
          <Button onClick={onNewTask}>
            <Plus className="mr-2 h-4 w-4" />
            {t('backlogPage.newTask')}
          </Button>
        ) : (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <span>
                  <Button
                    onClick={onNewTask}
                    disabled={selectedCycle === 'all' || !selectedCycle}
                  >
                    <Plus className="mr-2 h-4 w-4" />
                    {t('backlogPage.newTask')}
                  </Button>
                </span>
              </TooltipTrigger>
              {(selectedCycle === 'all' || !selectedCycle) && (
                <TooltipContent>
                  <p>{t('backlogPage.selectCycleToCreate')}</p>
                </TooltipContent>
              )}
            </Tooltip>
          </TooltipProvider>
        )}
      </div>
    </div>
  );
}
