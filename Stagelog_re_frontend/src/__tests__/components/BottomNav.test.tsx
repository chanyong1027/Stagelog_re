import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import BottomNav from '@/components/layout/BottomNav';

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <BottomNav />
    </MemoryRouter>,
  );

describe('BottomNav (모바일 5탭)', () => {
  it('5개 주요 탭을 접근성 라벨/href와 함께 렌더한다', () => {
    renderAt('/');

    expect(screen.getByRole('navigation', { name: '주요 네비게이션' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /발견/ })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /공연/ })).toHaveAttribute('href', '/shows');
    expect(screen.getByRole('link', { name: /아티스트/ })).toHaveAttribute('href', '/artists');
    expect(screen.getByRole('link', { name: /일기장/ })).toHaveAttribute('href', '/journal');
    expect(screen.getByRole('link', { name: /나/ })).toHaveAttribute('href', '/profile');
  });

  it('현재 탭에 aria-current=page를 표시한다', () => {
    renderAt('/shows');
    expect(screen.getByRole('link', { name: /공연/ })).toHaveAttribute('aria-current', 'page');
  });

  it('인증 화면(로그인)에서는 렌더하지 않는다', () => {
    renderAt('/login');
    expect(screen.queryByRole('navigation', { name: '주요 네비게이션' })).not.toBeInTheDocument();
  });
});
