import type { Meta, StoryObj } from '@storybook/react';
import StatusChip from './StatusChip';

const meta: Meta<typeof StatusChip> = {
  title: 'Components/StatusChip',
  component: StatusChip,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A visual indicator for pitch/task status. Uses color-coded badges to communicate current state.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    status: {
      control: 'select',
      options: ['PENDING', 'SHAPED', 'STARTED', 'IN_PROGRESS', 'TESTING', 'DONE', 'COOLDOWN', 'CANCELLED'],
      description: 'The current status to display',
    },
    size: {
      control: 'radio',
      options: ['small', 'medium'],
      description: 'Size variant of the chip',
    },
  },
};

export default meta;
type Story = StoryObj<typeof StatusChip>;

export const Pending: Story = {
  args: {
    status: 'PENDING',
    size: 'small',
  },
};

export const Shaped: Story = {
  args: {
    status: 'SHAPED',
    size: 'small',
  },
};

export const Started: Story = {
  args: {
    status: 'STARTED',
    size: 'small',
  },
};

export const InProgress: Story = {
  args: {
    status: 'IN_PROGRESS',
    size: 'small',
  },
};

export const Testing: Story = {
  args: {
    status: 'TESTING',
    size: 'small',
  },
};

export const Done: Story = {
  args: {
    status: 'DONE',
    size: 'small',
  },
};

export const Cooldown: Story = {
  args: {
    status: 'COOLDOWN',
    size: 'small',
  },
};

export const Cancelled: Story = {
  args: {
    status: 'CANCELLED',
    size: 'small',
  },
};

export const MediumSize: Story = {
  args: {
    status: 'IN_PROGRESS',
    size: 'medium',
  },
};

// Story showing all statuses together
export const AllStatuses: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      <StatusChip status="PENDING" />
      <StatusChip status="SHAPED" />
      <StatusChip status="STARTED" />
      <StatusChip status="IN_PROGRESS" />
      <StatusChip status="TESTING" />
      <StatusChip status="DONE" />
      <StatusChip status="COOLDOWN" />
      <StatusChip status="CANCELLED" />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'All available status chips displayed together for comparison.',
      },
    },
  },
};

export const AllStatusesMedium: Story = {
  render: () => (
    <div className="flex flex-wrap gap-2">
      <StatusChip status="PENDING" size="medium" />
      <StatusChip status="SHAPED" size="medium" />
      <StatusChip status="STARTED" size="medium" />
      <StatusChip status="IN_PROGRESS" size="medium" />
      <StatusChip status="TESTING" size="medium" />
      <StatusChip status="DONE" size="medium" />
      <StatusChip status="COOLDOWN" size="medium" />
      <StatusChip status="CANCELLED" size="medium" />
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'All status chips in medium size.',
      },
    },
  },
};
