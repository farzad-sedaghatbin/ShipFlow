import React, { useState, useEffect } from 'react';
import { Play, SkipForward, Compass } from 'lucide-react';
import { useTour } from '../contexts';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Button } from '@/components/ui/button';

const WELCOME_SHOWN_KEY = 'shipflow_welcome_shown';

export const WelcomeTourDialog: React.FC = () => {
  const [open, setOpen] = useState(false);
  const { startTour, hasCompletedTour } = useTour();

  useEffect(() => {
    // Show welcome dialog only once for new users who haven't completed the tour
    const hasSeenWelcome = localStorage.getItem(WELCOME_SHOWN_KEY) === 'true';
    if (!hasSeenWelcome && !hasCompletedTour) {
      // Delay showing the dialog to let the page render first
      const timer = setTimeout(() => setOpen(true), 1000);
      return () => clearTimeout(timer);
    }
  }, [hasCompletedTour]);

  const handleStartTour = () => {
    localStorage.setItem(WELCOME_SHOWN_KEY, 'true');
    setOpen(false);
    // Small delay to let dialog close smoothly
    setTimeout(() => startTour(), 300);
  };

  const handleSkip = () => {
    localStorage.setItem(WELCOME_SHOWN_KEY, 'true');
    setOpen(false);
  };

  const features = [
    {
      emoji: '📁',
      title: 'Create Projects',
      description: 'Organize your work into distinct projects',
    },
    {
      emoji: '🔄',
      title: 'Plan Cycles',
      description: 'Set up 6-week development cycles with build and cooldown phases',
    },
    {
      emoji: '📋',
      title: 'Shape Pitches',
      description: 'Define shaped work items with time appetites',
    },
    {
      emoji: '⛰️',
      title: 'Track with Hill Charts',
      description: 'Visualize progress from "figuring it out" to "making it happen"',
    },
  ];

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogContent className="sm:max-w-md bg-gradient-to-br from-[#1a1a2e] to-[#16213e] border-border text-white">
        <DialogHeader className="text-center pt-4">
          <div className="mx-auto mb-4 flex h-20 w-20 items-center justify-center rounded-full bg-gradient-to-br from-primary to-primary/80 shadow-lg shadow-primary/30">
            <Compass className="h-10 w-10" />
          </div>
          <DialogTitle className="text-2xl font-bold text-white">
            Welcome to ShipFlow! 👋
          </DialogTitle>
        </DialogHeader>

        <div className="text-center pb-2">
          <p className="text-sm text-white/80 mb-6">
            ShipFlow helps you manage projects using the Shape Up methodology - 
            with cycles, pitches, and hill charts to track progress.
          </p>
          
          <div className="space-y-3 text-left mb-6">
            {features.map((feature, index) => (
              <div key={index} className="flex gap-3 items-start">
                <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-white/10 shrink-0">
                  <span>{feature.emoji}</span>
                </div>
                <div>
                  <h4 className="text-sm font-semibold text-white">
                    {feature.title}
                  </h4>
                  <p className="text-xs text-white/60">
                    {feature.description}
                  </p>
                </div>
              </div>
            ))}
          </div>
          
          <p className="text-xs text-white/60">
            Would you like a quick guided tour to learn the basics?
          </p>
        </div>
        
        <DialogFooter className="gap-2 sm:justify-center">
          <Button
            variant="ghost"
            onClick={handleSkip}
            className="text-white/70 hover:text-white hover:bg-white/10"
          >
            <SkipForward className="h-4 w-4 mr-2" />
            Skip for Now
          </Button>
          <Button
            onClick={handleStartTour}
            className="bg-primary hover:bg-primary/90 px-6"
          >
            <Play className="h-4 w-4 mr-2" />
            Start Guided Tour
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default WelcomeTourDialog;
