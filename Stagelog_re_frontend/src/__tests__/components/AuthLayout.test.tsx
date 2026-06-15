// src/__tests__/components/AuthLayout.test.tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import AuthLayout from '@/components/auth/AuthLayout';

describe('AuthLayout', () => {
  it('제목·부제·children을 렌더한다', () => {
    render(
      <AuthLayout visual={<div data-testid="visual" />} title="다시 오셨네요" subtitle="잔향이 기다려요">
        <button>로그인</button>
      </AuthLayout>
    );
    expect(screen.getByText('다시 오셨네요')).toBeInTheDocument();
    expect(screen.getByText('잔향이 기다려요')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
  });

  it('데스크톱 분할 그리드 클래스를 가진다', () => {
    const { container } = render(
      <AuthLayout visual={<div />} title="t" subtitle="s"><span /></AuthLayout>
    );
    expect(container.firstChild).toHaveClass('lg:grid');
  });
});
