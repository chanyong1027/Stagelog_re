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
