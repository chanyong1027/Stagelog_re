interface ErrorStateProps {
  title?: string;
  description?: string;
  retryLabel?: string;
  onRetry?: () => void;
}

/**
 * 재사용 가능한 에러/빈 상태 표시. 정중체 + 마침표 없음(카피 규칙).
 * 색은 확정 중립(Capture) 팔레트.
 */
export default function ErrorState({
  title = '문제가 발생했어요',
  description = '잠시 후 다시 시도해주세요',
  retryLabel = '다시 시도',
  onRetry,
}: ErrorStateProps) {
  return (
    <div
      role="alert"
      className="flex min-h-[60vh] items-center justify-center px-6 text-capture-fg"
    >
      <div className="w-full max-w-sm text-center">
        <h1 className="text-xl font-semibold tracking-tight">{title}</h1>
        <p className="mt-3 text-sm leading-6 text-capture-fg-muted">{description}</p>
        {onRetry ? (
          <button
            type="button"
            onClick={onRetry}
            className="mt-6 inline-flex min-h-11 items-center rounded-xl bg-capture-accent px-5 text-sm font-semibold text-white transition-opacity hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-capture-ring focus:ring-offset-2"
          >
            {retryLabel}
          </button>
        ) : null}
      </div>
    </div>
  );
}
