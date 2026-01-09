import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import rehypeSanitize from 'rehype-sanitize';
import { cn } from '../../lib/utils';

interface MarkdownProps {
  content: string;
  className?: string;
}

/**
 * A component that renders Markdown content with proper formatting.
 * Supports GitHub Flavored Markdown (GFM) and sanitizes HTML for security.
 */
export const Markdown: React.FC<MarkdownProps> = ({ content, className }) => {
  return (
    <div className={cn('markdown-content', className)}>
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}
        rehypePlugins={[rehypeSanitize]}
        components={{
        // Style headings
        h1: ({ node, ...props }) => <h1 className="text-xl font-bold mb-2 mt-4" {...props} />,
        h2: ({ node, ...props }) => <h2 className="text-lg font-semibold mb-2 mt-3" {...props} />,
        h3: ({ node, ...props }) => <h3 className="text-base font-semibold mb-1 mt-2" {...props} />,
        
        // Style paragraphs
        p: ({ node, ...props }) => <p className="mb-2 last:mb-0" {...props} />,
        
        // Style lists
        ul: ({ node, ...props }) => <ul className="list-disc list-inside space-y-1 mb-2" {...props} />,
        ol: ({ node, ...props }) => <ol className="list-decimal list-inside space-y-1 mb-2" {...props} />,
        li: ({ node, ...props }) => <li className="ml-2" {...props} />,
        
        // Style emphasis
        strong: ({ node, ...props }) => <strong className="font-semibold text-foreground" {...props} />,
        em: ({ node, ...props }) => <em className="italic" {...props} />,
        
        // Style code
        code: ({ node, className, children, ...props }) => {
          const match = /language-(\w+)/.exec(className || '');
          const inline = !match;
          
          if (inline) {
            return (
              <code
                className="bg-muted px-1.5 py-0.5 rounded text-sm font-mono"
                {...props}
              >
                {children}
              </code>
            );
          }
          
          return (
            <code
              className={cn('block bg-muted p-3 rounded-lg text-sm font-mono overflow-x-auto', className)}
              {...props}
            >
              {children}
            </code>
          );
        },
        
        // Style blockquotes
        blockquote: ({ node, ...props }) => (
          <blockquote
            className="border-l-4 border-primary pl-4 italic text-muted-foreground my-2"
            {...props}
          />
        ),
        
        // Style links
        a: ({ node, ...props }) => (
          <a
            className="text-primary hover:underline"
            target="_blank"
            rel="noopener noreferrer"
            {...props}
          />
        ),
        
        // Style horizontal rules
        hr: ({ node, ...props }) => <hr className="my-4 border-border" {...props} />,
        
        // Style tables
        table: ({ node, ...props }) => (
          <div className="overflow-x-auto my-2">
            <table className="border-collapse border border-border w-full" {...props} />
          </div>
        ),
        th: ({ node, ...props }) => (
          <th className="border border-border px-3 py-2 bg-muted font-semibold text-left" {...props} />
        ),
        td: ({ node, ...props }) => (
          <td className="border border-border px-3 py-2" {...props} />
        ),
      }}
    >
      {content}
    </ReactMarkdown>
    </div>
  );
};
