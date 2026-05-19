import axios, { type AxiosError, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios';
import { STORAGE_KEYS, API_ENDPOINTS } from '../utils/constants';

// Axios 인스턴스 생성
const client = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // HttpOnly 쿠키 (refresh token) 전송용
});

// Request Interceptor: 토큰 자동 첨부
client.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN);
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error: AxiosError) => {
    return Promise.reject(error);
  }
);

// refresh 중복 호출 방지
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value: AxiosResponse) => void;
  reject: (error: unknown) => void;
  config: InternalAxiosRequestConfig;
}> = [];

const processQueue = (error: unknown) => {
  failedQueue.forEach(({ reject }) => reject(error));
  failedQueue = [];
};

// Response Interceptor: 401 시 자동 refresh 시도
client.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<{ message: string; status: number }>) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // 401이고, 재시도가 아니며, refresh/login/signup 요청이 아닌 경우에만 refresh 시도
    if (
      error.response?.status === 401 &&
      !originalRequest._retry &&
      !originalRequest.url?.includes('/api/auth/refresh') &&
      !originalRequest.url?.includes('/api/auth/login') &&
      !originalRequest.url?.includes('/api/auth/signup')
    ) {
      if (isRefreshing) {
        // 이미 refresh 중이면 큐에 대기
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject, config: originalRequest });
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        const response = await client.post(API_ENDPOINTS.AUTH.REFRESH);
        const { accessToken } = response.data;

        localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, accessToken);

        // 대기 중이던 요청들 재시도
        const queue = [...failedQueue];
        failedQueue = [];
        queue.forEach(({ resolve, reject, config }) => {
          config.headers.Authorization = `Bearer ${accessToken}`;
          client(config).then(resolve).catch(reject);
        });

        // 원래 요청 재시도
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return client(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError);
        // refresh 실패 → 로그인 페이지로 이동
        localStorage.removeItem(STORAGE_KEYS.ACCESS_TOKEN);
        localStorage.removeItem(STORAGE_KEYS.USER_INFO);
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);

export default client;
