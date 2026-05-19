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
     * Task 2 simplified refresh: find by hash → delete → INSERT new.
     *  EXPIRED/INVALID 분기는 Task 3와 동일한 getTokenValidationResult() 패턴 사용 (일관성).
     *  단 Task 2에선 markRevoked 이력을 남기지 않고 단순 delete — Task 3에서 markRotated/markRevoked로 교체.
     */
    @Transactional
    public AuthTokenResult refresh(String refreshTokenValue) {
        if (!StringUtils.hasText(refreshTokenValue)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        JwtTokenProvider.TokenValidationResult result = jwtTokenProvider.getTokenValidationResult(refreshTokenValue);
        if (result == JwtTokenProvider.TokenValidationResult.EXPIRED) {
            // Task 2: DB row가 있으면 그냥 delete (markRevoked는 Task 3에서)
            String hash = refreshTokenHasher.hash(refreshTokenValue);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(refreshTokenRepository::delete);
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
        if (result == JwtTokenProvider.TokenValidationResult.INVALID
            || !jwtTokenProvider.isRefreshToken(refreshTokenValue)) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }

        String hash = refreshTokenHasher.hash(refreshTokenValue);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID));

        OffsetDateTime now = OffsetDateTime.now();
        if (stored.isExpired(now)) {
            refreshTokenRepository.delete(stored);
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_EXPIRED);
        }
        User user = userRepository.findById(stored.getUserId())
            .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
        assertActiveUser(user);

        refreshTokenRepository.delete(stored);
        return authTokenIssuer.issueFor(user);
    }

    /** Task 2 simplified logout: delete all by user (이력 없음). Task 3에서 markRevoked로 교체. */
    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.findAllByUserId(userId)
                .forEach(refreshTokenRepository::delete);
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
