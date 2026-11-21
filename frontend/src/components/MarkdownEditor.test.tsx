import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import MarkdownEditor from './MarkdownEditor';

vi.mock('react-i18next', () => ({
  useTranslation: () => ({ t: (_k: string, d?: string) => d ?? _k }),
}));

describe('MarkdownEditor table button', () => {
  it('inserts a GFM table at the cursor', () => {
    const onChange = vi.fn();
    render(<MarkdownEditor value="Intro" onChange={onChange} />);

    fireEvent.click(screen.getByRole('button', { name: 'Insert table' }));

    const inserted = onChange.mock.calls[0][0] as string;
    expect(inserted).toContain('| Column 1 | Column 2 | Column 3 |');
    expect(inserted).toContain('| --- | --- | --- |');
    // blank line before the header row, or the table would merge into the paragraph
    expect(inserted).toMatch(/\n\n\| Column 1/);
  });
});
