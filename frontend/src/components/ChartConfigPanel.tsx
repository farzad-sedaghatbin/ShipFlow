import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Label } from '../components/ui/label';
import { Input } from '../components/ui/input';
import { Button } from '../components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../components/ui/select';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../components/ui/tabs';

export type ChartType = 'LINE' | 'BAR' | 'PIE' | 'AREA' | 'SCATTER';

export interface ChartConfig {
  chartType: ChartType;
  title?: string;
  xAxisLabel?: string;
  yAxisLabel?: string;
  colors?: string[];
  showLegend?: boolean;
  showGrid?: boolean;
  showDataLabels?: boolean;
  stacked?: boolean;
  animated?: boolean;
}

interface ChartConfigPanelProps {
  initialConfig?: Partial<ChartConfig>;
  onConfigChange: (config: ChartConfig) => void;
  onCancel: () => void;
}

const DEFAULT_COLORS = [
  '#3b82f6', // blue
  '#10b981', // green
  '#f59e0b', // amber
  '#ef4444', // red
  '#8b5cf6', // purple
  '#ec4899', // pink
];

export default function ChartConfigPanel({ 
  initialConfig = {}, 
  onConfigChange, 
  onCancel 
}: ChartConfigPanelProps) {
  const { t } = useTranslation();
  const [chartType, setChartType] = useState<ChartType>(initialConfig.chartType || 'LINE');
  const [title, setTitle] = useState(initialConfig.title || '');
  const [xAxisLabel, setXAxisLabel] = useState(initialConfig.xAxisLabel || '');
  const [yAxisLabel, setYAxisLabel] = useState(initialConfig.yAxisLabel || '');
  const [colors, setColors] = useState<string[]>(initialConfig.colors || DEFAULT_COLORS);
  const [showLegend, setShowLegend] = useState(initialConfig.showLegend !== false);
  const [showGrid, setShowGrid] = useState(initialConfig.showGrid !== false);
  const [showDataLabels, setShowDataLabels] = useState(initialConfig.showDataLabels || false);
  const [stacked, setStacked] = useState(initialConfig.stacked || false);
  const [animated, setAnimated] = useState(initialConfig.animated !== false);

  const handleSave = () => {
    const config: ChartConfig = {
      chartType,
      title: title || undefined,
      xAxisLabel: xAxisLabel || undefined,
      yAxisLabel: yAxisLabel || undefined,
      colors,
      showLegend,
      showGrid,
      showDataLabels,
      stacked,
      animated,
    };
    onConfigChange(config);
  };

  const updateColor = (index: number, color: string) => {
    const newColors = [...colors];
    newColors[index] = color;
    setColors(newColors);
  };

  return (
    <div className="space-y-6">
      <Tabs defaultValue="basic">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="basic">{t('chartConfig.basicTab')}</TabsTrigger>
          <TabsTrigger value="styling">{t('chartConfig.stylingTab')}</TabsTrigger>
          <TabsTrigger value="advanced">{t('chartConfig.advancedTab')}</TabsTrigger>
        </TabsList>

        <TabsContent value="basic" className="space-y-4">
          <div>
            <Label>{t('chartConfig.chartType')}</Label>
            <Select value={chartType} onValueChange={(value) => setChartType(value as ChartType)}>
              <SelectTrigger>
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="LINE">{t('chartConfig.lineChart')}</SelectItem>
                <SelectItem value="BAR">{t('chartConfig.barChart')}</SelectItem>
                <SelectItem value="PIE">{t('chartConfig.pieChart')}</SelectItem>
                <SelectItem value="AREA">{t('chartConfig.areaChart')}</SelectItem>
                <SelectItem value="SCATTER">{t('chartConfig.scatterPlot')}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div>
            <Label>{t('chartConfig.chartTitle')}</Label>
            <Input
              placeholder={t('chartConfig.chartTitlePlaceholder')}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
            />
          </div>

          {chartType !== 'PIE' && (
            <>
              <div>
                <Label>{t('chartConfig.xAxisLabel')}</Label>
                <Input
                  placeholder={t('chartConfig.xAxisPlaceholder')}
                  value={xAxisLabel}
                  onChange={(e) => setXAxisLabel(e.target.value)}
                />
              </div>

              <div>
                <Label>{t('chartConfig.yAxisLabel')}</Label>
                <Input
                  placeholder={t('chartConfig.yAxisPlaceholder')}
                  value={yAxisLabel}
                  onChange={(e) => setYAxisLabel(e.target.value)}
                />
              </div>
            </>
          )}
        </TabsContent>

        <TabsContent value="styling" className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Color Palette</CardTitle>
              <CardDescription>Choose colors for your chart series</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-3 gap-3">
                {colors.map((color, index) => (
                  <div key={index} className="flex items-center gap-2">
                    <input
                      type="color"
                      value={color}
                      onChange={(e) => updateColor(index, e.target.value)}
                      className="h-10 w-full rounded border cursor-pointer"
                    />
                    <span className="text-xs text-muted-foreground">#{index + 1}</span>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>

          <div className="flex items-center justify-between">
            <Label>{t('chartConfig.showLegend')}</Label>
            <input
              type="checkbox"
              checked={showLegend}
              onChange={(e) => setShowLegend(e.target.checked)}
              className="h-4 w-4 cursor-pointer"
            />
          </div>

          <div className="flex items-center justify-between">
            <Label>{t('chartConfig.showGrid')}</Label>
            <input
              type="checkbox"
              checked={showGrid}
              onChange={(e) => setShowGrid(e.target.checked)}
              className="h-4 w-4 cursor-pointer"
            />
          </div>

          <div className="flex items-center justify-between">
            <Label>{t('chartConfig.showDataLabels')}</Label>
            <input
              type="checkbox"
              checked={showDataLabels}
              onChange={(e) => setShowDataLabels(e.target.checked)}
              className="h-4 w-4 cursor-pointer"
            />
          </div>
        </TabsContent>

        <TabsContent value="advanced" className="space-y-4">
          {(chartType === 'BAR' || chartType === 'AREA') && (
            <div className="flex items-center justify-between">
              <div>
                <Label>{t('chartConfig.stacked')}</Label>
                <p className="text-sm text-muted-foreground">Stack multiple series on top of each other</p>
              </div>
              <input
                type="checkbox"
                checked={stacked}
                onChange={(e) => setStacked(e.target.checked)}
                className="h-4 w-4 cursor-pointer"
              />
            </div>
          )}

          <div className="flex items-center justify-between">
            <div>
              <Label>{t('chartConfig.animated')}</Label>
              <p className="text-sm text-muted-foreground">Enable smooth animations</p>
            </div>
            <input
              type="checkbox"
              checked={animated}
              onChange={(e) => setAnimated(e.target.checked)}
              className="h-4 w-4 cursor-pointer"
            />
          </div>

          <Card>
            <CardHeader>
              <CardTitle className="text-base">Preview</CardTitle>
              <CardDescription>Chart configuration preview</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="rounded border p-4 bg-muted/30">
                <div className="text-sm space-y-1">
                  <div><strong>Type:</strong> {chartType}</div>
                  {title && <div><strong>Title:</strong> {title}</div>}
                  {xAxisLabel && <div><strong>X-Axis:</strong> {xAxisLabel}</div>}
                  {yAxisLabel && <div><strong>Y-Axis:</strong> {yAxisLabel}</div>}
                  <div><strong>Colors:</strong> {colors.length} defined</div>
                  <div><strong>Legend:</strong> {showLegend ? 'Visible' : 'Hidden'}</div>
                  <div><strong>Grid:</strong> {showGrid ? 'Visible' : 'Hidden'}</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="flex justify-end gap-2 pt-4 border-t">
        <Button variant="outline" onClick={onCancel}>
          {t('chartConfig.cancel')}
        </Button>
        <Button onClick={handleSave}>
          {t('chartConfig.save')}
        </Button>
      </div>
    </div>
  );
}
