package com.stagelog.Stagelog.auth.service;

import static org.assertj.core.api.Assertions.*;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.UnauthorizedException;
import com.stagelog.Stagelog.global.jwt.repository.RefreshTokenRepository;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task 3 — Row-per-token refresh rotation + reuse detection 검증.
 *
 * 각 케이스:
 *  1. refresh_creates_new_row_and_marks_old_as_rotated
 *     → refresh 시 새 row INSERT + 옛 row를 markRotated("ROTATED")로 폐기.
 *
 *  2. refresh_reuse_of_old_token_revokes_all_user_tokens
 *     → 이미 rotation된 토큰을 다시 사용하면 user 모든 활성 토큰을 "REUSED"로 폐기 + AUTH_REFRESH_REUSED throw.
 *
 *  3. logout_marks_all_active_tokens_as_revoked_with_LOGOUT_reason
 *     → logout이 delete가 아닌 markRevoked("LOGOUT")로 이력 보존.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceRefreshRotationTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private User savedUser;
    private String initialRaw;

    @BeforeEach
    void seed() {
        // given — LOCAL 사용자 + 첫 로그인으로 refresh token 1개 발급
        OffsetDateTime now = OffsetDateTime.now();
        savedUser = userRepository.save(User.createLocalUser(
                "a@b.com", passwordEncoder.encode("Pwd!1234"), "닉네임",
                now, now, "2026-05-16"));
        AuthTokenResult init = authService.login(
                LoginRequest.builder().email("a@b.com").password("Pwd!1234").build(),
                "127.0.0.1");
        // AuthTokenResult는 record — getter는 method name(no 'get' prefix)
        initialRaw = init.refreshToken();
    }

    @Test
    void refresh_creates_new_row_and_marks_old_as_rotated() {
        // when
        authService.refresh(initialRaw);

        // then — 옛 row는 ROTATED + rotated_to_id가 새 row id, 새 row는 active
        var rows = refreshTokenRepository.findAllByUserIdOrderByIdAsc(savedUser.getId());
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).getRevokedReason()).isEqualTo("ROTATED");
        assertThat(rows.get(0).getRotatedToId()).isEqualTo(rows.get(1).getId());
        assertThat(rows.get(1).getRevokedAt()).isNull();
    }

    @Test
    void refresh_reuse_of_old_token_revokes_all_user_tokens() {
        // given — 1차 rotation 성공
        authService.refresh(initialRaw);

        // when / then — 옛 토큰 재사용 시도 → AUTH_REFRESH_REUSED + 모든 활성 토큰 폐기
        assertThatThrownBy(() -> authService.refresh(initialRaw))
                .isInstanceOf(UnauthorizedException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.AUTH_REFRESH_REUSED);

        var rows = refreshTokenRepository.findAllByUserIdOrderByIdAsc(savedUser.getId());
        assertThat(rows).allMatch(t -> t.getRevokedAt() != null);
    }

    @Test
    void logout_marks_all_active_tokens_as_revoked_with_LOGOUT_reason() {
        // given — 다른 디바이스로 한 번 더 로그인해 활성 row 2개
        authService.login(
                LoginRequest.builder().email("a@b.com").password("Pwd!1234").build(),
                "127.0.0.2");

        // when
        authService.logout(savedUser.getId());

        // then — 모든 row가 LOGOUT 사유로 폐기, delete 아님(이력 보존)
        var rows = refreshTokenRepository.findAllByUserIdOrderByIdAsc(savedUser.getId());
        assertThat(rows).allMatch(t -> "LOGOUT".equals(t.getRevokedReason()));
        assertThat(rows).isNotEmpty();
    }
}
