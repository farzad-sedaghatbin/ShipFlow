import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import StatusChip from '../components/StatusChip';
import ProgressBar from '../components/ProgressBar';

const renderWithProviders = (component: React.ReactNode) => {
  return render(
    <BrowserRouter>
      {component}
    </BrowserRouter>
  );
};

describe('StatusChip', () => {
  it('renders Pending status correctly', () => {
    renderWithProviders(<StatusChip status="PENDING" />);
    expect(screen.getByText('Pending')).toBeInTheDocument();
  });

  it('renders In Progress status correctly', () => {
    renderWithProviders(<StatusChip status="IN_PROGRESS" />);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
  });

  it('renders Done status correctly', () => {
    renderWithProviders(<StatusChip status="DONE" />);
    expect(screen.getByText('Done')).toBeInTheDocument();
  });

  it('renders Cancelled status correctly', () => {
    renderWithProviders(<StatusChip status="CANCELLED" />);
    expect(screen.getByText('Cancelled')).toBeInTheDocument();
  });

  it('renders Testing status correctly', () => {
    renderWithProviders(<StatusChip status="TESTING" />);
    expect(screen.getByText('Testing')).toBeInTheDocument();
  });
});

describe('ProgressBar', () => {
  it('renders with 0% progress when label is provided', () => {
    renderWithProviders(<ProgressBar value={0} label="Progress" />);
    expect(screen.getByText('0%')).toBeInTheDocument();
    expect(screen.getByText('Progress')).toBeInTheDocument();
  });

  it('renders with 50% progress when label is provided', () => {
    renderWithProviders(<ProgressBar value={50} label="Progress" />);
    expect(screen.getByText('50%')).toBeInTheDocument();
  });

  it('renders with 100% progress when label is provided', () => {
    renderWithProviders(<ProgressBar value={100} label="Complete" />);
    expect(screen.getByText('100%')).toBeInTheDocument();
  });

  it('renders progress bar without percentage text when no label', () => {
    renderWithProviders(<ProgressBar value={75} />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });
});
