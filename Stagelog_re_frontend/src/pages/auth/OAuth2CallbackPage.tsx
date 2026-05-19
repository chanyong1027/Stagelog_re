import React, { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authAPI } from '../../api/auth.api';
import { useAuthStore } from '../../store/authStore';
import { ROUTES } from '../../utils/constants';

/**
 * OAuth2 콜백 처리 페이지
 * - 백엔드 OAuth2 성공 후 프론트엔드로 리다이렉트되는 페이지
 * - URL에 ?error= 파라미터가 있으면 에러 처리
 * - 에러 없으면 refresh 호출로 accessToken 획득
 */
const OAuth2CallbackPage: React.FC = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const setAuth = useAuthStore((state) => state.setAuth);

  // URL ?error 파라미터는 동기적으로 읽을 수 있으므로 useState initializer로 처리한다
  // (effect 안에서 setState 호출하는 패턴 회피).
  const errorParam = searchParams.get('error');
  const [fetchError, setFetchError] = useState<string | null>(null);
  const error = errorParam ?? fetchError;

  useEffect(() => {
    if (errorParam) {
      const timer = setTimeout(() => navigate(ROUTES.LOGIN), 3000);
      return () => clearTimeout(timer);
    }

    // 에러 없으면 refresh 호출로 accessToken 획득
    let cancelled = false;
    const fetchToken = async () => {
      try {
        const response = await authAPI.refresh();
        const { accessToken, publicId, email, nickname } = response.data;
        if (cancelled) return;
        setAuth(accessToken, { publicId, email, nickname });
        navigate(ROUTES.HOME);
      } catch {
        if (cancelled) return;
        setFetchError('소셜 로그인 처리 중 오류가 발생했습니다.');
        setTimeout(() => navigate(ROUTES.LOGIN), 3000);
      }
    };

    fetchToken();
    return () => { cancelled = true; };
  }, [errorParam, setAuth, navigate]);

  return (
    <div className="min-h-screen bg-bg-deep flex items-center justify-center px-4 relative overflow-hidden">
      <div className="absolute inset-0 bg-mesh" />
      <div className="noise-overlay" />

      <div className="relative text-center">
        {error ? (
          <>
            <div className="flex items-center justify-center gap-2 p-4 bg-red-500/10 border border-red-500/30 rounded-xl mb-4">
              <svg className="w-6 h-6 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
              <p className="text-red-400">{error}</p>
            </div>
            <p className="text-text-muted text-sm">잠시 후 로그인 페이지로 이동합니다...</p>
          </>
        ) : (
          <>
            <div className="w-10 h-10 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto mb-4" />
            <p className="text-text-secondary">로그인 처리 중...</p>
          </>
        )}
      </div>
    </div>
  );
};

export default OAuth2CallbackPage;
