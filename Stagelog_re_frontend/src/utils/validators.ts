import { VALIDATION_REGEX, ERROR_MESSAGES } from './constants';

/**
 * 필수 입력 검증
 */
export const validateRequired = (value: string): string | undefined => {
  if (!value || value.trim() === '') {
    return ERROR_MESSAGES.VALIDATION.REQUIRED_FIELD;
  }
  return undefined;
};

/**
 * 닉네임 유효성 검증 (2-20자, 한글/영문/숫자/언더스코어)
 */
export const validateNickname = (nickname: string): string | undefined => {
  const requiredError = validateRequired(nickname);
  if (requiredError) return requiredError;

  if (!VALIDATION_REGEX.NICKNAME.test(nickname)) {
    return ERROR_MESSAGES.VALIDATION.INVALID_NICKNAME;
  }

  return undefined;
};

/**
 * 이메일 유효성 검증
 */
export const validateEmail = (email: string): string | undefined => {
  const requiredError = validateRequired(email);
  if (requiredError) return requiredError;

  if (!VALIDATION_REGEX.EMAIL.test(email)) {
    return ERROR_MESSAGES.VALIDATION.INVALID_EMAIL;
  }

  return undefined;
};

/**
 * 비밀번호 유효성 검증 (8-20자, 영문/숫자/특수문자 포함)
 */
export const validatePassword = (password: string): string | undefined => {
  const requiredError = validateRequired(password);
  if (requiredError) return requiredError;

  if (!VALIDATION_REGEX.PASSWORD.test(password)) {
    return ERROR_MESSAGES.VALIDATION.INVALID_PASSWORD;
  }

  return undefined;
};

/**
 * 비밀번호 확인 검증
 */
export const validatePasswordConfirm = (
  password: string,
  confirmPassword: string
): string | undefined => {
  const requiredError = validateRequired(confirmPassword);
  if (requiredError) return requiredError;

  if (password !== confirmPassword) {
    return '비밀번호가 일치하지 않습니다.';
  }

  return undefined;
};

/**
 * 로그인 폼 검증
 */
export const validateLoginForm = (email: string, password: string) => {
  const errors = {
    email: validateEmail(email),
    password: validateRequired(password),
  };

  const isValid = !errors.email && !errors.password;

  return { isValid, errors };
};

/**
 * 회원가입 폼 검증
 */
export const validateSignupForm = (formData: {
  email: string;
  password: string;
  nickname: string;
  agreedToTerms: boolean;
  confirmPassword?: string;
}) => {
  const errors = {
    email: validateEmail(formData.email),
    nickname: validateNickname(formData.nickname),
    password: validatePassword(formData.password),
    confirmPassword: formData.confirmPassword
      ? validatePasswordConfirm(formData.password, formData.confirmPassword)
      : undefined,
    agreedToTerms: formData.agreedToTerms
      ? undefined
      : ERROR_MESSAGES.VALIDATION.TERMS_NOT_AGREED,
  };

  const isValid = Object.values(errors).every((error) => !error);

  return { isValid, errors };
};

/**
 * 리뷰 제목 검증 (1-100자)
 */
export const validateReviewTitle = (title: string): string | undefined => {
  const requiredError = validateRequired(title);
  if (requiredError) return requiredError;

  if (title.length > 100) {
    return '제목은 100자 이내로 입력해주세요.';
  }

  return undefined;
};

/**
 * 리뷰 내용 검증 (1-5000자)
 */
export const validateReviewContent = (content: string): string | undefined => {
  const requiredError = validateRequired(content);
  if (requiredError) return requiredError;

  if (content.length > 5000) {
    return '내용은 5000자 이내로 입력해주세요.';
  }

  return undefined;
};

/**
 * 리뷰 폼 검증
 */
export const validateReviewForm = (title: string, content: string) => {
  const errors = {
    title: validateReviewTitle(title),
    content: validateReviewContent(content),
  };

  const isValid = !errors.title && !errors.content;

  return { isValid, errors };
};
