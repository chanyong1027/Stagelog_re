import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import TopNav from '@/components/layout/TopNav';

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <TopNav />
    </MemoryRouter>,
  );

describe('TopNav (데스크톱)', () => {
  it('브랜드 + 4개 주요 링크를 href와 함께 렌더한다', () => {
    renderAt('/');

    expect(screen.getByRole('navigation', { name: '주요 네비게이션' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Stagelog' })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /발견/ })).toHaveAttribute('href', '/');
    expect(screen.getByRole('link', { name: /공연/ })).toHaveAttribute('href', '/shows');
    expect(screen.getByRole('link', { name: /아티스트/ })).toHaveAttribute('href', '/artists');
    expect(screen.getByRole('link', { name: /일기장/ })).toHaveAttribute('href', '/journal');
  });

  it('로그인/가입 진입점을 제공한다', () => {
    renderAt('/');
    expect(screen.getByRole('link', { name: '로그인' })).toHaveAttribute('href', '/login');
    expect(screen.getByRole('link', { name: '가입하기' })).toHaveAttribute('href', '/signup');
  });

  it('현재 탭에 aria-current=page를 표시한다', () => {
    renderAt('/artists');
    expect(screen.getByRole('link', { name: /아티스트/ })).toHaveAttribute('aria-current', 'page');
  });

  it('인증 화면(회원가입)에서는 렌더하지 않는다', () => {
    renderAt('/signup');
    expect(screen.queryByRole('navigation', { name: '주요 네비게이션' })).not.toBeInTheDocument();
  });
});
