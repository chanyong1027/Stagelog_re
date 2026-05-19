import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSignup } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import { SignupRequest } from '../../types/auth.types';
import { ROUTES } from '../../utils/constants';

/**
 * 회원가입 페이지 - Neon Night 테마
 * 이메일/비밀번호/닉네임 + 단일 통합 동의 체크박스
 */
const SignupPage: React.FC = () => {
  const [formData, setFormData] = useState<SignupRequest>({
    email: '',
    password: '',
    nickname: '',
    agreedToTerms: false,
  });

  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [errors, setErrors] = useState<Partial<Record<keyof SignupRequest | 'passwordConfirm', string>>>({});

  const { mutate: signup, isPending } = useSignup();

  // 입력 진행률 계산
  const getProgress = () => {
    let filled = 0;
    if (formData.email) filled++;
    if (formData.nickname) filled++;
    if (formData.password) filled++;
    if (passwordConfirm) filled++;
    if (formData.agreedToTerms) filled++;
    return (filled / 5) * 100;
  };

  // 유효성 검사
  const validateForm = (): boolean => {
    const newErrors: Partial<Record<keyof SignupRequest | 'passwordConfirm', string>> = {};

    // 이메일
    if (!formData.email.trim()) {
      newErrors.email = '이메일은 필수입니다.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
      newErrors.email = '올바른 이메일 형식이 아닙니다.';
    }

    // 비밀번호
    if (!formData.password.trim()) {
      newErrors.password = '비밀번호는 필수입니다.';
    } else if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/.test(formData.password)) {
      newErrors.password = '비밀번호는 8-20자의 영문, 숫자, 특수문자를 포함해야 합니다.';
    }

    // 비밀번호 확인
    if (formData.password !== passwordConfirm) {
      newErrors.passwordConfirm = '비밀번호가 일치하지 않습니다.';
    }

    // 닉네임
    if (!formData.nickname.trim()) {
      newErrors.nickname = '닉네임은 필수입니다.';
    } else if (!/^[가-힣a-zA-Z0-9_]{2,20}$/.test(formData.nickname)) {
      newErrors.nickname = '닉네임은 2-20자의 한글, 영문, 숫자, 언더스코어만 사용 가능합니다.';
    }

    // 약관 동의
    if (!formData.agreedToTerms) {
      newErrors.agreedToTerms = '만 14세 이상이며 약관 및 개인정보 처리방침에 동의해주세요.';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!validateForm()) return;
    signup(formData);
  };

  const handleChange = (field: 'email' | 'password' | 'nickname') => (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    setFormData({ ...formData, [field]: e.target.value });
    if (errors[field]) {
      setErrors({ ...errors, [field]: undefined });
    }
  };

  const handlePasswordConfirmChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPasswordConfirm(e.target.value);
    if (errors.passwordConfirm) {
      setErrors({ ...errors, passwordConfirm: undefined });
    }
  };

  const handleAgreedToTermsChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, agreedToTerms: e.target.checked });
    if (errors.agreedToTerms) {
      setErrors({ ...errors, agreedToTerms: undefined });
    }
  };

  return (
    <div className="min-h-screen bg-bg-deep flex items-center justify-center px-4 py-12 relative overflow-hidden">
      {/* 배경 장식 */}
      <div className="absolute inset-0 bg-mesh" />

      <div className="absolute top-0 left-0 w-[500px] h-[500px] bg-accent/15 rounded-full blur-[100px] -translate-x-1/2 -translate-y-1/2" />
      <div className="absolute bottom-0 right-0 w-[600px] h-[600px] bg-primary/20 rounded-full blur-[120px] translate-x-1/3 translate-y-1/3" />
      <div className="absolute top-1/2 left-1/2 w-[400px] h-[400px] bg-secondary/10 rounded-full blur-[80px] -translate-x-1/2 -translate-y-1/2" />

      <div className="noise-overlay" />

      <div className="relative w-full max-w-md animate-scale-in">
        <div className="absolute -inset-1 bg-gradient-to-r from-accent/20 via-primary/20 to-accent/20 rounded-3xl blur-xl opacity-50" />

        <div className="relative glass-strong rounded-3xl p-8 md:p-10">
          <div className="absolute top-0 left-0 right-0 h-1 bg-bg-surface rounded-t-3xl overflow-hidden">
            <div
              className="h-full bg-gradient-to-r from-primary via-accent to-primary transition-all duration-500 ease-out"
              style={{ width: `${getProgress()}%` }}
            />
          </div>

          <div className="text-center mb-8 mt-2">
            <Link to={ROUTES.HOME} className="inline-block group">
              <h1 className="text-3xl font-extrabold tracking-tight mb-2">
                <span className="text-gradient glow-text-primary group-hover:glow-text-accent transition-all duration-500">
                  Stagelog
                </span>
              </h1>
            </Link>
            <p className="text-text-secondary text-sm mt-2">새로운 계정을 만들어보세요</p>
            <div className="flex justify-center items-center gap-2 mt-3">
              <span className="text-xs text-text-muted">진행률</span>
              <span className="text-xs font-semibold text-primary">{Math.round(getProgress())}%</span>
            </div>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <Input
              label="이메일"
              type="email"
              placeholder="example@email.com"
              value={formData.email}
              onChange={handleChange('email')}
              error={errors.email}
              required
              disabled={isPending}
            />

            <Input
              label="닉네임"
              type="text"
              placeholder="닉네임 (2-20자)"
              value={formData.nickname}
              onChange={handleChange('nickname')}
              error={errors.nickname}
              helperText="한글, 영문, 숫자, 언더스코어 사용 가능"
              required
              disabled={isPending}
            />

            <Input
              label="비밀번호"
              type="password"
              placeholder="비밀번호 (8-20자)"
              value={formData.password}
              onChange={handleChange('password')}
              error={errors.password}
              helperText="영문, 숫자, 특수문자 포함"
              required
              disabled={isPending}
            />

            <Input
              label="비밀번호 확인"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              value={passwordConfirm}
              onChange={handlePasswordConfirmChange}
              error={errors.passwordConfirm}
              required
              disabled={isPending}
            />

            {/* 통합 동의 체크박스 */}
            <div>
              <label className="flex items-start gap-2 text-sm text-text-secondary cursor-pointer">
                <input
                  type="checkbox"
                  checked={formData.agreedToTerms}
                  onChange={handleAgreedToTermsChange}
                  disabled={isPending}
                  className="mt-1 w-4 h-4 accent-primary"
                />
                <span>만 14세 이상이며 약관 및 개인정보 처리방침에 동의합니다</span>
              </label>
              {errors.agreedToTerms && (
                <p className="text-xs text-red-400 mt-1">{errors.agreedToTerms}</p>
              )}
            </div>

            <Button
              type="submit"
              className="w-full mt-6"
              size="lg"
              loading={isPending}
              disabled={isPending}
            >
              회원가입
            </Button>
          </form>

          <div className="flex items-center gap-4 my-6">
            <div className="flex-1 h-px bg-border" />
            <span className="text-text-muted text-xs">또는</span>
            <div className="flex-1 h-px bg-border" />
          </div>

          <div className="text-center">
            <p className="text-sm text-text-secondary">
              이미 계정이 있으신가요?{' '}
              <Link
                to={ROUTES.LOGIN}
                className="font-semibold text-accent hover:text-accent-light transition-colors"
              >
                로그인
              </Link>
            </p>
          </div>

          <div className="flex justify-center items-center gap-2 mt-6">
            <div className="w-8 h-px bg-gradient-to-r from-transparent to-accent/40" />
            <div className="w-1.5 h-1.5 rounded-full bg-accent/60" />
            <div className="w-8 h-px bg-gradient-to-l from-transparent to-primary/40" />
          </div>
        </div>
      </div>

      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-border to-transparent" />
    </div>
  );
};

export default SignupPage;
