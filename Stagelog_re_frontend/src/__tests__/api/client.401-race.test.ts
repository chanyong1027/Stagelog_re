import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import MockAdapter from 'axios-mock-adapter';
import client from '@/api/client';
import { authBus } from '@/api/authBus';
import { STORAGE_KEYS } from '@/utils/constants';

let mock: MockAdapter;

beforeEach(() => {
  mock = new MockAdapter(client);
  localStorage.setItem(STORAGE_KEYS.ACCESS_TOKEN, 'old-token');
});

afterEach(() => {
  mock.restore();
  localStorage.clear();
});

describe('client 401 race', () => {
  it('동시 3건 401 → refresh는 1회만, 모두 200으로 replay', async () => {
    let refreshCount = 0;
    mock.onPost('/api/auth/refresh').reply(() => {
      refreshCount += 1;
      return [200, { accessToken: 'new-token' }];
    });
    ['/a', '/b', '/c'].forEach((url) => {
      mock.onGet(url).replyOnce(401);
      mock.onGet(url).reply(200, { url });
    });

    const results = await Promise.all([client.get('/a'), client.get('/b'), client.get('/c')]);

    expect(results.map((r) => r.status)).toEqual([200, 200, 200]);
    expect(refreshCount).toBe(1);
    expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBe('new-token');
  });

  it('refresh 실패 → session-expired 이벤트 emit + 토큰 제거 + 대기 큐 reject', async () => {
    const onExpired = vi.fn();
    const off = authBus.on('session-expired', onExpired);

    mock.onPost('/api/auth/refresh').reply(401);
    mock.onGet('/a').reply(401);
    mock.onGet('/b').reply(401);

    const results = await Promise.allSettled([client.get('/a'), client.get('/b')]);

    expect(results.every((r) => r.status === 'rejected')).toBe(true);
    expect(onExpired).toHaveBeenCalledTimes(1);
    expect(localStorage.getItem(STORAGE_KEYS.ACCESS_TOKEN)).toBeNull();
    off();
  });
});
