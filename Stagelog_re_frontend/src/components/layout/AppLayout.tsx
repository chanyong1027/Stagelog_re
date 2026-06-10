import { Outlet } from 'react-router-dom';
import TopNav from './TopNav';
import BottomNav from './BottomNav';

/**
 * 앱 셸 레이아웃: 데스크톱 상단 네비바 + 모바일 바텀 네비.
 * 인증 화면(로그인/가입)은 각 네비 컴포넌트가 스스로 숨긴다.
 */
export default function AppLayout() {
  return (
    <div className="min-h-screen bg-capture-bg text-capture-fg">
      <TopNav />
      <main className="mx-auto w-full max-w-6xl px-4 pb-24 md:pb-12">
        <Outlet />
      </main>
      <BottomNav />
    </div>
  );
}
