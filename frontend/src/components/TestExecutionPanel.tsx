import React from 'react';
import {
  CheckCircle2,
  XCircle,
  Ban,
  SkipForward,
  Clock,
  Play,
  Bug,
  Eye,
} from 'lucide-react';
import { TestRun, TestRunStatus } from '../types';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from './ui/table';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import { cn } from '../lib/utils';

interface TestExecutionPanelProps {
  testRuns: TestRun[];
  onViewRun?: (run: TestRun) => void;
  onViewBug?: (bugId: number) => void;
  showTestCaseInfo?: boolean;
  maxRows?: number;
}

const statusConfig: Record<TestRunStatus, { 
  icon: React.ReactNode; 
  className: string;
}> = {
  PASSED: { 
    icon: <CheckCircle2 className="h-3.5 w-3.5" />, 
    className: 'bg-green-500/10 text-green-500 border-green-500/20' 
  },
  FAILED: { 
    icon: <XCircle className="h-3.5 w-3.5" />, 
    className: 'bg-red-500/10 text-red-500 border-red-500/20' 
  },
  BLOCKED: { 
    icon: <Ban className="h-3.5 w-3.5" />, 
    className: 'bg-amber-500/10 text-amber-500 border-amber-500/20' 
  },
  SKIPPED: { 
    icon: <SkipForward className="h-3.5 w-3.5" />, 
    className: 'bg-muted text-muted-foreground border-border' 
  },
  PENDING: { 
    icon: <Clock className="h-3.5 w-3.5" />, 
    className: 'bg-blue-500/10 text-blue-500 border-blue-500/20' 
  },
  RUNNING: { 
    icon: <Play className="h-3.5 w-3.5" />, 
    className: 'bg-violet-500/10 text-violet-500 border-violet-500/20' 
  },
};

const TestExecutionPanel: React.FC<TestExecutionPanelProps> = ({
  testRuns,
  onViewRun,
  onViewBug,
  showTestCaseInfo = true,
  maxRows,
}) => {
  const displayRuns = maxRows ? testRuns.slice(0, maxRows) : testRuns;

  const formatDuration = (seconds?: number) => {
    if (!seconds) return '-';
    if (seconds < 60) return `${seconds}s`;
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}m ${secs}s`;
  };

  const formatDate = (dateStr: string) => {
    const date = new Date(dateStr);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  if (testRuns.length === 0) {
    return (
      <div className="text-center py-8">
        <p className="text-sm text-muted-foreground">
          No test runs recorded yet
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-lg border border-border overflow-hidden">
      <Table>
        <TableHeader>
          <TableRow>
            {showTestCaseInfo && <TableHead>Test Case</TableHead>}
            <TableHead>Status</TableHead>
            <TableHead>Executed By</TableHead>
            <TableHead>Executed At</TableHead>
            <TableHead>Duration</TableHead>
            <TableHead>Build</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {displayRuns.map((run) => {
            const statusCfg = statusConfig[run.status];
            return (
              <TableRow key={run.id}>
                {showTestCaseInfo && (
                  <TableCell>
                    <div className="space-y-0.5">
                      <p className="text-sm font-medium">
                        {run.testCaseKey}
                      </p>
                      <p className="text-xs text-muted-foreground truncate max-w-[200px]">
                        {run.testCaseTitle}
                      </p>
                    </div>
                  </TableCell>
                )}
                <TableCell>
                  <Badge 
                    variant="outline" 
                    className={cn("gap-1", statusCfg.className)}
                  >
                    {statusCfg.icon}
                    {run.status}
                  </Badge>
                </TableCell>
                <TableCell>
                  <span className="text-sm">{run.executedByName}</span>
                </TableCell>
                <TableCell>
                  <span className="text-sm">{formatDate(run.executedAt)}</span>
                </TableCell>
                <TableCell>
                  <span className="text-sm">{formatDuration(run.durationSeconds)}</span>
                </TableCell>
                <TableCell>
                  <span className="text-sm text-muted-foreground">
                    {run.buildVersion || '-'}
                  </span>
                </TableCell>
                <TableCell>
                  <div className="flex items-center justify-end gap-1">
                    <TooltipProvider>
                      {onViewRun && (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8"
                              onClick={() => onViewRun(run)}
                            >
                              <Eye className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>View Details</TooltipContent>
                        </Tooltip>
                      )}
                      {run.bugReportId && onViewBug && (
                        <Tooltip>
                          <TooltipTrigger asChild>
                            <Button
                              variant="ghost"
                              size="icon"
                              className="h-8 w-8 text-red-500 hover:text-red-600"
                              onClick={() => onViewBug(run.bugReportId!)}
                            >
                              <Bug className="h-4 w-4" />
                            </Button>
                          </TooltipTrigger>
                          <TooltipContent>View Bug Report</TooltipContent>
                        </Tooltip>
                      )}
                    </TooltipProvider>
                  </div>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
      {maxRows && testRuns.length > maxRows && (
        <div className="text-center py-2 border-t border-border">
          <span className="text-xs text-muted-foreground">
            Showing {maxRows} of {testRuns.length} runs
          </span>
        </div>
      )}
    </div>
  );
};

export default TestExecutionPanel;
