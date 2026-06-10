import type { IconType } from 'react-icons';
import {
  HiOutlineSparkles,
  HiSparkles,
  HiOutlineTicket,
  HiTicket,
  HiOutlineMicrophone,
  HiMicrophone,
  HiOutlineBookOpen,
  HiBookOpen,
  HiOutlineUser,
  HiUser,
} from 'react-icons/hi2';
import { ROUTES } from '@/utils/constants';

export interface NavItem {
  label: string;
  to: string;
  Icon: IconType;
  ActiveIcon: IconType;
}

/** 데스크톱 상단 네비바 + 모바일 바텀 공통 4탭 */
export const PRIMARY_NAV: NavItem[] = [
  { label: '발견', to: ROUTES.HOME, Icon: HiOutlineSparkles, ActiveIcon: HiSparkles },
  { label: '공연', to: ROUTES.SHOWS, Icon: HiOutlineTicket, ActiveIcon: HiTicket },
  { label: '아티스트', to: ROUTES.ARTISTS, Icon: HiOutlineMicrophone, ActiveIcon: HiMicrophone },
  { label: '일기장', to: ROUTES.JOURNAL, Icon: HiOutlineBookOpen, ActiveIcon: HiBookOpen },
];

/** '나'(프로필)는 모바일 바텀에서만 탭으로 노출 (데스크톱은 우상단 처리) */
export const PROFILE_NAV: NavItem = {
  label: '나',
  to: ROUTES.PROFILE,
  Icon: HiOutlineUser,
  ActiveIcon: HiUser,
};

export const BOTTOM_NAV: NavItem[] = [...PRIMARY_NAV, PROFILE_NAV];

/** 인증 화면에서는 네비게이션을 숨긴다 */
export const AUTH_HIDDEN_PATHS = [ROUTES.LOGIN, ROUTES.SIGNUP, ROUTES.OAUTH2_CALLBACK];

export const isAuthScreen = (pathname: string): boolean =>
  AUTH_HIDDEN_PATHS.some((path) => pathname.startsWith(path));
