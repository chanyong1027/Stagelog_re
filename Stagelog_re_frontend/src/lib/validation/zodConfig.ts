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
    // 이메일 형식
    if (issue.code === 'invalid_format' && (issue as { format?: string }).format === 'email') {
      return { message: validationMessages.invalidEmail };
    }
    // 그 외는 공식 한국어 locale에 위임
    return koErrorMap(issue);
  },
});
