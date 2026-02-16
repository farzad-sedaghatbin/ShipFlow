import { useTranslation } from 'react-i18next';
import { Plus, List, Kanban } from 'lucide-react';
import { Button } from '../ui/button';
import { Combobox } from '../ui/combobox';
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from '../ui/tooltip';
import { Cycle, Project } from '../../types';

export interface BacklogHeaderProps {
  currentProject: Project | null;
  cycles: Cycle[];
  selectedCycle: number | 'all' | undefined;
  onCycleChange: (value: number | 'all') => void;
  viewMode: 'list' | 'kanban';
  onViewModeChange: (mode: 'list' | 'kanban') => void;
  isKanbanProject: boolean;
  onNewTask: () => void;
  categoryDescription: string;
}

export function BacklogHeader({
  currentProject,
  cycles,
  selectedCycle,
  onCycleChange,
  viewMode,
  onViewModeChange,
  isKanbanProject,
  onNewTask,
  categoryDescription,
}: BacklogHeaderProps) {
  const { t } = useTranslation();

  const canCreateTask = isKanbanProject || (selectedCycle !== 'all' && selectedCycle !== undefined);

  return (
    <div className="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">{t('backlogPage.title')}</h1>
        <p className="text-muted-foreground">{categoryDescription}</p>
      </div>
      <div className="flex items-center gap-2">
        {/* Cycle Selector - Hidden for Kanban projects */}
        {!isKanbanProject && (
          <Combobox
            options={[
              { value: 'all', label: t('backlogPage.allCycles') },
              ...(currentProject ? cycles.filter(c => c.projectId === currentProject.id).map(cycle => ({
                value: cycle.id.toString(),
                label: `${currentProject.name}: ${cycle.name}`
              })) : []),
              ...cycles.filter(c => !currentProject || c.projectId !== currentProject.id).map(cycle => ({
                value: cycle.id.toString(),
                label: cycle.name
              }))
            ]}
            value={selectedCycle === 'all' ? 'all' : selectedCycle?.toString() || ''}
            onValueChange={(value) => {
              // Guard against empty string from potential Combobox edge cases
              if (!value || value === 'all') {
                onCycleChange('all');
              } else {
                onCycleChange(Number(value));
              }
            }}
            placeholder={t('backlogPage.selectCycle')}
            searchPlaceholder={t('common.searchCycles')}
            triggerClassName="w-[250px]"
          />
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
                  className="rounded-r-none"
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
                  className="rounded-l-none"
                >
                  <Kanban className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>{t('backlogPage.viewMode.kanban')}</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        </div>

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
                    disabled={!canCreateTask}
                  >
                    <Plus className="mr-2 h-4 w-4" />
                    {t('backlogPage.newTask')}
                  </Button>
                </span>
              </TooltipTrigger>
              {!canCreateTask && (
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
