import { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import {
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import {
  Activity,
  Users,
  Wrench,
  CheckCircle2,
  XCircle,
  ArrowLeft,
  RefreshCw,
  Clock,
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/card';
import { Badge } from '../../components/ui/badge';
import { Button } from '../../components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '../../components/ui/tabs';
import {
  getMcpUsageSummary,
  getMcpUserUsage,
  getMcpToolUsage,
  getMcpUsageTimeline,
  getMcpRecentLogs,
  McpUsageSummary,
  McpUserUsage,
  McpToolUsage,
  McpUsageTimelinePoint,
  McpUsageLogEntry,
} from '../../services/mcpUsageService';

function formatDate(iso: string | null): string {
  if (!iso) return '—';
  return new Date(iso).toLocaleString();
}

function SuccessBadge({ success }: { success: boolean }) {
  return success ? (
    <Badge variant="default" className="gap-1">
      <CheckCircle2 className="h-3 w-3" />
      OK
    </Badge>
  ) : (
    <Badge variant="destructive" className="gap-1">
      <XCircle className="h-3 w-3" />
      Error
    </Badge>
  );
}

export default function McpUsageReport() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [summary, setSummary] = useState<McpUsageSummary | null>(null);
  const [userUsage, setUserUsage] = useState<McpUserUsage[]>([]);
  const [toolUsage, setToolUsage] = useState<McpToolUsage[]>([]);
  const [timeline, setTimeline] = useState<McpUsageTimelinePoint[]>([]);
  const [recentLogs, setRecentLogs] = useState<McpUsageLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [s, u, tl, tm, r] = await Promise.all([
        getMcpUsageSummary(),
        getMcpUserUsage(),
        getMcpToolUsage(),
        getMcpUsageTimeline(30),
        getMcpRecentLogs(50),
      ]);
      setSummary(s);
      setUserUsage(u);
      setToolUsage(tl);
      setTimeline(tm);
      setRecentLogs(r);
    } catch {
      setError(t('mcpUsage.loadError'));
    } finally {
      setLoading(false);
    }
  }, [t]);

  useEffect(() => { load(); }, [load]);

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <Button variant="ghost" size="sm" onClick={() => navigate(-1)}>
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">{t('mcpUsage.title')}</h1>
            <p className="text-sm text-muted-foreground">{t('mcpUsage.subtitle')}</p>
          </div>
        </div>
        <Button variant="outline" size="sm" onClick={load} disabled={loading}>
          <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
          {t('mcpUsage.refresh')}
        </Button>
      </div>

      {error && (
        <div className="rounded-md border border-destructive/50 bg-destructive/10 p-4 text-sm text-destructive">
          {error}
        </div>
      )}

      {/* Summary cards */}
      {summary && (
        <div className="grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <Activity className="h-3 w-3" />
                {t('mcpUsage.summary.totalCalls')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.totalCalls.toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <CheckCircle2 className="h-3 w-3 text-green-500" />
                {t('mcpUsage.summary.successRate')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.successRate}%</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <XCircle className="h-3 w-3 text-red-500" />
                {t('mcpUsage.summary.failures')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.failureCalls.toLocaleString()}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <Users className="h-3 w-3" />
                {t('mcpUsage.summary.activeUsers')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.activeUsersLast30Days}</p>
              <p className="text-xs text-muted-foreground">{t('mcpUsage.summary.last30Days')}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <Wrench className="h-3 w-3" />
                {t('mcpUsage.summary.uniqueTools')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.uniqueToolsUsed}</p>
            </CardContent>
          </Card>
          <Card>
            <CardHeader className="pb-2">
              <CardTitle className="text-xs font-medium text-muted-foreground flex items-center gap-1">
                <XCircle className="h-3 w-3 text-red-500" />
                {t('mcpUsage.summary.successCalls')}
              </CardTitle>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">{summary.successCalls.toLocaleString()}</p>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Tabs */}
      <Tabs defaultValue="timeline">
        <TabsList className="grid w-full max-w-lg grid-cols-4">
          <TabsTrigger value="timeline">{t('mcpUsage.tabs.timeline')}</TabsTrigger>
          <TabsTrigger value="byUser">{t('mcpUsage.tabs.byUser')}</TabsTrigger>
          <TabsTrigger value="byTool">{t('mcpUsage.tabs.byTool')}</TabsTrigger>
          <TabsTrigger value="recent">{t('mcpUsage.tabs.recent')}</TabsTrigger>
        </TabsList>

        {/* Timeline tab */}
        <TabsContent value="timeline" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('mcpUsage.timeline.title')}</CardTitle>
            </CardHeader>
            <CardContent>
              {timeline.length === 0 ? (
                <p className="text-center py-8 text-muted-foreground">{t('mcpUsage.noData')}</p>
              ) : (
                <ResponsiveContainer width="100%" height={280}>
                  <AreaChart data={timeline}>
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                    <XAxis dataKey="date" tick={{ fontSize: 11 }} />
                    <YAxis tick={{ fontSize: 11 }} />
                    <Tooltip />
                    <Area
                      type="monotone"
                      dataKey="totalCalls"
                      name={t('mcpUsage.timeline.totalCalls')}
                      stroke="hsl(var(--primary))"
                      fill="hsl(var(--primary) / 0.15)"
                    />
                    <Area
                      type="monotone"
                      dataKey="successCalls"
                      name={t('mcpUsage.timeline.successCalls')}
                      stroke="hsl(142 76% 36%)"
                      fill="hsl(142 76% 36% / 0.15)"
                    />
                  </AreaChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* By User tab */}
        <TabsContent value="byUser" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('mcpUsage.byUser.title')}</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              {userUsage.length === 0 ? (
                <p className="text-center py-8 text-muted-foreground">{t('mcpUsage.noData')}</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left">
                      <th className="pb-2 font-medium">{t('mcpUsage.byUser.username')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.byUser.email')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.byUser.totalCalls')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.byUser.successRate')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.byUser.lastSeen')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {userUsage.map((u) => (
                      <tr key={u.username} className="border-b last:border-0 hover:bg-muted/30">
                        <td className="py-2 font-mono">{u.username}</td>
                        <td className="py-2 text-muted-foreground">{u.email}</td>
                        <td className="py-2 text-right font-semibold">{u.totalCalls.toLocaleString()}</td>
                        <td className="py-2 text-right">
                          <span className={u.successRate >= 90 ? 'text-green-600' : u.successRate >= 70 ? 'text-yellow-600' : 'text-red-600'}>
                            {u.successRate}%
                          </span>
                        </td>
                        <td className="py-2 text-muted-foreground">{formatDate(u.lastCalledAt)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* By Tool tab */}
        <TabsContent value="byTool" className="mt-4 space-y-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('mcpUsage.byTool.chartTitle')}</CardTitle>
            </CardHeader>
            <CardContent>
              {toolUsage.length === 0 ? (
                <p className="text-center py-8 text-muted-foreground">{t('mcpUsage.noData')}</p>
              ) : (
                <ResponsiveContainer width="100%" height={Math.max(200, toolUsage.length * 32)}>
                  <BarChart data={toolUsage} layout="vertical">
                    <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                    <XAxis type="number" tick={{ fontSize: 11 }} />
                    <YAxis type="category" dataKey="toolName" tick={{ fontSize: 11 }} width={160} />
                    <Tooltip />
                    <Bar dataKey="totalCalls" name={t('mcpUsage.byTool.totalCalls')} fill="hsl(var(--primary))" />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('mcpUsage.byTool.tableTitle')}</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              {toolUsage.length > 0 && (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left">
                      <th className="pb-2 font-medium">{t('mcpUsage.byTool.toolName')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.byTool.totalCalls')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.byTool.successRate')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.byTool.uniqueUsers')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {toolUsage.map((tool) => (
                      <tr key={tool.toolName} className="border-b last:border-0 hover:bg-muted/30">
                        <td className="py-2 font-mono">{tool.toolName}</td>
                        <td className="py-2 text-right font-semibold">{tool.totalCalls.toLocaleString()}</td>
                        <td className="py-2 text-right">
                          <span className={tool.successRate >= 90 ? 'text-green-600' : tool.successRate >= 70 ? 'text-yellow-600' : 'text-red-600'}>
                            {tool.successRate}%
                          </span>
                        </td>
                        <td className="py-2 text-right">{tool.uniqueUsers}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </CardContent>
          </Card>
        </TabsContent>

        {/* Recent logs tab */}
        <TabsContent value="recent" className="mt-4">
          <Card>
            <CardHeader>
              <CardTitle className="text-base">{t('mcpUsage.recent.title')}</CardTitle>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              {recentLogs.length === 0 ? (
                <p className="text-center py-8 text-muted-foreground">{t('mcpUsage.noData')}</p>
              ) : (
                <table className="w-full text-sm">
                  <thead>
                    <tr className="border-b text-left">
                      <th className="pb-2 font-medium">{t('mcpUsage.recent.calledAt')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.recent.user')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.recent.toolName')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.recent.status')}</th>
                      <th className="pb-2 font-medium">{t('mcpUsage.recent.apiKey')}</th>
                      <th className="pb-2 font-medium text-right">{t('mcpUsage.recent.duration')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {recentLogs.map((entry) => (
                      <tr key={entry.id} className="border-b last:border-0 hover:bg-muted/30">
                        <td className="py-1.5 text-muted-foreground whitespace-nowrap">
                          {formatDate(entry.calledAt)}
                        </td>
                        <td className="py-1.5 font-mono">{entry.username}</td>
                        <td className="py-1.5 font-mono text-xs">{entry.toolName}</td>
                        <td className="py-1.5">
                          <SuccessBadge success={entry.success} />
                          {!entry.success && entry.errorMessage && (
                            <p className="text-xs text-muted-foreground mt-0.5 max-w-xs truncate" title={entry.errorMessage}>
                              {entry.errorMessage}
                            </p>
                          )}
                        </td>
                        <td className="py-1.5 font-mono text-xs text-muted-foreground">
                          {entry.apiKeyPrefix ?? '—'}
                        </td>
                        <td className="py-1.5 text-right text-muted-foreground">
                          {entry.durationMs !== null ? (
                            <span className="flex items-center justify-end gap-1">
                              <Clock className="h-3 w-3" />
                              {entry.durationMs}ms
                            </span>
                          ) : '—'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
