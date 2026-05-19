package com.stagelog.Stagelog.auth.oauth2.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stagelog.Stagelog.auth.oauth2.userinfo.OAuth2UserInfo;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.Role;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.service.UserService;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

/**
 * CustomOAuth2UserService 단위 테스트.
 * HTTP 호출(super.loadUser)을 제외한 검증·동기화 로직을 processUserInfo()를 통해 직접 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private UserService userService;

    private CustomOAuth2UserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOAuth2UserService(userService);
    }

    // ── providerId 검증 ─────────────────────────────────────────────────────

    @Test
    @DisplayName("providerId가 null이면 AUTH_OAUTH2_PROVIDER_ERROR로 인증 실패한다")
    void processUserInfo_withNullProviderId_throwsProviderError() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn(null);

        assertThatThrownBy(() -> service.processUserInfo(userInfo, Map.of(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(
                        ((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_OAUTH2_PROVIDER_ERROR.getCode()));

        verify(userService, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    // ── email 검증 ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("email이 null이면 AUTH_OAUTH2_MISSING_EMAIL로 인증 실패한다")
    void processUserInfo_withNullEmail_throwsMissingEmail() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("12345");
        when(userInfo.email()).thenReturn(null);

        assertThatThrownBy(() -> service.processUserInfo(userInfo, Map.of(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(
                        ((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_OAUTH2_MISSING_EMAIL.getCode()));

        verify(userService, never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    // ── 이메일 충돌 ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("동일 이메일이 다른 provider로 가입돼 있으면 AUTH_OAUTH2_EMAIL_CONFLICT로 인증 실패한다")
    void processUserInfo_withEmailConflict_throwsEmailConflict() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-123");
        when(userInfo.email()).thenReturn("conflict@example.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);

        User localUser = mock(User.class);
        when(localUser.getProvider()).thenReturn(Provider.LOCAL);
        when(userService.findByEmail("conflict@example.com")).thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> service.processUserInfo(userInfo, Map.of(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(
                        ((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_OAUTH2_EMAIL_CONFLICT.getCode()));

        verify(userService, never()).getOrCreateUser(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("동일 이메일이 동일 provider로 가입돼 있으면 충돌 없이 upsert로 진행한다")
    void processUserInfo_withSameProviderEmail_proceedsNormally() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-123");
        when(userInfo.email()).thenReturn("user@kakao.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);
        when(userInfo.nickname()).thenReturn("카카오유저");
        when(userInfo.profileImageUrl()).thenReturn(null);

        User existingUser = mock(User.class);
        when(existingUser.getProvider()).thenReturn(Provider.KAKAO);
        when(existingUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(existingUser.getRole()).thenReturn(Role.USER);
        when(userService.findByEmail("user@kakao.com")).thenReturn(Optional.of(existingUser));
        when(userService.getOrCreateUser("user@kakao.com", "카카오유저", null, Provider.KAKAO, "kakao-123"))
                .thenReturn(existingUser);

        org.springframework.security.oauth2.core.user.OAuth2User result =
                service.processUserInfo(userInfo, Map.of("id", 123L), "id");

        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        assertThat(((CustomOAuth2User) result).getUser()).isEqualTo(existingUser);
    }

    // ── 계정 상태 차단 ──────────────────────────────────────────────────────

    @Test
    @DisplayName("정지된 계정은 AUTH_ACCOUNT_BLOCKED로 인증 실패한다")
    void processUserInfo_withSuspendedUser_throwsAccountBlocked() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-456");
        when(userInfo.email()).thenReturn("suspended@example.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);
        when(userInfo.nickname()).thenReturn("정지유저");
        when(userInfo.profileImageUrl()).thenReturn(null);

        when(userService.findByEmail("suspended@example.com")).thenReturn(Optional.empty());

        User suspendedUser = mock(User.class);
        when(suspendedUser.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(userService.getOrCreateUser("suspended@example.com", "정지유저", null, Provider.KAKAO, "kakao-456"))
                .thenReturn(suspendedUser);

        assertThatThrownBy(() -> service.processUserInfo(userInfo, Map.of(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(
                        ((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED.getCode()));
    }

    @Test
    @DisplayName("탈퇴한 계정은 AUTH_ACCOUNT_BLOCKED로 인증 실패한다")
    void processUserInfo_withDeletedUser_throwsAccountBlocked() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-789");
        when(userInfo.email()).thenReturn("deleted@example.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);
        when(userInfo.nickname()).thenReturn("탈퇴유저");
        when(userInfo.profileImageUrl()).thenReturn(null);

        when(userService.findByEmail("deleted@example.com")).thenReturn(Optional.empty());

        User deletedUser = mock(User.class);
        when(deletedUser.getStatus()).thenReturn(UserStatus.DELETED);
        when(userService.getOrCreateUser("deleted@example.com", "탈퇴유저", null, Provider.KAKAO, "kakao-789"))
                .thenReturn(deletedUser);

        assertThatThrownBy(() -> service.processUserInfo(userInfo, Map.of(), "id"))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(
                        ((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED.getCode()));
    }

    // ── 신규 유저 정상 처리 ─────────────────────────────────────────────────

    @Test
    @DisplayName("신규 KAKAO 유저는 consent 3컬럼이 NULL로 영속된다 (auto-set 금지)")
    void new_kakao_user_is_persisted_with_null_consent_fields() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-consent");
        when(userInfo.email()).thenReturn("k@b.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);
        when(userInfo.nickname()).thenReturn("카카오닉");
        when(userInfo.profileImageUrl()).thenReturn(null);

        when(userService.findByEmail("k@b.com")).thenReturn(Optional.empty());
        // 실제 User.createSocialUser를 통해 invariant 검증
        User actual = User.createSocialUser("k@b.com", "카카오닉", null, Provider.KAKAO, "kakao-consent");
        when(userService.getOrCreateUser("k@b.com", "카카오닉", null, Provider.KAKAO, "kakao-consent"))
                .thenReturn(actual);

        org.springframework.security.oauth2.core.user.OAuth2User result =
                service.processUserInfo(userInfo, Map.of("id", 12345L), "id");

        User persisted = ((CustomOAuth2User) result).getUser();
        assertThat(persisted.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(persisted.getAgeConfirmedAt()).isNull();
        assertThat(persisted.getTermsAgreedAt()).isNull();
        assertThat(persisted.getTermsVersion()).isNull();
    }

    @Test
    @DisplayName("신규 소셜 유저는 CustomOAuth2User로 래핑되어 반환된다")
    void processUserInfo_withNewUser_returnsCustomOAuth2User() {
        OAuth2UserInfo userInfo = mock(OAuth2UserInfo.class);
        when(userInfo.providerId()).thenReturn("kakao-new");
        when(userInfo.email()).thenReturn("new@kakao.com");
        when(userInfo.provider()).thenReturn(Provider.KAKAO);
        when(userInfo.nickname()).thenReturn("신규유저");
        when(userInfo.profileImageUrl()).thenReturn("https://img.kakao.com/profile.jpg");

        when(userService.findByEmail("new@kakao.com")).thenReturn(Optional.empty());

        User newUser = mock(User.class);
        when(newUser.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(newUser.getRole()).thenReturn(Role.USER);
        when(userService.getOrCreateUser(
                "new@kakao.com", "신규유저", "https://img.kakao.com/profile.jpg",
                Provider.KAKAO, "kakao-new"))
                .thenReturn(newUser);

        Map<String, Object> attrs = Map.of("id", 999L);
        org.springframework.security.oauth2.core.user.OAuth2User result =
                service.processUserInfo(userInfo, attrs, "id");

        assertThat(result).isInstanceOf(CustomOAuth2User.class);
        CustomOAuth2User customUser = (CustomOAuth2User) result;
        assertThat(customUser.getUser()).isEqualTo(newUser);
        assertThat(customUser.getAttributes()).isEqualTo(attrs);
        assertThat(customUser.getAuthorities()).hasSize(1);
        assertThat(customUser.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_USER");
    }
}
