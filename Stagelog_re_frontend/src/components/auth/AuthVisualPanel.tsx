// src/components/auth/AuthVisualPanel.tsx
import React from 'react';
import Wordmark from '../brand/Wordmark';

interface AuthVisualPanelProps {
  /** Gaegu 인용 (줄바꿈 포함 가능) */
  quote: React.ReactNode;
  /** 인용 아래 보조 문구 */
  subquote: string;
}

// 기록의 벽 — 기울어진 placeholder 카드 (MVP는 더미, 추후 실제 공연 포스터로 교체)
const CARDS = [
  { left: '34px', top: '96px', rotate: '-7deg', hue: 285, title: '데이식스 〈The DECADE〉', meta: '올림픽홀 · 4.21' },
  { left: '208px', top: '54px', rotate: '5deg', hue: 25, title: '검정치마 〈THIRSTY〉', meta: '무신사 개러지 · 3.8' },
  { left: '120px', top: '236px', rotate: '-3deg', hue: 160, title: '실리카겔 단독 공연', meta: '예스24 라이브홀 · 2.17' },
];

/**
 * 데스크톱 좌측 비주얼 패널 (lg+). 모바일에선 hidden — AuthLayout이 그리드 한 칸으로 배치.
 */
export default function AuthVisualPanel({ quote, subquote }: AuthVisualPanelProps) {
  return (
    <div
      className="relative hidden flex-col overflow-hidden p-11 text-white lg:flex"
      style={{ background: 'linear-gradient(158deg, oklch(0.48 0.10 26) 0%, oklch(0.32 0.075 288) 88%)' }}
    >
      {/* 콜라주 */}
      <div className="pointer-events-none absolute inset-0" aria-hidden="true">
        {CARDS.map((c) => (
          <div
            key={c.title}
            className="absolute w-[152px] overflow-hidden rounded-[13px] shadow-[0_20px_44px_-14px_rgba(0,0,0,0.6)]"
            style={{ left: c.left, top: c.top, transform: `rotate(${c.rotate})` }}
          >
            <div
              className="h-[122px]"
              style={{ background: `linear-gradient(160deg, oklch(0.80 0.09 ${c.hue}), oklch(0.52 0.13 ${c.hue}))` }}
            />
            <div className="bg-white px-[11px] pb-[11px] pt-[9px]">
              <div className="text-[12px] font-bold leading-[1.3] tracking-[-0.01em] text-capture-fg">{c.title}</div>
              <div className="mt-[3px] text-[11px] text-capture-fg-muted">{c.meta}</div>
            </div>
          </div>
        ))}
      </div>

      {/* 하단 가독성 스크림 */}
      <div
        className="absolute inset-x-0 bottom-0 z-[1] h-[58%]"
        style={{ background: 'linear-gradient(to top, oklch(0.28 0.06 288) 10%, transparent)' }}
        aria-hidden="true"
      />

      <Wordmark className="relative z-10" starClassName="text-gold-300" textClassName="text-white" />

      <div className="relative z-10 mt-auto">
        <p className="font-gaegu text-[33px] leading-[1.4] tracking-[-0.01em]">{quote}</p>
        <p className="mt-3 text-sm opacity-80">{subquote}</p>
      </div>
    </div>
  );
}
