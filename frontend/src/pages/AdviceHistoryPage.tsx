import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useSearchParams } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import {
  ArrowLeft,
  Brain,
  Calendar,
  ChevronRight,
  Clock,
  ExternalLink,
  History,
  Loader2,
  MessageSquare,
  RefreshCw,
  ThumbsDown,
  ThumbsUp,
} from 'lucide-react';
import { Button } from '../components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Badge } from '../components/ui/badge';
import { ScrollArea } from '../components/ui/scroll-area';
import { Textarea } from '../components/ui/textarea';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../components/ui/dialog';
import { useToast } from '../contexts';
import { wiseArchitectureService } from '../services/wiseArchitectureService';
import {
  AdviceHistoryItem,
  ConversationHistoryPage,
  STACK_DISPLAY_NAMES,
  TechStackType,
} from '../types/wiseArchitecture';
import { formatDistanceToNow, format } from 'date-fns';

/**
 * Page for viewing and managing WISE Architecture advice history.
 * Users can review past AI-generated solutions and provide feedback.
 */
const AdviceHistoryPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();

  // State
  const [loading, setLoading] = useState(true);
  const [historyPage, setHistoryPage] = useState<ConversationHistoryPage | null>(null);
  const [selectedConversation, setSelectedConversation] = useState<AdviceHistoryItem[] | null>(
    null
  );
  const [selectedConversationId, setSelectedConversationId] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [feedbackDialogOpen, setFeedbackDialogOpen] = useState(false);
  const [feedbackAdviceId, setFeedbackAdviceId] = useState<number | null>(null);
  const [feedbackHelpful, setFeedbackHelpful] = useState<boolean | null>(null);
  const [feedbackText, setFeedbackText] = useState('');
  const [submittingFeedback, setSubmittingFeedback] = useState(false);

  // Load history on mount
  useEffect(() => {
    loadHistory();
  }, [page]);

  // Check for conversation ID in URL
  useEffect(() => {
    const conversationId = searchParams.get('conversation');
    if (conversationId && conversationId !== selectedConversationId) {
      loadConversation(conversationId);
    }
  }, [searchParams]);

  const loadHistory = async () => {
    try {
      setLoading(true);
      const result = await wiseArchitectureService.getMyHistory(page, 10);
      setHistoryPage(result);
    } catch (error) {
      console.error('Failed to load history:', error);
      showToast(t('wiseArchitecture.history.loadFailed') || 'Failed to load advice history', 'error');
    } finally {
      setLoading(false);
    }
  };

  const loadConversation = async (conversationId: string) => {
    try {
      setLoading(true);
      setSelectedConversationId(conversationId);
      const messages = await wiseArchitectureService.getConversation(conversationId);
      setSelectedConversation(messages);
      setSearchParams({ conversation: conversationId });
    } catch (error) {
      console.error('Failed to load conversation:', error);
      showToast(t('wiseArchitecture.history.loadFailed') || 'Failed to load conversation', 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleBackToList = () => {
    setSelectedConversation(null);
    setSelectedConversationId(null);
    setSearchParams({});
  };

  const openFeedbackDialog = (adviceId: number, currentHelpful?: boolean) => {
    setFeedbackAdviceId(adviceId);
    setFeedbackHelpful(currentHelpful ?? null);
    setFeedbackText('');
    setFeedbackDialogOpen(true);
  };

  const submitFeedback = async () => {
    if (feedbackAdviceId === null || feedbackHelpful === null) return;

    try {
      setSubmittingFeedback(true);
      const updated = await wiseArchitectureService.submitFeedback(feedbackAdviceId, {
        helpful: feedbackHelpful,
        feedbackText: feedbackText || undefined,
      });

      // Update in conversation if viewing
      if (selectedConversation) {
        setSelectedConversation(
          selectedConversation.map((item) => (item.id === feedbackAdviceId ? updated : item))
        );
      }

      showToast(t('wiseArchitecture.history.feedback.submitted') || 'Thank you for your feedback!', 'success');
      setFeedbackDialogOpen(false);
    } catch (error) {
      console.error('Failed to submit feedback:', error);
      showToast(t('wiseArchitecture.history.feedback.failed') || 'Failed to submit feedback', 'error');
    } finally {
      setSubmittingFeedback(false);
    }
  };

  const formatDate = (dateStr: string) => {
    try {
      return formatDistanceToNow(new Date(dateStr), { addSuffix: true });
    } catch {
      return dateStr;
    }
  };

  const formatFullDate = (dateStr: string) => {
    try {
      return format(new Date(dateStr), 'PPpp');
    } catch {
      return dateStr;
    }
  };

  const getStackBadgeColor = (stack: string): string => {
    if (stack.startsWith('MOBILE')) return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200';
    if (stack.startsWith('BACKEND')) return 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200';
    if (stack.startsWith('WEB')) return 'bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200';
    return 'bg-gray-100 text-gray-800 dark:bg-gray-800 dark:text-gray-200';
  };

  // Conversation list view
  const renderConversationList = () => (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-xl font-semibold">Your Advice History</h2>
          <p className="text-sm text-muted-foreground">
            Review past AI-generated solutions and provide feedback
          </p>
        </div>
        <Button variant="outline" onClick={() => loadHistory()}>
          <RefreshCw className="mr-2 h-4 w-4" />
          Refresh
        </Button>
      </div>

      {historyPage?.content.length === 0 ? (
        <Card>
          <CardContent className="py-12 text-center">
            <History className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <h3 className="text-lg font-medium mb-2">No Advice History</h3>
            <p className="text-muted-foreground mb-4">
              You haven't generated any AI solutions yet.
            </p>
            <Button onClick={() => navigate('/rd/wise-architecture')}>
              <Brain className="mr-2 h-4 w-4" />
              Go to WISE Architecture
            </Button>
          </CardContent>
        </Card>
      ) : (
        <>
          <div className="space-y-3">
            {historyPage?.content.map((conversation) => (
              <Card
                key={conversation.conversationId}
                className="cursor-pointer hover:shadow-md transition-shadow"
                onClick={() => loadConversation(conversation.conversationId)}
              >
                <CardContent className="py-4">
                  <div className="flex items-start justify-between">
                    <div className="flex-1 min-w-0">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-medium truncate">{conversation.pitchTitle}</h3>
                        {conversation.projectName && (
                          <Badge variant="outline" className="text-xs">
                            {conversation.projectName}
                          </Badge>
                        )}
                      </div>
                      <p className="text-sm text-muted-foreground line-clamp-2 mb-2">
                        {conversation.responsePreview}
                      </p>
                      <div className="flex items-center gap-4 text-xs text-muted-foreground">
                        <span className="flex items-center gap-1">
                          <Calendar className="h-3 w-3" />
                          {formatDate(conversation.createdAt)}
                        </span>
                        <span className="flex items-center gap-1">
                          <MessageSquare className="h-3 w-3" />
                          {conversation.messageCount} message{conversation.messageCount !== 1 ? 's' : ''}
                        </span>
                      </div>
                    </div>
                    <div className="flex flex-col items-end gap-2 ml-4">
                      <div className="flex flex-wrap gap-1 justify-end">
                        {conversation.techStacks.slice(0, 3).map((stack) => (
                          <Badge
                            key={stack}
                            variant="secondary"
                            className={`text-xs ${getStackBadgeColor(stack)}`}
                          >
                            {STACK_DISPLAY_NAMES[stack as TechStackType] || stack}
                          </Badge>
                        ))}
                        {conversation.techStacks.length > 3 && (
                          <Badge variant="secondary" className="text-xs">
                            +{conversation.techStacks.length - 3}
                          </Badge>
                        )}
                      </div>
                      <ChevronRight className="h-5 w-5 text-muted-foreground" />
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
          </div>

          {/* Pagination */}
          {historyPage && historyPage.totalPages > 1 && (
            <div className="flex items-center justify-center gap-2 mt-4">
              <Button
                variant="outline"
                size="sm"
                disabled={historyPage.first}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                Previous
              </Button>
              <span className="text-sm text-muted-foreground">
                Page {historyPage.number + 1} of {historyPage.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                disabled={historyPage.last}
                onClick={() => setPage((p) => p + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
    </div>
  );

  // Single conversation view
  const renderConversation = () => (
    <div className="space-y-4">
      <Button variant="ghost" onClick={handleBackToList}>
        <ArrowLeft className="mr-2 h-4 w-4" />
        Back to History
      </Button>

      {selectedConversation && selectedConversation.length > 0 && (
        <>
          <Card>
            <CardHeader>
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle>{selectedConversation[0].pitchTitle}</CardTitle>
                  <CardDescription>
                    Started {formatFullDate(selectedConversation[0].createdAt)}
                  </CardDescription>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate(`/pitches/${selectedConversation[0].pitchId}`)}
                >
                  <ExternalLink className="mr-2 h-4 w-4" />
                  View Pitch
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              <div className="flex flex-wrap gap-2 mb-4">
                {selectedConversation[0].techStacks.map((stack) => (
                  <Badge
                    key={stack}
                    variant="secondary"
                    className={getStackBadgeColor(stack)}
                  >
                    {STACK_DISPLAY_NAMES[stack as TechStackType] || stack}
                  </Badge>
                ))}
              </div>
              <div className="flex items-center gap-4 text-sm text-muted-foreground">
                {selectedConversation[0].hasFigmaContext && (
                  <Badge variant="outline">Figma Context</Badge>
                )}
                {selectedConversation[0].hasGitHubContext && (
                  <Badge variant="outline">GitHub Context</Badge>
                )}
                {selectedConversation[0].hasRoadmapContext && (
                  <Badge variant="outline">Roadmap Context</Badge>
                )}
              </div>
            </CardContent>
          </Card>

          <ScrollArea className="h-[calc(100vh-400px)]">
            <div className="space-y-4">
              {selectedConversation.map((item, index) => (
                <Card key={item.id} className={index === 0 ? 'border-primary/20' : ''}>
                  <CardHeader className="pb-2">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <Badge variant={item.messageType === 'INITIAL_SOLUTION' ? 'default' : 'secondary'}>
                          {item.messageType === 'INITIAL_SOLUTION' ? 'Initial Solution' : 'Follow-up'}
                        </Badge>
                        <span className="text-xs text-muted-foreground">
                          {formatDate(item.createdAt)}
                        </span>
                        {item.processingTimeMs && (
                          <span className="text-xs text-muted-foreground flex items-center gap-1">
                            <Clock className="h-3 w-3" />
                            {(item.processingTimeMs / 1000).toFixed(1)}s
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        {item.feedbackHelpful !== undefined && item.feedbackHelpful !== null ? (
                          <Badge
                            variant={item.feedbackHelpful ? 'default' : 'secondary'}
                            className={item.feedbackHelpful ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}
                          >
                            {item.feedbackHelpful ? (
                              <>
                                <ThumbsUp className="mr-1 h-3 w-3" />
                                Helpful
                              </>
                            ) : (
                              <>
                                <ThumbsDown className="mr-1 h-3 w-3" />
                                Not Helpful
                              </>
                            )}
                          </Badge>
                        ) : (
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => openFeedbackDialog(item.id)}
                          >
                            Rate this advice
                          </Button>
                        )}
                      </div>
                    </div>
                  </CardHeader>
                  <CardContent>
                    {item.userMessage && item.messageType === 'FOLLOW_UP' && (
                      <div className="mb-4 p-3 bg-muted rounded-lg">
                        <p className="text-sm font-medium mb-1">Your Question:</p>
                        <p className="text-sm">{item.userMessage}</p>
                      </div>
                    )}
                    <div className="prose prose-sm dark:prose-invert max-w-none">
                      <ReactMarkdown>{item.aiResponse}</ReactMarkdown>
                    </div>
                    {item.feedbackText && (
                      <div className="mt-4 p-3 bg-muted rounded-lg">
                        <p className="text-sm font-medium mb-1">Your Feedback:</p>
                        <p className="text-sm">{item.feedbackText}</p>
                      </div>
                    )}
                  </CardContent>
                </Card>
              ))}
            </div>
          </ScrollArea>

          <div className="flex justify-center">
            <Button onClick={() => navigate(`/rd/wise-architecture?pitch=${selectedConversation[0].pitchId}`)}>
              <Brain className="mr-2 h-4 w-4" />
              Continue in WISE Architecture
            </Button>
          </div>
        </>
      )}
    </div>
  );

  return (
    <div className="container mx-auto py-6 max-w-4xl">
      {/* Header */}
      <div className="flex items-center gap-4 mb-6">
        <Button variant="ghost" size="icon" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <History className="h-6 w-6" />
            Advice History
          </h1>
          <p className="text-muted-foreground">
            Review your past AI-generated solutions
          </p>
        </div>
      </div>

      {loading && !selectedConversation && !historyPage ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : selectedConversation ? (
        renderConversation()
      ) : (
        renderConversationList()
      )}

      {/* Feedback Dialog */}
      <Dialog open={feedbackDialogOpen} onOpenChange={setFeedbackDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Rate This Advice</DialogTitle>
            <DialogDescription>
              Help us improve by rating the quality of this AI-generated solution.
            </DialogDescription>
          </DialogHeader>
          <div className="py-4 space-y-4">
            <div className="flex justify-center gap-4">
              <Button
                variant={feedbackHelpful === true ? 'default' : 'outline'}
                size="lg"
                onClick={() => setFeedbackHelpful(true)}
                className={feedbackHelpful === true ? 'bg-green-600 hover:bg-green-700' : ''}
              >
                <ThumbsUp className="mr-2 h-5 w-5" />
                Helpful
              </Button>
              <Button
                variant={feedbackHelpful === false ? 'default' : 'outline'}
                size="lg"
                onClick={() => setFeedbackHelpful(false)}
                className={feedbackHelpful === false ? 'bg-red-600 hover:bg-red-700' : ''}
              >
                <ThumbsDown className="mr-2 h-5 w-5" />
                Not Helpful
              </Button>
            </div>
            <Textarea
              placeholder="Optional: Tell us more about why this was or wasn't helpful..."
              value={feedbackText}
              onChange={(e) => setFeedbackText(e.target.value)}
              rows={3}
            />
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setFeedbackDialogOpen(false)}>
              Cancel
            </Button>
            <Button onClick={submitFeedback} disabled={feedbackHelpful === null || submittingFeedback}>
              {submittingFeedback && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Submit Feedback
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default AdviceHistoryPage;
