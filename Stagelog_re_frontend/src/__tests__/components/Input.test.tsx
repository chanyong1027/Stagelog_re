// src/__tests__/components/Input.test.tsx
import { render, screen } from '@testing-library/react';
import { describe, it, expect } from 'vitest';
import Input from '@/components/common/Input';

describe('Input', () => {
  // ↓ 이 테스트가 진짜 RED를 만든다: 옛 Input은 label에 htmlFor / input에 id가 없어 둘이 연결 안 됨
  //   → getByLabelText 실패. 새 구현(id=name 연결)에서만 통과.
  it('label과 input을 연결해 getByLabelText로 접근 가능하다', () => {
    render(<Input label="이메일" name="email" />);
    expect(screen.getByLabelText('이메일')).toBeInTheDocument();
  });

  it('error가 있으면 표시하고 helperText는 숨긴다', () => {
    render(<Input label="이메일" name="email" error="형식이 올바르지 않아요" helperText="회사 메일 가능" />);
    expect(screen.getByText('형식이 올바르지 않아요')).toBeInTheDocument();
    expect(screen.queryByText('회사 메일 가능')).not.toBeInTheDocument();
  });

  it('error가 없으면 helperText를 렌더한다', () => {
    render(<Input label="비밀번호" name="password" helperText="8-20자" />);
    expect(screen.getByText('8-20자')).toBeInTheDocument();
  });
});
