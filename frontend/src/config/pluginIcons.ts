import type { ComponentType } from 'react';
import {
  Bell,
  BarChart3,
  BookOpen,
  Calendar,
  FileText,
  Flag,
  Link as LinkIcon,
  List,
  Puzzle,
  Rocket,
  Settings,
  Shield,
  Star,
  Users,
} from 'lucide-react';

/**
 * Icons a UI plugin may ask for by name. Keep in sync with the backend allow-list in
 * PluginUiSanitizer.ALLOWED_ICONS — the backend already replaces unknown names with "puzzle",
 * and this map falls back to the same icon, so a drift degrades gracefully.
 */
export const PLUGIN_ICONS: Record<string, ComponentType<{ className?: string }>> = {
  puzzle: Puzzle,
  chart: BarChart3,
  list: List,
  book: BookOpen,
  calendar: Calendar,
  shield: Shield,
  rocket: Rocket,
  flag: Flag,
  users: Users,
  settings: Settings,
  file: FileText,
  link: LinkIcon,
  star: Star,
  bell: Bell,
};

export function pluginIcon(name: string): ComponentType<{ className?: string }> {
  return PLUGIN_ICONS[name] ?? Puzzle;
}
