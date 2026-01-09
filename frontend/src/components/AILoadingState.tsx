import React from 'react';
import { Sparkles } from 'lucide-react';
import { cn } from '../lib/utils';

interface AILoadingStateProps {
  title?: string;
  subtitle?: string;
  steps?: string[];
}

export const AILoadingState: React.FC<AILoadingStateProps> = ({
  title = 'AI is analyzing your data',
  subtitle = 'This may take a moment...',
  steps = [
    'Gathering cycle information...',
    'Analyzing pitch health metrics...',
    'Evaluating risk factors...',
    'Generating insights...',
  ],
}) => {
  const [currentStep, setCurrentStep] = React.useState(0);

  React.useEffect(() => {
    const interval = setInterval(() => {
      setCurrentStep((prev) => (prev + 1) % steps.length);
    }, 2500);
    return () => clearInterval(interval);
  }, [steps.length]);

  return (
    <div
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label={`${title}. ${steps[currentStep]}`}
      className="flex flex-col items-center justify-center min-h-[60vh] p-8"
    >
      {/* Main AI Icon with Animation */}
      <div
        aria-hidden="true"
        className="relative mb-8 animate-float"
      >
        {/* Outer rotating ring */}
        <div
          className="absolute -top-5 -left-5 w-[140px] h-[140px] rounded-full border-[3px] border-dashed border-primary/30 animate-spin-slow"
        />
        
        {/* Middle pulsing ring */}
        <div
          className="absolute -top-2.5 -left-2.5 w-[120px] h-[120px] rounded-full bg-gradient-to-br from-primary/10 to-violet-500/10 animate-pulse"
        />

        {/* Center icon container */}
        <div
          className="w-[100px] h-[100px] rounded-full flex items-center justify-center bg-gradient-to-br from-primary to-violet-500 shadow-lg shadow-primary/40"
        >
          <Sparkles className="h-12 w-12 text-white" aria-hidden="true" />
        </div>

        {/* Floating particles */}
        {[...Array(6)].map((_, i) => (
          <div
            key={i}
            className={cn(
              "absolute w-2 h-2 rounded-full animate-pulse",
              i % 2 === 0 ? "bg-primary" : "bg-violet-500"
            )}
            style={{
              top: `${20 + Math.sin(i * 60 * Math.PI / 180) * 60}px`,
              left: `${50 + Math.cos(i * 60 * Math.PI / 180) * 60}px`,
              animationDelay: `${i * 0.2}s`,
            }}
          />
        ))}
      </div>

      {/* Title */}
      <h2
        className="mb-2 text-2xl font-bold bg-gradient-to-br from-primary to-violet-500 bg-clip-text text-transparent"
      >
        {title}
      </h2>

      {/* Subtitle */}
      <p className="mb-8 text-muted-foreground" aria-hidden="true">
        {subtitle}
      </p>

      {/* Reasoning Steps */}
      <div
        className="p-6 rounded-lg bg-primary/5 border border-primary/10 min-w-[320px] max-w-[400px]"
      >
        <div className="flex items-center gap-2 mb-4">
          <div
            className="w-2 h-2 rounded-full bg-green-500 animate-pulse"
          />
          <span className="text-sm text-muted-foreground">
            Currently processing
          </span>
        </div>

        {/* Current Step */}
        <div
          className="text-primary font-medium flex items-center gap-2"
        >
          {steps[currentStep]}
          <span className="flex gap-1 ml-2">
            {[0, 1, 2].map((dot) => (
              <span
                key={dot}
                className="w-1 h-1 rounded-full bg-primary animate-typing"
                style={{ animationDelay: `${dot * 0.2}s` }}
              />
            ))}
          </span>
        </div>

        {/* Progress indicator */}
        <div className="mt-6">
          <div
            className="h-1 rounded-full bg-primary/10 overflow-hidden"
          >
            <div
              className="h-full w-full animate-shimmer bg-gradient-to-r from-primary/10 via-primary to-primary/10"
              style={{ backgroundSize: '200% 100%' }}
            />
          </div>
        </div>
      </div>

      {/* Tip */}
      <p
        className="mt-6 text-sm text-muted-foreground text-center max-w-[400px]"
      >
        💡 AI-powered analysis uses machine learning to evaluate pitch health, identify risks, and provide actionable insights.
      </p>
    </div>
  );
};

/**
 * Compact AI Loading for use in cards and smaller sections
 */
interface AILoadingCompactProps {
  message?: string;
  size?: 'small' | 'medium';
}

export const AILoadingCompact: React.FC<AILoadingCompactProps> = ({
  message = 'AI is analyzing...',
  size = 'medium',
}) => {
  const iconSize = size === 'small' ? 'h-6 w-6' : 'h-8 w-8';
  const containerSize = size === 'small' ? 'w-10 h-10' : 'w-14 h-14';
  const ringSize = size === 'small' ? 'w-[52px] h-[52px] -top-1.5 -left-1.5' : 'w-[68px] h-[68px] -top-1.5 -left-1.5';

  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center px-4",
        size === 'small' ? 'py-4' : 'py-8'
      )}
    >
      {/* Animated AI Icon */}
      <div className="relative mb-4">
        {/* Rotating ring */}
        <div
          className={cn(
            "absolute rounded-full border-2 border-dashed border-primary/40 animate-spin-slow",
            ringSize
          )}
        />
        
        {/* Pulsing background */}
        <div
          className={cn(
            "rounded-full flex items-center justify-center bg-gradient-to-br from-primary/15 to-violet-500/15 animate-pulse",
            containerSize
          )}
        >
          <Sparkles 
            className={cn(iconSize, "text-primary")} 
          />
        </div>
      </div>

      {/* Message with animated dots */}
      <div className="flex items-center gap-1">
        <span 
          className={cn(
            "text-muted-foreground font-medium",
            size === 'small' ? 'text-xs' : 'text-sm'
          )}
        >
          {message}
        </span>
        <span className="flex gap-0.5 ml-1">
          {[0, 1, 2].map((dot) => (
            <span
              key={dot}
              className={cn(
                "rounded-full bg-primary animate-typing",
                size === 'small' ? 'w-0.5 h-0.5' : 'w-1 h-1'
              )}
              style={{ animationDelay: `${dot * 0.2}s` }}
            />
          ))}
        </span>
      </div>

      {/* Shimmer bar */}
      <div className="w-4/5 max-w-[150px] mt-3">
        <div
          className="h-0.5 rounded-full bg-primary/10 overflow-hidden"
        >
          <div
            className="h-full w-full animate-shimmer bg-gradient-to-r from-primary/10 via-primary to-primary/10"
            style={{ backgroundSize: '200% 100%' }}
          />
        </div>
      </div>
    </div>
  );
};

export default AILoadingState;
