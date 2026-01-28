import type { Meta, StoryObj } from '@storybook/react';
import {
  StatCardSkeleton,
  DashboardSkeleton,
  CycleListSkeleton,
  PitchDetailSkeleton,
  TableSkeleton,
  ProjectCardsSkeleton,
} from './Skeletons';

const meta: Meta = {
  title: 'Components/Skeletons',
  parameters: {
    layout: 'padded',
    docs: {
      description: {
        component: 'Loading skeleton components for various UI patterns. Used to show placeholder content while data is loading.',
      },
    },
  },
  tags: ['autodocs'],
};

export default meta;

// StatCardSkeleton Stories
export const StatCard: StoryObj = {
  render: () => <StatCardSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Skeleton for stat/metric cards displayed on dashboards.',
      },
    },
  },
};

export const MultipleStatCards: StoryObj = {
  render: () => (
    <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-3">
      <StatCardSkeleton />
      <StatCardSkeleton />
      <StatCardSkeleton />
      <StatCardSkeleton />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Multiple stat card skeletons in a grid layout.',
      },
    },
  },
};

// DashboardSkeleton Story
export const Dashboard: StoryObj = {
  render: () => <DashboardSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Full dashboard skeleton with stats, cycles, and recent activity.',
      },
    },
  },
};

// CycleListSkeleton Story
export const CycleList: StoryObj = {
  render: () => <CycleListSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Skeleton for cycle list with multiple cycle cards.',
      },
    },
  },
};

// PitchDetailSkeleton Story
export const PitchDetail: StoryObj = {
  render: () => <PitchDetailSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Skeleton for pitch detail page.',
      },
    },
  },
};

// TableSkeleton Story
export const Table: StoryObj = {
  render: () => <TableSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Skeleton for data tables with header and rows.',
      },
    },
  },
};

// ProjectCardsSkeleton Story
export const ProjectCards: StoryObj = {
  render: () => <ProjectCardsSkeleton />,
  parameters: {
    docs: {
      description: {
        story: 'Skeleton for project cards grid.',
      },
    },
  },
};

// All Skeletons Overview
export const AllSkeletons: StoryObj = {
  render: () => (
    <div className="space-y-8">
      <section>
        <h3 className="text-lg font-semibold mb-2">Stat Cards</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          <StatCardSkeleton />
          <StatCardSkeleton />
          <StatCardSkeleton />
          <StatCardSkeleton />
        </div>
      </section>

      <section>
        <h3 className="text-lg font-semibold mb-2">Table</h3>
        <TableSkeleton />
      </section>

      <section>
        <h3 className="text-lg font-semibold mb-2">Project Cards</h3>
        <ProjectCardsSkeleton />
      </section>

      <section>
        <h3 className="text-lg font-semibold mb-2">Pitch Detail</h3>
        <PitchDetailSkeleton />
      </section>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'Overview of all available skeleton components.',
      },
    },
  },
};
