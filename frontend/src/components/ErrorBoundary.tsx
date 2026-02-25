import React, { Component, ErrorInfo, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { AlertTriangle, RefreshCw, Home, Bug } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from '@/components/ui/card';

interface ErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
  showDetails?: boolean;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
  errorInfo: ErrorInfo | null;
}

// Class component for error boundary (React requirement)
class ErrorBoundaryClass extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null, errorInfo: null };
  }

  static getDerivedStateFromError(error: Error): Partial<ErrorBoundaryState> {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ errorInfo });
    
    // Log error to console in development
    if (process.env.NODE_ENV === 'development') {
      console.error('Error Boundary caught an error:', error, errorInfo);
    }
    
    // Call optional error handler
    this.props.onError?.(error, errorInfo);
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, errorInfo: null });
  };

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      
      return (
        <ErrorFallback
          error={this.state.error}
          errorInfo={this.state.errorInfo}
          onReset={this.handleReset}
          showDetails={this.props.showDetails}
        />
      );
    }

    return this.props.children;
  }
}

// Functional fallback component with i18n support
interface ErrorFallbackProps {
  error: Error | null;
  errorInfo: ErrorInfo | null;
  onReset: () => void;
  showDetails?: boolean;
}

function ErrorFallback({ error, errorInfo, onReset, showDetails = false }: ErrorFallbackProps) {
  const { t } = useTranslation();
  const [showFullDetails, setShowFullDetails] = React.useState(false);
  
  const handleGoHome = () => {
    window.location.href = '/dashboard';
  };

  const handleRefresh = () => {
    window.location.reload();
  };

  const handleReportBug = () => {
    // Could integrate with a bug reporting service
    const subject = encodeURIComponent(`Bug Report: ${error?.message || 'Unknown error'}`);
    const body = encodeURIComponent(`
Error: ${error?.message}

Stack Trace:
${error?.stack}

Component Stack:
${errorInfo?.componentStack}

URL: ${window.location.href}
Time: ${new Date().toISOString()}
User Agent: ${navigator.userAgent}
    `);
    window.open(`mailto:support@shipflow.dev?subject=${subject}&body=${body}`);
  };

  return (
    <div className="h-screen overflow-y-auto flex items-center justify-center p-4 bg-background">
      <Card className="w-full max-w-lg">
        <CardHeader className="text-center">
          <div className="mx-auto mb-4 w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center">
            <AlertTriangle className="w-6 h-6 text-destructive" />
          </div>
          <CardTitle className="text-xl">
            {t('errors.genericError', 'Something went wrong')}
          </CardTitle>
          <CardDescription>
            {t('errors.errorDescription', 'An unexpected error occurred. Please try again or contact support if the problem persists.')}
          </CardDescription>
        </CardHeader>
        
        <CardContent>
          {(showDetails || process.env.NODE_ENV === 'development') && error && (
            <div className="space-y-4">
              <div className="p-3 bg-muted rounded-md">
                <p className="text-sm font-mono text-destructive break-all">
                  {error.message}
                </p>
              </div>
              
              {showFullDetails && errorInfo && (
                <div className="p-3 bg-muted rounded-md max-h-48 overflow-auto">
                  <pre className="text-xs font-mono text-muted-foreground whitespace-pre-wrap">
                    {error.stack}
                  </pre>
                </div>
              )}
              
              <Button
                variant="ghost"
                size="sm"
                onClick={() => setShowFullDetails(!showFullDetails)}
                className="w-full"
              >
                {showFullDetails ? t('common.hideDetails', 'Hide Details') : t('common.showDetails', 'Show Details')}
              </Button>
            </div>
          )}
        </CardContent>
        
        <CardFooter className="flex flex-col gap-2">
          <div className="flex gap-2 w-full">
            <Button onClick={onReset} variant="default" className="flex-1">
              <RefreshCw className="w-4 h-4 mr-2" />
              {t('common.tryAgain', 'Try Again')}
            </Button>
            <Button onClick={handleGoHome} variant="outline" className="flex-1">
              <Home className="w-4 h-4 mr-2" />
              {t('common.goHome', 'Go Home')}
            </Button>
          </div>
          <div className="flex gap-2 w-full">
            <Button onClick={handleRefresh} variant="ghost" className="flex-1">
              {t('common.refreshPage', 'Refresh Page')}
            </Button>
            <Button onClick={handleReportBug} variant="ghost" className="flex-1">
              <Bug className="w-4 h-4 mr-2" />
              {t('common.reportBug', 'Report Bug')}
            </Button>
          </div>
        </CardFooter>
      </Card>
    </div>
  );
}

// Page-level error boundary with full-page fallback
export function PageErrorBoundary({ children }: { children: ReactNode }) {
  return (
    <ErrorBoundaryClass showDetails={process.env.NODE_ENV === 'development'}>
      {children}
    </ErrorBoundaryClass>
  );
}

// Component-level error boundary with inline fallback
interface ComponentErrorBoundaryProps {
  children: ReactNode;
  fallback?: ReactNode;
  componentName?: string;
}

export function ComponentErrorBoundary({ 
  children, 
  fallback,
  componentName = 'Component'
}: ComponentErrorBoundaryProps) {
  const { t } = useTranslation();
  
  const defaultFallback = (
    <div className="p-4 border border-destructive/20 rounded-md bg-destructive/5">
      <div className="flex items-center gap-2 text-destructive">
        <AlertTriangle className="w-4 h-4" />
        <span className="text-sm font-medium">
          {t('errors.componentError', '{{component}} failed to load', { component: componentName })}
        </span>
      </div>
    </div>
  );

  return (
    <ErrorBoundaryClass fallback={fallback || defaultFallback}>
      {children}
    </ErrorBoundaryClass>
  );
}

// Export the main ErrorBoundary
export const ErrorBoundary = ErrorBoundaryClass;
export default ErrorBoundary;
