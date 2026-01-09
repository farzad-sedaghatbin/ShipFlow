import React, { useState } from 'react';
import {
  Sparkles,
  ChevronDown,
  ChevronUp,
  Check,
  Loader2,
} from 'lucide-react';
import { AILoadingCompact } from './AILoadingState';
import {
  GenerateTestCasesRequest,
  GenerateTestCasesResponse,
  TestCaseSuggestion,
} from '../types';
import { Card, CardContent } from './ui/card';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Label } from './ui/label';
import { Textarea } from './ui/textarea';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { Checkbox } from './ui/checkbox';
import { Separator } from './ui/separator';
import { Markdown } from './ui/markdown';
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from './ui/collapsible';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import { cn } from '../lib/utils';

interface AISuggestionPanelProps {
  pitchId: number;
  onGenerateTestCases: (request: GenerateTestCasesRequest) => Promise<GenerateTestCasesResponse>;
  onAcceptSuggestion: (suggestion: TestCaseSuggestion) => Promise<void>;
  onAcceptAll?: (suggestions: TestCaseSuggestion[]) => Promise<void>;
}

const AISuggestionPanel: React.FC<AISuggestionPanelProps> = ({
  pitchId,
  onGenerateTestCases,
  onAcceptSuggestion,
  onAcceptAll,
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [response, setResponse] = useState<GenerateTestCasesResponse | null>(null);
  const [additionalContext, setAdditionalContext] = useState('');
  const [focusAreas, setFocusAreas] = useState<string[]>([]);
  const [maxTestCases, setMaxTestCases] = useState(5);
  const [expandedSuggestions, setExpandedSuggestions] = useState<Set<number>>(new Set());
  const [selectedSuggestions, setSelectedSuggestions] = useState<Set<number>>(new Set());
  const [acceptingIndex, setAcceptingIndex] = useState<number | null>(null);
  const [showOptions, setShowOptions] = useState(false);

  const handleGenerate = async () => {
    setLoading(true);
    setError(null);
    setResponse(null);
    setSelectedSuggestions(new Set());

    try {
      const result = await onGenerateTestCases({
        pitchId,
        additionalContext: additionalContext || undefined,
        focusAreas: focusAreas.length > 0 ? focusAreas : undefined,
        maxTestCases,
      });

      if (result.errorMessage) {
        setError(result.errorMessage);
      } else {
        setResponse(result);
        // Auto-expand first suggestion
        if (result.suggestions.length > 0) {
          setExpandedSuggestions(new Set([0]));
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to generate test cases');
    } finally {
      setLoading(false);
    }
  };

  const handleAcceptSuggestion = async (suggestion: TestCaseSuggestion, index: number) => {
    setAcceptingIndex(index);
    try {
      await onAcceptSuggestion(suggestion);
      // Remove accepted suggestion from list
      if (response) {
        const updatedSuggestions = response.suggestions.filter((_, i) => i !== index);
        setResponse({ ...response, suggestions: updatedSuggestions });
        selectedSuggestions.delete(index);
        setSelectedSuggestions(new Set(selectedSuggestions));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestion');
    } finally {
      setAcceptingIndex(null);
    }
  };

  const handleAcceptSelected = async () => {
    if (!response || !onAcceptAll || selectedSuggestions.size === 0) return;

    const suggestionsToAccept = response.suggestions.filter((_, i) => selectedSuggestions.has(i));
    setLoading(true);
    
    try {
      await onAcceptAll(suggestionsToAccept);
      // Remove accepted suggestions
      const remainingSuggestions = response.suggestions.filter((_, i) => !selectedSuggestions.has(i));
      setResponse({ ...response, suggestions: remainingSuggestions });
      setSelectedSuggestions(new Set());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to accept suggestions');
    } finally {
      setLoading(false);
    }
  };

  const toggleExpand = (index: number) => {
    const newExpanded = new Set(expandedSuggestions);
    if (newExpanded.has(index)) {
      newExpanded.delete(index);
    } else {
      newExpanded.add(index);
    }
    setExpandedSuggestions(newExpanded);
  };

  const toggleSelect = (index: number) => {
    const newSelected = new Set(selectedSuggestions);
    if (newSelected.has(index)) {
      newSelected.delete(index);
    } else {
      newSelected.add(index);
    }
    setSelectedSuggestions(newSelected);
  };

  const getPriorityBadgeClass = (priority: string) => {
    switch (priority) {
      case 'CRITICAL':
        return 'bg-red-500/15 text-red-500 border-red-500/30';
      case 'HIGH':
        return 'bg-yellow-500/15 text-yellow-500 border-yellow-500/30';
      default:
        return '';
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-primary" />
            <h3 className="text-lg font-semibold">AI Test Case Generator</h3>
          </div>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setShowOptions(!showOptions)}
            className="gap-1"
          >
            Options
            {showOptions ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
          </Button>
        </div>

        <p className="text-sm text-muted-foreground mb-4">
          Generate test case suggestions based on the pitch description and meeting notes using AI.
        </p>

        <Collapsible open={showOptions}>
          <CollapsibleContent>
            <div className="mb-4 p-4 bg-muted/30 rounded-lg space-y-4">
              <div className="space-y-2">
                <Label htmlFor="additional-context">Additional Context</Label>
                <Textarea
                  id="additional-context"
                  value={additionalContext}
                  onChange={(e) => setAdditionalContext(e.target.value)}
                  placeholder="Provide any additional context or requirements..."
                  rows={2}
                />
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="max-test-cases">Max Test Cases</Label>
                  <Input
                    id="max-test-cases"
                    type="number"
                    value={maxTestCases}
                    onChange={(e) => setMaxTestCases(parseInt(e.target.value) || 5)}
                    min={1}
                    max={20}
                    className="w-full"
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="focus-areas">Focus Areas (comma-separated)</Label>
                  <Input
                    id="focus-areas"
                    value={focusAreas.join(', ')}
                    onChange={(e) => setFocusAreas(e.target.value.split(',').map((s) => s.trim()).filter(Boolean))}
                    placeholder="e.g., authentication, error handling"
                  />
                </div>
              </div>
            </div>
          </CollapsibleContent>
        </Collapsible>

        <div className="flex gap-2 mb-4">
          <Button
            onClick={handleGenerate}
            disabled={loading}
            className="gap-2"
          >
            {loading ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Sparkles className="h-4 w-4" />
            )}
            {loading ? 'Generating...' : 'Generate Test Cases'}
          </Button>

          {response && response.suggestions.length > 0 && selectedSuggestions.size > 0 && onAcceptAll && (
            <Button
              variant="outline"
              onClick={handleAcceptSelected}
              disabled={loading}
              className="gap-2 text-green-600 border-green-600 hover:bg-green-600/10"
            >
              <Check className="h-4 w-4" />
              Accept Selected ({selectedSuggestions.size})
            </Button>
          )}
        </div>

        {error && (
          <Alert variant="destructive" className="mb-4">
            <AlertDescription>{error}</AlertDescription>
          </Alert>
        )}

        {loading && (
          <div className="my-6">
            <AILoadingCompact message="Generating test cases..." />
            <p className="text-xs text-muted-foreground text-center mt-2">
              AI is analyzing pitch details and generating comprehensive test cases
            </p>
          </div>
        )}

        {!loading && response && (
          <>
            {response.processingTimeMs && (
              <p className="text-xs text-muted-foreground mb-4">
                Generated in {(response.processingTimeMs / 1000).toFixed(2)}s
              </p>
            )}

            {response.suggestions.length === 0 ? (
              <Alert>
                <AlertDescription>No suggestions generated. Try adding more context.</AlertDescription>
              </Alert>
            ) : (
              <div className="space-y-3">
                {response.suggestions.map((suggestion, index) => (
                  <Card
                    key={index}
                    className={cn(
                      "transition-colors",
                      selectedSuggestions.has(index) && "border-primary bg-primary/5"
                    )}
                  >
                    <Collapsible
                      open={expandedSuggestions.has(index)}
                      onOpenChange={() => toggleExpand(index)}
                    >
                      <div className="p-4 flex items-center justify-between cursor-pointer">
                        <div className="flex items-center gap-3 flex-1">
                          <Checkbox
                            checked={selectedSuggestions.has(index)}
                            onCheckedChange={() => toggleSelect(index)}
                            onClick={(e) => e.stopPropagation()}
                          />
                          <span className="font-medium">{suggestion.title}</span>
                          {suggestion.suggestedType && (
                            <Badge variant="outline">{suggestion.suggestedType}</Badge>
                          )}
                          {suggestion.suggestedPriority && (
                            <Badge variant="outline" className={getPriorityBadgeClass(suggestion.suggestedPriority)}>
                              {suggestion.suggestedPriority}
                            </Badge>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <TooltipProvider>
                            <Tooltip>
                              <TooltipTrigger asChild>
                                <Button
                                  variant="ghost"
                                  size="icon"
                                  className="h-8 w-8 text-green-600 hover:text-green-700 hover:bg-green-600/10"
                                  onClick={(e) => {
                                    e.stopPropagation();
                                    handleAcceptSuggestion(suggestion, index);
                                  }}
                                  disabled={acceptingIndex === index}
                                >
                                  {acceptingIndex === index ? (
                                    <Loader2 className="h-4 w-4 animate-spin" />
                                  ) : (
                                    <Check className="h-4 w-4" />
                                  )}
                                </Button>
                              </TooltipTrigger>
                              <TooltipContent>Accept and Create</TooltipContent>
                            </Tooltip>
                          </TooltipProvider>
                          <CollapsibleTrigger asChild>
                            <Button variant="ghost" size="icon" className="h-8 w-8">
                              {expandedSuggestions.has(index) ? (
                                <ChevronUp className="h-4 w-4" />
                              ) : (
                                <ChevronDown className="h-4 w-4" />
                              )}
                            </Button>
                          </CollapsibleTrigger>
                        </div>
                      </div>

                      <CollapsibleContent>
                        <Separator />
                        <div className="p-4 space-y-4">
                          {suggestion.description && (
                            <div>
                              <p className="text-sm font-medium mb-1">Description</p>
                              <div className="text-sm text-muted-foreground">
                                <Markdown content={suggestion.description} />
                              </div>
                            </div>
                          )}

                          {suggestion.preconditions && (
                            <div>
                              <p className="text-sm font-medium mb-1">Preconditions</p>
                              <div className="text-sm text-muted-foreground">
                                <Markdown content={suggestion.preconditions} />
                              </div>
                            </div>
                          )}

                          {suggestion.steps && (
                            <div>
                              <p className="text-sm font-medium mb-1">Test Steps</p>
                              <div className="text-sm text-muted-foreground">
                                <Markdown content={suggestion.steps} />
                              </div>
                            </div>
                          )}

                          {suggestion.expectedResult && (
                            <div>
                              <p className="text-sm font-medium mb-1">Expected Result</p>
                              <div className="text-sm text-muted-foreground">
                                <Markdown content={suggestion.expectedResult} />
                              </div>
                            </div>
                          )}

                          {suggestion.suggestedTags && suggestion.suggestedTags.length > 0 && (
                            <div className="flex flex-wrap gap-1">
                              {suggestion.suggestedTags.map((tag, i) => (
                                <Badge key={i} variant="outline" className="text-xs">
                                  {tag}
                                </Badge>
                              ))}
                            </div>
                          )}
                        </div>
                      </CollapsibleContent>
                    </Collapsible>
                  </Card>
                ))}
              </div>
            )}
          </>
        )}
      </CardContent>
    </Card>
  );
};

export default AISuggestionPanel;
