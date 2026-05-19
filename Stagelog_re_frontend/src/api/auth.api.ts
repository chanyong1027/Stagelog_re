import client from './client';
import { LoginRequest, TokenResponse, SignupRequest } from '../types/auth.types';
import { API_ENDPOINTS } from '../utils/constants';

/**
 * 인증 관련 API
 */
export const authAPI = {
  /** 로그인 → accessToken + 사용자 정보 반환, refreshToken은 HttpOnly 쿠키 */
  login: (data: LoginRequest) =>
    client.post<TokenResponse>(API_ENDPOINTS.AUTH.LOGIN, data),

  /** 회원가입 → 201 Created */
  signup: (data: SignupRequest) =>
    client.post(API_ENDPOINTS.AUTH.SIGNUP, data),

  /** 로그아웃 → 서버에서 refreshToken 무효화 + 쿠키 삭제 */
  logout: () =>
    client.post(API_ENDPOINTS.AUTH.LOGOUT),

  /** 토큰 갱신 → HttpOnly 쿠키의 refreshToken으로 새 accessToken 발급 */
  refresh: () =>
    client.post<TokenResponse>(API_ENDPOINTS.AUTH.REFRESH),
};
