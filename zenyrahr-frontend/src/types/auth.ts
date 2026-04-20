/** Platform (multi-tenant) operator; not an organization catalog role. */
export const MAIN_PLATFORM_ADMIN_ROLE = 'zenyrahr_admin' as const;

export type SystemRole =
  | typeof MAIN_PLATFORM_ADMIN_ROLE
  | 'org_admin'
  | 'hr'
  | 'manager'
  | 'employee';
export type Role = SystemRole | (string & {});
export type CapabilityPack =
  | "recruitment"
  | "finance"
  | "compliance"
  | "learning"
  | "engagement";

export function isMainPlatformAdmin(role: string | undefined | null): boolean {
  return (role || '').toLowerCase() === MAIN_PLATFORM_ADMIN_ROLE;
}
 
// User interface
export interface User {
  id: string;
  email: string;
  firstName: string;
  fullName: string;
  role: Role;
  department?: string;
  avatar?: string;
  organizationId?: number | null;
  capabilityPacks?: CapabilityPack[];
}
 
// Keep these open to match existing route/menu checks across the app.
export type Action = string;
 
export type Subject = string;
 
// Permission interface with strict typing
export interface Permission {
  action: Action;
  subject: Subject;
}

export const capabilityPackPermissions: Record<CapabilityPack, Permission[]> = {
  recruitment: [
    { action: "read", subject: "recruitment" },
    { action: "manage", subject: "recruitment" },
    { action: "approve", subject: "recruitment" },
  ],
  finance: [
    { action: "read", subject: "finance" },
    { action: "manage", subject: "finance" },
    { action: "approve", subject: "reimbursements" },
    { action: "read", subject: "tax" },
    { action: "manage", subject: "tax" },
  ],
  compliance: [
    { action: "read", subject: "compliance" },
    { action: "manage", subject: "compliance" },
    { action: "approve", subject: "compliance" },
    { action: "read", subject: "audit" },
    { action: "manage", subject: "audit" },
  ],
  learning: [
    { action: "read", subject: "learning" },
    { action: "manage", subject: "learning" },
    { action: "assign", subject: "learning" },
    { action: "read", subject: "certification" },
    { action: "manage", subject: "certification" },
  ],
  engagement: [
    { action: "read", subject: "engagement" },
    { action: "manage", subject: "engagement" },
    { action: "read", subject: "survey" },
    { action: "manage", subject: "survey" },
    { action: "read", subject: "recognition" },
    { action: "manage", subject: "recognition" },
  ],
};
 
// Helper type for permission inheritance
type InheritedPermissions = {
  [key in SystemRole]: SystemRole[];
};
 
// Define role inheritance
export const roleInheritance: InheritedPermissions = {
  zenyrahr_admin: [], // platform admin — full access via manage/all
  org_admin: ['hr'],
  hr: ['employee'], // HR inherits employee permissions
  manager: ['employee'], // Manager inherits employee permissions
  employee: [], // Employee is base role
};
 
// Define base permissions for each role
export const rolePermissions: Record<SystemRole, Permission[]> = {
  zenyrahr_admin: [
    { action: 'manage', subject: 'all' }
  ],
  org_admin: [
    { action: 'manage', subject: 'employees' },
    { action: 'read', subject: 'employees' },
    { action: 'manage', subject: 'projects' },
    { action: 'manage', subject: 'leave-balance' },
    { action: 'manage', subject: 'leave' },
    { action: 'approve', subject: 'leave' },
    { action: 'approve', subject: 'timesheet' },
    { action: 'approve', subject: 'expenses' },
    { action: 'manage', subject: 'settings' }
  ],
  hr: [
    { action: 'read', subject: 'employees' },
    { action: 'manage', subject: 'employees' },
    { action: 'manage', subject: 'projects' },
    { action: 'manage', subject: 'leave-balance' },
    { action: 'manage', subject: 'leave' },
    { action: 'manage', subject: 'performance' },
    { action: 'approve', subject: 'leave' },
    { action: 'approve', subject: 'timesheet' },
    { action: 'approve', subject: 'expenses' }
  ],
  manager: [
    { action: 'read', subject: 'employees' },
    { action: 'read', subject: 'performance' },
    { action: 'read', subject: 'analytics' },
    { action: 'approve', subject: 'timesheet' },
    { action: 'approve', subject: 'leave' },
    { action: 'approve', subject: 'expenses' }
  ],
  employee: [
    { action: 'read', subject: 'self' },
    { action: 'submit', subject: 'leave' },
    { action: 'submit', subject: 'timesheet' },
    { action: 'read', subject: 'learning' },
    { action: 'submit', subject: 'expenses' },
    { action: 'manage', subject: 'performance' },
  ]
};
 
// Helper function to get all permissions for a role including inherited ones
/**
 * Maps JWT/database role to a permission template.
 * Legacy platform accounts used role {@code admin} with no organization; org-scoped {@code admin} is a catalog role only.
 */
export function resolvePermissionRole(role: string, organizationId?: number | null): SystemRole {
  const normalized = (role || '').toLowerCase();
  const noOrganization = organizationId === null || organizationId === undefined;
  if (normalized === "admin" && noOrganization) {
    return MAIN_PLATFORM_ADMIN_ROLE;
  }
  if (Object.prototype.hasOwnProperty.call(rolePermissions, normalized)) {
    return normalized as SystemRole;
  }
  // Custom roles default to employee-level permissions unless extended later.
  return "employee";
}

export function normalizeCapabilityPacks(
  capabilityPacks: readonly string[] | null | undefined
): CapabilityPack[] {
  if (!capabilityPacks || capabilityPacks.length === 0) {
    return [];
  }
  const allowed = new Set<CapabilityPack>([
    "recruitment",
    "finance",
    "compliance",
    "learning",
    "engagement",
  ]);
  return capabilityPacks
    .map((pack) => String(pack || "").toLowerCase())
    .filter((pack): pack is CapabilityPack => allowed.has(pack as CapabilityPack));
}

export function getCapabilityPermissions(
  capabilityPacks: readonly string[] | null | undefined
): Permission[] {
  const normalized = normalizeCapabilityPacks(capabilityPacks);
  return normalized.flatMap((pack) => capabilityPackPermissions[pack]);
}

export function hasCapabilityPack(
  capabilityPacks: readonly string[] | null | undefined,
  requiredPack: CapabilityPack
): boolean {
  return normalizeCapabilityPacks(capabilityPacks).includes(requiredPack);
}

export function hasAnyCapabilityPack(
  capabilityPacks: readonly string[] | null | undefined,
  requiredPacks: readonly CapabilityPack[]
): boolean {
  const normalized = normalizeCapabilityPacks(capabilityPacks);
  return requiredPacks.some((pack) => normalized.includes(pack));
}

export function getAllPermissions(
  role: Role,
  organizationId?: number | null,
  capabilityPacks?: readonly string[] | null
): Permission[] {
  const resolvedRole = resolvePermissionRole(String(role), organizationId);
  const inheritedRoles = roleInheritance[resolvedRole];
  const inheritedPermissions = inheritedRoles.flatMap(r => rolePermissions[r]);
  const packPermissions = getCapabilityPermissions(capabilityPacks);
  return [...rolePermissions[resolvedRole], ...inheritedPermissions, ...packPermissions];
}
 
// Helper function to check if a permission implies another
export function doesPermissionImply(
  having: Permission,
  needed: Permission
): boolean {
  // 'manage' action implies all other actions
  if (having.action === 'manage') {
    // 'all' subject implies all other subjects
    if (having.subject === 'all') return true;
    // For same subject, 'manage' implies all actions
    return having.subject === needed.subject;
  }
 
  // For exact permission match
  return having.action === needed.action && having.subject === needed.subject;
}
 
// Helper function to check if user has specific permission
export function hasPermission(
  userPermissions: Permission[],
  action: Action,
  subject: Subject
): boolean {
  const needed: Permission = { action, subject };
  return userPermissions.some(having => doesPermissionImply(having, needed));
}