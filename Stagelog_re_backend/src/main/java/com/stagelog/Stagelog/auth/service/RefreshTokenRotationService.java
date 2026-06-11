package com.stagelog.Stagelog.auth.service;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.RefreshOutcome;
import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.jwt.JwtProperties;
import com.stagelog.Stagelog.global.jwt.RefreshTokenHasher;
import com.stagelog.Stagelog.global.jwt.domain.RefreshToken;
import com.stagelog.Stagelog.global.jwt.repository.RefreshTokenRepository;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh 회전의 단일 트랜잭션 쓰기 경계.
 * 회전 권한을 조건부 원자 UPDATE로 획득(동시 경합 안전)하고,
 * 실패(affected==0)면 사유를 판별해 family 폐기까지 같은 트랜잭션에서 커밋한다.
 * 예외를 던지지 않고 RefreshOutcome을 반환 → 호출측(AuthService)이 HTTP로 매핑.
 *
 * <p>refresh 토큰의 권위는 JWT 서명이 아니라 DB의 token_hash(HMAC-SHA256 + pepper)다.
 * 위조/변조 토큰은 문자열이 달라 해시가 불일치 → DB 미존재 → INVALID. (ADR-0005 Update 2026-06-12)
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenRotationService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final JwtProperties jwtProperties;
    private final AuthTokenIssuer authTokenIssuer;

    @Transactional
    public RefreshOutcome rotate(String rawToken, OffsetDateTime now) {
        String hash = refreshTokenHasher.hash(rawToken);

        RefreshToken row = refreshTokenRepository.findByTokenHash(hash).orElse(null);
        if (row == null) {
            return RefreshOutcome.invalid();
        }
        Long ownerId = row.getUserId();

        int claimed = refreshTokenRepository.claimForRotation(hash, now);
        if (claimed == 0) {
            // 경합 패배 또는 이미 폐기/만료. 사유를 fresh로 재판별.
            RefreshToken fresh = refreshTokenRepository.findByTokenHash(hash).orElse(null);
            if (fresh != null && fresh.getRevokedAt() != null) {
                refreshTokenRepository.revokeAllActiveByUserId(ownerId, "REUSED", now);
                return RefreshOutcome.reused();
            }
            // claim 실패 + 미폐기 = 자연 만료(expires_at <= now). 만료 row를 EXPIRED로 마킹
            // → "revoked_at NULL = 사용 가능" 불변식 유지 + 만료 row 누적 방지 (옛 동작 보존).
            refreshTokenRepository.revokeByHashIfActive(hash, "EXPIRED", now);
            return RefreshOutcome.expired();
        }

        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            // 차단 유저: 방금 claim한 회전은 이미 ROTATED로 소모됨 → 같은 토큰 재시도는 REUSED 경로 (의도)
            return RefreshOutcome.blocked();
        }

        OffsetDateTime expiresAt = now.plus(Duration.ofMillis(jwtProperties.getRefreshTokenValidity()));
        TokenPairWithHash pair = authTokenIssuer.createTokenPair(
                user.getEmail(), user.getRole().getValue(), user.getPublicId());
        RefreshToken newRow = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), pair.refreshTokenHash(), now, expiresAt));
        refreshTokenRepository.linkRotatedTo(hash, newRow.getId());

        return RefreshOutcome.rotated(AuthTokenResult.of(
                pair.accessToken(), pair.refreshToken(),
                user.getPublicId().toString(), user.getEmail(), user.getNickname()));
    }
}
