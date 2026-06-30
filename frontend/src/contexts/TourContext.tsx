import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { driver, DriveStep, Driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import '../styles/tour.css';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ConfirmDialog } from '../components/ui/confirm-dialog';
import { useProject } from './ProjectContext';

interface TourStep extends DriveStep {
  route?: string;
}

interface TourContextType {
  startTour: () => void;
  stopTour: () => void;
  isTourActive: boolean;
  hasCompletedTour: boolean;
  resetTour: () => void;
}

const TourContext = createContext<TourContextType | undefined>(undefined);

const TOUR_COMPLETED_KEY = 'shipflow_tour_completed';

export function TourProvider({ children }: { children: React.ReactNode }) {
  const [isTourActive, setIsTourActive] = useState(false);
  const [driverInstance, setDriverInstance] = useState<Driver | null>(null);
  const [hasCompletedTour, setHasCompletedTour] = useState(() => {
    return localStorage.getItem(TOUR_COMPLETED_KEY) === 'true';
  });
  const navigate = useNavigate();
  const location = useLocation();
  const { t } = useTranslation();
  const { isScrumProject } = useProject();

  // Track if navigation was triggered by the tour driver
  const isNavigatingRef = useRef(false);
  const expectedRouteRef = useRef<string | null>(null);
  const [skipConfirmOpen, setSkipConfirmOpen] = useState(false);
  const pendingDestroyRef = useRef<Driver | null>(null);
  // Track the pending navigation timeout so it can be cancelled if the tour stops early
  const navTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Tour steps definition
  // The Sprint Planning step is only included when the active project is SCRUM.
  // Non-SCRUM projects redirect away from /sprint-planning, which would break the tour.
  const getTourSteps = useCallback((): TourStep[] => {
    const allSteps: TourStep[] = [
    {
      element: '[data-tour="sidebar"]',
      popover: {
        title: '👋 Welcome to ShipFlow!',
        description: 'This is the navigation sidebar. Let\'s walk you through the main features of the app to help you get started.',
        side: 'right',
        align: 'start',
      },
      route: '/',
    },
    {
      element: '[data-tour="projects-menu"]',
      popover: {
        title: '📁 Projects',
        description: 'Projects are the foundation of your work. Each project contains multiple cycles and pitches. Let\'s start by creating one!',
        side: 'right',
        align: 'start',
      },
      route: '/',
    },
    {
      element: '[data-tour="new-project-btn"]',
      popover: {
        title: '➕ Create Your First Project',
        description: 'Click "New Project" to create your first project. Give it a name and a unique project key (like "PROJ" or "MVP").',
        side: 'bottom',
        align: 'start',
      },
      route: '/projects',
    },
    {
      element: '[data-tour="project-card"]',
      popover: {
        title: '🎉 Project Created!',
        description: 'Once created, your project will appear here as a card. You can view cycles, edit, or archive projects from this view.',
        side: 'right',
        align: 'start',
      },
      route: '/projects',
    },
    {
      element: '[data-tour="cycles-menu"]',
      popover: {
        title: '🔄 Cycles',
        description: 'Shape Up uses 6-week cycles. Each cycle has a build phase (for development) and a cooldown phase (for bug fixes and exploration).',
        side: 'right',
        align: 'start',
      },
      route: '/projects',
    },
    {
      element: '[data-tour="new-cycle-btn"]',
      popover: {
        title: '➕ Create a Cycle',
        description: 'Click "New Cycle" to create a development cycle. Set the start and end dates, typically 6 weeks apart.',
        side: 'bottom',
        align: 'start',
      },
      route: '/cycles',
    },
    {
      element: '[data-tour="cycle-card"]',
      popover: {
        title: '📅 Your Cycle',
        description: 'Your cycle will show the phase (Build/Cooldown), the number of pitches, and quick actions to manage it.',
        side: 'right',
        align: 'start',
      },
      route: '/cycles',
    },
    {
      element: '[data-tour="pitches-menu"]',
      popover: {
        title: '📋 Pitch Board',
        description: 'Pitches are your shaped work items. They have a defined appetite (time budget) and move through status columns.',
        side: 'right',
        align: 'start',
      },
      route: '/cycles',
    },
    {
      element: '[data-tour="new-pitch-btn"]',
      popover: {
        title: '➕ Create a Pitch',
        description: 'Create a new pitch by clicking "New Pitch". Define the title, description, and appetite (recommended: 1-6 weeks).',
        side: 'bottom',
        align: 'start',
      },
      route: '/pitches',
    },
    {
      element: '[data-tour="pitch-board"]',
      popover: {
        title: '📊 Kanban Board',
        description: 'Pitches flow through columns: Pending → Started → In Progress → Testing → Done. Drag cards to update status.',
        side: 'top',
        align: 'center',
      },
      route: '/pitches',
    },
    {
      element: '[data-tour="betting-menu"]',
      popover: {
        title: '🎰 Betting Table',
        description: 'The Betting Table is where you decide what to build next. Drag shaped pitches onto team tracks to plan your cycle.',
        side: 'right',
        align: 'start',
      },
      route: '/pitches',
    },
    {
      element: '[data-tour="betting-table"]',
      popover: {
        title: '📋 Plan Your Cycle',
        description: 'Shaped pitches appear on the left. Drag them onto team slots on the right. Each slot shows available capacity for the cycle.',
        side: 'top',
        align: 'center',
      },
      route: '/betting',
    },
    {
      element: '[data-tour="health-menu"]',
      popover: {
        title: '📈 Health Overview',
        description: 'The Health Overview shows risk analysis, progress metrics, and Hill Charts for tracking project health.',
        side: 'right',
        align: 'start',
      },
      route: '/betting',
    },
    {
      element: '[data-tour="hill-chart-section"]',
      popover: {
        title: '⛰️ Hill Chart',
        description: 'The Hill Chart visualizes progress. Work climbs the "figuring it out" phase (left), then descends the "making it happen" phase (right).',
        side: 'top',
        align: 'center',
      },
      route: '/health',
    },
    {
      element: '[data-tour="retros-menu"]',
      popover: {
        title: '🧠 Retrospectives',
        description: 'Capture learnings after every cycle. Document what went well and what didn\'t to improve your process.',
        side: 'right',
        align: 'start',
      },
      route: '/retros',
    },
    {
      element: '[data-tour="reports-menu"]',
      popover: {
        title: '📊 Reports',
        description: 'View detailed analytics about your team\'s performance, cycle velocity, and issue trends.',
        side: 'right',
        align: 'start',
      },
      route: '/reports',
    },
    {
      element: '[data-tour="meetings-menu"]',
      popover: {
        title: '📅 Meetings',
        description: 'Manage your cycle meetings, including kick-offs and betting tables, directly within the context of your work.',
        side: 'right',
        align: 'start',
      },
      route: '/meetings',
    },
    {
      element: '[data-tour="backlog-menu"]',
      popover: {
        title: '📝 Backlog',
        description: 'A place for raw ideas and tasks that aren\'t yet shaped into pitches. Keep your cycle focused by moving noise here.',
        side: 'right',
        align: 'start',
      },
      route: '/backlog',
    },
    {
      element: '[data-tour="worklogs-menu"]',
      popover: {
        title: '⏱️ Work Logs',
        description: 'Track time and effort spent on pitches. Essential for verifying if you\'re staying within appetite.',
        side: 'right',
        align: 'start',
      },
      route: '/time/logs',
    },
    {
      element: '[data-tour="import-nav"]',
      popover: {
        title: t('tour.importNav.title'),
        description: t('tour.importNav.description'),
        side: 'right',
        align: 'start',
      },
      route: '/import',
    },
    {
      element: '[data-tour="backlog-board"]',
      popover: {
        title: t('tour.backlogBoard.title'),
        description: t('tour.backlogBoard.description'),
        side: 'top',
        align: 'center',
      },
      route: '/backlog',
    },
    {
      element: '[data-tour="reports-overview"]',
      popover: {
        title: t('tour.reportsOverview.title'),
        description: t('tour.reportsOverview.description'),
        side: 'top',
        align: 'center',
      },
      route: '/reports',
    },
    {
      element: '[data-tour="sprint-planning-board"]',
      popover: {
        title: t('tour.sprintPlanning.title'),
        description: t('tour.sprintPlanning.description'),
        side: 'top',
        align: 'center',
      },
      route: '/sprint-planning',
    },
    {
      element: '[data-tour="knowledge-add-source"]',
      popover: {
        title: t('tour.knowledgeCenter.title'),
        description: t('tour.knowledgeCenter.description'),
        side: 'bottom',
      },
      route: '/knowledge',
    },
    {
      element: '[data-tour="wiki-link"]',
      popover: {
        title: t('tour.wikiLink.title'),
        description: t('tour.wikiLink.description'),
        side: 'right',
        align: 'start',
      },
      route: '/wiki',
    },
    {
      element: '[data-tour="project-selector"]',
      popover: {
        title: '🎯 Project Selector',
        description: 'Use this dropdown to switch between projects or view all projects at once. Your selection filters the entire app.',
        side: 'bottom',
        align: 'start',
      },
      route: '/health',
    },
    {
      element: '[data-tour="user-menu"]',
      popover: {
        title: '🎊 You\'re All Set!',
        description: 'That\'s the basics! Explore the app, create your first project, and start tracking your Shape Up cycles. Good luck!',
        side: 'bottom',
        align: 'end',
      },
      route: '/health',
    },
  ];

    // The Sprint Planning step navigates to /sprint-planning, which immediately redirects
    // non-SCRUM projects to /backlog, breaking the tour. Only include the step when the
    // active project is SCRUM.
    return allSteps.filter(
      (step) => step.route !== '/sprint-planning' || isScrumProject,
    );
  }, [isScrumProject]);

  const stopTour = useCallback(() => {
    if (navTimeoutRef.current) { clearTimeout(navTimeoutRef.current); navTimeoutRef.current = null; }
    if (driverInstance) {
      driverInstance.destroy();
      setDriverInstance(null);
    }
    document.body.classList.remove('tour-active');
    setIsTourActive(false);
  }, [driverInstance]);

  const startTour = useCallback(() => {
    // Stop any existing tour
    if (driverInstance) {
      driverInstance.destroy();
    }

    const steps = getTourSteps();
    let currentStepIndex = 0;

    const newDriver = driver({
      showProgress: true,
      steps: steps.map((step) => ({
        element: step.element,
        popover: step.popover,
      })),
      animate: true,
      overlayColor: 'rgba(0, 0, 0, 0.7)',
      stagePadding: 8,
      stageRadius: 8,
      allowClose: true,
      doneBtnText: 'Finish Tour',
      nextBtnText: 'Next →',
      prevBtnText: '← Back',
      progressText: '{{current}} of {{total}}',
      popoverClass: 'shipflow-tour-popover',
      onHighlightStarted: () => {
        const tourStep = steps[currentStepIndex];
        if (tourStep?.route && location.pathname !== tourStep.route) {
          isNavigatingRef.current = true;
          expectedRouteRef.current = tourStep.route;
          navigate(tourStep.route);
        }
      },
      onNextClick: () => {
        currentStepIndex++;
        const nextStep = steps[currentStepIndex];

        if (nextStep?.route && location.pathname !== nextStep.route) {
          isNavigatingRef.current = true;
          expectedRouteRef.current = nextStep.route;
          navigate(nextStep.route);
          if (navTimeoutRef.current) clearTimeout(navTimeoutRef.current);
          navTimeoutRef.current = setTimeout(() => {
            navTimeoutRef.current = null;
            isNavigatingRef.current = false;
            newDriver.moveNext();
          }, 600);
        } else {
          newDriver.moveNext();
        }
      },
      onPrevClick: () => {
        currentStepIndex--;
        const prevStep = steps[currentStepIndex];

        if (prevStep?.route && location.pathname !== prevStep.route) {
          isNavigatingRef.current = true;
          expectedRouteRef.current = prevStep.route;
          navigate(prevStep.route);
          if (navTimeoutRef.current) clearTimeout(navTimeoutRef.current);
          navTimeoutRef.current = setTimeout(() => {
            navTimeoutRef.current = null;
            isNavigatingRef.current = false;
            newDriver.movePrevious();
          }, 600);
        } else {
          newDriver.movePrevious();
        }
      },
      onDestroyStarted: () => {
        if (newDriver.hasNextStep()) {
          // User clicked close/skip - show confirmation dialog
          if (navTimeoutRef.current) { clearTimeout(navTimeoutRef.current); navTimeoutRef.current = null; }
          pendingDestroyRef.current = newDriver;
          setSkipConfirmOpen(true);
          return;
        }
        // Tour completed naturally
        if (navTimeoutRef.current) { clearTimeout(navTimeoutRef.current); navTimeoutRef.current = null; }
        localStorage.setItem(TOUR_COMPLETED_KEY, 'true');
        setHasCompletedTour(true);
        newDriver.destroy();
        document.body.classList.remove('tour-active');
        setIsTourActive(false);
      },
    });

    setDriverInstance(newDriver);
    setIsTourActive(true);
    document.body.classList.add('tour-active');

    // Navigate to first route if needed
    const firstStep = steps[0];
    if (firstStep?.route && location.pathname !== firstStep.route) {
      navigate(firstStep.route);
      setTimeout(() => newDriver.drive(), 500);
    } else {
      newDriver.drive();
    }
  }, [driverInstance, getTourSteps, navigate, location.pathname]);

  const handleSkipConfirm = useCallback(() => {
    if (pendingDestroyRef.current) {
      localStorage.setItem(TOUR_COMPLETED_KEY, 'true');
      setHasCompletedTour(true);
      pendingDestroyRef.current.destroy();
      pendingDestroyRef.current = null;
      document.body.classList.remove('tour-active');
      setIsTourActive(false);
    }
    setSkipConfirmOpen(false);
  }, []);

  const handleSkipCancel = useCallback(() => {
    pendingDestroyRef.current = null;
    setSkipConfirmOpen(false);
  }, []);

  const resetTour = useCallback(() => {
    localStorage.removeItem(TOUR_COMPLETED_KEY);
    setHasCompletedTour(false);
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (navTimeoutRef.current) { clearTimeout(navTimeoutRef.current); navTimeoutRef.current = null; }
      if (driverInstance) {
        driverInstance.destroy();
      }
      document.body.classList.remove('tour-active');
      pendingDestroyRef.current = null;
    };
  }, [driverInstance]);

  // Stop tour only when the user manually navigates away (not tour-driven navigation)
  useEffect(() => {
    // If the tour triggered this navigation, update the flag and do nothing else
    if (isNavigatingRef.current) {
      if (location.pathname === expectedRouteRef.current) {
        isNavigatingRef.current = false;
        expectedRouteRef.current = null;
      }
      return;
    }

    // Tour is not active — nothing to do
    if (!isTourActive || !driverInstance) return;

    // At this point: tour IS active and navigation was NOT tour-driven.
    // Only destroy if user navigated to a route that is completely outside the tour.
    const steps = getTourSteps();
    const isValidTourRoute = steps.some(step => step.route === location.pathname);

    if (!isValidTourRoute) {
      // Genuinely manual navigation to an unknown route — stop the tour
      if (navTimeoutRef.current) { clearTimeout(navTimeoutRef.current); navTimeoutRef.current = null; }
      driverInstance.destroy();
      setDriverInstance(null);
      document.body.classList.remove('tour-active');
      setIsTourActive(false);
    }
    // If the route IS a valid tour route but we didn't trigger it, we leave the
    // tour running — the user may have just navigated there manually and it's
    // fine for the popover to remain visible.
  }, [location.pathname, isTourActive, driverInstance, getTourSteps]);

  return (
    <TourContext.Provider
      value={{
        startTour,
        stopTour,
        isTourActive,
        hasCompletedTour,
        resetTour,
      }}
    >
      {children}

      {/* Skip Tour Confirmation Dialog */}
      <ConfirmDialog
        open={skipConfirmOpen}
        onOpenChange={handleSkipCancel}
        title={t('tour.skipTitle')}
        description={t('tour.skipDescription')}
        confirmLabel={t('tour.skip')}
        cancelLabel={t('tour.continue')}
        onConfirm={handleSkipConfirm}
      />
    </TourContext.Provider>
  );
}

export function useTour() {
  const context = useContext(TourContext);
  if (context === undefined) {
    throw new Error('useTour must be used within a TourProvider');
  }
  return context;
}
