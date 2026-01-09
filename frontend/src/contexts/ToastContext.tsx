import { createContext, useContext, ReactNode, useCallback } from 'react';
import { toast, Toaster } from 'sonner';

type ToastSeverity = 'info' | 'success' | 'warning' | 'error';

interface ToastContextType {
  showToast: (message: string, severity?: ToastSeverity) => void;
  showError: (message: string) => void;
  showSuccess: (message: string) => void;
  showWarning: (message: string) => void;
  showInfo: (message: string) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export function ToastProvider({ children }: { children: ReactNode }) {
  const showToast = useCallback((message: string, severity: ToastSeverity = 'info') => {
    switch (severity) {
      case 'error':
        toast.error(message);
        break;
      case 'success':
        toast.success(message);
        break;
      case 'warning':
        toast.warning(message);
        break;
      case 'info':
      default:
        toast.info(message);
        break;
    }
  }, []);

  const showError = useCallback((message: string) => {
    toast.error(message);
  }, []);

  const showSuccess = useCallback((message: string) => {
    toast.success(message);
  }, []);

  const showWarning = useCallback((message: string) => {
    toast.warning(message);
  }, []);

  const showInfo = useCallback((message: string) => {
    toast.info(message);
  }, []);

  return (
    <ToastContext.Provider value={{ showToast, showError, showSuccess, showWarning, showInfo }}>
      {children}
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 5000,
          classNames: {
            toast: 'bg-card text-foreground border-border shadow-lg',
            title: 'text-foreground font-medium',
            description: 'text-muted-foreground',
            actionButton: 'bg-primary text-primary-foreground',
            cancelButton: 'bg-muted text-muted-foreground',
            error: 'bg-destructive/10 border-destructive/50 text-destructive',
            success: 'bg-success/10 border-success/50 text-success',
            warning: 'bg-warning/10 border-warning/50 text-warning',
            info: 'bg-info/10 border-info/50 text-info',
          },
        }}
      />
    </ToastContext.Provider>
  );
}

export function useToast() {
  const context = useContext(ToastContext);
  if (context === undefined) {
    throw new Error('useToast must be used within a ToastProvider');
  }
  return context;
}

// For use outside React components (like in api interceptors)
let toastHandler: ((message: string, severity: ToastSeverity) => void) | null = null;

export const setToastHandler = (handler: (message: string, severity: ToastSeverity) => void) => {
  toastHandler = handler;
};

export const showGlobalToast = (message: string, severity: ToastSeverity = 'error') => {
  if (toastHandler) {
    toastHandler(message, severity);
  } else {
    console.error('Toast handler not initialized:', message);
  }
};
