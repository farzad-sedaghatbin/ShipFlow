import React, { useState } from 'react';
import { ThumbsUp, ThumbsDown, MessageSquare } from 'lucide-react';
import { 
  riskFeedbackService, 
  CreateRiskFeedbackRequest,
  FeedbackRating,
  getFeedbackRatingLabel,
} from '../services/riskFeedbackService';
import { useToast } from '../contexts';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from './ui/dialog';
import { Button } from './ui/button';
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
import { Slider } from './ui/slider';
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from './ui/tooltip';

interface RiskFeedbackFormProps {
  pitchId: number;
  pitchTitle: string;
  currentRiskScore: number;
  onFeedbackSubmitted?: () => void;
}

export const RiskFeedbackForm: React.FC<RiskFeedbackFormProps> = ({
  pitchId,
  pitchTitle,
  currentRiskScore,
  onFeedbackSubmitted,
}) => {
  const { showToast } = useToast();
  const [open, setOpen] = useState(false);
  const [rating, setRating] = useState<FeedbackRating>('ACCURATE');
  const [suggestedScore, setSuggestedScore] = useState<number>(currentRiskScore);
  const [notes, setNotes] = useState('');
  const [missedFactors, setMissedFactors] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [alreadySubmitted, setAlreadySubmitted] = useState(false);

  // Check if user already submitted feedback
  React.useEffect(() => {
    const checkSubmission = async () => {
      try {
        const hasSubmitted = await riskFeedbackService.hasUserSubmittedFeedback(pitchId);
        setAlreadySubmitted(hasSubmitted);
      } catch (err) {
        // Ignore errors for this check
      }
    };
    checkSubmission();
  }, [pitchId]);

  const handleSubmit = async () => {
    try {
      setSubmitting(true);
      const request: CreateRiskFeedbackRequest = {
        pitchId,
        originalRiskScore: currentRiskScore,
        rating,
        suggestedRiskScore: rating !== 'ACCURATE' ? suggestedScore : undefined,
        notes: notes.trim() || undefined,
        missedFactors: missedFactors.trim() || undefined,
      };
      
      await riskFeedbackService.submitFeedback(request);
      showToast('Feedback submitted successfully. Thank you!', 'success');
      setOpen(false);
      setAlreadySubmitted(true);
      onFeedbackSubmitted?.();
    } catch (err) {
      showToast('Failed to submit feedback', 'error');
      console.error(err);
    } finally {
      setSubmitting(false);
    }
  };

  const showSuggestedScore = rating !== 'ACCURATE';

  return (
    <>
      <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground">
          Was this assessment helpful?
        </span>
        
        {alreadySubmitted ? (
          <Badge variant="outline" className="bg-green-500/10 text-green-500 border-green-500/30">
            Feedback submitted
          </Badge>
        ) : (
          <TooltipProvider>
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-green-500 hover:text-green-400 hover:bg-green-500/10"
                  onClick={() => {
                    setRating('ACCURATE');
                    setOpen(true);
                  }}
                >
                  <ThumbsUp className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Mark as accurate</TooltipContent>
            </Tooltip>
            
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-red-500 hover:text-red-400 hover:bg-red-500/10"
                  onClick={() => {
                    setRating('INACCURATE');
                    setOpen(true);
                  }}
                >
                  <ThumbsDown className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Mark as inaccurate</TooltipContent>
            </Tooltip>
            
            <Tooltip>
              <TooltipTrigger asChild>
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8 text-primary hover:text-primary/80 hover:bg-primary/10"
                  onClick={() => setOpen(true)}
                >
                  <MessageSquare className="h-4 w-4" />
                </Button>
              </TooltipTrigger>
              <TooltipContent>Provide detailed feedback</TooltipContent>
            </Tooltip>
          </TooltipProvider>
        )}
      </div>

      <Dialog open={open} onOpenChange={setOpen}>
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>AI Risk Assessment Feedback</DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            <Alert>
              <AlertDescription>
                Your feedback helps improve AI risk analysis accuracy for future assessments.
              </AlertDescription>
            </Alert>

            <div>
              <p className="text-sm font-medium">Pitch: {pitchTitle}</p>
              <p className="text-sm text-muted-foreground">
                Current AI Risk Score: <strong>{currentRiskScore}</strong>
              </p>
            </div>

            <div className="space-y-2">
              <Label>How accurate was this assessment?</Label>
              <Select
                value={rating}
                onValueChange={(value) => setRating(value as FeedbackRating)}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACCURATE">{getFeedbackRatingLabel('ACCURATE')}</SelectItem>
                  <SelectItem value="PARTIALLY_ACCURATE">{getFeedbackRatingLabel('PARTIALLY_ACCURATE')}</SelectItem>
                  <SelectItem value="INACCURATE">{getFeedbackRatingLabel('INACCURATE')}</SelectItem>
                  <SelectItem value="OVERLY_PESSIMISTIC">{getFeedbackRatingLabel('OVERLY_PESSIMISTIC')}</SelectItem>
                  <SelectItem value="OVERLY_OPTIMISTIC">{getFeedbackRatingLabel('OVERLY_OPTIMISTIC')}</SelectItem>
                </SelectContent>
              </Select>
            </div>

            {showSuggestedScore && (
              <div className="space-y-3">
                <Label>What do you think the risk score should be?</Label>
                <div className="flex items-center gap-4">
                  <span className="text-sm text-green-500">Low</span>
                  <div className="flex-1">
                    <Slider
                      value={[suggestedScore]}
                      onValueChange={(value) => setSuggestedScore(value[0])}
                      min={0}
                      max={100}
                      step={1}
                    />
                    <div className="flex justify-between mt-1 text-xs text-muted-foreground">
                      <span>0</span>
                      <span>30</span>
                      <span>60</span>
                      <span>100</span>
                    </div>
                  </div>
                  <span className="text-sm text-red-500">High</span>
                </div>
                <p className="text-center text-sm font-medium">{suggestedScore}</p>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="feedback-notes">Additional notes (optional)</Label>
              <Textarea
                id="feedback-notes"
                placeholder="Any context about why you agree or disagree with this assessment..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={2}
              />
            </div>

            {rating !== 'ACCURATE' && (
              <div className="space-y-2">
                <Label htmlFor="missed-factors">What did the AI miss? (optional)</Label>
                <Textarea
                  id="missed-factors"
                  placeholder="E.g., team experience, similar past projects, external dependencies..."
                  value={missedFactors}
                  onChange={(e) => setMissedFactors(e.target.value)}
                  rows={2}
                />
              </div>
            )}
          </div>

          <DialogFooter>
            <Button variant="outline" onClick={() => setOpen(false)}>
              Cancel
            </Button>
            <Button onClick={handleSubmit} disabled={submitting}>
              {submitting ? 'Submitting...' : 'Submit Feedback'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
};

export default RiskFeedbackForm;
