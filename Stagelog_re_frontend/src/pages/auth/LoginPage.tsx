import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useLogin } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { LoginRequest } from '../../types/auth.types';
import { ROUTES, API_ENDPOINTS } from '../../utils/constants';

/**
 * 로그인 페이지 - Neon Night 테마
 * 풀스크린 다크 배경 + 글래스 카드 + 네온 악센트
 */
const LoginPage: React.FC = () => {
  const [formData, setFormData] = useState<LoginRequest>({
    email: '',
    password: '',
  });
  const [loginError, setLoginError] = useState<string>('');

  const { mutate: login, isPending } = useLogin();

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(''); // 에러 초기화

    if (!formData.email.trim()) {
      setLoginError('이메일을 입력해주세요.');
      return;
    }
    if (!formData.password.trim()) {
      setLoginError('비밀번호를 입력해주세요.');
      return;
    }

    login(formData, {
      onError: () => {
        setLoginError('이메일/비밀번호 정보가 일치하지 않습니다.');
      },
    });
  };

  const handleChange = (field: keyof LoginRequest) => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData({ ...formData, [field]: e.target.value });
    if (loginError) setLoginError(''); // 입력 시 에러 클리어
  };

  const handleKakaoLogin = () => {
    const baseUrl = import.meta.env.VITE_API_BASE_URL;
    window.location.href = `${baseUrl}${API_ENDPOINTS.AUTH.OAUTH2_KAKAO}`;
  };

  return (
    <div className="min-h-screen bg-bg-deep flex items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* 배경 장식 */}
      <div className="absolute inset-0 bg-mesh" />

      {/* 글로우 오브 - 좌상단 */}
      <div className="absolute top-0 left-0 w-[600px] h-[600px] bg-primary/20 rounded-full blur-[120px] -translate-x-1/2 -translate-y-1/2" />

      {/* 글로우 오브 - 우하단 */}
      <div className="absolute bottom-0 right-0 w-[500px] h-[500px] bg-accent/15 rounded-full blur-[100px] translate-x-1/3 translate-y-1/3" />

      {/* 노이즈 오버레이 */}
      <div className="noise-overlay" />

      {/* 로그인 카드 */}
      <div className="relative w-full max-w-md animate-scale-in">
        {/* 카드 글로우 효과 */}
        <div className="absolute -inset-1 bg-gradient-to-r from-primary/20 via-accent/20 to-primary/20 rounded-3xl blur-xl opacity-50" />

        {/* 카드 본체 */}
        <div className="relative glass-strong rounded-3xl p-8 md:p-10">
          {/* 상단 그라데이션 라인 */}
          <div className="absolute top-0 left-8 right-8 h-px bg-gradient-to-r from-transparent via-primary/50 to-transparent" />

          {/* 로고 & 헤더 */}
          <div className="text-center mb-10">
            <Link to={ROUTES.HOME} className="inline-block group">
              <h1 className="text-4xl font-extrabold tracking-tight mb-2">
                <span className="text-gradient glow-text-primary group-hover:glow-text-accent transition-all duration-500">
                  Stagelog
                </span>
              </h1>
            </Link>
            <p className="text-text-secondary text-sm mt-3">
              공연의 순간을 기록하세요
            </p>
          </div>

          {/* 로그인 폼 */}
          <form onSubmit={handleSubmit} className="space-y-6">
            <Input
              label="이메일"
              type="email"
              placeholder="example@email.com"
              value={formData.email}
              onChange={handleChange('email')}
              required
              disabled={isPending}
            />

            <Input
              label="비밀번호"
              type="password"
              placeholder="비밀번호를 입력하세요"
              value={formData.password}
              onChange={handleChange('password')}
              required
              disabled={isPending}
            />

            {/* 인라인 에러 메시지 */}
            {loginError && (
              <div className="flex items-center gap-2 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
                <svg className="w-5 h-5 text-red-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <p className="text-sm text-red-400">{loginError}</p>
              </div>
            )}

            <Button
              type="submit"
              className="w-full mt-8"
              size="lg"
              loading={isPending}
              disabled={isPending}
            >
              로그인
            </Button>
          </form>

          {/* 구분선 */}
          <div className="flex items-center gap-4 my-8">
            <div className="flex-1 h-px bg-border" />
            <span className="text-text-muted text-xs">또는</span>
            <div className="flex-1 h-px bg-border" />
          </div>

          {/* 카카오 소셜 로그인 */}
          <button
            type="button"
            onClick={handleKakaoLogin}
            className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-xl font-semibold text-sm transition-all duration-300"
            style={{ backgroundColor: '#FEE500', color: '#191919' }}
          >
            <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path
                fillRule="evenodd"
                clipRule="evenodd"
                d="M9 0.6C4.029 0.6 0 3.713 0 7.55C0 9.944 1.558 12.064 3.932 13.326L2.933 16.898C2.845 17.202 3.195 17.442 3.461 17.268L7.693 14.47C8.122 14.511 8.558 14.533 9 14.533C13.971 14.533 18 11.42 18 7.583C18 3.746 13.971 0.6 9 0.6Z"
                fill="#191919"
              />
            </svg>
            카카오로 시작하기
          </button>

          {/* 회원가입 링크 */}
          <div className="text-center mt-6">
            <p className="text-sm text-text-secondary">
              아직 계정이 없으신가요?{' '}
              <Link
                to={ROUTES.SIGNUP}
                className="font-semibold text-primary hover:text-primary-light transition-colors"
              >
                회원가입
              </Link>
            </p>
          </div>

          {/* 하단 장식 */}
          <div className="flex justify-center items-center gap-2 mt-8">
            <div className="w-8 h-px bg-gradient-to-r from-transparent to-primary/40" />
            <div className="w-1.5 h-1.5 rounded-full bg-primary/60" />
            <div className="w-8 h-px bg-gradient-to-l from-transparent to-accent/40" />
          </div>
        </div>
      </div>

      {/* 바닥 장식 라인 */}
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />
    </div>
  );
};

export default LoginPage;
