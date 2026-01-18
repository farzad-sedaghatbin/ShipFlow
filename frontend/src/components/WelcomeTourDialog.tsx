import React, { useState, useEffect } from 'react';
import { Play, SkipForward, Compass } from 'lucide-react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation();
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
      emoji: t('welcomeTour.features.createProjects.emoji'),
      title: t('welcomeTour.features.createProjects.title'),
      description: t('welcomeTour.features.createProjects.description'),
    },
    {
      emoji: t('welcomeTour.features.planCycles.emoji'),
      title: t('welcomeTour.features.planCycles.title'),
      description: t('welcomeTour.features.planCycles.description'),
    },
    {
      emoji: t('welcomeTour.features.shapePitches.emoji'),
      title: t('welcomeTour.features.shapePitches.title'),
      description: t('welcomeTour.features.shapePitches.description'),
    },
    {
      emoji: t('welcomeTour.features.trackHillCharts.emoji'),
      title: t('welcomeTour.features.trackHillCharts.title'),
      description: t('welcomeTour.features.trackHillCharts.description'),
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
            {t('welcomeTour.title')}
          </DialogTitle>
        </DialogHeader>

        <div className="text-center pb-2">
          <p className="text-sm text-white/80 mb-6">
            {t('welcomeTour.description')}
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
            {t('welcomeTour.takeAQuickTour')}
          </p>
        </div>
        
        <DialogFooter className="gap-2 sm:justify-center">
          <Button
            variant="ghost"
            onClick={handleSkip}
            className="text-white/70 hover:text-white hover:bg-white/10"
          >
            <SkipForward className="h-4 w-4 mr-2" />
            {t('welcomeTour.skipForNow')}
          </Button>
          <Button
            onClick={handleStartTour}
            className="bg-primary hover:bg-primary/90 px-6"
          >
            <Play className="h-4 w-4 mr-2" />
            {t('welcomeTour.startTour')}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
};

export default WelcomeTourDialog;
