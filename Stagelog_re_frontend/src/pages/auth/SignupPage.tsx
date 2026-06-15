// src/pages/auth/SignupPage.tsx
import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useSignup } from '../../hooks/useAuth';
import Button from '../../components/common/Button';
import Input from '../../components/common/Input';
import AuthLayout from '../../components/auth/AuthLayout';
import AuthVisualPanel from '../../components/auth/AuthVisualPanel';
import { SignupRequest } from '../../types/auth.types';
import { ROUTES, VALIDATION_REGEX } from '../../utils/constants';

// passwordConfirm은 SignupRequest에 없는 화면 전용 필드이므로 에러 키를 별도 union으로 확장
type ErrorKey = keyof SignupRequest | 'passwordConfirm';
type FieldErrors = Partial<Record<ErrorKey, string>>;

/** 회원가입 — 이메일/닉네임/비밀번호/비밀번호 확인 + 단일 동의. 중립(Capture)·반응형 분할. */
const SignupPage: React.FC = () => {
  const [formData, setFormData] = useState<SignupRequest>({ email: '', password: '', nickname: '', agreedToTerms: false });
  const [passwordConfirm, setPasswordConfirm] = useState('');
  const [errors, setErrors] = useState<FieldErrors>({});
  const { mutate: signup, isPending } = useSignup();

  const validate = (): boolean => {
    const next: FieldErrors = {};
    if (!formData.email.trim()) next.email = '이메일을 입력해주세요';
    else if (!VALIDATION_REGEX.EMAIL.test(formData.email)) next.email = '올바른 이메일 형식이 아니에요';

    if (!formData.nickname.trim()) next.nickname = '닉네임을 입력해주세요';
    else if (!VALIDATION_REGEX.NICKNAME.test(formData.nickname)) next.nickname = '2-20자의 한글·영문·숫자·_만 사용할 수 있어요';

    if (!formData.password.trim()) next.password = '비밀번호를 입력해주세요';
    else if (!VALIDATION_REGEX.PASSWORD.test(formData.password))
      next.password = '8-20자의 영문·숫자·특수문자를 포함해주세요';

    if (!next.password && formData.password !== passwordConfirm) next.passwordConfirm = '비밀번호가 일치하지 않아요';

    if (!formData.agreedToTerms) next.agreedToTerms = '만 14세 이상이며 약관 및 개인정보 처리방침에 동의해주세요';

    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) signup(formData);
  };

  const handleChange = (field: 'email' | 'password' | 'nickname') => (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [field]: e.target.value });
    if (errors[field]) setErrors({ ...errors, [field]: undefined });
  };

  const handlePasswordConfirm = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPasswordConfirm(e.target.value);
    if (errors.passwordConfirm) setErrors({ ...errors, passwordConfirm: undefined });
  };

  const handleConsent = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, agreedToTerms: e.target.checked });
    if (errors.agreedToTerms) setErrors({ ...errors, agreedToTerms: undefined });
  };

  return (
    <AuthLayout
      visual={<AuthVisualPanel quote={<>첫 기록을<br />시작해볼까요</>} subquote="한 번만 적어두면 공연이 쌓여요" />}
      title="기록을 시작해볼까요"
      subtitle="이메일로 30초면 끝나요"
    >
      <form onSubmit={handleSubmit} className="space-y-[18px]">
        <Input label="이메일" name="email" type="email" placeholder="example@email.com"
          value={formData.email} onChange={handleChange('email')} error={errors.email} disabled={isPending} autoComplete="email" />
        <Input label="닉네임" name="nickname" type="text" placeholder="닉네임 (2-20자)"
          value={formData.nickname} onChange={handleChange('nickname')} error={errors.nickname}
          helperText="한글·영문·숫자·언더스코어 사용 가능" disabled={isPending} />
        <Input label="비밀번호" name="password" type="password" placeholder="비밀번호 (8-20자)"
          value={formData.password} onChange={handleChange('password')} error={errors.password}
          helperText="영문·숫자·특수문자 포함" disabled={isPending} autoComplete="new-password" />
        <Input label="비밀번호 확인" name="passwordConfirm" type="password" placeholder="비밀번호를 다시 입력하세요"
          value={passwordConfirm} onChange={handlePasswordConfirm} error={errors.passwordConfirm}
          disabled={isPending} autoComplete="new-password" />

        <div>
          <label className="flex cursor-pointer items-start gap-2.5 text-[13.5px] leading-relaxed text-capture-fg-muted">
            <input type="checkbox" checked={formData.agreedToTerms} onChange={handleConsent} disabled={isPending}
              className="mt-0.5 h-[18px] w-[18px] shrink-0 accent-capture-accent" />
            <span>만 14세 이상이며 <span className="text-capture-fg underline underline-offset-2">이용약관</span>과 <span className="text-capture-fg underline underline-offset-2">개인정보 처리방침</span>에 동의해요</span>
          </label>
          {errors.agreedToTerms && <p role="alert" className="mt-1.5 text-[12.5px] text-error">{errors.agreedToTerms}</p>}
        </div>

        <Button type="submit" size="lg" loading={isPending} className="mt-1 w-full">가입 완료</Button>
      </form>

      <div className="mt-auto py-8 text-center text-sm text-capture-fg-muted lg:mt-7 lg:py-0">
        이미 계정이 있으세요?{' '}
        <Link to={ROUTES.LOGIN} className="font-semibold text-capture-fg underline underline-offset-2">로그인</Link>
      </div>
    </AuthLayout>
  );
};

export default SignupPage;
