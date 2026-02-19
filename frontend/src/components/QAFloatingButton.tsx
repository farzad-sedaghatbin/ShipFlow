import React, { useState } from 'react';
import { MessageCircleQuestion } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from '@/components/ui/tooltip';
import QAPanel from './QAPanel';

interface QAFloatingButtonProps {
  contextType: 'pitch' | 'meeting' | 'team' | 'cycle';
  contextId?: number;
  contextName?: string;
  cycleId?: number;
  teamId?: number;
}

export const QAFloatingButton: React.FC<QAFloatingButtonProps> = (props) => {
  const [open, setOpen] = useState(false);

  return (
    <>
      <Tooltip>
        <TooltipTrigger asChild>
          <Button
            onClick={() => setOpen(true)}
            aria-label="Open AI-powered question and answer panel"
            aria-haspopup="dialog"
            aria-expanded={open}
            className={cn(
              "fixed bottom-20 sm:bottom-6 left-6 z-50 h-14 w-14 rounded-full shadow-lg",
              "bg-primary hover:bg-primary/90",
              "transition-transform hover:scale-105",
              "focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            )}
            size="icon"
          >
            <MessageCircleQuestion className="h-6 w-6" />
            {/* Notification dot */}
            <span className="absolute top-0 right-0 h-3 w-3 rounded-full bg-chart-2 border-2 border-background" />
          </Button>
        </TooltipTrigger>
        <TooltipContent side="right">
          Ask a Question (Press ? for keyboard shortcuts)
        </TooltipContent>
      </Tooltip>

      <QAPanel
        {...props}
        variant="dialog"
        open={open}
        onClose={() => setOpen(false)}
      />
    </>
  );
};

export default QAFloatingButton;
