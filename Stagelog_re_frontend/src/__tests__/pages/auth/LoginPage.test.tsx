// src/__tests__/pages/auth/LoginPage.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import LoginPage from '@/pages/auth/LoginPage';

const mockLogin = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useLogin: () => ({ mutate: mockLogin, isPending: false }),
}));

const renderPage = () => render(<MemoryRouter><LoginPage /></MemoryRouter>);

describe('LoginPage', () => {
  beforeEach(() => mockLogin.mockClear());

  it('이메일/비밀번호 입력과 로그인·카카오 버튼을 렌더한다', () => {
    renderPage();
    expect(screen.getByLabelText('이메일')).toBeInTheDocument();
    expect(screen.getByLabelText('비밀번호')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '로그인' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /카카오/ })).toBeInTheDocument();
  });

  it('빈 값으로 제출하면 검증 에러를 보이고 login을 호출하지 않는다', () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));
    expect(screen.getByText('이메일을 입력해주세요.')).toBeInTheDocument();
    expect(mockLogin).not.toHaveBeenCalled();
  });

  it('정상 입력 시 login mutation을 호출한다', () => {
    renderPage();
    fireEvent.change(screen.getByLabelText('이메일'), { target: { value: 'a@b.com' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: 'Pwd!1234' } });
    fireEvent.click(screen.getByRole('button', { name: '로그인' }));
    expect(mockLogin).toHaveBeenCalledWith(
      { email: 'a@b.com', password: 'Pwd!1234' },
      expect.objectContaining({ onError: expect.any(Function) })
    );
  });
});
