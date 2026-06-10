import { NavLink, useLocation } from 'react-router-dom';
import { ROUTES } from '@/utils/constants';
import { BOTTOM_NAV, isAuthScreen } from './navItems';

/**
 * 모바일 바텀 네비게이션 (5탭: 발견·공연·아티스트·일기장·나).
 * 데스크톱(md+)에서는 숨기고 TopNav가 대신 노출된다.
 */
export default function BottomNav() {
  const { pathname } = useLocation();
  if (isAuthScreen(pathname)) {
    return null;
  }

  return (
    <nav
      aria-label="주요 네비게이션"
      className="fixed inset-x-0 bottom-0 z-40 border-t border-capture-muted bg-capture-surface/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
    >
      <ul className="mx-auto grid h-16 max-w-md grid-cols-5">
        {BOTTOM_NAV.map(({ label, to, Icon, ActiveIcon }) => (
          <li key={to} className="flex items-center justify-center">
            <NavLink
              to={to}
              end={to === ROUTES.HOME}
              className={({ isActive }) =>
                [
                  'flex min-h-11 min-w-11 flex-col items-center justify-center gap-1 rounded-md px-2 text-xs font-medium transition-colors',
                  isActive ? 'text-capture-accent' : 'text-capture-fg-muted hover:text-capture-fg',
                ].join(' ')
              }
            >
              {({ isActive }) => {
                const CurrentIcon = isActive ? ActiveIcon : Icon;
                return (
                  <>
                    <CurrentIcon className="h-6 w-6" aria-hidden="true" />
                    <span>{label}</span>
                  </>
                );
              }}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
