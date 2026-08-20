import { Navigate, useLocation } from 'react-router-dom';
import { Loader2 } from 'lucide-react';
import { useAuth } from '../contexts';
import { ReactNode } from 'react';
import { buildLoginPath, toRelativePath } from '../lib/redirect';

interface ProtectedRouteProps {
  children: ReactNode;
  requiredRoles?: string[];
}

export default function ProtectedRoute({ children, requiredRoles }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <Loader2 className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!isAuthenticated) {
    // Redirect to login while saving the attempted location. It travels twice:
    // in router state (survives the client-side navigation) and in `?redirect=`
    // (survives a reload of the login page or a copy/pasted URL).
    return (
      <Navigate
        to={buildLoginPath(toRelativePath(location))}
        state={{ from: location }}
        replace
      />
    );
  }

  // Check role-based access if requiredRoles is specified
  if (requiredRoles && requiredRoles.length > 0 && user) {
    const hasRequiredRole = requiredRoles.includes(user.role);
    if (!hasRequiredRole) {
      // User doesn't have required role - redirect to dashboard
      return <Navigate to="/dashboard" replace />;
    }
  }

  return <>{children}</>;
}
