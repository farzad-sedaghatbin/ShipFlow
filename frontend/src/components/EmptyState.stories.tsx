import type { Meta, StoryObj } from '@storybook/react';
import { fn } from '@storybook/test';
import { Plus, FolderOpen, FileText, Users, Calendar, Bug } from 'lucide-react';
import EmptyState from './EmptyState';

const meta = {
  title: 'Components/EmptyState',
  component: EmptyState,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A versatile empty state component used to display when there is no data to show, with optional onboarding guidance.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    size: {
      control: 'select',
      options: ['small', 'medium', 'large'],
      description: 'Size variant for the empty state',
    },
    compact: {
      control: 'boolean',
      description: 'Whether to reduce padding',
    },
    animate: {
      control: 'boolean',
      description: 'Whether to enable animations',
    },
  },
} satisfies Meta<typeof EmptyState>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {
    icon: FolderOpen,
    title: 'No projects found',
    description: 'Get started by creating your first project.',
    action: {
      label: 'Create Project',
      onClick: fn(),
      startIcon: <Plus className="h-4 w-4" />,
    },
  },
};

export const WithSecondaryAction: Story = {
  args: {
    icon: FileText,
    title: 'No pitches yet',
    description: 'Create a new pitch or import from your backlog.',
    action: {
      label: 'New Pitch',
      onClick: fn(),
      startIcon: <Plus className="h-4 w-4" />,
    },
    secondaryAction: {
      label: 'Import from Backlog',
      onClick: fn(),
    },
  },
};

export const SmallSize: Story = {
  args: {
    icon: Users,
    title: 'No team members',
    description: 'Add people to this team.',
    size: 'small',
    action: {
      label: 'Add Member',
      onClick: fn(),
    },
  },
};

export const LargeSize: Story = {
  args: {
    icon: Calendar,
    title: 'No meetings scheduled',
    description: 'Schedule a meeting to collaborate with your team. You can invite team members and set an agenda.',
    size: 'large',
    action: {
      label: 'Schedule Meeting',
      onClick: fn(),
    },
  },
};

export const Compact: Story = {
  args: {
    icon: Bug,
    title: 'No bugs reported',
    description: 'Great job! No bugs have been reported yet.',
    compact: true,
    size: 'small',
  },
};

export const WithOnboardingSteps: Story = {
  args: {
    icon: FolderOpen,
    title: 'Welcome to ShipFlow',
    description: 'Follow these steps to get started with your project.',
    onboardingSteps: [
      {
        icon: '📁',
        title: 'Create a project',
        description: 'Set up your first project to organize your work.',
      },
      {
        icon: '🔄',
        title: 'Start a cycle',
        description: 'Create a 6-week cycle to plan your work.',
      },
      {
        icon: '💡',
        title: 'Add pitches',
        description: 'Define the features you want to build.',
      },
    ],
    action: {
      label: 'Get Started',
      onClick: fn(),
    },
  },
};

export const NoActions: Story = {
  args: {
    icon: FileText,
    title: 'Search results',
    description: 'No results match your search criteria. Try adjusting your filters.',
  },
};
