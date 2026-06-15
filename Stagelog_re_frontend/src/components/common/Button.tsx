// src/components/common/Button.tsx
import React from 'react';

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'kakao' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  loading?: boolean;
  children: React.ReactNode;
}

/**
 * 버튼 — 중립(Capture) 디자인. primary = capture-accent(중립 다크).
 */
const Button: React.FC<ButtonProps> = ({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  children,
  className = '',
  ...props
}) => {
  const base =
    'inline-flex items-center justify-center gap-2 font-semibold tracking-[-0.01em] ' +
    'transition-[opacity,background-color,box-shadow] duration-200 ' +
    'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-capture-accent/20 ' +
    'disabled:opacity-50 disabled:pointer-events-none';

  const variants = {
    primary: 'bg-capture-accent text-white hover:opacity-90 active:opacity-80',
    secondary: 'bg-capture-surface text-capture-fg border border-capture-muted hover:bg-capture-muted/50',
    ghost: 'bg-transparent text-capture-fg-muted hover:text-capture-fg hover:bg-capture-surface',
    kakao: 'bg-[#FEE500] text-[#181600] hover:brightness-95',
    danger: 'bg-error text-white hover:opacity-90',
  };

  const sizes = {
    sm: 'h-10 px-4 text-sm rounded-[12px]',
    md: 'h-12 px-6 text-[15px] rounded-[14px]',
    lg: 'h-[54px] px-6 text-[15.5px] rounded-[14px]',
  };

  return (
    <button
      className={`${base} ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? (
        <>
          <svg className="h-5 w-5 animate-spin" viewBox="0 0 24 24" fill="none" aria-hidden="true">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="3" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span>처리 중...</span>
        </>
      ) : (
        children
      )}
    </button>
  );
};

export default Button;
