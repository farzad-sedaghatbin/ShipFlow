import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Send, MessageSquare, Bot, User, Loader2, X } from 'lucide-react';
import { askQuestionAsync, JobStatusResponse } from '../services/riskService';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Badge } from './ui/badge';
import { Alert, AlertDescription } from './ui/alert';
import { Markdown } from './ui/markdown';
import { cn } from '../lib/utils';

interface RiskQAProps {
  pitchId: number;
  pitchTitle?: string;
}

interface Message {
  type: 'question' | 'answer';
  text: string;
  timestamp: Date;
  confidenceScore?: number;
}

export const RiskQA: React.FC<RiskQAProps> = ({ pitchId }) => {
  const { t } = useTranslation();
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadingStatus, setLoadingStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

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
      setLoadingStatus('Sending to AI advisor...');
      
      // Use async Q&A to prevent blocking on slow AI responses
      const response = await askQuestionAsync(pitchId, userQuestion, (status: JobStatusResponse) => {
        if (status.status === 'PENDING') {
          setLoadingStatus('Queued for AI processing...');
        } else if (status.status === 'PROCESSING') {
          setLoadingStatus('AI thinking...');
        }
      });
      
      if (response) {
        if (response.errorMessage) {
          setError(response.errorMessage);
        } else if (response.answer) {
          setMessages(prev => [...prev, {
            type: 'answer',
            text: response.answer!,
            timestamp: new Date(response.answeredAt),
            confidenceScore: response.confidenceScore,
          }]);
        }
      } else {
        setError('Failed to get AI response. Please try again.');
      }
    } catch (err: any) {
      setError(err.message || t('errors.getAnswerFailed'));
    } finally {
      setLoading(false);
      setLoadingStatus(null);
    }
  };

  const suggestedQuestions = [
    "What are the main risks for this pitch?",
    "How can we reduce the risk score?",
    "Is the timeline realistic?",
    "What should we prioritize?",
  ];

  const handleSuggestedQuestion = (q: string) => {
    setQuestion(q);
  };

  return (
    <div>
      <div className="flex items-center gap-2 mb-4">
        <MessageSquare className="h-4 w-4 text-primary" />
        <h4 className="text-sm font-semibold">Ask the AI Risk Advisor</h4>
      </div>

      {/* Suggested Questions */}
      {messages.length === 0 && (
        <div className="mb-4">
          <p className="text-xs text-muted-foreground mb-2">Try asking:</p>
          <div className="flex flex-wrap gap-1.5">
            {suggestedQuestions.map((q, idx) => (
              <Badge
                key={idx}
                variant="outline"
                className="cursor-pointer hover:bg-primary/10 text-xs"
                onClick={() => handleSuggestedQuestion(q)}
              >
                {q}
              </Badge>
            ))}
          </div>
        </div>
      )}

      {/* Messages */}
      {messages.length > 0 && (
        <div className="max-h-[300px] overflow-y-auto mb-4 pr-2 space-y-2">
          {messages.map((msg, idx) => (
            <div
              key={idx}
              className={cn(
                "p-3 rounded-lg",
                msg.type === 'question' 
                  ? "bg-primary/10" 
                  : "bg-muted border-l-2 border-primary"
              )}
            >
              <div className="flex items-start gap-2">
                {msg.type === 'question' ? (
                  <User className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                ) : (
                  <Bot className="h-4 w-4 text-primary shrink-0 mt-0.5" />
                )}
                <div className="flex-1 min-w-0">
                  {msg.type === 'answer' ? (
                    <Markdown content={msg.text} className="text-sm" />
                  ) : (
                    <p className="text-sm whitespace-pre-wrap">{msg.text}</p>
                  )}
                  {msg.type === 'answer' && msg.confidenceScore && (
                    <p className="text-xs text-muted-foreground mt-1">
                      Confidence: {msg.confidenceScore}%
                    </p>
                  )}
                </div>
              </div>
            </div>
          ))}
          
          {loading && (
            <div className="flex items-center gap-2 p-3">
              <Loader2 className="h-4 w-4 animate-spin" />
              <span className="text-sm text-muted-foreground">{loadingStatus || 'Thinking...'}</span>
            </div>
          )}
        </div>
      )}

      {/* Error Alert */}
      {error && (
        <Alert variant="destructive" className="mb-4">
          <AlertDescription className="flex items-center justify-between">
            <span>{error}</span>
            <Button 
              variant="ghost" 
              size="icon" 
              className="h-6 w-6" 
              onClick={() => setError(null)}
            >
              <X className="h-4 w-4" />
            </Button>
          </AlertDescription>
        </Alert>
      )}

      {/* Input Form */}
      <form onSubmit={handleSubmit} className="flex gap-2">
        <Input
          placeholder="Ask a question about this pitch's risk..."
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
      </form>
    </div>
  );
};

export default RiskQA;
