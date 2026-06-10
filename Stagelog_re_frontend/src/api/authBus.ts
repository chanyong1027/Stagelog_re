/**
 * 인증 관련 전역 이벤트 버스.
 * API client(라우터 밖)에서 발생한 세션 만료를, 라우터 안의 핸들러가 구독해
 * navigate로 처리하기 위한 얇은 pub/sub. window.location 하드 이동을 대체한다.
 */
type AuthEvent = 'session-expired';
type Listener = () => void;

const listeners: Record<AuthEvent, Set<Listener>> = {
  'session-expired': new Set(),
};

export const authBus = {
  /** 구독. 반환된 함수를 호출하면 구독 해제. */
  on(event: AuthEvent, listener: Listener): () => void {
    listeners[event].add(listener);
    return () => listeners[event].delete(listener);
  },
  emit(event: AuthEvent): void {
    listeners[event].forEach((listener) => listener());
  },
};
