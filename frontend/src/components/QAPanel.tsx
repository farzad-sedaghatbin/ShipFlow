import React, { useState, useEffect, useRef } from 'react';
import {
  Send,
  MessageCircle,
  Bot,
  User,
  ThumbsUp,
  ThumbsDown,
  Pencil,
  X,
  ChevronDown,
  ChevronUp,
  FileText,
  Lightbulb,
  Loader2,
  AlertCircle,
  Info,
} from 'lucide-react';
import { qaService, QAResponse, QAFeedbackType, SourceCitation } from '../services/qaService';
import { Button } from './ui/button';
import { Card } from './ui/card';
import { Input } from './ui/input';
import { Textarea } from './ui/textarea';
import { Badge } from './ui/badge';
import { Markdown } from './ui/markdown';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';
import {
  Collapsible,
  CollapsibleContent,
} from './ui/collapsible';
import { cn } from '../lib/utils';

interface QAPanelProps {
  contextType: 'pitch' | 'meeting' | 'team' | 'cycle';
  contextId?: number;
  contextName?: string;
  cycleId?: number;
  teamId?: number;
  variant?: 'panel' | 'dialog';
  onClose?: () => void;
  open?: boolean;
}

interface Message {
  type: 'question' | 'answer';
  text: string;
  timestamp: Date;
  interactionId?: number;
  confidenceScore?: number;
  sources?: SourceCitation[];
  suggestedFollowUps?: string[];
  feedbackGiven?: QAFeedbackType;
  cached?: boolean;
}

export const QAPanel: React.FC<QAPanelProps> = ({
  contextType,
  contextId,
  contextName,
  cycleId,
  teamId,
  variant = 'panel',
  onClose,
  open = true,
}) => {
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [expanded, setExpanded] = useState(true);
  const [showSources, setShowSources] = useState<number | null>(null);
  const [showCorrectionDialog, setShowCorrectionDialog] = useState<number | null>(null);
  const [correction, setCorrection] = useState('');
  const [qaStatus, setQaStatus] = useState<{ qaEnabled?: boolean; aiAvailable?: boolean } | null>(null);
  const [conversationId, setConversationId] = useState<string | undefined>(undefined);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Check Q&A status on mount
  useEffect(() => {
    qaService.getStatus()
      .then(response => setQaStatus(response.data))
      .catch(() => setQaStatus({ qaEnabled: false }));
  }, []);

  // Scroll to bottom when new messages arrive
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || loading) return;

    const userQuestion = question.trim();
    setQuestion('');
    setError(null);

    // Add user question to messages
    setMessages(prev => [...prev, {
      type: 'question',
      text: userQuestion,
      timestamp: new Date(),
    }]);

    try {
      setLoading(true);
      const response = await qaService.askQuestion({
        question: userQuestion,
        contextType,
        contextId,
        contextName,
        cycleId,
        teamId,
        includeSources: true,
        conversationId,
      });

      const data: QAResponse = response.data;

      // Track conversationId for multi-turn context
      if (data.conversationId && !conversationId) {
        setConversationId(data.conversationId);
      }

      if (data.errorMessage) {
        setError(data.errorMessage);
      } else if (data.answer) {
        setMessages(prev => [...prev, {
          type: 'answer',
          text: data.answer!,
          timestamp: new Date(data.answeredAt || new Date()),
          interactionId: data.interactionId,
          confidenceScore: data.confidenceScore,
          sources: data.sources,
          suggestedFollowUps: data.suggestedFollowUps,
          cached: data.cached,
        }]);
      }
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to get answer');
    } finally {
      setLoading(false);
    }
  };

  const handleFeedback = async (interactionId: number, feedbackType: QAFeedbackType, correctionText?: string) => {
    try {
      await qaService.submitFeedback({
        interactionId,
        feedbackType,
        correction: correctionText,
      });

      // Update message with feedback
      setMessages(prev => prev.map(msg => 
        msg.interactionId === interactionId 
          ? { ...msg, feedbackGiven: feedbackType }
          : msg
      ));

      setShowCorrectionDialog(null);
      setCorrection('');
    } catch (err: any) {
      console.error('Failed to submit feedback:', err);
    }
  };

  const handleSuggestedQuestion = (q: string) => {
    setQuestion(q);
  };

  const suggestedQuestions = getSuggestedQuestions(contextType);

  const getConfidenceStyle = (score: number) => {
    if (score >= 70) return 'bg-green-500/10 text-green-500 border-green-500/20';
    if (score >= 40) return 'bg-amber-500/10 text-amber-500 border-amber-500/20';
    return 'bg-red-500/10 text-red-500 border-red-500/20';
  };

  // If Q&A is disabled, show a message
  if (qaStatus && !qaStatus.qaEnabled) {
    return (
      <div className="flex items-center gap-2 p-4 m-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-500">
        <Info className="h-4 w-4" />
        <span className="text-sm">The Q&A feature is currently disabled. Contact your administrator to enable it.</span>
      </div>
    );
  }

  const content = (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="flex items-center justify-between p-4 border-b border-border bg-card">
        <div className="flex items-center gap-2">
          <div className="relative">
            <MessageCircle className="h-5 w-5 text-primary" />
            <span className={cn(
              "absolute -top-0.5 -right-0.5 h-2 w-2 rounded-full",
              qaStatus?.aiAvailable ? "bg-green-500" : "bg-amber-500"
            )} />
          </div>
          <span className="font-semibold">
            Ask about {contextName || contextType}
          </span>
        </div>
        <div className="flex items-center gap-1">
          {variant === 'panel' && (
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={() => setExpanded(!expanded)}>
              {expanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
            </Button>
          )}
          {onClose && variant === 'panel' && (
            <Button variant="ghost" size="icon" className="h-8 w-8" onClick={onClose}>
              <X className="h-4 w-4" />
            </Button>
          )}
        </div>
      </div>

      <Collapsible open={expanded}>
        <CollapsibleContent>
          {/* Messages Area */}
          <div 
            className={cn(
              "flex-1 overflow-y-auto overflow-x-auto p-4 bg-background",
              variant === 'dialog' ? "min-h-[200px] max-h-[400px]" : "min-h-[200px] max-h-[350px]"
            )}
          >
            {messages.length === 0 ? (
              <div>
                <p className="text-sm text-muted-foreground mb-2">
                  Ask me anything about {contextName || `this ${contextType}`}. I can help you understand:
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {suggestedQuestions.map((q, idx) => (
                    <Badge
                      key={idx}
                      variant="outline"
                      className="cursor-pointer hover:bg-accent gap-1"
                      onClick={() => handleSuggestedQuestion(q)}
                    >
                      <Lightbulb className="h-3 w-3" />
                      {q}
                    </Badge>
                  ))}
                </div>
              </div>
            ) : (
              <div className="space-y-3">
                {messages.map((msg, idx) => (
                  <div
                    key={idx}
                    className={cn(
                      "p-3 rounded-lg transition-all",
                      msg.type === 'question' 
                        ? "bg-muted" 
                        : "bg-card border-l-3 border-l-primary"
                    )}
                    style={msg.type === 'answer' ? { borderLeftWidth: '3px' } : undefined}
                  >
                    <div className="flex items-start gap-2">
                      {msg.type === 'question' ? (
                        <User className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                      ) : (
                        <Bot className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                      )}
                      <div className="flex-1 min-w-0">
                        {msg.type === 'answer' ? (
                          <Markdown content={msg.text} className="text-sm break-words" />
                        ) : (
                          <p className="text-sm whitespace-pre-wrap break-words">
                            {msg.text}
                          </p>
                        )}

                        {/* Answer metadata and actions */}
                        {msg.type === 'answer' && (
                          <>
                            {/* Confidence and sources */}
                            <div className="flex items-center gap-1.5 mt-2 flex-wrap">
                              {msg.confidenceScore !== undefined && (
                                <Badge
                                  variant="outline"
                                  className={cn("text-xs", getConfidenceStyle(msg.confidenceScore))}
                                >
                                  {msg.confidenceScore}% confidence
                                </Badge>
                              )}
                              {msg.cached && (
                                <TooltipProvider>
                                  <Tooltip>
                                    <TooltipTrigger>
                                      <Badge variant="outline" className="text-xs bg-blue-500/10 text-blue-500 border-blue-500/20">
                                        Cached
                                      </Badge>
                                    </TooltipTrigger>
                                    <TooltipContent>
                                      <p>This response was served from cache (similar question was asked recently)</p>
                                    </TooltipContent>
                                  </Tooltip>
                                </TooltipProvider>
                              )}
                              {msg.sources && msg.sources.length > 0 && (
                                <Badge
                                  variant="outline"
                                  className="text-xs cursor-pointer gap-1"
                                  onClick={() => setShowSources(showSources === idx ? null : idx)}
                                >
                                  <FileText className="h-3 w-3" />
                                  {msg.sources.length} sources
                                </Badge>
                              )}
                            </div>

                            {/* Sources expansion */}
                            <Collapsible open={showSources === idx}>
                              <CollapsibleContent>
                                <div className="mt-2 p-2 bg-muted rounded-lg break-words">
                                  <span className="text-xs font-semibold">Sources:</span>
                                  {msg.sources?.map((source, sIdx) => (
                                    <div key={sIdx} className="mt-1">
                                      <span className="text-xs text-primary break-words">
                                        • {source.title || source.entityType}
                                      </span>
                                      {source.snippet && (
                                        <p className="text-xs text-muted-foreground pl-3 italic break-words whitespace-normal">
                                          "{source.snippet}"
                                        </p>
                                      )}
                                    </div>
                                  ))}
                                </div>
                              </CollapsibleContent>
                            </Collapsible>

                            {/* Feedback buttons */}
                            {msg.interactionId && !msg.feedbackGiven && (
                              <TooltipProvider>
                                <div className="flex gap-1 mt-2">
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-7 w-7"
                                        onClick={() => handleFeedback(msg.interactionId!, 'ACCURATE')}
                                      >
                                        <ThumbsUp className="h-3.5 w-3.5" />
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>This answer was helpful</TooltipContent>
                                  </Tooltip>
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-7 w-7"
                                        onClick={() => handleFeedback(msg.interactionId!, 'INACCURATE')}
                                      >
                                        <ThumbsDown className="h-3.5 w-3.5" />
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>This answer was not helpful</TooltipContent>
                                  </Tooltip>
                                  <Tooltip>
                                    <TooltipTrigger asChild>
                                      <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-7 w-7"
                                        onClick={() => setShowCorrectionDialog(msg.interactionId!)}
                                      >
                                        <Pencil className="h-3.5 w-3.5" />
                                      </Button>
                                    </TooltipTrigger>
                                    <TooltipContent>Provide a correction</TooltipContent>
                                  </Tooltip>
                                </div>
                              </TooltipProvider>
                            )}

                            {msg.feedbackGiven && (
                              <p className="text-xs text-green-500 mt-2">
                                ✓ Thanks for your feedback!
                              </p>
                            )}

                            {/* Suggested follow-ups */}
                            {msg.suggestedFollowUps && msg.suggestedFollowUps.length > 0 && (
                              <div className="mt-3">
                                <span className="text-xs text-muted-foreground">
                                  You might also ask:
                                </span>
                                <div className="flex flex-wrap gap-1 mt-1">
                                  {msg.suggestedFollowUps.map((followUp, fIdx) => (
                                    <Badge
                                      key={fIdx}
                                      variant="outline"
                                      className="text-xs cursor-pointer hover:bg-primary/10"
                                      onClick={() => handleSuggestedQuestion(followUp)}
                                    >
                                      {followUp}
                                    </Badge>
                                  ))}
                                </div>
                              </div>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
            
            {loading && (
              <div className="flex items-center gap-2 p-4">
                <Loader2 className="h-4 w-4 animate-spin" />
                <span className="text-sm text-muted-foreground">Thinking...</span>
              </div>
            )}
            
            {error && (
              <div className="flex items-center gap-2 p-3 mt-2 rounded-lg bg-red-500/10 border border-red-500/20 text-red-500">
                <AlertCircle className="h-4 w-4" />
                <span className="text-sm">{error}</span>
              </div>
            )}
            
            <div ref={messagesEndRef} />
          </div>

          {/* Input Area */}
          <form onSubmit={handleSubmit} className="p-4 border-t border-border bg-card">
            <div className="flex gap-2">
              <Input
                placeholder="Ask a question..."
                value={question}
                onChange={(e) => setQuestion(e.target.value)}
                disabled={loading}
                className="flex-1"
              />
              <Button 
                type="submit" 
                size="icon"
                disabled={!question.trim() || loading}
              >
                <Send className="h-4 w-4" />
              </Button>
            </div>
            {!qaStatus?.aiAvailable && (
              <p className="text-xs text-amber-500 mt-1.5">
                AI is not available. Answers will be based on retrieved context only.
              </p>
            )}
          </form>
        </CollapsibleContent>
      </Collapsible>

      {/* Correction Dialog */}
      <Dialog 
        open={showCorrectionDialog !== null} 
        onOpenChange={() => setShowCorrectionDialog(null)}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Provide a Correction</DialogTitle>
          </DialogHeader>
          <div className="space-y-4">
            <p className="text-sm text-muted-foreground">
              Please provide the correct answer. Your correction will help improve future responses.
            </p>
            <Textarea
              placeholder="Enter the correct answer..."
              value={correction}
              onChange={(e) => setCorrection(e.target.value)}
              rows={4}
            />
          </div>
          <DialogFooter>
            <Button variant="ghost" onClick={() => setShowCorrectionDialog(null)}>
              Cancel
            </Button>
            <Button 
              onClick={() => {
                if (showCorrectionDialog && correction.trim()) {
                  handleFeedback(showCorrectionDialog, 'CORRECTED', correction);
                }
              }}
              disabled={!correction.trim()}
            >
              Submit Correction
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );

  if (variant === 'dialog') {
    return (
      <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose?.()}>
        <DialogContent className="max-w-[90vw] sm:max-w-5xl h-[80vh] max-h-[700px] p-0 overflow-hidden">
          {content}
        </DialogContent>
      </Dialog>
    );
  }

  return (
    <Card className="rounded-xl overflow-hidden shadow-lg">
      {content}
    </Card>
  );
};

// Helper function for suggested questions
function getSuggestedQuestions(contextType: string): string[] {
  switch (contextType) {
    case 'pitch':
      return [
        'What is the current status?',
        'Are there any risks?',
        'What meetings have been held?',
        'How much progress has been made?',
      ];
    case 'meeting':
      return [
        'What decisions were made?',
        'What are the action items?',
        'Were there any blockers discussed?',
      ];
    case 'team':
      return [
        'What pitches is this team working on?',
        'How is the team progressing?',
        'Who are the team members?',
      ];
    case 'cycle':
      return [
        'What pitches are in this cycle?',
        'How is the cycle progressing?',
        'Are there any at-risk pitches?',
        'What is the timeline?',
      ];
    default:
      return [
        'Tell me more about this',
        'What should I know?',
        'Are there any issues?',
      ];
  }
}

export default QAPanel;
