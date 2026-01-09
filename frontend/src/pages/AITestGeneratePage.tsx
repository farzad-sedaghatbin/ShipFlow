import React, { useState, useEffect } from 'react';
import { ArrowLeft, Bot, FlaskConical, Loader2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '../components/ui/card';
import { Alert, AlertDescription } from '../components/ui/alert';
import { Checkbox } from '../components/ui/checkbox';
import { Label } from '../components/ui/label';
import { Textarea } from '../components/ui/textarea';
import { Separator } from '../components/ui/separator';
import { ScrollArea } from '../components/ui/scroll-area';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '../components/ui/select';
import { AILoadingState } from '../components/AILoadingState';
import qaTestManagementService from '../services/qaTestManagementService';
import { pitchService } from '../services/pitchService';
import { TestCaseSuggestion, Pitch, TestCaseType, GenerateTestCasesResponse } from '../types';

const testCaseTypes: TestCaseType[] = ['FUNCTIONAL', 'INTEGRATION', 'UNIT', 'E2E', 'REGRESSION', 'SMOKE', 'PERFORMANCE', 'SECURITY', 'USABILITY', 'ACCESSIBILITY'];

const AITestGeneratePage: React.FC = () => {
  const navigate = useNavigate();

  const [pitches, setPitches] = useState<Pitch[]>([]);
  const [selectedPitchId, setSelectedPitchId] = useState<string>('');
  const [selectedTypes, setSelectedTypes] = useState<TestCaseType[]>(['FUNCTIONAL', 'INTEGRATION']);
  const [context, setContext] = useState('');
  
  const [generating, setGenerating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [generatedTests, setGeneratedTests] = useState<TestCaseSuggestion[]>([]);
  const [selectedTests, setSelectedTests] = useState<number[]>([]);

  useEffect(() => {
    loadPitches();
  }, []);

  const loadPitches = async () => {
    try {
      const response = await pitchService.getAll();
      setPitches(response.data);
    } catch (err) {
      console.error('Failed to load pitches', err);
    }
  };

  const handleTypeToggle = (type: TestCaseType) => {
    setSelectedTypes(prev =>
      prev.includes(type)
        ? prev.filter(t => t !== type)
        : [...prev, type]
    );
  };

  const handleGenerate = async () => {
    if (!selectedPitchId) {
      setError('Please select a pitch');
      return;
    }

    setGenerating(true);
    setError(null);
    setGeneratedTests([]);

    try {
      const response = await qaTestManagementService.generateTestCases({
        pitchId: Number(selectedPitchId),
        testTypes: selectedTypes,
        additionalContext: context || undefined,
      });
      const result: GenerateTestCasesResponse = response.data;
      
      if (!result.aiEnabled) {
        setError(result.errorMessage || 'AI test generation is not enabled');
        return;
      }
      
      if (result.errorMessage) {
        setError(result.errorMessage);
        return;
      }
      
      setGeneratedTests(result.suggestions || []);
      setSelectedTests((result.suggestions || []).map((_: TestCaseSuggestion, idx: number) => idx));
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.errorMessage || 'Failed to generate test cases. AI service may not be configured.');
    } finally {
      setGenerating(false);
    }
  };

  const handleTestToggle = (index: number) => {
    setSelectedTests(prev =>
      prev.includes(index)
        ? prev.filter(i => i !== index)
        : [...prev, index]
    );
  };

  const handleSaveSelected = async () => {
    if (selectedTests.length === 0) {
      setError('Please select at least one test case to save');
      return;
    }

    setSaving(true);
    setError(null);

    try {
      const testsToSave = selectedTests.map(idx => generatedTests[idx]);
      for (const tc of testsToSave) {
        await qaTestManagementService.createTestCase({
          title: tc.title,
          description: tc.description,
          preconditions: tc.preconditions,
          steps: tc.steps,
          expectedResult: tc.expectedResult,
          pitchId: Number(selectedPitchId) || undefined,
          type: (tc.suggestedType as TestCaseType) || 'FUNCTIONAL',
          priority: (tc.suggestedPriority as 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL') || 'MEDIUM',
          status: 'DRAFT',
          tags: tc.suggestedTags || [],
          aiGenerated: true,
        });
      }
      navigate('/qa/test-cases');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save test cases');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-3">
        <Button variant="ghost" size="sm" onClick={() => navigate('/qa/test-cases')}>
          <ArrowLeft className="h-4 w-4 mr-1" />
          Back
        </Button>
        <Bot className="h-6 w-6 text-primary" />
        <h1 className="text-2xl font-bold">AI Test Case Generator</h1>
      </div>

      {/* Error Alert */}
      {error && (
        <Alert variant="destructive">
          <AlertDescription>
            {error}
            {(error.includes('not enabled') || error.includes('not available')) && (
              <p className="mt-2 text-sm">
                To enable AI test generation, ensure Ollama is running locally with a model like Mistral.
                Set <code className="bg-muted px-1 py-0.5 rounded text-xs">app.ai.risk-analysis.enabled=true</code> in application properties.
              </p>
            )}
          </AlertDescription>
        </Alert>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Settings Panel */}
        <div className="lg:col-span-1">
          <Card>
            <CardHeader>
              <CardTitle>Generation Settings</CardTitle>
            </CardHeader>
            <CardContent className="space-y-6">
              {/* Pitch Select */}
              <div className="space-y-2">
                <Label htmlFor="pitch-select">Select Pitch *</Label>
                <Select
                  value={selectedPitchId}
                  onValueChange={setSelectedPitchId}
                >
                  <SelectTrigger id="pitch-select">
                    <SelectValue placeholder="Choose a pitch" />
                  </SelectTrigger>
                  <SelectContent>
                    {pitches.map((pitch) => (
                      <SelectItem key={pitch.id} value={String(pitch.id)}>
                        {pitch.title}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Test Types */}
              <div className="space-y-2">
                <Label>Test Types to Generate</Label>
                <div className="rounded-md border">
                  <ScrollArea className="h-[200px] p-2">
                    <div className="space-y-1">
                      {testCaseTypes.map((type) => (
                        <div
                          key={type}
                          className="flex items-center space-x-2 py-1.5 px-2 hover:bg-muted rounded-sm cursor-pointer"
                          onClick={() => handleTypeToggle(type)}
                        >
                          <Checkbox
                            id={`type-${type}`}
                            checked={selectedTypes.includes(type)}
                            onCheckedChange={() => handleTypeToggle(type)}
                          />
                          <Label
                            htmlFor={`type-${type}`}
                            className="text-sm font-normal cursor-pointer flex-1"
                          >
                            {type}
                          </Label>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                </div>
              </div>

              {/* Additional Context */}
              <div className="space-y-2">
                <Label htmlFor="context">Additional Context (Optional)</Label>
                <Textarea
                  id="context"
                  placeholder="Provide any additional context or specific areas to focus on..."
                  rows={4}
                  value={context}
                  onChange={(e) => setContext(e.target.value)}
                />
              </div>

              {/* Generate Button */}
              <Button
                className="w-full"
                onClick={handleGenerate}
                disabled={generating || !selectedPitchId}
              >
                {generating ? (
                  <Loader2 className="h-4 w-4 animate-spin mr-2" />
                ) : (
                  <FlaskConical className="h-4 w-4 mr-2" />
                )}
                {generating ? 'Generating...' : 'Generate Test Cases'}
              </Button>
            </CardContent>
          </Card>
        </div>

        {/* Results Panel */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-4">
              <CardTitle>Generated Test Cases</CardTitle>
              {generatedTests.length > 0 && (
                <Button
                  onClick={handleSaveSelected}
                  disabled={saving || selectedTests.length === 0}
                >
                  {saving ? (
                    <>
                      <Loader2 className="h-4 w-4 animate-spin mr-2" />
                      Saving...
                    </>
                  ) : (
                    `Save Selected (${selectedTests.length})`
                  )}
                </Button>
              )}
            </CardHeader>
            <Separator />
            <CardContent className="pt-6">
              {generating ? (
                <AILoadingState
                  title="Generating Test Cases"
                  subtitle="AI is analyzing your pitch and creating comprehensive test cases..."
                  steps={[
                    'Reading pitch requirements...',
                    'Analyzing acceptance criteria...',
                    'Identifying test scenarios...',
                    'Creating functional tests...',
                    'Generating edge cases...',
                    'Finalizing test cases...',
                  ]}
                />
              ) : generatedTests.length === 0 ? (
                <div className="text-center py-16">
                  <Bot className="h-16 w-16 mx-auto text-muted-foreground mb-4" />
                  <p className="text-muted-foreground">
                    Select a pitch and click "Generate Test Cases" to create AI-powered test cases.
                  </p>
                  <p className="text-sm text-muted-foreground mt-2">
                    The AI will analyze the pitch details and generate relevant test cases.
                  </p>
                </div>
              ) : (
                <div className="space-y-4">
                  {generatedTests.map((tc, idx) => (
                    <div
                      key={idx}
                      className="rounded-lg border p-4 hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex items-start gap-3">
                        <Checkbox
                          checked={selectedTests.includes(idx)}
                          onCheckedChange={() => handleTestToggle(idx)}
                          className="mt-1"
                        />
                        <div className="flex-1 space-y-2">
                          <h4 className="font-semibold">{tc.title}</h4>
                          <p className="text-sm text-muted-foreground">
                            Type: {tc.suggestedType || 'FUNCTIONAL'} | Priority: {tc.suggestedPriority || 'MEDIUM'}
                            {tc.confidenceScore && ` | Confidence: ${Math.round(tc.confidenceScore * 100)}%`}
                          </p>
                          <p className="text-sm">{tc.description}</p>
                          
                          {tc.steps && (
                            <div>
                              <p className="text-sm font-medium text-muted-foreground">Steps:</p>
                              <p className="text-sm whitespace-pre-wrap">{tc.steps}</p>
                            </div>
                          )}
                          
                          {tc.expectedResult && (
                            <div>
                              <p className="text-sm font-medium text-muted-foreground">Expected Result:</p>
                              <p className="text-sm whitespace-pre-wrap">{tc.expectedResult}</p>
                            </div>
                          )}
                          
                          {tc.suggestedTags && tc.suggestedTags.length > 0 && (
                            <p className="text-sm text-muted-foreground">
                              Tags: {tc.suggestedTags.join(', ')}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
};

export default AITestGeneratePage;
