import React from 'react';
import ReactDOM from 'react-dom/client';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
// import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
// import { Toaster } from 'react-hot-toast';
import App from './App';
import './index.css';
import './lib/validation/zodConfig'; // zod 전역 한국어 errorMap 적용 (side-effect)

// React Query 클라이언트 설정
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1, // 실패 시 1번만 재시도
      refetchOnWindowFocus: false, // 윈도우 포커스 시 자동 refetch 비활성화
      staleTime: 1000 * 60 * 5, // 5분 동안 캐시 데이터를 fresh로 간주
    },
  },
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      {/* Toast 알림 */}
      {/* <Toaster position="top-right" /> */}
      {/* React Query Devtools (개발 환경에서만 표시) */}
      {/* {import.meta.env.VITE_ENV === 'development' && <ReactQueryDevtools />} */}
    </QueryClientProvider>
  </React.StrictMode>,
);
