import React, { useState } from 'react';
import {
  Save,
  Trash2,
  Sparkles,
  AlertCircle,
  X,
} from 'lucide-react';
import {
  TestCase,
  CreateTestCaseRequest,
  UpdateTestCaseRequest,
  TestCaseType,
  TestCasePriority,
  TestCaseStatus,
} from '../types';
import { Button } from './ui/button';
import { Card, CardContent } from './ui/card';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Label } from './ui/label';
import { Badge } from './ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from './ui/select';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import { cn } from '../lib/utils';

interface TestCaseEditorProps {
  testCase?: TestCase | null;
  onSave: (data: CreateTestCaseRequest | UpdateTestCaseRequest) => Promise<void>;
  onDelete?: () => Promise<void>;
  onGenerateWithAI?: () => void;
  pitchId?: number;
  cycleId?: number;
  teamId?: number;
  isLoading?: boolean;
}

const testTypes: TestCaseType[] = [
  'FUNCTIONAL',
  'INTEGRATION',
  'UNIT',
  'E2E',
  'REGRESSION',
  'SMOKE',
  'PERFORMANCE',
  'SECURITY',
  'USABILITY',
  'ACCESSIBILITY',
];

const priorities: TestCasePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

const statuses: TestCaseStatus[] = ['DRAFT', 'READY', 'APPROVED', 'DEPRECATED', 'ARCHIVED'];

const priorityStyles: Record<TestCasePriority, string> = {
  LOW: 'bg-muted text-muted-foreground',
  MEDIUM: 'bg-blue-500/10 text-blue-500',
  HIGH: 'bg-amber-500/10 text-amber-500',
  CRITICAL: 'bg-red-500/10 text-red-500',
};

const statusStyles: Record<TestCaseStatus, string> = {
  DRAFT: 'bg-muted text-muted-foreground',
  READY: 'bg-violet-500/10 text-violet-500',
  APPROVED: 'bg-green-500/10 text-green-500',
  DEPRECATED: 'bg-amber-500/10 text-amber-500',
  ARCHIVED: 'bg-red-500/10 text-red-500',
};

const TestCaseEditor: React.FC<TestCaseEditorProps> = ({
  testCase,
  onSave,
  onDelete,
  onGenerateWithAI,
  pitchId,
  cycleId,
  teamId,
  isLoading = false,
}) => {
  const isEdit = !!testCase;
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [tagInput, setTagInput] = useState('');

  const [formData, setFormData] = useState<Partial<CreateTestCaseRequest>>({
    title: testCase?.title || '',
    description: testCase?.description || '',
    preconditions: testCase?.preconditions || '',
    steps: testCase?.steps || '',
    expectedResult: testCase?.expectedResult || '',
    type: testCase?.type || 'FUNCTIONAL',
    priority: testCase?.priority || 'MEDIUM',
    status: testCase?.status || 'DRAFT',
    tags: testCase?.tagList || [],
    estimatedMinutes: testCase?.estimatedMinutes,
    pitchId: testCase?.pitchId || pitchId,
    cycleId: testCase?.cycleId || cycleId,
    teamId: testCase?.teamId || teamId,
  });

  const handleChange = (field: keyof CreateTestCaseRequest, value: unknown) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
    setError(null);
  };

  const handleAddTag = (tag: string) => {
    const trimmedTag = tag.trim().toLowerCase();
    if (trimmedTag && !formData.tags?.includes(trimmedTag)) {
      handleChange('tags', [...(formData.tags || []), trimmedTag]);
    }
    setTagInput('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    handleChange('tags', formData.tags?.filter(t => t !== tagToRemove) || []);
  };

  const handleSave = async () => {
    if (!formData.title?.trim()) {
      setError('Title is required');
      return;
    }
    if (!formData.type) {
      setError('Test type is required');
      return;
    }
    if (!formData.priority) {
      setError('Priority is required');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      await onSave(formData as CreateTestCaseRequest);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save test case');
    } finally {
      setSaving(false);
    }
  };

  const suggestedTags = ['smoke', 'regression', 'critical-path', 'edge-case', 'happy-path', 'error-handling'];

  return (
    <Card className="border border-border">
      <CardContent className="p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">
            {isEdit ? `Edit Test Case: ${testCase.testCaseKey}` : 'New Test Case'}
          </h3>
          <div className="flex items-center gap-2">
            <TooltipProvider>
              {onGenerateWithAI && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button variant="ghost" size="icon" onClick={onGenerateWithAI}>
                      <Sparkles className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>Generate with AI</TooltipContent>
                </Tooltip>
              )}
              {isEdit && onDelete && (
                <Tooltip>
                  <TooltipTrigger asChild>
                    <Button variant="ghost" size="icon" className="text-red-500 hover:text-red-600" onClick={onDelete}>
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </TooltipTrigger>
                  <TooltipContent>Delete Test Case</TooltipContent>
                </Tooltip>
              )}
            </TooltipProvider>
          </div>
        </div>

        {error && (
          <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
            <AlertCircle className="h-4 w-4" />
            <span className="text-sm">{error}</span>
          </div>
        )}

        {testCase?.aiGenerated && (
          <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500">
            <Sparkles className="h-4 w-4" />
            <span className="text-sm">This test case was generated by AI</span>
          </div>
        )}

        <div className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">Title *</Label>
            <Input
              id="title"
              value={formData.title}
              onChange={(e) => handleChange('title', e.target.value)}
              placeholder="Clear, descriptive test case title"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <Textarea
              id="description"
              value={formData.description}
              onChange={(e) => handleChange('description', e.target.value)}
              rows={3}
              placeholder="What is being tested and why (Markdown supported)"
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="space-y-2">
              <Label>Type *</Label>
              <Select
                value={formData.type}
                onValueChange={(value) => handleChange('type', value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select type" />
                </SelectTrigger>
                <SelectContent>
                  {testTypes.map((type) => (
                    <SelectItem key={type} value={type}>
                      {type}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label>Priority *</Label>
              <Select
                value={formData.priority}
                onValueChange={(value) => handleChange('priority', value)}
              >
                <SelectTrigger>
                  <SelectValue placeholder="Select priority" />
                </SelectTrigger>
                <SelectContent>
                  {priorities.map((priority) => (
                    <SelectItem key={priority} value={priority}>
                      <div className="flex items-center gap-2">
                        <Badge className={cn("text-xs", priorityStyles[priority])}>
                          {priority}
                        </Badge>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

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
                      <div className="flex items-center gap-2">
                        <Badge className={cn("text-xs", statusStyles[status])}>
                          {status}
                        </Badge>
                      </div>
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>
          </div>

          <div className="border-t border-border pt-4" />

          <div className="space-y-2">
            <Label htmlFor="preconditions">Preconditions</Label>
            <Textarea
              id="preconditions"
              value={formData.preconditions}
              onChange={(e) => handleChange('preconditions', e.target.value)}
              rows={2}
              placeholder="Setup required before running this test"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="steps">Test Steps</Label>
            <Textarea
              id="steps"
              value={formData.steps}
              onChange={(e) => handleChange('steps', e.target.value)}
              rows={5}
              placeholder="1. Navigate to...&#10;2. Click on...&#10;3. Enter...&#10;4. Verify..."
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="expectedResult">Expected Result</Label>
            <Textarea
              id="expectedResult"
              value={formData.expectedResult}
              onChange={(e) => handleChange('expectedResult', e.target.value)}
              rows={3}
              placeholder="What should happen when the test passes"
            />
          </div>

          <div className="border-t border-border pt-4" />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="space-y-2">
              <Label htmlFor="estimatedMinutes">Estimated Time (minutes)</Label>
              <Input
                id="estimatedMinutes"
                type="number"
                min={1}
                value={formData.estimatedMinutes || ''}
                onChange={(e) => handleChange('estimatedMinutes', parseInt(e.target.value) || undefined)}
              />
            </div>

            <div className="space-y-2">
              <Label>Tags</Label>
              <div className="flex flex-wrap gap-2 mb-2">
                {formData.tags?.map((tag) => (
                  <Badge key={tag} variant="outline" className="gap-1">
                    {tag}
                    <button
                      type="button"
                      onClick={() => handleRemoveTag(tag)}
                      className="ml-1 hover:text-red-500"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  </Badge>
                ))}
              </div>
              <Input
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault();
                    handleAddTag(tagInput);
                  }
                }}
                placeholder="Type and press Enter to add"
              />
              <div className="flex flex-wrap gap-1 mt-2">
                {suggestedTags
                  .filter(tag => !formData.tags?.includes(tag))
                  .map((tag) => (
                    <button
                      key={tag}
                      type="button"
                      onClick={() => handleAddTag(tag)}
                      className="text-xs px-2 py-1 rounded bg-muted hover:bg-accent text-muted-foreground transition-colors"
                    >
                      + {tag}
                    </button>
                  ))}
              </div>
            </div>
          </div>

          <div className="flex justify-end pt-4">
            <Button
              onClick={handleSave}
              disabled={saving || isLoading}
              className="gap-2"
            >
              <Save className="h-4 w-4" />
              {saving ? 'Saving...' : isEdit ? 'Update Test Case' : 'Create Test Case'}
            </Button>
          </div>
        </div>
      </CardContent>
    </Card>
  );
};

export default TestCaseEditor;
