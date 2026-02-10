import { CheckCircle, Clock, PlayCircle, Ban, SkipForward } from 'lucide-react';
import { CooldownActivityType, CooldownActivityStatus } from '../services/cooldownActivityService';

export const getActivityTypeIcon = (type: CooldownActivityType): string => {
  const icons: Record<CooldownActivityType, string> = {
    TECH_DEBT: '🔧',
    BUG_FIX: '🐛',
    REFACTORING: '♻️',
    KICKOFF_PREP: '🚀',
    LEARNING: '📚',
    QA: '✅',
    DOCUMENTATION: '📝',
    INFRASTRUCTURE: '🏗️',
    RESEARCH: '🔬',
    PLANNING: '📅',
    OTHER: '📋',
  };
  return icons[type] || '📋';
};

export const getActivityTypeBadgeColor = (type: CooldownActivityType): string => {
  const colors: Record<CooldownActivityType, string> = {
    TECH_DEBT: 'bg-orange-100 text-orange-800 border-orange-200',
    BUG_FIX: 'bg-red-100 text-red-800 border-red-200',
    REFACTORING: 'bg-cyan-100 text-cyan-800 border-cyan-200',
    KICKOFF_PREP: 'bg-indigo-100 text-indigo-800 border-indigo-200',
    LEARNING: 'bg-green-100 text-green-800 border-green-200',
    QA: 'bg-emerald-100 text-emerald-800 border-emerald-200',
    DOCUMENTATION: 'bg-slate-100 text-slate-800 border-slate-200',
    INFRASTRUCTURE: 'bg-amber-100 text-amber-800 border-amber-200',
    RESEARCH: 'bg-purple-100 text-purple-800 border-purple-200',
    PLANNING: 'bg-violet-100 text-violet-800 border-violet-200',
    OTHER: 'bg-gray-100 text-gray-800 border-gray-200',
  };
  return colors[type] || 'bg-gray-100 text-gray-800 border-gray-200';
};

export const getStatusBadgeColor = (status: CooldownActivityStatus): string => {
  const colors: Record<CooldownActivityStatus, string> = {
    PLANNED: 'bg-slate-100 text-slate-700 border-slate-300',
    IN_PROGRESS: 'bg-blue-100 text-blue-700 border-blue-300',
    COMPLETED: 'bg-green-100 text-green-700 border-green-300',
    SKIPPED: 'bg-amber-100 text-amber-700 border-amber-300',
    BLOCKED: 'bg-red-100 text-red-700 border-red-300',
  };
  return colors[status] || 'bg-gray-100 text-gray-700 border-gray-300';
};

export const getStatusIcon = (status: CooldownActivityStatus): JSX.Element => {
  const icons: Record<CooldownActivityStatus, JSX.Element> = {
    PLANNED: <Clock className="h-4 w-4" />,
    IN_PROGRESS: <PlayCircle className="h-4 w-4" />,
    COMPLETED: <CheckCircle className="h-4 w-4" />,
    SKIPPED: <SkipForward className="h-4 w-4" />,
    BLOCKED: <Ban className="h-4 w-4" />,
  };
  return icons[status] || <Clock className="h-4 w-4" />;
};
