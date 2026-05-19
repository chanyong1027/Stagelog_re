package com.stagelog.Stagelog.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.global.config.TermsProperties;
import com.stagelog.Stagelog.global.exception.ErrorCode;
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
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private RefreshTokenHasher refreshTokenHasher;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private AuthTokenIssuer authTokenIssuer;
    @Mock
    private TermsProperties termsProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessTokenValidity(3600000L);
        jwtProperties.setRefreshTokenValidity(1209600000L);
        authService = new AuthService(
                userRepository,
                jwtTokenProvider,
                jwtProperties,
                refreshTokenHasher,
                refreshTokenRepository,
                passwordEncoder,
                loginAttemptService,
                authTokenIssuer,
                termsProperties
        );
    }

    @Test
    @DisplayName("존재하지 않는 email로 로그인하면 실패 횟수를 기록하고 공통 인증 실패를 반환한다")
    void login_withUnknownEmail_recordsFailureAndThrowsInvalidCredentials() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(throwable -> {
                    UnauthorizedException exception = (UnauthorizedException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });

        verify(loginAttemptService).validateNotLocked("ghost@example.com", "127.0.0.1");
        verify(loginAttemptService).recordFailure("ghost@example.com", "127.0.0.1");
        verify(loginAttemptService, never()).clearFailures("ghost@example.com", "127.0.0.1");
    }

    @Test
    @DisplayName("비밀번호가 틀리면 실패 횟수를 기록하고 공통 인증 실패를 반환한다")
    void login_withWrongPassword_recordsFailureAndThrowsInvalidCredentials() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("user@example.com");
        when(request.getPassword()).thenReturn("wrong-password");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("encoded-password");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-password", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(throwable -> {
                    UnauthorizedException exception = (UnauthorizedException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });

        verify(loginAttemptService).validateNotLocked("user@example.com", "127.0.0.1");
        verify(loginAttemptService).recordFailure("user@example.com", "127.0.0.1");
        verify(loginAttemptService, never()).clearFailures("user@example.com", "127.0.0.1");
    }

    @Test
    @DisplayName("소셜 계정처럼 비밀번호가 null인 사용자는 로컬 로그인에 실패한다")
    void login_withNullPassword_failsAsInvalidCredentials() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("social@example.com");
        when(request.getPassword()).thenReturn("any-password");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn(null);
        when(userRepository.findByEmail("social@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(throwable -> {
                    UnauthorizedException exception = (UnauthorizedException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_INVALID_CREDENTIALS);
                });

        verify(loginAttemptService).validateNotLocked("social@example.com", "127.0.0.1");
        verify(loginAttemptService).recordFailure("social@example.com", "127.0.0.1");
        verify(loginAttemptService, never()).clearFailures("social@example.com", "127.0.0.1");
        verify(passwordEncoder, never()).matches("any-password", null);
    }

    @Test
    @DisplayName("정지 계정은 비밀번호가 맞아도 토큰 발급이 차단된다")
    void login_withSuspendedUser_throwsAccountBlocked() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("suspended@example.com");
        when(request.getPassword()).thenReturn("correct-password");

        User user = mock(User.class);
        when(user.getPassword()).thenReturn("encoded-password");
        when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("suspended@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-password", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(throwable -> {
                    UnauthorizedException exception = (UnauthorizedException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED);
                });

        verify(loginAttemptService).validateNotLocked("suspended@example.com", "127.0.0.1");
        verify(loginAttemptService, never()).recordFailure("suspended@example.com", "127.0.0.1");
        verify(loginAttemptService, never()).clearFailures("suspended@example.com", "127.0.0.1");
        verify(user, never()).updateLastLoginAt(any(OffsetDateTime.class));
        verify(authTokenIssuer, never()).issueFor(any());
    }

    @Test
    @DisplayName("로그인 시도 제한에 걸리면 사용자 조회 전에 429 에러를 반환한다")
    void login_whenLocked_throwsTooManyAttemptsBeforeUserLookup() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("user@example.com");
        doThrow(new UnauthorizedException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS))
                .when(loginAttemptService).validateNotLocked("user@example.com", "127.0.0.1");

        assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(throwable -> {
                    UnauthorizedException exception = (UnauthorizedException) throwable;
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_TOO_MANY_ATTEMPTS);
                });

        verify(userRepository, never()).findByEmail("user@example.com");
        verify(loginAttemptService, never()).recordFailure("user@example.com", "127.0.0.1");
    }

    @Test
    @DisplayName("refresh(Task 2 simplified): 옛 토큰 row 삭제 후 새 토큰 발급한다")
    void refresh_deletesOldRowAndIssuesNew() {
        String oldRefreshToken = "old-refresh-token";
        String oldRefreshHash = "old-refresh-hash";
        Long userId = 42L;

        RefreshToken stored = mock(RefreshToken.class);
        User user = mock(User.class);

        when(jwtTokenProvider.getTokenValidationResult(oldRefreshToken))
                .thenReturn(JwtTokenProvider.TokenValidationResult.VALID);
        when(jwtTokenProvider.isRefreshToken(oldRefreshToken)).thenReturn(true);
        when(refreshTokenHasher.hash(oldRefreshToken)).thenReturn(oldRefreshHash);
        when(refreshTokenRepository.findByTokenHash(oldRefreshHash)).thenReturn(Optional.of(stored));
        when(stored.isExpired(any(OffsetDateTime.class))).thenReturn(false);
        when(stored.getUserId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        AuthTokenResult expected = AuthTokenResult.of(
                "new-access", "new-refresh", UUID.randomUUID().toString(),
                "user@example.com", "tester");
        when(authTokenIssuer.issueFor(user)).thenReturn(expected);

        AuthTokenResult result = authService.refresh(oldRefreshToken);

        assertThat(result).isEqualTo(expected);
        verify(refreshTokenRepository).delete(stored);
        verify(authTokenIssuer).issueFor(user);
    }
}
