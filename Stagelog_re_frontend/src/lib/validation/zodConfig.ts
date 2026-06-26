import { z } from 'zod';
import { ko } from 'zod/locales';
import { validationMessages } from './messages';

/**
 * zod v4 전역 에러 메시지 설정 (side-effect).
 * 공식 한국어 locale ko()를 기본으로 깔고, 우리가 더 맞춤하고 싶은 항목만 override.
 *
 * v4 주의:
 * - z.setErrorMap()은 deprecated → z.config({ localeError }) 사용
 * - 누락 필드: code 'invalid_type' + input === undefined
 * - 이메일 형식: code 'invalid_format' + format === 'email' (v3의 invalid_string 아님)
 */
const koErrorMap = ko().localeError;

z.config({
  localeError: (issue) => {
    // 필수 입력 누락
    if (issue.code === 'invalid_type' && (issue as { input?: unknown }).input === undefined) {
      return { message: validationMessages.required };
    }
    // 형식 오류 — 이메일은 전용 카피, 그 외 형식(정규식 등)은 일반 카피
    if (issue.code === 'invalid_format') {
      const format = (issue as { format?: string }).format;
      return {
        message: format === 'email' ? validationMessages.invalidEmail : validationMessages.invalidFormat,
      };
    }
    // 최소 길이 (문자열)
    if (issue.code === 'too_small' && (issue as { origin?: string }).origin === 'string') {
      return { message: validationMessages.minLength(Number((issue as { minimum?: unknown }).minimum)) };
    }
    // 최대 길이 (문자열)
    if (issue.code === 'too_big' && (issue as { origin?: string }).origin === 'string') {
      return { message: validationMessages.maxLength(Number((issue as { maximum?: unknown }).maximum)) };
    }
    // 그 외는 공식 한국어 locale에 위임
    return koErrorMap(issue);
  },
});
