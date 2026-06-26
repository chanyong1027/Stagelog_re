import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Wordmark from '@/components/brand/Wordmark';

describe('Wordmark', () => {
  it('Stagelog 워드마크 텍스트를 렌더한다', () => {
    render(<Wordmark />);
    expect(screen.getByText('Stagelog')).toBeInTheDocument();
  });

  it('별 마크는 장식이므로 aria-hidden이다', () => {
    const { container } = render(<Wordmark />);
    expect(container.querySelector('svg')).toHaveAttribute('aria-hidden', 'true');
  });
});
