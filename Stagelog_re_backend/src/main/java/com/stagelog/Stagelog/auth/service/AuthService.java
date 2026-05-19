package com.stagelog.Stagelog.auth.service;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.auth.dto.SignupRequest;
import com.stagelog.Stagelog.global.config.TermsProperties;
import com.stagelog.Stagelog.global.exception.DuplicateEntityException;
import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.InvalidInputException;
import com.stagelog.Stagelog.global.exception.UnauthorizedException;
import com.stagelog.Stagelog.global.jwt.JwtProperties;
import com.stagelog.Stagelog.global.jwt.JwtTokenProvider;
import com.stagelog.Stagelog.global.jwt.RefreshTokenHasher;
import com.stagelog.Stagelog.global.jwt.domain.RefreshToken;
import com.stagelog.Stagelog.global.jwt.repository.RefreshTokenRepository;
import java.time.Duration;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthTokenIssuer authTokenIssuer;
    private final TermsProperties termsProperties;

    @Transactional
    public Long signUp(SignupRequest request) {
        // Defense in depth — controller @Valid가 false를 막지만, 다른 호출 경로 방어.
        if (!request.isAgreedToTerms()) {
            throw new InvalidInputException(ErrorCode.AUTH_TERMS_NOT_AGREED);
        }
        String encoded = passwordEncoder.encode(request.getPassword());
        OffsetDateTime now = OffsetDateTime.now();
        User user = User.createLocalUser(
                request.getEmail(), encoded, request.getNickname(),
                now, now, termsProperties.getCurrentVersion()
        );
        // AC (epics.md Story 1.2): try-find-then-insert 금지 → DB UNIQUE 위반 catch.
        try {
            return userRepository.saveAndFlush(user).getId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEntityException(ErrorCode.USER_EMAIL_DUPLICATE);
        }
    }

    @Transactional
    public AuthTokenResult login(LoginRequest request, String clientIp) {
        String email = request.getEmail();
        loginAttemptService.validateNotLocked(email, clientIp);

        Optional<User> opt = userRepository.findByEmail(email);
        if (opt.isEmpty()) {
            loginAttemptService.recordFailure(email, clientIp);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        User user = opt.get();
        if (!isValidPassword(request.getPassword(), user.getPassword())) {
            loginAttemptService.recordFailure(email, clientIp);
            throw new UnauthorizedException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        assertActiveUser(user);
        loginAttemptService.clearFailures(email, clientIp);
        user.updateLastLoginAt(OffsetDateTime.now());

        return authTokenIssuer.issueFor(user);
    }

    /**
     * Refresh token rotation + reuse detection.
     *
     * 정상 흐름: 옛 row를 markRotated("ROTATED")로 폐기 + 새 row INSERT + rotated_to_id 연결.
     *
     * Reuse detection: 이미 rotate(또는 revoke)된 토큰을 다시 사용하려는 시도는 탈취 의심 신호로 본다.
     *  → user의 모든 활성 토큰을 "REUSED"로 일괄 폐기 + AUTH_REFRESH_REUSED throw.
     *
     * EXPIRED/INVALID 분기는 JWT 자체 검증 결과(getTokenValidationResult)를 우선 사용한다
     *  — validateToken()(boolean) 한 가지로 묶으면 EXPIRED와 INVALID를 구분할 수 없기 때문.
     */
    @Transactional
    public AuthTokenResult refresh(String refreshTokenValue) {
        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        JwtTokenProvider.TokenValidationResult result =
                jwtTokenProvider.getTokenValidationResult(refreshTokenValue);
        OffsetDateTime now = OffsetDateTime.now();

        if (result == JwtTokenProvider.TokenValidationResult.EXPIRED) {
            // JWT 자체 만료 — DB row가 살아 있으면 EXPIRED revoke + throw
            String hash = refreshTokenHasher.hash(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(stored -> {
                if (!stored.isRevoked()) {
                    stored.markRevoked("EXPIRED", now);
                }
            });
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
        if (result == JwtTokenProvider.TokenValidationResult.INVALID
                || !jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        String hash = refreshTokenHasher.hash(refreshTokenValue);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID));

        // 탈취 의심: 이미 ROTATED 또는 REVOKED된 토큰 재사용 → 전체 user 세션 폐기
        if (stored.isRotated() || stored.isRevoked()) {
            refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(stored.getUserId())
                    .forEach(t -> t.markRevoked("REUSED", now));
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED);
        }
        // DB expires_at이 JWT exp보다 짧게 설정된 비대칭 케이스 방어 (보통 위에서 잡힘)
        if (stored.isExpired(now)) {
            stored.markRevoked("EXPIRED", now);
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        assertActiveUser(user);

        // 새 토큰 발급 + 옛 row를 markRotated로 chain 연결
        OffsetDateTime expiresAt = now.plus(Duration.ofMillis(jwtProperties.getRefreshTokenValidity()));
        TokenPairWithHash pair = authTokenIssuer.createTokenPair(user.getEmail(), user.getRole().getValue());
        RefreshToken newRow = refreshTokenRepository.save(
                RefreshToken.issue(user.getId(), pair.refreshTokenHash(), now, expiresAt));
        stored.markRotated(newRow.getId(), now);

        // 외부 응답엔 publicId만 — 내부 PK 절대 노출 금지
        return AuthTokenResult.of(
                pair.accessToken(), pair.refreshToken(),
                user.getPublicId().toString(), user.getEmail(), user.getNickname());
    }

    /**
     * 로그아웃 — 해당 user의 모든 활성 refresh token을 markRevoked("LOGOUT")로 폐기.
     *  delete가 아닌 이유: 감사 로그 + reuse detection 일관성(추후 옛 token 재사용 시도 시 chain 추적 가능).
     */
    @Transactional
    public void logout(Long userId) {
        OffsetDateTime now = OffsetDateTime.now();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId)
                .forEach(t -> t.markRevoked("LOGOUT", now));
    }

    private boolean isValidPassword(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword)) {
            return false;
        }
        try {
            return passwordEncoder.matches(rawPassword, encodedPassword);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void assertActiveUser(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_BLOCKED);
        }
    }
}
