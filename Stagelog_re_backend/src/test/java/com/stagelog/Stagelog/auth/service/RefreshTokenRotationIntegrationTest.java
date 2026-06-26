package com.stagelog.Stagelog.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.stagelog.Stagelog.auth.dto.RefreshOutcome;
import com.stagelog.Stagelog.global.jwt.RefreshTokenHasher;
import com.stagelog.Stagelog.global.jwt.domain.RefreshToken;
import com.stagelog.Stagelog.global.jwt.repository.RefreshTokenRepository;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 실 트랜잭션 경계(매 호출 독립 커밋)를 재현하기 위해 클래스에 @Transactional을 붙이지 않는다.
 * → 재사용 폐기의 롤백/커밋을 메모리 더티가 아닌 커밋된 DB 상태로 검증.
 */
@SpringBootTest
@ActiveProfiles("test")   // Testcontainers(jdbc:tc) 사용 + 기존 컨텍스트 캐시 공유(부팅 1회)
class RefreshTokenRotationIntegrationTest {

    @Autowired RefreshTokenRotationService rotationService;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenHasher hasher;

    private Long userId;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
        OffsetDateTime now = OffsetDateTime.now();
        User user = User.createLocalUser("rot@b.com", "{bcrypt}x", "회전",
                now, now, "2026-05-16");
        userId = userRepository.saveAndFlush(user).getId();
    }

    /** 같은 원시 토큰으로 회전 후 재사용 → REUSED + DB 활성 토큰 0개(커밋 영속 확인). */
    @Test
    void reuse_of_rotated_token_revokes_all_active_and_persists() {
        OffsetDateTime now = OffsetDateTime.now();
        String raw = "raw-refresh-token-1";
        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(userId, hasher.hash(raw), now, now.plusDays(14)));

        RefreshOutcome first = rotationService.rotate(raw, OffsetDateTime.now());
        assertThat(first.status()).isEqualTo(RefreshOutcome.Status.ROTATED);

        RefreshOutcome second = rotationService.rotate(raw, OffsetDateTime.now());
        assertThat(second.status()).isEqualTo(RefreshOutcome.Status.REUSED);

        // 새 트랜잭션(새 쿼리)로 조회 — 메모리 더티가 아닌 커밋된 DB 상태
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).isEmpty();
    }

    /**
     * #5 회귀: 자연 만료된 토큰을 재시도해도 REUSED로 오분류해 family를 몰살하지 않는다.
     * 첫 rotate가 만료 토큰을 EXPIRED로 마킹 → 같은 토큰 재시도는 revokedReason=EXPIRED를 보고
     * 다시 EXPIRED 응답(폐기 없음). 다른 디바이스의 활성 토큰은 살아남아야 한다.
     */
    @Test
    void retry_of_expired_token_returns_expired_and_keeps_other_sessions() {
        OffsetDateTime now = OffsetDateTime.now();
        // 이미 만료된 토큰 (expires_at < now)
        String expiredRaw = "raw-expired-token";
        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(userId, hasher.hash(expiredRaw), now.minusDays(15), now.minusDays(1)));
        // 다른 디바이스의 활성 토큰 (몰살되면 안 됨)
        String activeRaw = "raw-active-token";
        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(userId, hasher.hash(activeRaw), now, now.plusDays(14)));

        RefreshOutcome first = rotationService.rotate(expiredRaw, OffsetDateTime.now());
        assertThat(first.status()).isEqualTo(RefreshOutcome.Status.EXPIRED);

        RefreshOutcome second = rotationService.rotate(expiredRaw, OffsetDateTime.now());
        assertThat(second.status()).isEqualTo(RefreshOutcome.Status.EXPIRED);

        // 만료 토큰 재시도가 활성 세션을 몰살하지 않았음 (버그였다면 여기서 0개)
        assertThat(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)).hasSize(1);
    }

    /** 차단(SUSPENDED) 유저의 rotate → BLOCKED. 토큰은 이미 claim되어 소모됨 (의도 — spec 3.3). */
    @Test
    void rotate_with_suspended_user_returns_blocked() {
        OffsetDateTime now = OffsetDateTime.now();
        String raw = "raw-refresh-token-blocked";
        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(userId, hasher.hash(raw), now, now.plusDays(14)));
        User user = userRepository.findById(userId).orElseThrow();
        user.suspend();
        userRepository.saveAndFlush(user);

        RefreshOutcome outcome = rotationService.rotate(raw, OffsetDateTime.now());

        assertThat(outcome.status()).isEqualTo(RefreshOutcome.Status.BLOCKED);
    }

    /** 같은 토큰 동시 2건 → 정확히 하나만 ROTATED, 나머지는 ROTATED 아님(경합 패배=REUSED). */
    @Test
    void concurrent_rotation_of_same_token_allows_only_one_winner() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        String raw = "raw-refresh-token-concurrent";
        refreshTokenRepository.saveAndFlush(
                RefreshToken.issue(userId, hasher.hash(raw), now, now.plusDays(14)));

        int threads = 2;
        var barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<RefreshOutcome.Status> results = Collections.synchronizedList(new ArrayList<>());

        List<Callable<Void>> tasks = List.of(
                () -> { barrier.await(); results.add(rotationService.rotate(raw, OffsetDateTime.now()).status()); return null; },
                () -> { barrier.await(); results.add(rotationService.rotate(raw, OffsetDateTime.now()).status()); return null; });
        for (Future<Void> f : pool.invokeAll(tasks)) {
            f.get();
        }
        pool.shutdown();

        long rotated = results.stream().filter(s -> s == RefreshOutcome.Status.ROTATED).count();
        assertThat(rotated).isEqualTo(1);
        assertThat(results).hasSize(2);
    }
}
