// src/pages/auth/LoginPage.tsx
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useLogin } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import AuthLayout from '../../components/auth/AuthLayout';
import AuthVisualPanel from '../../components/auth/AuthVisualPanel';
import { LoginRequest } from '../../types/auth.types';
import { ROUTES, API_ENDPOINTS } from '../../utils/constants';

/** 로그인 — 중립(Capture) 디자인, 데스크톱 좌우 분할 / 모바일 단일. */
const LoginPage: React.FC = () => {
  const [formData, setFormData] = useState<LoginRequest>({ email: '', password: '' });
  const [loginError, setLoginError] = useState('');
  const { mutate: login, isPending } = useLogin();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError('');
    if (!formData.email.trim()) return setLoginError('이메일을 입력해주세요.');
    if (!formData.password.trim()) return setLoginError('비밀번호를 입력해주세요.');
    login(formData, { onError: () => setLoginError('이메일 또는 비밀번호가 일치하지 않아요') });
  };

  const handleChange = (field: keyof LoginRequest) => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [field]: e.target.value });
    if (loginError) setLoginError('');
  };

  const handleKakaoLogin = () => {
    window.location.href = `${import.meta.env.VITE_API_BASE_URL}${API_ENDPOINTS.AUTH.OAUTH2_KAKAO}`;
  };

  return (
    <AuthLayout
      visual={<AuthVisualPanel quote={<>그날 밤의 잔향을<br />한 줄로 남겨두는 곳</>} subquote="올해 본 공연이 모두 여기 모여 있어요" />}
      title="다시 오셨네요"
      subtitle="무대의 잔향이 기다리고 있어요"
    >
      <form onSubmit={handleSubmit} className="space-y-[18px]">
        <Input label="이메일" name="email" type="email" placeholder="example@email.com"
          value={formData.email} onChange={handleChange('email')} disabled={isPending} autoComplete="email" />
        <Input label="비밀번호" name="password" type="password" placeholder="비밀번호를 입력하세요"
          value={formData.password} onChange={handleChange('password')} disabled={isPending} autoComplete="current-password" />

        {loginError && (
          <p className="flex items-center gap-1.5 text-[12.5px] text-error" role="alert">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" aria-hidden="true">
              <circle cx="12" cy="12" r="9" /><path d="M12 8v5M12 16.5v.01" />
            </svg>
            {loginError}
          </p>
        )}

        <Button type="submit" size="lg" loading={isPending} className="mt-1 w-full">로그인</Button>
      </form>

      <div className="my-5 flex items-center gap-3.5 text-[12.5px] text-capture-fg-muted">
        <span className="h-px flex-1 bg-capture-muted" />또는<span className="h-px flex-1 bg-capture-muted" />
      </div>

      <Button type="button" variant="kakao" size="lg" onClick={handleKakaoLogin} className="w-full">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="#181600" aria-hidden="true">
          <path d="M12 3C6.5 3 2 6.6 2 11c0 2.8 1.9 5.3 4.7 6.7-.2.7-.7 2.6-.8 3-.1.5.2.5.4.4.2-.1 2.6-1.8 3.7-2.5.7.1 1.4.2 2 .2 5.5 0 10-3.6 10-8S17.5 3 12 3z" />
        </svg>
        카카오로 시작하기
      </Button>

      <div className="mt-auto py-8 text-center text-sm text-capture-fg-muted lg:mt-7 lg:py-0">
        아직 회원이 아니세요?{' '}
        <Link to={ROUTES.SIGNUP} className="font-semibold text-capture-fg underline underline-offset-2">가입하기</Link>
      </div>
    </AuthLayout>
  );
};

export default LoginPage;
