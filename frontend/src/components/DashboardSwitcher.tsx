import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { LayoutDashboard, Star, Settings, ChevronDown } from 'lucide-react';
import { Button } from './ui/button';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from './ui/dropdown-menu';
import { customDashboardService } from '../services/customDashboardService';
import { CustomDashboard } from '../types/customDashboard';

interface DashboardSwitcherProps {
  onDashboardChange?: (dashboardId: number) => void;
}

export default function DashboardSwitcher({ onDashboardChange }: DashboardSwitcherProps) {
  const [dashboards, setDashboards] = useState<CustomDashboard[]>([]);
  const [currentDashboard, setCurrentDashboard] = useState<CustomDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    loadDashboards();
  }, []);

  const loadDashboards = async () => {
    try {
      setLoading(true);
      const dashboardList = await customDashboardService.getAll();
      setDashboards(dashboardList);
      
      // Get saved dashboard ID from localStorage or use default
      const savedDashboardId = localStorage.getItem('selectedDashboardId');
      let selected = dashboardList.find((d: CustomDashboard) => d.isDefault);
      
      if (savedDashboardId) {
        const savedDashboard = dashboardList.find((d: CustomDashboard) => d.id === parseInt(savedDashboardId));
        if (savedDashboard) {
          selected = savedDashboard;
        }
      }
      
      if (selected) {
        setCurrentDashboard(selected);
        onDashboardChange?.(selected.id);
      }
    } catch (err) {
      console.error('Failed to load dashboards:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleDashboardSelect = (dashboard: CustomDashboard) => {
    setCurrentDashboard(dashboard);
    localStorage.setItem('selectedDashboardId', dashboard.id.toString());
    onDashboardChange?.(dashboard.id);
    // Navigate to report view
    navigate(`/dashboards/${dashboard.id}`);
  };

  if (loading || dashboards.length === 0) {
    return null;
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="outline" className="gap-2">
          <LayoutDashboard className="h-4 w-4" />
          <span className="hidden sm:inline">
            {currentDashboard?.name || 'Select Dashboard'}
          </span>
          {currentDashboard?.isDefault && (
            <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
          )}
          <ChevronDown className="h-4 w-4 opacity-50" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" className="w-56">
        <DropdownMenuLabel>My Dashboards</DropdownMenuLabel>
        <DropdownMenuSeparator />
        {dashboards.map((dashboard) => (
          <DropdownMenuItem
            key={dashboard.id}
            onClick={() => handleDashboardSelect(dashboard)}
            className="cursor-pointer"
          >
            <div className="flex items-center justify-between w-full">
              <span>{dashboard.name}</span>
              {dashboard.isDefault && (
                <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
              )}
            </div>
          </DropdownMenuItem>
        ))}
        <DropdownMenuSeparator />
        <DropdownMenuItem
          onClick={() => navigate('/dashboards')}
          className="cursor-pointer"
        >
          <Settings className="mr-2 h-4 w-4" />
          Manage Dashboards
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
