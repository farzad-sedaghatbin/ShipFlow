import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  GripVertical,
  MoreVertical,
  Eye,
  ExternalLink,
  Link as LinkIcon,
  Pencil,
  Trash2,
  MessageSquare,
  Bug,
} from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ScrollArea } from '@/components/ui/scroll-area';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import { cn } from '@/lib/utils';
import { detectTextDirection } from '../utils/rtlDetection';
import { useToast } from '../contexts';
import { BugReport, BugStatus, BugSeverity } from '../types';

// Bug status columns configuration
const BUG_KANBAN_COLUMNS = [
  { status: 'OPEN' as BugStatus, labelKey: 'bugReports.status.open', color: 'bg-blue-500' },
  { status: 'IN_PROGRESS' as BugStatus, labelKey: 'bugReports.status.inProgress', color: 'bg-yellow-500' },
  { status: 'RESOLVED' as BugStatus, labelKey: 'bugReports.status.resolved', color: 'bg-purple-500' },
  { status: 'VERIFIED' as BugStatus, labelKey: 'bugReports.status.verified', color: 'bg-green-500' },
  { status: 'CLOSED' as BugStatus, labelKey: 'bugReports.status.closed', color: 'bg-gray-500' },
];

// Severity badge variants
const severityBadgeVariants: Record<BugSeverity, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  TRIVIAL: 'secondary',
  MINOR: 'outline', 
  MAJOR: 'default',
  CRITICAL: 'destructive',
  BLOCKER: 'destructive',
};

interface BugKanbanBoardProps {
  bugs: BugReport[];
  onStatusChange: (bugId: number, newStatus: BugStatus) => Promise<void>;
  onViewBug: (bug: BugReport) => void;
  onEditBug: (bug: BugReport) => void;
  onDeleteBug: (bugId: number) => void;
  loading?: boolean;
  updatingBugId?: number | null;
}

interface BugKanbanCardProps {
  bug: BugReport;
  onViewBug: (bug: BugReport) => void;
  onEditBug: (bug: BugReport) => void;
  onDeleteBug: (bugId: number) => void;
  dragging?: boolean;
  updating?: boolean;
}

interface BugKanbanColumnProps {
  status: BugStatus;
  labelKey: string;
  color: string;
  bugs: BugReport[];
  onStatusChange: (bugId: number, newStatus: BugStatus) => Promise<void>;
  onViewBug: (bug: BugReport) => void;
  onEditBug: (bug: BugReport) => void;
  onDeleteBug: (bugId: number) => void;
  updatingBugId?: number | null;
}


function BugKanbanCard({ bug, onViewBug, onEditBug, onDeleteBug, dragging, updating }: BugKanbanCardProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();

  const handleCopyLink = () => {
    const url = `${window.location.origin}/qa/bug-reports/${bug.id}`;
    navigator.clipboard.writeText(url);
    showToast(t('bugReports.linkCopied', 'Link copied to clipboard'), 'success');
  };
  
  const handleDragStart = (e: React.DragEvent) => {
    e.dataTransfer.setData('bugId', bug.id.toString());
    e.dataTransfer.effectAllowed = 'move';
  };

  return (
    <Card 
      className={cn(
        "mb-3 cursor-grab hover:shadow-md transition-shadow",
        dragging && "opacity-50 shadow-lg",
        updating && "opacity-50"
      )}
      draggable={!updating}
      onDragStart={handleDragStart}
    >
      <CardContent className="p-3">
        {/* Header with bug key and actions */}
        <div className="flex items-start justify-between gap-2 mb-2">
          <div className="flex items-center gap-2">
            <GripVertical className="h-4 w-4 text-muted-foreground cursor-grab" />
            <Bug className="h-3 w-3 text-destructive shrink-0" />
            <span className="text-xs text-muted-foreground font-mono">{bug.bugKey}</span>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" className="h-6 w-6">
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={() => onViewBug(bug)}>
                <Eye className="h-4 w-4 mr-2" />
                {t('common.preview', 'Preview')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => navigate(`/qa/bug-reports/${bug.id}`)}>
                <ExternalLink className="h-4 w-4 mr-2" />
                {t('common.openFullPage', 'Open full page')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={handleCopyLink}>
                <LinkIcon className="h-4 w-4 mr-2" />
                {t('bugReports.copyBugLink', 'Copy link')}
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => onEditBug(bug)}>
                <Pencil className="h-4 w-4 mr-2" />
                {t('common.edit')}
              </DropdownMenuItem>
              <DropdownMenuItem 
                onClick={() => onDeleteBug(bug.id)}
                className="text-destructive"
              >
                <Trash2 className="h-4 w-4 mr-2" />
                {t('common.delete')}
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>

        {/* Title */}
        <h4 
          className="font-medium text-sm mb-2 line-clamp-2 cursor-pointer hover:text-primary"
          onClick={() => onViewBug(bug)}
          dir={detectTextDirection(bug.title)}
        >
          {bug.title}
        </h4>

        {/* Severity Badge */}
        <div className="flex items-center justify-between mb-2">
          <Badge variant={severityBadgeVariants[bug.severity]} className="text-[10px]">
            {bug.severity}
          </Badge>
          
          {/* Assignee */}
          {bug.assigneeName && (
            <TooltipProvider>
              <Tooltip>
                <TooltipTrigger asChild>
                  <Avatar className="h-5 w-5">
                    <AvatarFallback className="text-[0.6rem]">
                      {bug.assigneeName.charAt(0)}
                    </AvatarFallback>
                  </Avatar>
                </TooltipTrigger>
                <TooltipContent>{bug.assigneeName}</TooltipContent>
              </Tooltip>
            </TooltipProvider>
          )}
        </div>

        {/* Comments count */}
        {(bug.commentCount ?? 0) > 0 && (
          <div className="flex items-center gap-1 text-xs text-muted-foreground">
            <MessageSquare className="h-3 w-3" />
            <span>{bug.commentCount}</span>
          </div>
        )}

        {/* Project info */}
        {bug.projectName && (
          <div className="text-xs text-muted-foreground mt-1">
            {bug.projectKey} • {bug.projectName}
          </div>
        )}
      </CardContent>
    </Card>
  );
}

function BugKanbanColumn({ 
  status, 
  labelKey, 
  color, 
  bugs, 
  onStatusChange, 
  onViewBug, 
  onEditBug, 
  onDeleteBug,
  updatingBugId
}: BugKanbanColumnProps) {
  const { t } = useTranslation();
  const [isDragOver, setIsDragOver] = useState(false);

  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
    setIsDragOver(true);
  };

  const handleDragLeave = () => {
    setIsDragOver(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragOver(false);
    const bugId = parseInt(e.dataTransfer.getData('bugId'), 10);
    if (!isNaN(bugId)) {
      await onStatusChange(bugId, status);
    }
  };

  const columnBugs = bugs.filter(bug => bug.status === status);

  return (
    <div 
      className={cn(
        "flex-shrink-0 w-72 bg-muted/30 rounded-lg p-3 transition-colors",
        isDragOver && "bg-primary/10 ring-2 ring-primary ring-inset"
      )}
      onDragOver={handleDragOver}
      onDragLeave={handleDragLeave}
      onDrop={handleDrop}
    >
      {/* Column Header */}
      <div className="flex items-center gap-2 mb-3 px-1">
        <div className={cn("w-3 h-3 rounded-full", color)} />
        <h3 className="font-semibold text-sm">{t(labelKey)}</h3>
        <Badge variant="secondary" className="ml-auto">
          {columnBugs.length}
        </Badge>
      </div>

      {/* Bugs */}
      <ScrollArea className="h-[calc(100vh-280px)]">
        <div className="pr-2">
          {columnBugs.length === 0 ? (
            <div className="text-center py-8 text-muted-foreground text-sm">
              {t('bugReports.kanban.noBugs')}
            </div>
          ) : (
            columnBugs.map(bug => (
              <BugKanbanCard
                key={bug.id}
                bug={bug}
                onViewBug={onViewBug}
                onEditBug={onEditBug}
                onDeleteBug={onDeleteBug}
                updating={updatingBugId === bug.id}
              />
            ))
          )}
        </div>
      </ScrollArea>
    </div>
  );
}

export default function BugKanbanBoard({ 
  bugs, 
  onStatusChange, 
  onViewBug, 
  onEditBug, 
  onDeleteBug,
  loading,
  updatingBugId
}: BugKanbanBoardProps) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {/* Kanban Board */}
      <div className="flex gap-4 overflow-x-auto pb-4">
        {BUG_KANBAN_COLUMNS.map(column => (
          <BugKanbanColumn
            key={column.status}
            status={column.status}
            labelKey={column.labelKey}
            color={column.color}
            bugs={bugs}
            onStatusChange={onStatusChange}
            onViewBug={onViewBug}
            onEditBug={onEditBug}
            onDeleteBug={onDeleteBug}
            updatingBugId={updatingBugId}
          />
        ))}
      </div>
    </div>
  );
}