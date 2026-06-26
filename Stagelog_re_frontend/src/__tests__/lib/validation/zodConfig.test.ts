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

  it('too_small string → 커스텀 minLength 메시지', () => {
    const schema = z.string().min(3);
    const result = schema.safeParse('ab');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('3자 이상 입력해주세요');
    }
  });

  it('too_big string → 커스텀 maxLength 메시지', () => {
    const schema = z.string().max(5);
    const result = schema.safeParse('abcdef');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('5자 이내로 적어주세요');
    }
  });

  it('이메일 외 invalid_format(정규식) → 일반 형식 메시지', () => {
    const schema = z.string().regex(/^\d+$/);
    const result = schema.safeParse('abc');
    expect(result.success).toBe(false);
    if (!result.success) {
      expect(result.error.issues[0]?.message).toBe('형식을 다시 확인해주세요');
    }
  });
});
