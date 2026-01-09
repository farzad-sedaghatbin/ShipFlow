import React, { createContext, useContext, useState, useCallback, useEffect, useRef } from 'react';
import { driver, DriveStep, Driver } from 'driver.js';
import 'driver.js/dist/driver.css';
import { useNavigate, useLocation } from 'react-router-dom';

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
  
  // Track if navigation was triggered by the tour driver
  const isNavigatingRef = useRef(false);
  const expectedRouteRef = useRef<string | null>(null);

  // Tour steps definition
  const getTourSteps = useCallback((): TourStep[] => [
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
        description: 'ShapeUp uses 6-week cycles. Each cycle has a build phase (for development) and a cooldown phase (for bug fixes and exploration).',
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
        description: 'That\'s the basics! Explore the app, create your first project, and start tracking your ShapeUp cycles. Good luck!',
        side: 'bottom',
        align: 'end',
      },
      route: '/health',
    },
  ], []);

  const stopTour = useCallback(() => {
    if (driverInstance) {
      driverInstance.destroy();
      setDriverInstance(null);
    }
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
          setTimeout(() => {
            isNavigatingRef.current = false;
            newDriver.moveNext();
          }, 400);
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
          setTimeout(() => {
            isNavigatingRef.current = false;
            newDriver.movePrevious();
          }, 400);
        } else {
          newDriver.movePrevious();
        }
      },
      onDestroyStarted: () => {
        if (newDriver.hasNextStep()) {
          // User clicked close/skip
          const confirmed = window.confirm('Are you sure you want to skip the tour? You can restart it anytime from the help button.');
          if (!confirmed) {
            return;
          }
        }
        // Tour completed or confirmed skip
        localStorage.setItem(TOUR_COMPLETED_KEY, 'true');
        setHasCompletedTour(true);
        newDriver.destroy();
        setIsTourActive(false);
      },
    });

    setDriverInstance(newDriver);
    setIsTourActive(true);

    // Navigate to first route if needed
    const firstStep = steps[0];
    if (firstStep?.route && location.pathname !== firstStep.route) {
      navigate(firstStep.route);
      setTimeout(() => newDriver.drive(), 500);
    } else {
      newDriver.drive();
    }
  }, [driverInstance, getTourSteps, navigate, location.pathname]);

  const resetTour = useCallback(() => {
    localStorage.removeItem(TOUR_COMPLETED_KEY);
    setHasCompletedTour(false);
  }, []);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (driverInstance) {
        driverInstance.destroy();
      }
    };
  }, [driverInstance]);

  // Stop tour if user navigates away manually (not via tour navigation)
  useEffect(() => {
    // Skip if navigation was triggered by the tour driver
    if (isNavigatingRef.current) {
      // Reset the flag after navigation completes
      if (location.pathname === expectedRouteRef.current) {
        isNavigatingRef.current = false;
        expectedRouteRef.current = null;
      }
      return;
    }
    
    // Only clean up if tour is active and user navigated manually
    if (isTourActive && driverInstance) {
      const steps = getTourSteps();
      const currentRoute = location.pathname;
      
      // Check if this route matches the expected tour flow
      // If user manually navigated (not via tour), destroy the tour
      const isValidTourRoute = steps.some(step => step.route === currentRoute);
      
      if (!isValidTourRoute) {
        // User navigated to a route not in the tour - definitely manual
        driverInstance.destroy();
        setDriverInstance(null);
        setIsTourActive(false);
      } else {
        // User navigated to a valid tour route, but manually
        // This is the key fix: if we didn't trigger the navigation, destroy the tour
        driverInstance.destroy();
        setDriverInstance(null);
        setIsTourActive(false);
      }
    }
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
