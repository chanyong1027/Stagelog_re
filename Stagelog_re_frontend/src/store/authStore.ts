import { create } from 'zustand';
import { STORAGE_KEYS } from '../utils/constants';
import { UserInfo } from '../types/auth.types';

interface AuthState {
  isAuthenticated: boolean;
  token: string | null;
  publicId: string | null;
  email: string | null;
  nickname: string | null;
  setAuth: (token: string, userInfo: UserInfo) => void;
  clearAuth: () => void;
}

// localStorage에서 사용자 정보 복원
const loadUserInfo = (): UserInfo | null => {
  try {
    const raw = localStorage.getItem(STORAGE_KEYS.USER_INFO);
    if (!raw) return null;
    return JSON.parse(raw) as UserInfo;
  } catch {
    return null;
  }
};

/**
 * 인증 상태 관리 Zustand 스토어
 * - TokenResponse에서 직접 사용자 정보 저장 (JWT 디코딩 의존 제거)
 * - localStorage에 토큰 + 사용자 정보 저장하여 새로고침 시 복원
 */
export const useAuthStore = create<AuthState>((set) => {
  const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
  const userInfo = loadUserInfo();

  return {
    isAuthenticated: !!token,
    token,
    publicId: userInfo?.publicId ?? null,
    email: userInfo?.email ?? null,
    nickname: userInfo?.nickname ?? null,

    setAuth: (token, userInfo) => {
      localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, token);
      localStorage.setItem(STORAGE_KEYS.USER_INFO, JSON.stringify(userInfo));
      set({
        isAuthenticated: true,
        token,
        publicId: userInfo.publicId,
        email: userInfo.email,
        nickname: userInfo.nickname,
      });
    },

    clearAuth: () => {
      localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
      localStorage.removeItem(STORAGE_KEYS.USER_INFO);
      set({
        isAuthenticated: false,
        token: null,
        publicId: null,
        email: null,
        nickname: null,
      });
    },
  };
});
