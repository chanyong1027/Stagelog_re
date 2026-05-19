import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { authAPI } from '../api/auth.api';
import { LoginRequest, SignupRequest } from '../types/auth.types';
import { useAuthStore } from '../store/authStore';
import { ROUTES } from '../utils/constants';

/**
 * 로그인 Hook
 * - TokenResponse에서 accessToken + 사용자 정보를 함께 저장
 */
export const useLogin = () => {
  const navigate = useNavigate();
  const setAuth = useAuthStore((state) => state.setAuth);

  return useMutation({
    mutationFn: (data: LoginRequest) => authAPI.login(data),
    onSuccess: (response) => {
      const { accessToken, publicId, email, nickname } = response.data;
      setAuth(accessToken, { publicId, email, nickname });
      navigate(ROUTES.HOME);
    },
  });
};

/**
 * 회원가입 Hook
 */
export const useSignup = () => {
  const navigate = useNavigate();

  return useMutation({
    mutationFn: (data: SignupRequest) => authAPI.signup(data),
    onSuccess: () => {
      navigate(ROUTES.LOGIN);
    },
  });
};

/**
 * 로그아웃 Hook
 * - 서버에 로그아웃 API 호출 후 store 클리어
 */
export const useLogout = () => {
  const clearAuth = useAuthStore((state) => state.clearAuth);
  const navigate = useNavigate();

  return async () => {
    try {
      await authAPI.logout();
    } catch {
      // 로그아웃 API 실패해도 로컬 상태는 클리어
    }
    clearAuth();
    navigate(ROUTES.LOGIN);
  };
};
