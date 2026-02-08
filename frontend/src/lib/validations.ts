import { z } from 'zod';

/**
 * Common validation schemas for forms
 * These can be imported and used with react-hook-form + zodResolver
 */

// Login form schema
export const loginSchema = z.object({
  username: z
    .string()
    .min(1, 'Username is required')
    .min(3, 'Username must be at least 3 characters'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(6, 'Password must be at least 6 characters'),
});

export type LoginFormData = z.infer<typeof loginSchema>;

// Registration form schema
export const registerSchema = z.object({
  username: z
    .string()
    .min(1, 'Username is required')
    .min(3, 'Username must be at least 3 characters')
    .max(50, 'Username must be less than 50 characters')
    .regex(/^[a-zA-Z0-9_-]+$/, 'Username can only contain letters, numbers, underscores, and hyphens'),
  email: z
    .string()
    .min(1, 'Email is required')
    .email('Please enter a valid email address'),
  password: z
    .string()
    .min(1, 'Password is required')
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain at least one uppercase letter')
    .regex(/[a-z]/, 'Password must contain at least one lowercase letter')
    .regex(/[0-9]/, 'Password must contain at least one number'),
  confirmPassword: z.string().min(1, 'Please confirm your password'),
}).refine((data) => data.password === data.confirmPassword, {
  message: 'Passwords do not match',
  path: ['confirmPassword'],
});

export type RegisterFormData = z.infer<typeof registerSchema>;

// Pitch form schema
export const pitchSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .min(3, 'Title must be at least 3 characters')
    .max(200, 'Title must be less than 200 characters'),
  description: z
    .string()
    .min(1, 'Description is required')
    .min(10, 'Description must be at least 10 characters'),
  problem: z
    .string()
    .min(1, 'Problem statement is required')
    .min(10, 'Problem statement must be at least 10 characters'),
  solution: z
    .string()
    .min(1, 'Solution is required')
    .min(10, 'Solution must be at least 10 characters'),
  appetite: z
    .enum(['SMALL_BATCH', 'BIG_BATCH'], {
      message: 'Please select an appetite',
    }),
  estimatedWeeks: z
    .number()
    .min(1, 'Estimated weeks must be at least 1')
    .max(12, 'Estimated weeks cannot exceed 12'),
  status: z.enum(['DRAFT', 'SUBMITTED', 'APPROVED', 'REJECTED', 'IN_PROGRESS', 'COMPLETED']).optional(),
});

export type PitchFormData = z.infer<typeof pitchSchema>;

// Cycle form schema
export const cycleSchema = z.object({
  name: z
    .string()
    .min(1, 'Cycle name is required')
    .min(3, 'Cycle name must be at least 3 characters')
    .max(100, 'Cycle name must be less than 100 characters'),
  description: z.string().optional(),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
  cooldownDays: z
    .number()
    .min(0, 'Cooldown days cannot be negative')
    .max(14, 'Cooldown days cannot exceed 14'),
}).refine((data) => {
  if (data.startDate && data.endDate) {
    return new Date(data.endDate) > new Date(data.startDate);
  }
  return true;
}, {
  message: 'End date must be after start date',
  path: ['endDate'],
});

export type CycleFormData = z.infer<typeof cycleSchema>;

// Bet form schema
export const betSchema = z.object({
  pitchId: z.number().min(1, 'Please select a pitch'),
  cycleId: z.number().min(1, 'Please select a cycle'),
  confidence: z
    .number()
    .min(1, 'Confidence must be at least 1')
    .max(100, 'Confidence cannot exceed 100'),
  notes: z.string().optional(),
});

export type BetFormData = z.infer<typeof betSchema>;

// Scope/Task form schema
export const scopeSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .min(3, 'Title must be at least 3 characters')
    .max(200, 'Title must be less than 200 characters'),
  description: z.string().optional(),
  status: z.enum(['NOT_STARTED', 'IN_PROGRESS', 'DONE', 'BLOCKED']).optional(),
  progress: z
    .number()
    .min(0, 'Progress cannot be negative')
    .max(100, 'Progress cannot exceed 100')
    .optional(),
  assigneeId: z.number().optional().nullable(),
});

export type ScopeFormData = z.infer<typeof scopeSchema>;

// Bug report form schema
export const bugReportSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .min(5, 'Title must be at least 5 characters')
    .max(200, 'Title must be less than 200 characters'),
  description: z
    .string()
    .min(1, 'Description is required')
    .min(10, 'Please provide more detail about the bug'),
  stepsToReproduce: z.string().optional(),
  expectedBehavior: z.string().optional(),
  actualBehavior: z.string().optional(),
  severity: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'], {
    message: 'Please select a severity level',
  }),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'URGENT']).optional(),
});

export type BugReportFormData = z.infer<typeof bugReportSchema>;

// Test case form schema
export const testCaseSchema = z.object({
  title: z
    .string()
    .min(1, 'Title is required')
    .min(3, 'Title must be at least 3 characters')
    .max(200, 'Title must be less than 200 characters'),
  description: z.string().optional(),
  preconditions: z.string().optional(),
  steps: z
    .string()
    .min(1, 'Test steps are required')
    .min(10, 'Please provide detailed test steps'),
  expectedResult: z
    .string()
    .min(1, 'Expected result is required'),
  priority: z.enum(['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']).optional(),
});

export type TestCaseFormData = z.infer<typeof testCaseSchema>;

// Project form schema
export const projectSchema = z.object({
  name: z
    .string()
    .min(1, 'Project name is required')
    .min(3, 'Project name must be at least 3 characters')
    .max(100, 'Project name must be less than 100 characters'),
  description: z.string().optional(),
  projectType: z.enum(['GREENFIELD', 'BROWNFIELD', 'EXPERIMENTAL', 'MAINTENANCE', 'MIGRATION'], {
    message: 'Please select a project type',
  }),
});

export type ProjectFormData = z.infer<typeof projectSchema>;
