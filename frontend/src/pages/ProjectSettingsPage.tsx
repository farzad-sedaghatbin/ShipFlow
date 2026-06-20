import { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ArrowLeft, Users, Sliders } from 'lucide-react';
import { Button } from '../components/ui/button';
import { ProjectMembersTab } from '../components/projectSettings/ProjectMembersTab';
import { CustomFieldsSection } from '../components/CustomFieldsSection';
import { useProjectRole } from '../hooks/useProjectRole';
import { safeParseId } from '../utils/validation';

type Tab = 'members' | 'customFields';

export default function ProjectSettingsPage() {
  const { t } = useTranslation();
  const { projectId: projectIdParam } = useParams<{ projectId: string }>();
  const projectId = safeParseId(projectIdParam);
  const [activeTab, setActiveTab] = useState<Tab>('members');
  const { isManager, loading } = useProjectRole(projectId ?? undefined);

  if (!projectId) {
    return <p className="text-sm text-destructive p-8">{t('projectSettings.noAccess')}</p>;
  }

  const TABS: { id: Tab; label: string; icon: React.ReactNode }[] = [
    { id: 'members', label: t('projectSettings.members'), icon: <Users className="h-4 w-4" /> },
    { id: 'customFields', label: t('projectSettings.customFields'), icon: <Sliders className="h-4 w-4" /> },
  ];

  return (
    <div className="max-w-3xl mx-auto px-4 py-6 space-y-6">
      {/* Back link */}
      <Button variant="ghost" size="sm" asChild className="-ml-2">
        <Link to={`/projects/${projectId}`}>
          <ArrowLeft className="h-4 w-4 mr-1.5" />
          Back to project
        </Link>
      </Button>

      <h1 className="text-2xl font-semibold">{t('projectSettings.title')}</h1>

      {/* Tabs */}
      <div className="flex gap-2 border-b pb-2">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`flex items-center gap-1.5 px-3 py-1.5 text-sm rounded-md transition-colors ${
              activeTab === tab.id
                ? 'bg-primary text-primary-foreground'
                : 'text-muted-foreground hover:text-foreground hover:bg-muted'
            }`}
          >
            {tab.icon}
            {tab.label}
          </button>
        ))}
      </div>

      {/* Content */}
      {loading ? null : activeTab === 'members' ? (
        <ProjectMembersTab projectId={projectId} isManager={isManager} />
      ) : (
        <div className="space-y-2">
          <p className="text-sm text-muted-foreground">
            Custom fields for this project's entities are configured in{' '}
            <Link to="/settings" className="underline">
              Organization Settings → Custom Fields
            </Link>
            .
          </p>
          <CustomFieldsSection entityType="TASK" entityId={0} projectId={projectId} readonly />
        </div>
      )}
    </div>
  );
}
