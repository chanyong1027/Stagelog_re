/**
 * 우리가 zod 공식 ko() 기본 메시지보다 더 상황 맞춤으로 쓰고 싶은 항목만 정의.
 * 카피 규칙: 정중체, 마침표 없음, 명령형 회피.
 */
export const validationMessages = {
  required: '필수 입력 항목이에요',
  invalidEmail: '이메일 형식을 다시 확인해주세요',
  invalidFormat: '형식을 다시 확인해주세요',
  minLength: (n: number) => `${n}자 이상 입력해주세요`,
  maxLength: (n: number) => `${n}자 이내로 적어주세요`,
  underAge: '만 14세 이상만 가입할 수 있어요',
  termsRequired: '약관 동의가 필요해요',
} as const;
