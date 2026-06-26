import { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router-dom';
import { ROUTES } from '../utils/constants';
import { authBus } from '../api/authBus';
import ProtectedRoute from './ProtectedRoute';
import AppLayout from '../components/layout/AppLayout';

/**
 * API client(라우터 밖)에서 발행한 'session-expired'를 구독해 로그인으로 보낸다.
 * 현재 위치를 state.from에 보존(로그인 후 복귀용). BrowserRouter 안에서만 동작.
 */
function SessionExpiredHandler() {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(
    () =>
      authBus.on('session-expired', () => {
        navigate(ROUTES.LOGIN, { state: { from: location }, replace: true });
      }),
    [navigate, location],
  );

  return null;
}

// Auth 페이지 (백엔드 완료 · 디자인 재작업은 후속 Task)
import LoginPage from '../pages/auth/LoginPage';
import SignupPage from '../pages/auth/SignupPage';
import OAuth2CallbackPage from '../pages/auth/OAuth2CallbackPage';

/**
 * 더미 페이지 — 재설계 중. 각 화면은 후속 Task에서 실제 구현.
 * (공연 데이터는 KOPIS 재수집 예정이라 현재 전부 더미)
 */
function DummyPage({ title, note }: { title: string; note?: string }) {
  return (
    <section className="py-20 text-center">
      <h1 className="text-2xl font-bold tracking-tight">{title}</h1>
      <p className="mt-3 text-sm text-capture-fg-muted">{note ?? '곧 만들 예정이에요'}</p>
    </section>
  );
}

/**
 * 앱 라우팅 — 신규 5탭 IA.
 * 발견(/) · 공연(/shows) · 아티스트(/artists) · 일기장(/journal) · 나(/profile)
 * 일기장·나는 로그인 필요(ProtectedRoute).
 */
const AppRoutes: React.FC = () => {
  return (
    <BrowserRouter>
      <SessionExpiredHandler />
      <Routes>
        {/* 앱 셸 (TopNav + BottomNav) */}
        <Route element={<AppLayout />}>
          <Route path={ROUTES.HOME} element={<DummyPage title="발견" note="큐레이션 메인 — 페스티벌·콘서트·내한·아티스트·에디터 픽" />} />
          <Route path={ROUTES.SHOWS} element={<DummyPage title="공연" note="공연 목록 + 필터(콘서트·페스티벌·내한)" />} />
          <Route path={ROUTES.ARTISTS} element={<DummyPage title="아티스트" note="아티스트 프로필 — 신규 엔티티(후속 백엔드)" />} />
          <Route
            path={ROUTES.JOURNAL}
            element={
              <ProtectedRoute>
                <DummyPage title="일기장" note="공연 회고 기록" />
              </ProtectedRoute>
            }
          />
          <Route
            path={ROUTES.PROFILE}
            element={
              <ProtectedRoute>
                <DummyPage title="나" note="프로필" />
              </ProtectedRoute>
            }
          />
        </Route>

        {/* 인증 (네비 없음) */}
        <Route path={ROUTES.LOGIN} element={<LoginPage />} />
        <Route path={ROUTES.SIGNUP} element={<SignupPage />} />
        <Route path={ROUTES.OAUTH2_CALLBACK} element={<OAuth2CallbackPage />} />

        {/* 404 → 발견 */}
        <Route path="*" element={<Navigate to={ROUTES.HOME} replace />} />
      </Routes>
    </BrowserRouter>
  );
};

export default AppRoutes;
