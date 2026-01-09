import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeToggle } from '../components/ThemeToggle';
import { ThemeContext, ThemeMode } from '../contexts/ThemeContext';
import { lightTheme } from '../theme';

// Create a mock theme context provider
const mockSetMode = vi.fn();
const mockUpdatePreferences = vi.fn();

const createMockThemeContext = (mode: ThemeMode) => ({
  mode,
  actualMode: mode === 'system' ? 'light' as const : mode as 'light' | 'dark',
  setMode: mockSetMode,
  toggleTheme: vi.fn(),
  theme: lightTheme,
  preferences: {
    themeMode: mode,
    compactView: false,
    enableAnimations: true,
  },
  updatePreferences: mockUpdatePreferences,
  isLoading: false,
});

const renderWithProviders = (mode: ThemeMode = 'light') => {
  return render(
    <ThemeContext.Provider value={createMockThemeContext(mode)}>
      <ThemeToggle />
    </ThemeContext.Provider>
  );
};

describe('ThemeToggle', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders theme toggle button', () => {
    renderWithProviders('light');
    
    expect(screen.getByRole('button')).toBeInTheDocument();
  });

  it('opens menu on click', async () => {
    const user = userEvent.setup();
    renderWithProviders('light');
    
    await user.click(screen.getByRole('button'));
    
    expect(screen.getByText('Light')).toBeInTheDocument();
    expect(screen.getByText('Dark')).toBeInTheDocument();
    expect(screen.getByText('System')).toBeInTheDocument();
  });

  it('calls setMode with "light" when Light option is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders('dark');
    
    await user.click(screen.getByRole('button'));
    await user.click(screen.getByText('Light'));
    
    expect(mockSetMode).toHaveBeenCalledWith('light');
  });

  it('calls setMode with "dark" when Dark option is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders('light');
    
    await user.click(screen.getByRole('button'));
    await user.click(screen.getByText('Dark'));
    
    expect(mockSetMode).toHaveBeenCalledWith('dark');
  });

  it('calls setMode with "system" when System option is clicked', async () => {
    const user = userEvent.setup();
    renderWithProviders('light');
    
    await user.click(screen.getByRole('button'));
    await user.click(screen.getByText('System'));
    
    expect(mockSetMode).toHaveBeenCalledWith('system');
  });

  it('shows label when showLabel prop is true', () => {
    render(
      <ThemeContext.Provider value={createMockThemeContext('dark')}>
        <ThemeToggle showLabel />
      </ThemeContext.Provider>
    );
    
    expect(screen.getByText('Dark')).toBeInTheDocument();
  });
});
