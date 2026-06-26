// 로그인 요청
export interface LoginRequest {
  email: string;
  password: string;
}

// 로그인/리프레시 응답 (백엔드 TokenResponse)
export interface TokenResponse {
  accessToken: string;
  tokenType: string;
  publicId: string;
  email: string;
  nickname: string;
}

// 회원가입 요청
export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  agreedToTerms: boolean;
}

// 사용자 역할
export type Role = 'USER' | 'ADMIN';

// 사용자 정보 (프로필 응답 등)
export interface User {
  id: number;
  publicId: string;
  email: string;
  nickname: string;
  role: Role;
}

// localStorage에 저장할 사용자 정보
export interface UserInfo {
  publicId: string;
  email: string;
  nickname: string;
}
