import type { Meta, StoryObj } from '@storybook/react';
import { ComponentErrorBoundary } from './ErrorBoundary';

// A component that throws an error for testing
const BuggyComponent = ({ shouldThrow = false }: { shouldThrow?: boolean }) => {
  if (shouldThrow) {
    throw new Error('This is a test error to demonstrate the error boundary');
  }
  return <div className="p-4 bg-green-100 rounded">Component rendered successfully!</div>;
};

const meta = {
  title: 'Components/ErrorBoundary',
  component: ComponentErrorBoundary,
  parameters: {
    layout: 'centered',
    docs: {
      description: {
        component: 'An error boundary component that catches JavaScript errors in child components and displays a fallback UI.',
      },
    },
  },
  tags: ['autodocs'],
} satisfies Meta<typeof ComponentErrorBoundary>;

export default meta;
type Story = StoryObj<typeof meta>;

export const WithoutError: Story = {
  args: {
    componentName: 'Test Component',
    children: <BuggyComponent shouldThrow={false} />,
  },
};

export const WithError: Story = {
  args: {
    componentName: 'Widget',
    children: <BuggyComponent shouldThrow={true} />,
  },
};

export const CustomFallback: Story = {
  args: {
    componentName: 'Dashboard Widget',
    children: <BuggyComponent shouldThrow={true} />,
    fallback: (
      <div className="p-6 border-2 border-dashed border-yellow-400 bg-yellow-50 rounded-lg text-center">
        <p className="text-yellow-700 font-medium">⚠️ Custom fallback UI</p>
        <p className="text-yellow-600 text-sm mt-2">
          Something went wrong with this component.
        </p>
        <button 
          className="mt-4 px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600"
          onClick={() => window.location.reload()}
        >
          Refresh Page
        </button>
      </div>
    ),
  },
};
