// src/__tests__/pages/auth/SignupPage.test.tsx
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import SignupPage from '@/pages/auth/SignupPage';

const mockSignup = vi.fn();
vi.mock('@/hooks/useAuth', () => ({
  useSignup: () => ({ mutate: mockSignup, isPending: false }),
}));

const renderPage = () => render(<MemoryRouter><SignupPage /></MemoryRouter>);

describe('SignupPage', () => {
  beforeEach(() => mockSignup.mockClear());

  const fillValid = (overrides: Partial<Record<string, string>> = {}) => {
    fireEvent.change(screen.getByLabelText('이메일'), { target: { value: overrides.email ?? 'a@b.com' } });
    fireEvent.change(screen.getByLabelText('닉네임'), { target: { value: overrides.nickname ?? '공연덕후' } });
    fireEvent.change(screen.getByLabelText('비밀번호'), { target: { value: overrides.password ?? 'Pwd!1234' } });
    fireEvent.change(screen.getByLabelText('비밀번호 확인'), { target: { value: overrides.passwordConfirm ?? 'Pwd!1234' } });
  };

  it('이메일·닉네임·비밀번호·비밀번호 확인 입력과 동의 체크박스를 렌더한다', () => {
    renderPage();
    expect(screen.getByLabelText('이메일')).toBeInTheDocument();
    expect(screen.getByLabelText('닉네임')).toBeInTheDocument();
    expect(screen.getByLabelText('비밀번호')).toBeInTheDocument();
    expect(screen.getByLabelText('비밀번호 확인')).toBeInTheDocument();
    expect(screen.getByRole('checkbox')).toBeInTheDocument();
  });

  it('비밀번호와 확인이 다르면 검증 에러를 보이고 signup을 호출하지 않는다', () => {
    renderPage();
    fillValid({ passwordConfirm: 'Different!9' });
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: '가입 완료' }));
    expect(mockSignup).not.toHaveBeenCalled();
    expect(screen.getByText('비밀번호가 일치하지 않아요')).toBeInTheDocument();
  });

  it('동의 안 하면 검증 에러를 보이고 signup을 호출하지 않는다', () => {
    renderPage();
    fillValid();
    fireEvent.click(screen.getByRole('button', { name: '가입 완료' }));
    expect(mockSignup).not.toHaveBeenCalled();
    expect(screen.getByRole('alert')).toHaveTextContent(/동의해주세요/);
  });

  it('모든 값이 유효하면 signup mutation을 호출한다 (passwordConfirm은 페이로드에서 제외)', () => {
    renderPage();
    fillValid();
    fireEvent.click(screen.getByRole('checkbox'));
    fireEvent.click(screen.getByRole('button', { name: '가입 완료' }));
    expect(mockSignup).toHaveBeenCalledWith({
      email: 'a@b.com', password: 'Pwd!1234', nickname: '공연덕후', agreedToTerms: true,
    });
  });
});
