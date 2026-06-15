// src/components/common/Input.tsx
import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
}

/**
 * 입력 필드 — 중립(Capture) 디자인. 높이 54px, 라운드 14px, 포커스 시 capture-accent 링.
 */
const Input: React.FC<InputProps> = ({ label, error, helperText, className = '', id, ...props }) => {
  const inputId = id ?? props.name;
  const errorId = error && inputId ? `${inputId}-error` : undefined;
  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="mb-2 block text-[13px] font-semibold tracking-[-0.01em] text-capture-fg">
          {label}
        </label>
      )}
      <input
        id={inputId}
        aria-invalid={!!error}
        aria-describedby={errorId}
        className={
          'h-[54px] w-full rounded-[14px] border bg-capture-surface px-4 text-[15px] text-capture-fg ' +
          'placeholder:text-capture-fg-muted/60 transition-colors focus:outline-none focus:bg-capture-bg ' +
          (error
            ? 'border-error focus:border-error focus:ring-2 focus:ring-error/15 '
            : 'border-capture-muted focus:border-capture-accent focus:ring-2 focus:ring-capture-accent/15 ') +
          (props.disabled ? 'opacity-50 cursor-not-allowed ' : '') +
          className
        }
        {...props}
      />
      {error && (
        <p id={errorId} role="alert" className="mt-[7px] flex items-center gap-1.5 text-[12.5px] text-error">
          <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.4" aria-hidden="true">
            <circle cx="12" cy="12" r="9" />
            <path d="M12 8v5M12 16.5v.01" />
          </svg>
          {error}
        </p>
      )}
      {helperText && !error && <p className="mt-[7px] text-[12.5px] text-capture-fg-muted">{helperText}</p>}
    </div>
  );
};

export default Input;
