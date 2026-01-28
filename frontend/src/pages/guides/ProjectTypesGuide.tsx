import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Layers,
  GitBranch,
  Zap,
  Target,
  TrendingUp,
  CheckCircle,
  AlertCircle,
  ArrowRight,
} from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/card';
import { Button } from '../../components/ui/button';
import { Badge } from '../../components/ui/badge';
import { Separator } from '../../components/ui/separator';
import { Alert, AlertDescription, AlertTitle } from '../../components/ui/alert';

export default function ProjectTypesGuide() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-background">
      {/* Header */}
      <header className="border-b border-border bg-background/95 backdrop-blur sticky top-0 z-50">
        <div className="container mx-auto px-4 max-w-5xl">
          <div className="flex items-center justify-between h-16">
            <Button variant="ghost" size="sm" onClick={() => navigate('/guides')}>
              <ArrowLeft className="h-4 w-4 mr-2" />
              {t('common.back')}
            </Button>
            <Badge variant="outline" className="bg-primary/10 text-primary border-primary/20">
              <Layers className="h-3 w-3 mr-1" />
              {t('helpGuides.projectTypes')}
            </Badge>
          </div>
        </div>
      </header>

      <div className="container mx-auto px-4 max-w-5xl py-12">
        {/* Hero */}
        <div className="mb-12 text-center">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-gradient-to-br from-primary/20 to-secondary/20 mb-6">
            <Layers className="h-8 w-8 text-primary" />
          </div>
          <h1 className="text-4xl font-bold text-foreground mb-4">
            Dual Project Modes
          </h1>
          <p className="text-xl text-muted-foreground max-w-3xl mx-auto">
            ShipFlow is the only project management tool that natively supports both <strong>Shape Up</strong> (6-week cycles) 
            and <strong>Kanban</strong> (continuous flow) methodologies. Choose the right approach for each project.
          </p>
        </div>

        {/* Unique Feature Alert */}
        <Alert className="mb-8 border-primary/50 bg-primary/5">
          <Zap className="h-5 w-5 text-primary" />
          <AlertTitle className="text-primary">Unique to ShipFlow</AlertTitle>
          <AlertDescription>
            Unlike Linear, Jira, Asana, or Monday.com which only support Kanban/Scrum, ShipFlow gives you 
            the flexibility to run Shape Up projects alongside Kanban projects in the same workspace.
          </AlertDescription>
        </Alert>

        {/* Shape Up Mode */}
        <Card className="mb-8">
          <CardHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 rounded-lg bg-blue-500/10">
                <Target className="h-6 w-6 text-blue-500" />
              </div>
              <CardTitle>Shape Up Mode</CardTitle>
            </div>
            <CardDescription>
              Fixed-time, variable-scope methodology with 6-week cycles and betting tables
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <h4 className="font-semibold text-sm mb-3">Key Features:</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>6-Week Cycles</strong> - Fixed timeboxes with building and cooldown phases</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Betting Table</strong> - Stakeholders bet on which pitches to commit to</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Appetite-Based Budgeting</strong> - Define time budgets (Small Batch: 1-2 weeks, Big Batch: 6 weeks)</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Circuit Breaker</strong> - Automatic scope protection when appetite is exceeded</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Hill Charts</strong> - Visual progress tracking (figuring out → making it happen)</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Pitch-Centric</strong> - Work organized around shaped pitches with scopes</span>
                </li>
              </ul>
            </div>

            <Separator />

            <div>
              <h4 className="font-semibold text-sm mb-2">Best For:</h4>
              <p className="text-sm text-muted-foreground">
                Product teams shipping new features, innovation projects, R&D work, and teams that need 
                fixed delivery windows with flexible scope.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Kanban Mode */}
        <Card className="mb-8">
          <CardHeader>
            <div className="flex items-center gap-3 mb-2">
              <div className="p-2 rounded-lg bg-purple-500/10">
                <GitBranch className="h-6 w-6 text-purple-500" />
              </div>
              <CardTitle>Kanban Mode</CardTitle>
            </div>
            <CardDescription>
              Continuous flow methodology with automatic "Continuous Flow" cycle
            </CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <h4 className="font-semibold text-sm mb-3">Key Features:</h4>
              <ul className="space-y-2 text-sm text-muted-foreground">
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Continuous Flow</strong> - No fixed timeboxes, work flows continuously</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Automatic Cycle</strong> - "Continuous Flow" cycle created automatically on project creation</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Kanban Board</strong> - Drag-and-drop board with status columns (TODO, In Progress, Done, etc.)</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Feature Tasks</strong> - Task-focused workflow without pitches or scopes</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Simplified UI</strong> - Cycle selector, betting table, and pitch fields hidden</span>
                </li>
                <li className="flex items-start gap-2">
                  <CheckCircle className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
                  <span><strong>Work Log Timers</strong> - Track time with automated timers and subtask support</span>
                </li>
              </ul>
            </div>

            <Separator />

            <div>
              <h4 className="font-semibold text-sm mb-2">Best For:</h4>
              <p className="text-sm text-muted-foreground">
                Support teams, maintenance work, operations, bug fixes, and teams that need flexible 
                delivery without fixed cycles.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* How to Choose */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>How to Choose the Right Mode</CardTitle>
            <CardDescription>Decision guide for selecting Shape Up vs Kanban</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="grid md:grid-cols-2 gap-6">
              <div>
                <h4 className="font-semibold text-sm mb-3 flex items-center gap-2">
                  <Target className="h-4 w-4 text-blue-500" />
                  Choose Shape Up If:
                </h4>
                <ul className="space-y-2 text-sm text-muted-foreground">
                  <li>• You're building new product features</li>
                  <li>• You need predictable delivery windows</li>
                  <li>• You want to limit work-in-progress</li>
                  <li>• You need appetite-based budgeting</li>
                  <li>• Your team can commit to 6-week cycles</li>
                  <li>• You want circuit breaker protection</li>
                </ul>
              </div>
              <div>
                <h4 className="font-semibold text-sm mb-3 flex items-center gap-2">
                  <GitBranch className="h-4 w-4 text-purple-500" />
                  Choose Kanban If:
                </h4>
                <ul className="space-y-2 text-muted-foreground text-sm">
                  <li>• You're handling support tickets or bugs</li>
                  <li>• Work arrives unpredictably</li>
                  <li>• You need continuous delivery</li>
                  <li>• You don't want fixed timeboxes</li>
                  <li>• Your team prefers flow-based work</li>
                  <li>• You're doing maintenance or operations</li>
                </ul>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Creating a Project */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Creating a Project with a Specific Mode</CardTitle>
            <CardDescription>Step-by-step guide</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-3">
              <div className="flex items-start gap-3">
                <Badge variant="outline" className="mt-0.5">1</Badge>
                <div className="flex-1">
                  <p className="text-sm font-medium">Navigate to Projects</p>
                  <p className="text-sm text-muted-foreground">
                    Click <strong>Projects</strong> in the main navigation menu
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Badge variant="outline" className="mt-0.5">2</Badge>
                <div className="flex-1">
                  <p className="text-sm font-medium">Create New Project</p>
                  <p className="text-sm text-muted-foreground">
                    Click the <strong>"+ New Project"</strong> button in the top-right corner
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Badge variant="outline" className="mt-0.5">3</Badge>
                <div className="flex-1">
                  <p className="text-sm font-medium">Select Project Type</p>
                  <p className="text-sm text-muted-foreground">
                    Choose between <strong>Shape Up</strong> or <strong>Kanban</strong> from the dropdown
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Badge variant="outline" className="mt-0.5">4</Badge>
                <div className="flex-1">
                  <p className="text-sm font-medium">Fill in Project Details</p>
                  <p className="text-sm text-muted-foreground">
                    Enter project name, description, and select a team
                  </p>
                </div>
              </div>
              
              <div className="flex items-start gap-3">
                <Badge variant="outline" className="mt-0.5">5</Badge>
                <div className="flex-1">
                  <p className="text-sm font-medium">Save and Start Working</p>
                  <p className="text-sm text-muted-foreground">
                    <strong>Shape Up:</strong> Create cycles and pitches<br />
                    <strong>Kanban:</strong> Start adding tasks to the board
                  </p>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* UI Differences */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>UI Differences by Project Type</CardTitle>
            <CardDescription>How the interface adapts to your chosen mode</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-2 font-semibold">Feature</th>
                    <th className="text-center py-2 font-semibold">Shape Up</th>
                    <th className="text-center py-2 font-semibold">Kanban</th>
                  </tr>
                </thead>
                <tbody className="text-muted-foreground">
                  <tr className="border-b">
                    <td className="py-3">Cycles menu in navigation</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><span className="text-xs">Hidden</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Cycle selector in backlog</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><span className="text-xs">Hidden</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Pitch and scope fields</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><span className="text-xs">Hidden</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Betting table access</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><span className="text-xs">Hidden</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Hill charts</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><span className="text-xs">N/A</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Kanban board drag-and-drop</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Task terminology</td>
                    <td className="text-center"><span className="text-xs">"Pitch Tasks"</span></td>
                    <td className="text-center"><span className="text-xs">"Feature Tasks"</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Default view</td>
                    <td className="text-center"><span className="text-xs">List</span></td>
                    <td className="text-center"><span className="text-xs">Board</span></td>
                  </tr>
                  <tr className="border-b">
                    <td className="py-3">Subtask creation</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                  </tr>
                  <tr>
                    <td className="py-3">Work log timers</td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                    <td className="text-center"><CheckCircle className="h-4 w-4 text-green-500 inline" /></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </CardContent>
        </Card>

        {/* Important Notes */}
        <Alert className="mb-8">
          <AlertCircle className="h-5 w-5" />
          <AlertTitle>Important Notes</AlertTitle>
          <AlertDescription className="space-y-2 mt-2">
            <p>
              • <strong>Project type cannot be changed</strong> after creation. Choose carefully based on your workflow needs.
            </p>
            <p>
              • <strong>Kanban projects</strong> automatically get a "Continuous Flow" cycle that cannot be deleted.
            </p>
            <p>
              • <strong>All existing projects</strong> default to Shape Up mode for backward compatibility.
            </p>
            <p>
              • <strong>You can run both types</strong> in the same workspace - e.g., Shape Up for product development, Kanban for support.
            </p>
          </AlertDescription>
        </Alert>

        {/* Next Steps */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <TrendingUp className="h-5 w-5 text-primary" />
              Next Steps
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Button 
              variant="outline" 
              className="w-full justify-between" 
              onClick={() => navigate('/projects')}
            >
              <span>Create Your First Project</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
            <Button 
              variant="outline" 
              className="w-full justify-between" 
              onClick={() => navigate('/guides/cycle-setup')}
            >
              <span>Learn About Cycle Setup (Shape Up)</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
            <Button 
              variant="outline" 
              className="w-full justify-between" 
              onClick={() => navigate('/guides')}
            >
              <span>Back to All Guides</span>
              <ArrowRight className="h-4 w-4" />
            </Button>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
