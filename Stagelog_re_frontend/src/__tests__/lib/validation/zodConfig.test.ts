import { describe, it, expect } from 'vitest';
import { z } from 'zod';
import '@/lib/validation/zodConfig';

describe('zod v4 ko locale + 커스텀 override', () => {
  it('required (undefined) → 한국어 정중체 메시지', () => {
    const schema = z.object({ name: z.string() });
    const result = schema.safeParse({});
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('필수 입력 항목이에요');
    }
  });

  it('invalid email → 한국어 정중체 메시지', () => {
    const schema = z.email();
    const result = schema.safeParse('not-an-email');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('이메일 형식을 다시 확인해주세요');
    }
  });

  it('그 외는 공식 ko() locale에 위임 — too_small string은 한국어', () => {
    const schema = z.string().min(3);
    const result = schema.safeParse('ab');
    expect(result.success).toBe(false);
    if (!result.success) {
      // ko() 공식 메시지로 위임됨 (한국어 문구 확인)
      expect(result.error.issues[0]?.message).toMatch(/너무 작습니다|문자/);
    }
  });
});
