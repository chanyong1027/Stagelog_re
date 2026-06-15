interface WordmarkProps {
  className?: string;
  /** 별 마크 색 유틸 (기본: 골드). 어두운 배경에선 text-gold-300 등으로 조정 */
  starClassName?: string;
  /** 워드마크 텍스트 색 유틸 (기본: capture-fg). 어두운 배경에선 text-white */
  textClassName?: string;
}

/**
 * Stagelog 워드마크 — 골드 손그림 별 + 대문자 S, 마침표 없음 (디자인 결정 §6).
 * 데스크톱 비주얼 패널·모바일 헤더에서 공용.
 */
export default function Wordmark({
  className = '',
  starClassName = 'text-gold-500',
  textClassName = 'text-capture-fg',
}: WordmarkProps) {
  return (
    <span className={`inline-flex items-center gap-1.5 ${className}`}>
      <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" className={`h-[22px] w-[22px] ${starClassName}`}>
        <path d="M12 3c.4 1.8 1 4.5 2.2 5.6 1.2 1 3.9 1.4 5.8 1.6-1.6 1-3.9 2.5-4.6 3.9-.6 1.3-.4 4-.3 5.9-1.5-1.2-3.7-2.9-5.1-3-1.4-.1-3.8 1-5.4 1.8.7-1.7 1.7-4.2 1.4-5.6-.3-1.4-2-3.4-3.2-4.8 1.9.2 4.6.4 5.9-.3C9.9 7.4 11.4 4.8 12 3z" />
      </svg>
      <span className={`text-[25px] font-bold tracking-[-0.03em] ${textClassName}`}>Stagelog</span>
    </span>
  );
}
