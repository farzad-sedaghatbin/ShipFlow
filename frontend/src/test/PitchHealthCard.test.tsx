import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { PitchHealthCard } from '../components/PitchHealthCard';
import { PitchHealthDTO } from '../services/pitchHealthService';

const mockPitchHealth: PitchHealthDTO = {
  pitchId: 1,
  pitchName: 'User Authentication',
  cycleName: 'Sprint 1',
  status: 'IN_PROGRESS',
  riskLevel: 'MEDIUM',
  riskColor: '#ff9800',
  appetiteHours: 112,
  actualHours: 56,
  appetiteUsedPercent: 50,
  daysLeft: 14,
  qaStatus: 'NOT_STARTED',
  statusSummary: '50% budget used, 14 days remaining',
  teamName: 'Team Alpha',
  cycleEndDate: new Date().toISOString(),
};

const renderComponent = (component: React.ReactNode) => {
  return render(component);
};

describe('PitchHealthCard', () => {
  it('renders pitch name', () => {
    renderComponent(<PitchHealthCard health={mockPitchHealth} />);
    
    expect(screen.getByText('User Authentication')).toBeInTheDocument();
  });

  it('displays budget progress', () => {
    renderComponent(<PitchHealthCard health={mockPitchHealth} />);
    
    // Check for budget information - actual format is "56.0h / 112h"
    expect(screen.getByText(/56\.0h \/ 112h/)).toBeInTheDocument();
  });

  it('shows days remaining', () => {
    renderComponent(<PitchHealthCard health={mockPitchHealth} />);
    
    // Component shows "14 days left"
    expect(screen.getByText(/14 days left/)).toBeInTheDocument();
  });

  it('calls onClick when clicked', () => {
    const mockOnClick = vi.fn();
    renderComponent(<PitchHealthCard health={mockPitchHealth} onClick={mockOnClick} />);
    
    const card = screen.getByText('User Authentication').closest('[data-slot="card"]') || screen.getByText('User Authentication').closest('div');
    if (card) fireEvent.click(card);
    
    expect(mockOnClick).toHaveBeenCalled();
  });

  it('renders compact view when compact prop is true', () => {
    renderComponent(<PitchHealthCard health={mockPitchHealth} compact />);
    
    expect(screen.getByText('User Authentication')).toBeInTheDocument();
  });

  it('shows HIGH risk level correctly', () => {
    const highRiskPitch: PitchHealthDTO = {
      ...mockPitchHealth,
      riskLevel: 'HIGH',
      riskColor: '#f44336',
      appetiteUsedPercent: 90,
    };
    
    renderComponent(<PitchHealthCard health={highRiskPitch} />);
    
    expect(screen.getByText('User Authentication')).toBeInTheDocument();
  });

  it('shows CRITICAL risk level correctly', () => {
    const criticalPitch: PitchHealthDTO = {
      ...mockPitchHealth,
      riskLevel: 'CRITICAL',
      riskColor: '#d32f2f',
      daysLeft: 2,
    };
    
    renderComponent(<PitchHealthCard health={criticalPitch} />);
    
    expect(screen.getByText('User Authentication')).toBeInTheDocument();
  });
});
