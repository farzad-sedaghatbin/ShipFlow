import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  ArrowLeft,
  Lightbulb,
  FileText,
  Target,
  Layers,
  Map,
  ArrowRight,
  CheckCircle2,
  AlertCircle,
  Dices,
  Repeat,
  Zap,
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Separator } from '@/components/ui/separator';
import { Badge } from '@/components/ui/badge';

export default function IdeaToRoadmapGuide() {
  const { t } = useTranslation();
  return (
    <div className="w-full max-w-none space-y-6">
      {/* Back Navigation */}
      <Button asChild variant="ghost" size="sm">
        <Link to="/help" className="gap-2">
          <ArrowLeft className="h-4 w-4" />
          {t('guides.backToHelp')}
        </Link>
      </Button>

      {/* Header */}
      <div>
        <h1 className="text-4xl font-bold tracking-tight">{t('guides.ideaToRoadmap.title')}</h1>
        <p className="text-lg text-muted-foreground mt-2">
          {t('guides.ideaToRoadmap.subtitle')}
        </p>
      </div>

      <Separator />

      {/* Shape Up Notice */}
      <div className="rounded-lg bg-amber-50 dark:bg-amber-950 border-2 border-amber-200 dark:border-amber-800 p-4">
        <div className="flex items-start gap-3">
          <AlertCircle className="h-5 w-5 text-amber-600 dark:text-amber-400 mt-0.5 flex-shrink-0" />
          <div className="space-y-1">
            <p className="text-sm font-semibold text-amber-900 dark:text-amber-100">
              {t('guides.ideaToRoadmap.shapeUpNoticeTitle')}
            </p>
            <p className="text-sm text-amber-800 dark:text-amber-200">
              {t('guides.ideaToRoadmap.shapeUpNoticeDesc')}{' '}
              <Link to="/help/project-types" className="underline font-semibold">
                {t('guides.ideaToRoadmap.projectTypesLink')}
              </Link>
            </p>
          </div>
        </div>
      </div>

      {/* Visual Lifecycle Flow */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Map className="h-5 w-5" />
            {t('guides.ideaToRoadmap.lifecycleTitle')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.lifecycleDesc')}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-center justify-center gap-2 py-4">
            {[
              { label: 'IDEA', color: 'bg-gray-500', icon: Lightbulb },
              { label: 'DRAFT', color: 'bg-blue-400', icon: FileText },
              { label: 'SHAPED', color: 'bg-blue-600', icon: Target },
              { label: 'PENDING', color: 'bg-purple-500', icon: Dices },
              { label: 'STARTED', color: 'bg-yellow-500', icon: Repeat },
              { label: 'DONE', color: 'bg-green-500', icon: CheckCircle2 },
            ].map((step, i, arr) => (
              <div key={step.label} className="flex items-center gap-2">
                <div className="flex flex-col items-center gap-1">
                  <div className={`flex h-10 w-10 items-center justify-center rounded-full ${step.color} text-white`}>
                    <step.icon className="h-5 w-5" />
                  </div>
                  <span className="text-xs font-semibold">{step.label}</span>
                </div>
                {i < arr.length - 1 && (
                  <ArrowRight className="h-4 w-4 text-muted-foreground" />
                )}
              </div>
            ))}
          </div>
          <div className="mt-4 rounded-lg bg-blue-50 dark:bg-blue-950 border border-blue-200 dark:border-blue-800 p-4">
            <p className="text-sm text-blue-900 dark:text-blue-100">
              <strong>{t('guides.ideaToRoadmap.keyConcept')}</strong>{' '}
              {t('guides.ideaToRoadmap.keyConceptDesc')}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Step 1: Capture an Idea */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-gray-500 text-white text-sm font-bold">1</div>
            {t('guides.ideaToRoadmap.step1Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step1Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step1Intro')}</p>
          <ol className="list-decimal list-inside space-y-3">
            <li>{t('guides.ideaToRoadmap.step1Action1')}</li>
            <li>{t('guides.ideaToRoadmap.step1Action2')}</li>
            <li>{t('guides.ideaToRoadmap.step1Action3')}</li>
            <li>{t('guides.ideaToRoadmap.step1Action4')}</li>
          </ol>
          <div className="rounded-lg bg-muted/50 p-4">
            <p className="text-sm text-muted-foreground">
              <strong>{t('guides.ideaToRoadmap.tip')}:</strong> {t('guides.ideaToRoadmap.step1Tip')}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Step 2: Draft the Pitch */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-400 text-white text-sm font-bold">2</div>
            {t('guides.ideaToRoadmap.step2Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step2Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step2Intro')}</p>
          <div className="grid gap-3 sm:grid-cols-2">
            <div className="rounded-lg border p-3 space-y-1">
              <p className="font-medium text-sm">{t('guides.ideaToRoadmap.step2Field1')}</p>
              <p className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step2Field1Desc')}</p>
            </div>
            <div className="rounded-lg border p-3 space-y-1">
              <p className="font-medium text-sm">{t('guides.ideaToRoadmap.step2Field2')}</p>
              <p className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step2Field2Desc')}</p>
            </div>
            <div className="rounded-lg border p-3 space-y-1">
              <p className="font-medium text-sm">{t('guides.ideaToRoadmap.step2Field3')}</p>
              <p className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step2Field3Desc')}</p>
            </div>
            <div className="rounded-lg border p-3 space-y-1">
              <p className="font-medium text-sm">{t('guides.ideaToRoadmap.step2Field4')}</p>
              <p className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step2Field4Desc')}</p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Step 3: Shape the Pitch */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-blue-600 text-white text-sm font-bold">3</div>
            {t('guides.ideaToRoadmap.step3Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step3Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step3Intro')}</p>
          <ul className="space-y-2">
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
              <span>{t('guides.ideaToRoadmap.step3Check1')}</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
              <span>{t('guides.ideaToRoadmap.step3Check2')}</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
              <span>{t('guides.ideaToRoadmap.step3Check3')}</span>
            </li>
            <li className="flex items-start gap-2">
              <CheckCircle2 className="h-4 w-4 text-green-500 mt-0.5 flex-shrink-0" />
              <span>{t('guides.ideaToRoadmap.step3Check4')}</span>
            </li>
          </ul>
          <div className="rounded-lg bg-purple-50 dark:bg-purple-950 border border-purple-200 dark:border-purple-800 p-4">
            <p className="text-sm text-purple-900 dark:text-purple-100">
              <strong>{t('guides.ideaToRoadmap.step3AiTitle')}</strong>{' '}
              {t('guides.ideaToRoadmap.step3AiDesc')}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Step 4: Bet on the Pitch */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-purple-500 text-white text-sm font-bold">4</div>
            {t('guides.ideaToRoadmap.step4Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step4Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step4Intro')}</p>
          <ol className="list-decimal list-inside space-y-3">
            <li>{t('guides.ideaToRoadmap.step4Action1')}</li>
            <li>{t('guides.ideaToRoadmap.step4Action2')}</li>
            <li>{t('guides.ideaToRoadmap.step4Action3')}</li>
          </ol>
          <div className="rounded-lg bg-muted/50 p-4">
            <p className="text-sm text-muted-foreground">
              <strong>{t('guides.ideaToRoadmap.tip')}:</strong> {t('guides.ideaToRoadmap.step4Tip')}
            </p>
          </div>
          <p className="text-sm">
            {t('guides.ideaToRoadmap.step4LearnMore')}{' '}
            <Link to="/help/betting-meeting" className="text-primary underline font-medium">
              {t('guides.ideaToRoadmap.bettingGuideLink')}
            </Link>
          </p>
        </CardContent>
      </Card>

      {/* Step 5: Link to Epic & Initiative */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-teal-500 text-white text-sm font-bold">5</div>
            {t('guides.ideaToRoadmap.step5Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step5Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step5Intro')}</p>
          <div className="rounded-lg border p-4 space-y-3">
            <div className="flex items-center gap-3">
              <Badge variant="outline" className="text-xs">
                {t('guides.ideaToRoadmap.step5HierarchyLevel1')}
              </Badge>
              <div className="flex items-center gap-2">
                <Target className="h-4 w-4 text-muted-foreground" />
                <span className="font-medium">{t('guides.ideaToRoadmap.step5Initiative')}</span>
              </div>
              <span className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step5InitiativeDesc')}</span>
            </div>
            <Separator />
            <div className="flex items-center gap-3">
              <Badge variant="outline" className="text-xs">
                {t('guides.ideaToRoadmap.step5HierarchyLevel2')}
              </Badge>
              <div className="flex items-center gap-2">
                <Layers className="h-4 w-4 text-muted-foreground" />
                <span className="font-medium">{t('guides.ideaToRoadmap.step5Epic')}</span>
              </div>
              <span className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step5EpicDesc')}</span>
            </div>
            <Separator />
            <div className="flex items-center gap-3">
              <Badge variant="outline" className="text-xs">
                {t('guides.ideaToRoadmap.step5HierarchyLevel3')}
              </Badge>
              <div className="flex items-center gap-2">
                <FileText className="h-4 w-4 text-muted-foreground" />
                <span className="font-medium">{t('guides.ideaToRoadmap.step5Pitch')}</span>
              </div>
              <span className="text-sm text-muted-foreground">{t('guides.ideaToRoadmap.step5PitchDesc')}</span>
            </div>
          </div>
          <ol className="list-decimal list-inside space-y-3">
            <li>{t('guides.ideaToRoadmap.step5Action1')}</li>
            <li>{t('guides.ideaToRoadmap.step5Action2')}</li>
            <li>{t('guides.ideaToRoadmap.step5Action3')}</li>
          </ol>
        </CardContent>
      </Card>

      {/* Step 6: See it on the Roadmap */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-green-500 text-white text-sm font-bold">6</div>
            {t('guides.ideaToRoadmap.step6Title')}
          </CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.step6Subtitle')}</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <p>{t('guides.ideaToRoadmap.step6Intro')}</p>
          <ol className="list-decimal list-inside space-y-3">
            <li>{t('guides.ideaToRoadmap.step6Action1')}</li>
            <li>{t('guides.ideaToRoadmap.step6Action2')}</li>
            <li>{t('guides.ideaToRoadmap.step6Action3')}</li>
          </ol>
          <div className="rounded-lg bg-green-50 dark:bg-green-950 border border-green-200 dark:border-green-800 p-4">
            <p className="text-sm text-green-900 dark:text-green-100">
              <strong>{t('guides.ideaToRoadmap.step6TimelineTip')}</strong>{' '}
              {t('guides.ideaToRoadmap.step6TimelineDesc')}
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Status Reference */}
      <Card>
        <CardHeader>
          <CardTitle>{t('guides.ideaToRoadmap.statusRefTitle')}</CardTitle>
          <CardDescription>{t('guides.ideaToRoadmap.statusRefDesc')}</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b">
                  <th className="text-left py-2 pr-4">{t('guides.ideaToRoadmap.statusColStatus')}</th>
                  <th className="text-left py-2 pr-4">{t('guides.ideaToRoadmap.statusColPhase')}</th>
                  <th className="text-left py-2 pr-4">{t('guides.ideaToRoadmap.statusColRequires')}</th>
                  <th className="text-left py-2">{t('guides.ideaToRoadmap.statusColDescription')}</th>
                </tr>
              </thead>
              <tbody className="divide-y">
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-gray-500">IDEA</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phasePreCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresNothing')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusIdeaDesc')}</td>
                </tr>
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-blue-400">DRAFT</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phasePreCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresNothing')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusDraftDesc')}</td>
                </tr>
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-blue-600">SHAPED</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phasePreCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresAppetite')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusShapedDesc')}</td>
                </tr>
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-purple-500">PENDING</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phaseInCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresCycle')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusPendingDesc')}</td>
                </tr>
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-yellow-500">STARTED</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phaseInCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresCycle')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusStartedDesc')}</td>
                </tr>
                <tr>
                  <td className="py-2 pr-4"><Badge className="bg-green-500">DONE</Badge></td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.phaseInCycle')}</td>
                  <td className="py-2 pr-4">{t('guides.ideaToRoadmap.requiresCycle')}</td>
                  <td className="py-2">{t('guides.ideaToRoadmap.statusDoneDesc')}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      {/* Related Guides */}
      <Card>
        <CardHeader>
          <CardTitle>{t('guides.ideaToRoadmap.relatedTitle')}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-3 sm:grid-cols-2">
            <Link
              to="/help/betting-meeting"
              className="flex items-center gap-3 rounded-lg border p-3 hover:bg-muted/50 transition-colors"
            >
              <Dices className="h-5 w-5 text-purple-500" />
              <div>
                <p className="font-medium text-sm">{t('helpGuides.bettingMeeting')}</p>
                <p className="text-xs text-muted-foreground">{t('helpGuides.bettingMeetingDesc')}</p>
              </div>
            </Link>
            <Link
              to="/help/cycle-setup"
              className="flex items-center gap-3 rounded-lg border p-3 hover:bg-muted/50 transition-colors"
            >
              <Repeat className="h-5 w-5 text-cyan-500" />
              <div>
                <p className="font-medium text-sm">{t('helpGuides.cycleSetup')}</p>
                <p className="text-xs text-muted-foreground">{t('helpGuides.cycleSetupDesc')}</p>
              </div>
            </Link>
            <Link
              to="/help/hill-charts"
              className="flex items-center gap-3 rounded-lg border p-3 hover:bg-muted/50 transition-colors"
            >
              <Target className="h-5 w-5 text-green-500" />
              <div>
                <p className="font-medium text-sm">{t('helpGuides.hillCharts')}</p>
                <p className="text-xs text-muted-foreground">{t('helpGuides.hillChartsDesc')}</p>
              </div>
            </Link>
            <Link
              to="/help/circuit-breaker"
              className="flex items-center gap-3 rounded-lg border p-3 hover:bg-muted/50 transition-colors"
            >
              <Zap className="h-5 w-5 text-amber-500" />
              <div>
                <p className="font-medium text-sm">{t('helpGuides.circuitBreaker')}</p>
                <p className="text-xs text-muted-foreground">{t('helpGuides.circuitBreakerDesc')}</p>
              </div>
            </Link>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
