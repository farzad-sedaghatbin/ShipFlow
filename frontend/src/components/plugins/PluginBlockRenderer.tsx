import { Link } from 'react-router-dom';
import { ExternalLink } from 'lucide-react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Card, CardContent } from '@/components/ui/card';
import { Markdown } from '@/components/ui/markdown';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { PluginBlock } from '@/services/pluginService';

const ALERT_VARIANT = {
  info: 'info',
  success: 'success',
  warning: 'warning',
  error: 'destructive',
} as const;

/**
 * Renders one plugin block with ShipFlow's own components. Plugins supply data only, so nothing
 * here executes plugin code; markdown goes through the sanitising {@link Markdown} component and
 * link targets were already restricted by the backend to in-app paths and https URLs.
 */
export function PluginBlockRenderer({ block }: { block: PluginBlock }) {
  switch (block.type) {
    case 'MARKDOWN':
      return <Markdown content={block.data.text} />;

    case 'ALERT':
      return (
        <Alert variant={ALERT_VARIANT[block.data.level] ?? 'info'}>
          <AlertDescription>{block.data.text}</AlertDescription>
        </Alert>
      );

    case 'STAT_CARDS':
      return (
        <div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {block.data.stats.map((stat, i) => (
            <Card key={`${stat.label}-${i}`}>
              <CardContent className="p-4">
                <p className="text-sm text-muted-foreground">{stat.label}</p>
                <p className="text-2xl font-semibold break-words">{stat.value}</p>
                {stat.hint && <p className="mt-1 text-xs text-muted-foreground">{stat.hint}</p>}
              </CardContent>
            </Card>
          ))}
        </div>
      );

    case 'TABLE':
      return (
        <Card>
          <CardContent className="p-0">
            <Table>
              <TableHeader>
                <TableRow>
                  {block.data.columns.map((col, i) => (
                    <TableHead key={`${col}-${i}`}>{col}</TableHead>
                  ))}
                </TableRow>
              </TableHeader>
              <TableBody>
                {block.data.rows.map((row, r) => (
                  <TableRow key={r}>
                    {row.map((cell, c) => (
                      <TableCell key={c}>{cell}</TableCell>
                    ))}
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      );

    case 'LINK_LIST':
      return (
        <ul className="flex flex-col gap-2">
          {block.data.links.map((link, i) => (
            <li key={`${link.target}-${i}`}>
              {link.target.startsWith('/') ? (
                <Link to={link.target} className="text-primary underline-offset-4 hover:underline">
                  {link.label}
                </Link>
              ) : (
                <a
                  href={link.target}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center gap-1 text-primary underline-offset-4 hover:underline"
                >
                  {link.label}
                  <ExternalLink className="h-3.5 w-3.5" aria-hidden="true" />
                </a>
              )}
            </li>
          ))}
        </ul>
      );

    default:
      return null;
  }
}
