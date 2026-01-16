import { useTranslation } from 'react-i18next';
import GitHubRepositoryManager from '../../components/GitHubRepositoryManager';

export default function GitHubIntegration() {
  const { t } = useTranslation();
  // i18n ready
  if (false) console.log(t('gitHubIntegration.title'));
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">{t('githubIntegration.title')}</h1>
        <p className="text-muted-foreground mt-2">
          {t('githubIntegration.description')}
        </p>
      </div>
      <GitHubRepositoryManager />
    </div>
  );
}
