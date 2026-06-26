package com.stagelog.Stagelog.auth.service;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.auth.dto.RefreshOutcome;
import com.stagelog.Stagelog.auth.dto.SignupRequest;
import com.stagelog.Stagelog.global.config.TermsProperties;
import com.stagelog.Stagelog.global.exception.DuplicateEntityException;
import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.InvalidInputException;
import com.stagelog.Stagelog.global.exception.UnauthorizedException;
import com.stagelog.Stagelog.global.jwt.repository.RefreshTokenRepository;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
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
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final AuthTokenIssuer authTokenIssuer;
    private final RefreshTokenRotationService rotationService;
    private final TermsProperties termsProperties;

    @Transactional
    public UUID signUp(SignupRequest request) {
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
            // 내부 PK(Long)가 아니라 외부 노출 안전 식별자(publicId)를 반환한다.
            return userRepository.saveAndFlush(user).getPublicId();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEntityException(ErrorCode.USER_EMAIL_DUPLICATE);
        }
    }

    @Transactional
    public AuthTokenResult login(LoginRequest request, String clientIp) {
        // 저장 시 정규화된 email과 일치시키고, 잠금 카운터 키도 정규화로 통일한다.
        String email = User.normalizeEmail(request.getEmail());
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
     * Refresh token rotation. 실제 회전/폐기는 RefreshTokenRotationService(단일 트랜잭션)에 위임하고,
     * 본 메서드는 결과를 HTTP 예외로 매핑하는 얇은 경계(비트랜잭션)다.
     *
     * <p>회전 권한은 RotationService의 조건부 원자 UPDATE로 획득하고, 폐기는 같은 트랜잭션에서 커밋된다.
     * throw가 트랜잭션 밖이라 재사용 폐기를 롤백하지 않는다. (ADR-0005 Update 2026-06-12)
     */
    public AuthTokenResult refresh(String refreshTokenValue) {
        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        RefreshOutcome outcome = rotationService.rotate(refreshTokenValue, OffsetDateTime.now());
        return switch (outcome.status()) {
            case ROTATED -> outcome.tokens();
            case REUSED -> throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_REUSED);
            case EXPIRED -> throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_EXPIRED);
            case INVALID -> throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
            case BLOCKED -> throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_BLOCKED);
        };
    }

    /**
     * 로그아웃 — 해당 user의 모든 활성 refresh token을 markRevoked("LOGOUT")로 폐기.
     *  delete가 아닌 이유: 감사 로그 + reuse detection 일관성(추후 옛 token 재사용 시도 시 chain 추적 가능).
     */
    @Transactional
    public void logout(UUID publicId) {
        User user = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        OffsetDateTime now = OffsetDateTime.now();
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(user.getId())
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
