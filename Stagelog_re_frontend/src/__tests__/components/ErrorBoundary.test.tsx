import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ErrorBoundary from '@/components/common/ErrorBoundary';

let shouldThrow = true;

function ProblemChild() {
  if (shouldThrow) {
    throw new Error('boom');
  }
  return <p>정상 화면</p>;
}

describe('ErrorBoundary', () => {
  const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);

  afterEach(() => {
    shouldThrow = true;
    consoleError.mockClear();
  });

  it('재시도 가능한 한국어 fallback을 보여주고 새로고침 없이 복구한다', async () => {
    render(
      <ErrorBoundary>
        <ProblemChild />
      </ErrorBoundary>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('문제가 발생했어요');

    shouldThrow = false;
    await userEvent.click(screen.getByRole('button', { name: '다시 시도' }));

    expect(screen.getByText('정상 화면')).toBeInTheDocument();
  });
});
