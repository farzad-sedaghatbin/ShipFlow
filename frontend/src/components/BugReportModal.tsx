import React, { useState } from 'react';
import { X } from 'lucide-react';
import {
  BugReport,
  CreateBugReportRequest,
  UpdateBugReportRequest,
  BugSeverity,
  BugStatus,
} from '../types';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { cn } from '../lib/utils';

interface BugReportModalProps {
  open: boolean;
  onClose: () => void;
  onSubmit: (data: CreateBugReportRequest | UpdateBugReportRequest) => Promise<void>;
  bugReport?: BugReport | null;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  testRunId?: number;
}

const severities: BugSeverity[] = ['TRIVIAL', 'MINOR', 'MAJOR', 'CRITICAL', 'BLOCKER'];
const statuses: BugStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'VERIFIED', 'CLOSED', 'REOPENED', 'WONT_FIX', 'DUPLICATE'];

const severityVariants: Record<BugSeverity, string> = {
  TRIVIAL: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  MINOR: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  MAJOR: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  CRITICAL: 'bg-red-500/20 text-red-400 border-red-500/30',
  BLOCKER: 'bg-red-600/20 text-red-500 border-red-600/30',
};

const statusVariants: Record<BugStatus, string> = {
  OPEN: 'bg-red-500/20 text-red-400 border-red-500/30',
  IN_PROGRESS: 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30',
  RESOLVED: 'bg-blue-500/20 text-blue-400 border-blue-500/30',
  VERIFIED: 'bg-green-500/20 text-green-400 border-green-500/30',
  CLOSED: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  REOPENED: 'bg-red-500/20 text-red-400 border-red-500/30',
  WONT_FIX: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
  DUPLICATE: 'bg-zinc-500/20 text-zinc-400 border-zinc-500/30',
};

const BugReportModal: React.FC<BugReportModalProps> = ({
  open,
  onClose,
  onSubmit,
  bugReport,
  pitchId,
  cycleId,
  teamId,
  testRunId,
}) => {
  const isEdit = !!bugReport;
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [tagInput, setTagInput] = useState('');

  const [formData, setFormData] = useState<Partial<CreateBugReportRequest>>({
    title: bugReport?.title || '',
    description: bugReport?.description || '',
    stepsToReproduce: bugReport?.stepsToReproduce || '',
    expectedBehavior: bugReport?.expectedBehavior || '',
    actualBehavior: bugReport?.actualBehavior || '',
    environment: bugReport?.environment || '',
    severity: bugReport?.severity || 'MAJOR',
    status: bugReport?.status || 'OPEN',
    tags: bugReport?.tagList || [],
    pitchId: bugReport?.pitchId || pitchId,
    cycleId: bugReport?.cycleId || cycleId,
    teamId: bugReport?.teamId || teamId,
    testRunId: bugReport?.testRunId || testRunId,
  });

  const handleChange = (field: keyof CreateBugReportRequest, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleAddTag = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && tagInput.trim()) {
      e.preventDefault();
      const newTag = tagInput.trim();
      if (!formData.tags?.includes(newTag)) {
        handleChange('tags', [...(formData.tags || []), newTag]);
      }
      setTagInput('');
    }
  };

  const handleRemoveTag = (tagToRemove: string) => {
    handleChange('tags', formData.tags?.filter((tag) => tag !== tagToRemove) || []);
  };

  const handleSubmit = async () => {
    if (!formData.title?.trim()) {
      setError('Title is required');
      return;
    }
    if (!formData.description?.trim()) {
      setError('Description is required');
      return;
    }
    if (!formData.severity) {
      setError('Severity is required');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      await onSubmit(formData as CreateBugReportRequest);
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save bug report');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>
            {isEdit ? `Edit Bug Report: ${bugReport.bugKey}` : 'Report New Bug'}
          </DialogTitle>
        </DialogHeader>
        
        <div className="space-y-4 py-4">
          {error && (
            <Alert variant="destructive">
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* Title */}
          <div className="space-y-2">
            <Label htmlFor="bug-title">Title *</Label>
            <Input
              id="bug-title"
              value={formData.title}
              onChange={(e) => handleChange('title', e.target.value)}
              placeholder="Brief summary of the bug"
            />
            <p className="text-xs text-muted-foreground">Brief summary of the bug</p>
          </div>

          {/* Description */}
          <div className="space-y-2">
            <Label htmlFor="bug-description">Description *</Label>
            <Textarea
              id="bug-description"
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              placeholder="Detailed description of the bug (Markdown supported)"
              rows={4}
            />
            <p className="text-xs text-muted-foreground">Detailed description (Markdown supported)</p>
          </div>

          {/* Severity & Status */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label>Severity *</Label>
              <Select
                value={formData.severity}
                onValueChange={(value) => handleChange('severity', value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select severity" />
                </SelectTrigger>
                <SelectContent>
                  {severities.map((severity) => (
                    <SelectItem key={severity} value={severity}>
                      <Badge variant="outline" className={cn('mr-2', severityVariants[severity])}>
                        {severity}
                      </Badge>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            {isEdit && (
              <div className="space-y-2">
                <Label>Status</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => handleChange('status', value)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {statuses.map((status) => (
                      <SelectItem key={status} value={status}>
                        <Badge variant="outline" className={cn('mr-2', statusVariants[status])}>
                          {status.replace('_', ' ')}
                        </Badge>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}
          </div>

          {/* Steps to Reproduce */}
          <div className="space-y-2">
            <Label htmlFor="steps-to-reproduce">Steps to Reproduce</Label>
            <Textarea
              id="steps-to-reproduce"
              value={formData.stepsToReproduce}
              onChange={(e) => handleChange('stepsToReproduce', e.target.value)}
              placeholder={`1. Go to...\n2. Click on...\n3. See error`}
              rows={3}
            />
          </div>

          {/* Expected & Actual Behavior */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="expected-behavior">Expected Behavior</Label>
              <Textarea
                id="expected-behavior"
                value={formData.expectedBehavior}
                onChange={(e) => handleChange('expectedBehavior', e.target.value)}
                placeholder="What should happen"
                rows={2}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="actual-behavior">Actual Behavior</Label>
              <Textarea
                id="actual-behavior"
                value={formData.actualBehavior}
                onChange={(e) => handleChange('actualBehavior', e.target.value)}
                placeholder="What actually happens"
                rows={2}
              />
            </div>
          </div>

          {/* Environment */}
          <div className="space-y-2">
            <Label htmlFor="environment">Environment</Label>
            <Input
              id="environment"
              value={formData.environment}
              onChange={(e) => handleChange('environment', e.target.value)}
              placeholder="Browser, OS, version, device, etc."
            />
          </div>

          {/* Tags */}
          <div className="space-y-2">
            <Label htmlFor="tags">Tags</Label>
            <Input
              id="tags"
              value={tagInput}
              onChange={(e) => setTagInput(e.target.value)}
              onKeyDown={handleAddTag}
              placeholder="Add tags (press Enter)"
            />
            {formData.tags && formData.tags.length > 0 && (
              <div className="flex flex-wrap gap-1 mt-2">
                {formData.tags.map((tag) => (
                  <Badge key={tag} variant="outline" className="gap-1">
                    {tag}
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(tag)}
                      className="ml-1 hover:text-destructive"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
            )}
          </div>

          {/* Resolution (edit only) */}
          {isEdit && (
            <div className="space-y-2">
              <Label htmlFor="resolution">Resolution</Label>
              <Textarea
                id="resolution"
                value={(formData as UpdateBugReportRequest).resolution || ''}
                onChange={(e) => handleChange('resolution' as keyof CreateBugReportRequest, e.target.value)}
                placeholder="How was this bug fixed?"
                rows={2}
              />
            </div>
          )}
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={onClose} disabled={loading}>
            Cancel
          </Button>
          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? 'Saving...' : isEdit ? 'Update' : 'Create'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default BugReportModal;
