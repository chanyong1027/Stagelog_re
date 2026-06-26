import { NavLink, useLocation } from 'react-router-dom';
import { ROUTES } from '@/utils/constants';
import { PRIMARY_NAV, isAuthScreen } from './navItems';

/**
 * 데스크톱(md+) 상단 네비바: 브랜드 + 4개 주요 링크 + 로그인/가입 진입점.
 * '나'(프로필)는 로그인 후 우상단 처리 예정(현재는 더미라 로그인/가입 노출).
 * 모바일에서는 숨기고 BottomNav가 대신 노출된다.
 */
export default function TopNav() {
  const { pathname } = useLocation();
  if (isAuthScreen(pathname)) {
    return null;
  }

  return (
    <nav
      aria-label="주요 네비게이션"
      className="sticky top-0 z-40 hidden h-16 items-center gap-8 border-b border-capture-muted bg-capture-bg/95 px-8 backdrop-blur md:flex"
    >
      <NavLink to={ROUTES.HOME} className="flex items-center gap-1.5 text-xl font-bold tracking-tight">
        <span aria-hidden="true" className="text-gold-500">✦</span>
        Stagelog
      </NavLink>

      <ul className="flex gap-7">
        {PRIMARY_NAV.map(({ label, to }) => (
          <li key={to}>
            <NavLink
              to={to}
              end={to === ROUTES.HOME}
              className={({ isActive }) =>
                [
                  'text-[15px] transition-colors',
                  isActive
                    ? 'font-semibold text-capture-fg'
                    : 'font-medium text-capture-fg-muted hover:text-capture-fg',
                ].join(' ')
              }
            >
              {label}
            </NavLink>
          </li>
        ))}
      </ul>

      <div className="ml-auto flex items-center gap-4">
        <NavLink to={ROUTES.LOGIN} className="text-sm font-medium text-capture-fg-muted hover:text-capture-fg">
          로그인
        </NavLink>
        <NavLink
          to={ROUTES.SIGNUP}
          className="flex h-9 items-center rounded-[10px] bg-capture-accent px-4 text-sm font-semibold text-white"
        >
          가입하기
        </NavLink>
      </div>
    </nav>
  );
}
