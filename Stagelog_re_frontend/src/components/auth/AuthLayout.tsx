// src/components/auth/AuthLayout.tsx
import React from 'react';
import Wordmark from '../brand/Wordmark';

interface AuthLayoutProps {
  /** 데스크톱 좌측 비주얼 패널 (AuthVisualPanel). 자체적으로 lg 미만 hidden 처리됨 */
  visual: React.ReactNode;
  /** 데스크톱 폼 헤딩 = 모바일 부제 제목 */
  title: string;
  subtitle: string;
  children: React.ReactNode;
}

/**
 * 인증 화면 반응형 셸.
 * - lg+ : 좌(비주얼) / 우(폼) 좌우 분할. 우측 폼은 세로 가운데 정렬.
 * - <lg : 단일 컬럼. 비주얼 숨김, 폼 상단에 워드마크 노출. 화면 하단 foot 링크는 mt-auto로 바닥 고정.
 */
export default function AuthLayout({ visual, title, subtitle, children }: AuthLayoutProps) {
  return (
    <div className="min-h-screen bg-capture-bg text-capture-fg lg:grid lg:grid-cols-[1fr_1.15fr]">
      {visual}
      <div className="flex min-h-screen flex-col px-6 lg:min-h-0 lg:justify-center lg:px-16">
        {/* 브랜드/헤딩 존 */}
        <div className="pt-7 lg:pt-0">
          {/* 워드마크는 모바일에서만 (데스크톱은 좌측 비주얼이 브랜드 담당) */}
          <Wordmark className="lg:hidden" />
          <h1 className="mt-3.5 text-[19px] font-semibold tracking-[-0.01em] lg:mt-0 lg:text-[26px] lg:font-bold lg:tracking-[-0.02em]">
            {title}
          </h1>
          <p className="mt-1 text-sm text-capture-fg-muted lg:mt-2 lg:text-[15px]">{subtitle}</p>
        </div>

        <div className="mt-8 flex flex-1 flex-col lg:flex-none">{children}</div>
      </div>
    </div>
  );
}
