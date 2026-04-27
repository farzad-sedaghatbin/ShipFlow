import React, { useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Search, Loader2, X, Bot, Sparkles, AlertCircle } from 'lucide-react';
import { Card, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { helpGuideService, HelpAIResponse } from '@/services/helpGuideService';
import ReactMarkdown from 'react-markdown';

export default function HelpSearch() {
    const { t } = useTranslation();
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(false);
    const [response, setResponse] = useState<HelpAIResponse | null>(null);
    const [error, setError] = useState<string | null>(null);
    const inputRef = useRef<HTMLInputElement>(null);

    const suggestedQuestions = [
        t('helpGuides.suggestedQ1', 'How do I create a new cycle?'),
        t('helpGuides.suggestedQ2', 'How do I run a betting meeting?'),
        t('helpGuides.suggestedQ3', 'How do I use hill charts?'),
        t('helpGuides.suggestedQ4', 'How do I connect an external service to ShipFlow?'),
        t('helpGuides.suggestedQ5', 'How do inbound webhooks work?'),
    ];

    const handleSubmit = async (e?: React.FormEvent) => {
        e?.preventDefault();
        if (!query.trim() || loading) return;

        setLoading(true);
        setError(null);
        setResponse(null);

        try {
            const result = await helpGuideService.ask(query.trim());
            setResponse(result.data);
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : 'Something went wrong';
            setError(message);
        } finally {
            setLoading(false);
        }
    };

    const handleSuggestionClick = (question: string) => {
        setQuery(question);
        // Submit after setting the query
        setLoading(true);
        setError(null);
        setResponse(null);

        helpGuideService
            .ask(question.trim())
            .then((result) => setResponse(result.data))
            .catch((err: unknown) => {
                const message = err instanceof Error ? err.message : 'Something went wrong';
                setError(message);
            })
            .finally(() => setLoading(false));
    };

    const handleClear = () => {
        setQuery('');
        setResponse(null);
        setError(null);
        inputRef.current?.focus();
    };

    return (
        <Card className="border-primary/20 bg-gradient-to-r from-primary/5 via-transparent to-primary/5">
            <CardContent className="pt-6 space-y-4">
                {/* Search Bar */}
                <form onSubmit={handleSubmit} className="flex gap-2">
                    <div className="relative flex-1">
                        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                        <Input
                            ref={inputRef}
                            value={query}
                            onChange={(e) => setQuery(e.target.value)}
                            placeholder={t(
                                'helpGuides.searchPlaceholder',
                                'Ask how to do something in ShipFlow…'
                            )}
                            className="pl-9 pr-8"
                            disabled={loading}
                        />
                        {query && (
                            <button
                                type="button"
                                onClick={handleClear}
                                className="absolute right-2 top-1/2 -translate-y-1/2 p-1 rounded hover:bg-muted"
                            >
                                <X className="h-3.5 w-3.5 text-muted-foreground" />
                            </button>
                        )}
                    </div>
                    <Button type="submit" disabled={loading || !query.trim()} size="default">
                        {loading ? (
                            <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                            <Sparkles className="h-4 w-4" />
                        )}
                        <span className="ml-1.5">{t('helpGuides.searchButton', 'Ask')}</span>
                    </Button>
                </form>

                {/* Suggested Questions — show only when there's no response */}
                {!response && !loading && !error && (
                    <div className="flex flex-wrap gap-2">
                        {suggestedQuestions.map((q) => (
                            <button
                                key={q}
                                onClick={() => handleSuggestionClick(q)}
                                className="text-xs px-3 py-1.5 rounded-full border border-border bg-background hover:bg-accent hover:text-accent-foreground transition-colors"
                            >
                                {q}
                            </button>
                        ))}
                    </div>
                )}

                {/* Loading Indicator */}
                {loading && (
                    <div className="flex items-center gap-2 text-sm text-muted-foreground py-4">
                        <Loader2 className="h-4 w-4 animate-spin" />
                        {t('helpGuides.aiThinking', 'Thinking…')}
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="flex items-start gap-2 text-sm text-destructive bg-destructive/10 rounded-lg p-3">
                        <AlertCircle className="h-4 w-4 mt-0.5 shrink-0" />
                        <span>{error}</span>
                    </div>
                )}

                {/* AI Answer */}
                {response?.answer && (
                    <div className="space-y-3">
                        <div className="flex items-center gap-2 text-sm font-medium">
                            <Bot className="h-4 w-4 text-primary" />
                            {t('helpGuides.aiAnswerTitle', 'AI Answer')}
                        </div>
                        <div className="rounded-lg border bg-card p-4 prose prose-sm dark:prose-invert max-w-none">
                            <ReactMarkdown>{response.answer}</ReactMarkdown>
                        </div>

                        {/* Follow-ups */}
                        {response.suggestedFollowUps && response.suggestedFollowUps.length > 0 && (
                            <div className="flex flex-wrap gap-2 pt-1">
                                {response.suggestedFollowUps.map((followUp) => (
                                    <button
                                        key={followUp}
                                        onClick={() => handleSuggestionClick(followUp)}
                                        className="text-xs px-3 py-1.5 rounded-full border border-primary/30 text-primary hover:bg-primary/10 transition-colors"
                                    >
                                        {followUp}
                                    </button>
                                ))}
                            </div>
                        )}

                        {/* Disclaimer */}
                        <p className="text-xs text-muted-foreground">
                            {t(
                                'helpGuides.aiDisclaimerText',
                                'AI answers are generated from ShipFlow documentation and may not be 100% accurate.'
                            )}
                        </p>
                    </div>
                )}
            </CardContent>
        </Card>
    );
}
