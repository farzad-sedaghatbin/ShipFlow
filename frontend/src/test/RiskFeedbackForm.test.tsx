import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { RiskFeedbackForm } from '../components/RiskFeedbackForm';
import { ToastProvider } from '../contexts/ToastContext';

const defaultProps = {
  pitchId: 1,
  pitchTitle: 'User Authentication',
  currentRiskScore: 75,
  onFeedbackSubmitted: vi.fn(),
};

const renderWithProviders = (props = {}) => {
  return render(
    <ToastProvider>
      <RiskFeedbackForm {...defaultProps} {...props} />
    </ToastProvider>
  );
};

describe('RiskFeedbackForm', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders feedback buttons', () => {
    renderWithProviders();
    
    // The component shows 3 icon buttons by default
    expect(screen.getByLabelText('Mark as accurate')).toBeInTheDocument();
    expect(screen.getByLabelText('Mark as inaccurate')).toBeInTheDocument();
    expect(screen.getByLabelText('Provide detailed feedback')).toBeInTheDocument();
  });

  it('opens dialog when accurate button is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders();
    
    const feedbackButton = screen.getByLabelText('Mark as accurate');
    await user.click(feedbackButton);
    
    // Dialog should now be open
    expect(screen.getByText('AI Risk Assessment Feedback')).toBeInTheDocument();
  });

  it('shows rating options in dialog', async () => {
    const user = userEvent.setup();
    renderWithProviders();
    
    // Open dialog
    await user.click(screen.getByLabelText('Provide detailed feedback'));
    
    expect(screen.getByText(/Accurate/)).toBeInTheDocument();
  });

  it('closes dialog when cancel is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders();
    
    // Open dialog
    await user.click(screen.getByLabelText('Provide detailed feedback'));
    expect(screen.getByText('AI Risk Assessment Feedback')).toBeInTheDocument();
    
    // Click cancel
    const cancelButton = screen.getByRole('button', { name: /Cancel/i });
    // Radix keeps `pointer-events: none` on the page until the dialog has finished mounting; on a
    // slow CI runner user-event can click inside that window and is (correctly) refused. Wait for
    // the button to become clickable instead of racing the dialog.
    await waitFor(() => {
      expect(getComputedStyle(cancelButton).pointerEvents).not.toBe('none');
    });
    await user.click(cancelButton);
    
    // Dialog should be closed (use waitFor to account for animation)
    await waitFor(() => {
      expect(screen.queryByText('AI Risk Assessment Feedback')).not.toBeInTheDocument();
    });
  });
});
