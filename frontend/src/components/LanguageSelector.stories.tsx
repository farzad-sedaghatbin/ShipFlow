import type { Meta, StoryObj } from '@storybook/react';
import { LanguageSelector } from './LanguageSelector';

const meta = {
  title: 'Components/LanguageSelector',
  component: LanguageSelector,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'A dropdown selector for changing the application language. Supports English, Persian (Farsi), and Spanish.',
      },
    },
  },
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['default', 'outline', 'ghost'],
      description: 'The visual style of the button',
    },
    size: {
      control: 'select',
      options: ['default', 'sm', 'lg', 'icon'],
      description: 'The size of the button',
    },
    showLabel: {
      control: 'boolean',
      description: 'Whether to show the current language name',
    },
  },
} satisfies Meta<typeof LanguageSelector>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {
  args: {},
};

export const IconOnly: Story = {
  args: {
    size: 'icon',
    showLabel: false,
  },
};

export const WithLabel: Story = {
  args: {
    showLabel: true,
    size: 'default',
  },
};

export const GhostVariant: Story = {
  args: {
    variant: 'ghost',
  },
};

export const OutlineVariant: Story = {
  args: {
    variant: 'outline',
  },
};

export const SmallSize: Story = {
  args: {
    size: 'sm',
    showLabel: true,
  },
};

export const LargeSize: Story = {
  args: {
    size: 'lg',
    showLabel: true,
  },
};
