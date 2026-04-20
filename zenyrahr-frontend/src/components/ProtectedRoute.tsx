import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import type { CapabilityPack } from '../types/auth';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredPermissions?: { action: string; subject: string }[];
  requiredCapabilityPacks?: CapabilityPack[];
  capabilityMatch?: 'all' | 'any';
}

export default function ProtectedRoute({
  children,
  requiredPermissions,
  requiredCapabilityPacks,
  capabilityMatch = 'all',
}: ProtectedRouteProps) {
  const { isAuthenticated, hasPermission, hasCapabilityPack, hasAnyCapabilityPack } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  if (requiredPermissions) {
    const hasAllPermissions = requiredPermissions.every(
      ({ action, subject }) => hasPermission(action, subject)
    );

    if (!hasAllPermissions) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  if (requiredCapabilityPacks && requiredCapabilityPacks.length > 0) {
    const hasRequiredPacks =
      capabilityMatch === 'any'
        ? hasAnyCapabilityPack(requiredCapabilityPacks)
        : requiredCapabilityPacks.every((pack) => hasCapabilityPack(pack));
    if (!hasRequiredPacks) {
      return <Navigate to="/unauthorized" replace />;
    }
  }

  return <>{children}</>;
}



