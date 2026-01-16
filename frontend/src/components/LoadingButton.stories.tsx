import type { Meta, StoryObj } from '@storybook/react';
import { LoadingButton } from './LoadingButton';
import { Save, Send, Download } from 'lucide-react';

const meta: Meta<typeof LoadingButton> = {
  title: 'Components/LoadingButton',
  component: LoadingButton,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A button component with built-in loading state. Shows a spinner and optionally different text when loading.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    loading: {
      control: 'boolean',
      description: 'Whether the button is in loading state',
    },
    loadingText: {
      control: 'text',
      description: 'Text to show when loading (defaults to children)',
    },
    variant: {
      control: 'select',
      options: ['default', 'destructive', 'outline', 'secondary', 'ghost', 'link'],
      description: 'Visual style variant',
    },
    size: {
      control: 'select',
      options: ['default', 'sm', 'lg', 'icon'],
      description: 'Size of the button',
    },
    disabled: {
      control: 'boolean',
      description: 'Whether the button is disabled',
    },
  },
};

export default meta;
type Story = StoryObj<typeof LoadingButton>;

export const Default: Story = {
  args: {
    children: 'Save Changes',
    loading: false,
  },
};

export const Loading: Story = {
  args: {
    children: 'Save Changes',
    loading: true,
  },
};

export const LoadingWithText: Story = {
  args: {
    children: 'Save Changes',
    loading: true,
    loadingText: 'Saving...',
  },
};

export const WithIcon: Story = {
  args: {
    children: (
      <>
        <Save className="w-4 h-4 mr-2" />
        Save
      </>
    ),
    loading: false,
  },
};

export const WithIconLoading: Story = {
  args: {
    children: (
      <>
        <Save className="w-4 h-4 mr-2" />
        Save
      </>
    ),
    loading: true,
    loadingText: 'Saving...',
  },
};

export const DestructiveVariant: Story = {
  args: {
    children: 'Delete',
    variant: 'destructive',
    loading: false,
  },
};

export const DestructiveLoading: Story = {
  args: {
    children: 'Delete',
    variant: 'destructive',
    loading: true,
    loadingText: 'Deleting...',
  },
};

export const OutlineVariant: Story = {
  args: {
    children: 'Export',
    variant: 'outline',
    loading: false,
  },
};

export const SmallSize: Story = {
  args: {
    children: 'Submit',
    size: 'sm',
    loading: false,
  },
};

export const LargeSize: Story = {
  args: {
    children: 'Continue',
    size: 'lg',
    loading: false,
  },
};

// Showcase all states
export const AllVariants: Story = {
  render: () => (
    <div className="flex flex-col gap-4">
      <div className="flex gap-2 items-center">
        <span className="w-20 text-sm text-muted-foreground">Default:</span>
        <LoadingButton>Normal</LoadingButton>
        <LoadingButton loading>Loading</LoadingButton>
        <LoadingButton loading loadingText="Saving...">Save</LoadingButton>
      </div>
      <div className="flex gap-2 items-center">
        <span className="w-20 text-sm text-muted-foreground">Secondary:</span>
        <LoadingButton variant="secondary">Normal</LoadingButton>
        <LoadingButton variant="secondary" loading>Loading</LoadingButton>
      </div>
      <div className="flex gap-2 items-center">
        <span className="w-20 text-sm text-muted-foreground">Outline:</span>
        <LoadingButton variant="outline">Normal</LoadingButton>
        <LoadingButton variant="outline" loading>Loading</LoadingButton>
      </div>
      <div className="flex gap-2 items-center">
        <span className="w-20 text-sm text-muted-foreground">Destructive:</span>
        <LoadingButton variant="destructive">Normal</LoadingButton>
        <LoadingButton variant="destructive" loading>Loading</LoadingButton>
      </div>
    </div>
  ),
  parameters: {
    docs: {
      description: {
        story: 'All button variants in normal and loading states.',
      },
    },
  },
};

// Interactive example
export const InteractiveDemo: Story = {
  render: () => {
    const handleClick = () => {
      return new Promise((resolve) => setTimeout(resolve, 2000));
    };

    return (
      <div className="flex flex-col gap-4 items-start">
        <p className="text-sm text-muted-foreground">
          Click the buttons to see the loading state in action (simulated 2s delay)
        </p>
        <div className="flex gap-2">
          <LoadingButton
            onClick={async (e) => {
              const btn = e.currentTarget;
              btn.setAttribute('data-loading', 'true');
              await handleClick();
              btn.setAttribute('data-loading', 'false');
            }}
          >
            <Send className="w-4 h-4 mr-2" />
            Send Message
          </LoadingButton>
          <LoadingButton variant="outline">
            <Download className="w-4 h-4 mr-2" />
            Download
          </LoadingButton>
        </div>
      </div>
    );
  },
};
