import React, { useState, useEffect } from 'react';
import {
  ArrowLeft,
  Save,
  Loader2,
  AlertCircle,
  X,
} from 'lucide-react';
import { useNavigate, useParams } from 'react-router-dom';
import { safeParseId } from '../utils/validation';
import qaTestManagementService from '../services/qaTestManagementService';
import { pitchService } from '../services/pitchService';
import {
  TestCaseStatus,
  TestCaseType,
  TestCasePriority,
  CreateTestCaseRequest,
  UpdateTestCaseRequest,
  Pitch,
} from '../types';
import { Button } from '../components/ui/button';
import { Card, CardContent } from '../components/ui/card';
import { Input } from '../components/ui/input';
import { Textarea } from '../components/ui/textarea';
import { Label } from '../components/ui/label';
import { Badge } from '../components/ui/badge';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';

const testCaseTypes: TestCaseType[] = ['FUNCTIONAL', 'INTEGRATION', 'UNIT', 'E2E', 'REGRESSION', 'SMOKE', 'PERFORMANCE', 'SECURITY', 'USABILITY', 'ACCESSIBILITY'];
const testCasePriorities: TestCasePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const testCaseStatuses: TestCaseStatus[] = ['DRAFT', 'READY', 'APPROVED', 'DEPRECATED', 'ARCHIVED'];

const TestCaseFormPage: React.FC = () => {
  const navigate = useNavigate();
  const { id: idParam } = useParams<{ id: string }>();
  const id = safeParseId(idParam);
  const isEdit = !!id;

  const [loading, setLoading] = useState(isEdit);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [tagInput, setTagInput] = useState('');

  const [formData, setFormData] = useState<CreateTestCaseRequest>({
    title: '',
    description: '',
    preconditions: '',
    steps: '',
    expectedResult: '',
    pitchId: undefined,
    type: 'FUNCTIONAL',
    priority: 'MEDIUM',
    status: 'DRAFT',
    tags: [],
    estimatedMinutes: undefined,
  });

  useEffect(() => {
    loadPitches();
    if (isEdit) {
      loadTestCase();
    }
  }, [id]);

  const loadPitches = async () => {
    try {
      const response = await pitchService.getAll();
      setPitches(response.data);
    } catch (err) {
      console.error('Failed to load pitches', err);
    }
  };

  const loadTestCase = async () => {
    setLoading(true);
    try {
      const response = await qaTestManagementService.getTestCaseById(id!);
      const tc = response.data;
      setFormData({
        title: tc.title,
        description: tc.description || '',
        preconditions: tc.preconditions || '',
        steps: tc.steps || '',
        expectedResult: tc.expectedResult || '',
        pitchId: tc.pitchId,
        type: tc.type,
        priority: tc.priority,
        status: tc.status,
        tags: tc.tags?.split(',').map(t => t.trim()).filter(Boolean) || [],
        estimatedMinutes: tc.estimatedMinutes,
      });
    } catch (err) {
      setError('Failed to load test case');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      if (isEdit) {
        const updateData: UpdateTestCaseRequest = {
          ...formData,
          tags: formData.tags,
        };
        await qaTestManagementService.updateTestCase(id!, updateData);
      } else {
        await qaTestManagementService.createTestCase(formData);
      }
      navigate('/qa/test-cases');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save test case');
    } finally {
      setSaving(false);
    }
  };

  const handleAddTag = (tag: string) => {
    const trimmedTag = tag.trim().toLowerCase();
    if (trimmedTag && !formData.tags?.includes(trimmedTag)) {
      setFormData({ ...formData, tags: [...(formData.tags || []), trimmedTag] });
    }
    setTagInput('');
  };

  const handleRemoveTag = (tagToRemove: string) => {
    setFormData({ ...formData, tags: formData.tags?.filter(t => t !== tagToRemove) || [] });
  };

  if (isEdit && id === null) {
    return (
      <div className="p-6">
        <div className="flex items-center gap-2 p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">Invalid test case ID</span>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-[400px]">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="p-6">
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" onClick={() => navigate('/qa/test-cases')} className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          Back
        </Button>
        <h1 className="text-2xl font-bold">{isEdit ? 'Edit Test Case' : 'New Test Case'}</h1>
      </div>

      {error && (
        <div className="flex items-center gap-2 p-3 mb-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
          <AlertCircle className="h-4 w-4" />
          <span className="text-sm">{error}</span>
        </div>
      )}

      <Card>
        <CardContent className="p-6">
          <form onSubmit={handleSubmit} className="space-y-6">
            <div className="space-y-2">
              <Label htmlFor="title">Title *</Label>
              <Input
                id="title"
                required
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="description">Description</Label>
              <Textarea
                id="description"
                rows={3}
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Pitch</Label>
                <Select
                  value={formData.pitchId ? String(formData.pitchId) : 'none'}
                  onValueChange={(value) => setFormData({ ...formData, pitchId: value === 'none' ? undefined : Number(value) })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select a pitch" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">None</SelectItem>
                    {pitches.map((pitch) => (
                      <SelectItem key={pitch.id} value={String(pitch.id)}>
                        {pitch.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Type *</Label>
                <Select
                  value={formData.type}
                  onValueChange={(value) => setFormData({ ...formData, type: value as TestCaseType })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select type" />
                  </SelectTrigger>
                  <SelectContent>
                    {testCaseTypes.map((type) => (
                      <SelectItem key={type} value={type}>
                        {type}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div className="space-y-2">
                <Label>Priority *</Label>
                <Select
                  value={formData.priority}
                  onValueChange={(value) => setFormData({ ...formData, priority: value as TestCasePriority })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select priority" />
                  </SelectTrigger>
                  <SelectContent>
                    {testCasePriorities.map((priority) => (
                      <SelectItem key={priority} value={priority}>
                        {priority}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label>Status</Label>
                <Select
                  value={formData.status}
                  onValueChange={(value) => setFormData({ ...formData, status: value as TestCaseStatus })}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select status" />
                  </SelectTrigger>
                  <SelectContent>
                    {testCaseStatuses.map((status) => (
                      <SelectItem key={status} value={status}>
                        {status}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="estimatedMinutes">Estimated Time (minutes)</Label>
                <Input
                  id="estimatedMinutes"
                  type="number"
                  value={formData.estimatedMinutes || ''}
                  onChange={(e) => setFormData({ ...formData, estimatedMinutes: e.target.value ? Number(e.target.value) : undefined })}
                />
              </div>
            </div>

            <div className="space-y-2">
              <Label htmlFor="preconditions">Preconditions</Label>
              <Textarea
                id="preconditions"
                rows={2}
                placeholder="What needs to be set up before running this test?"
                value={formData.preconditions}
                onChange={(e) => setFormData({ ...formData, preconditions: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="steps">Test Steps</Label>
              <Textarea
                id="steps"
                rows={4}
                placeholder="Step-by-step instructions for executing the test"
                value={formData.steps}
                onChange={(e) => setFormData({ ...formData, steps: e.target.value })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="expectedResult">Expected Result</Label>
              <Textarea
                id="expectedResult"
                rows={2}
                placeholder="What should happen when the test is successful?"
                value={formData.expectedResult}
                onChange={(e) => setFormData({ ...formData, expectedResult: e.target.value })}
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
                placeholder="Type and press Enter to add tags"
              />
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button type="button" variant="ghost" onClick={() => navigate('/qa/test-cases')}>
                Cancel
              </Button>
              <Button type="submit" disabled={saving} className="gap-2">
                {saving ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <Save className="h-4 w-4" />
                )}
                {saving ? 'Saving...' : isEdit ? 'Update Test Case' : 'Create Test Case'}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
};

export default TestCaseFormPage;
