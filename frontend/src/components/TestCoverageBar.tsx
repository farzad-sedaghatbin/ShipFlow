import React from 'react';
import { CheckCircle, XCircle, Ban, SkipForward } from 'lucide-react';
import { TestCoverage } from '../types';
import { Progress } from './ui/progress';
import { Badge } from './ui/badge';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import { cn } from '../lib/utils';

interface TestCoverageBarProps {
  coverage: TestCoverage;
  showDetails?: boolean;
  compact?: boolean;
}

const TestCoverageBar: React.FC<TestCoverageBarProps> = ({
  coverage,
  showDetails = true,
  compact = false,
}) => {
  const getCoverageClass = (score: number) => {
    if (score >= 80) return '[&>div]:bg-green-500';
    if (score >= 50) return '[&>div]:bg-yellow-500';
    return '[&>div]:bg-red-500';
  };

  const getPassRateColor = (rate: number) => {
    if (rate >= 90) return 'text-green-500';
    if (rate >= 70) return 'text-yellow-500';
    return 'text-red-500';
  };

  if (compact) {
    return (
      <TooltipProvider>
        <Tooltip>
          <TooltipTrigger asChild>
            <div className="w-full">
              <Progress
                value={coverage.coverageScore}
                className={cn("h-2", getCoverageClass(coverage.coverageScore))}
              />
            </div>
          </TooltipTrigger>
          <TooltipContent>
            <div className="space-y-1 p-1">
              <p className="text-sm">Coverage: {coverage.coverageScore.toFixed(1)}%</p>
              <p className="text-sm">Pass Rate: {coverage.passRate.toFixed(1)}%</p>
              <p className="text-sm">Test Cases: {coverage.totalTestCases}</p>
              <p className="text-sm">Open Bugs: {coverage.openBugs}</p>
            </div>
          </TooltipContent>
        </Tooltip>
      </TooltipProvider>
    );
  }

  return (
    <div className="w-full space-y-4">
      {/* Coverage Score */}
      <div>
        <div className="flex justify-between mb-1">
          <span className="text-sm text-muted-foreground">Coverage Score</span>
          <span className={cn("text-sm font-bold", getPassRateColor(coverage.coverageScore))}>
            {coverage.coverageScore.toFixed(1)}%
          </span>
        </div>
        <Progress
          value={coverage.coverageScore}
          className={cn("h-2.5", getCoverageClass(coverage.coverageScore))}
        />
      </div>

      {/* Pass Rate */}
      <div>
        <div className="flex justify-between mb-1">
          <span className="text-sm text-muted-foreground">Pass Rate</span>
          <span className={cn("text-sm font-bold", getPassRateColor(coverage.passRate))}>
            {coverage.passRate.toFixed(1)}%
          </span>
        </div>
        <Progress
          value={coverage.passRate}
          className={cn("h-2.5", getCoverageClass(coverage.passRate))}
        />
      </div>

      {showDetails && (
        <>
          {/* Test Run Stats */}
          <TooltipProvider>
            <div className="flex flex-wrap gap-2">
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="outline" className="gap-1 bg-green-500/10 text-green-500 border-green-500/30">
                    <CheckCircle className="h-3 w-3" />
                    {coverage.passedRuns}
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>Passed Tests</TooltipContent>
              </Tooltip>
              
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="outline" className="gap-1 bg-red-500/10 text-red-500 border-red-500/30">
                    <XCircle className="h-3 w-3" />
                    {coverage.failedRuns}
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>Failed Tests</TooltipContent>
              </Tooltip>
              
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="outline" className="gap-1 bg-yellow-500/10 text-yellow-500 border-yellow-500/30">
                    <Ban className="h-3 w-3" />
                    {coverage.blockedRuns}
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>Blocked Tests</TooltipContent>
              </Tooltip>
              
              <Tooltip>
                <TooltipTrigger asChild>
                  <Badge variant="outline" className="gap-1">
                    <SkipForward className="h-3 w-3" />
                    {coverage.skippedRuns}
                  </Badge>
                </TooltipTrigger>
                <TooltipContent>Skipped Tests</TooltipContent>
              </Tooltip>
            </div>
          </TooltipProvider>

          {/* Bug Stats */}
          <div className="flex justify-between items-center">
            <span className="text-sm text-muted-foreground">
              Test Cases: <strong>{coverage.totalTestCases}</strong>
            </span>
            <span className="text-sm text-muted-foreground">
              Open Bugs:{' '}
              <strong className={coverage.openBugs > 0 ? 'text-red-500' : 'text-green-500'}>
                {coverage.openBugs}
              </strong>
            </span>
          </div>

          {/* Critical/Blocker Bugs Warning */}
          {(coverage.criticalBugs > 0 || coverage.blockerBugs > 0) && (
            <div className="flex flex-wrap gap-2">
              {coverage.blockerBugs > 0 && (
                <Badge className="bg-red-500 text-white">
                  {coverage.blockerBugs} Blocker
                </Badge>
              )}
              {coverage.criticalBugs > 0 && (
                <Badge className="bg-yellow-500 text-black">
                  {coverage.criticalBugs} Critical
                </Badge>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
};

export default TestCoverageBar;
