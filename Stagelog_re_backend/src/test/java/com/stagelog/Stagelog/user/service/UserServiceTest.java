package com.stagelog.Stagelog.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.UnauthorizedException;
import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.dto.UserProfileResponse;
import com.stagelog.Stagelog.user.dto.UserUpdateRequest;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * UserService — 차단(SUSPENDED/DELETED) 계정의 일반 API 접근 차단 검증.
 *
 * <p>stateless 필터는 claims-only라 status를 모른다. 그래서 user를 이미 로드하는
 * service 계층에서 추가 쿼리 없이 status를 검사한다(login/refresh와 동일한 403 AUTH_ACCOUNT_BLOCKED).
 * 차단 유저는 ≤15분 잔존 access token으로 이 경로에 도달할 수 있는데, 그 창을 여기서 닫는다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private static final UUID PUBLIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    @DisplayName("ACTIVE 유저는 프로필을 정상 조회한다")
    void getMyProfile_withActiveUser_returnsProfile() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPublicId()).thenReturn(PUBLIC_ID);
        when(user.getProvider()).thenReturn(Provider.LOCAL);
        when(userRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getMyProfile(PUBLIC_ID);

        assertThat(response).isNotNull();
        assertThat(response.getPublicId()).isEqualTo(PUBLIC_ID.toString());
    }

    @Test
    @DisplayName("탈퇴(DELETED) 유저가 유효한 access token으로 프로필을 조회하면 403으로 차단된다")
    void getMyProfile_withDeletedUser_throwsAccountBlocked() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.DELETED);
        when(userRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.getMyProfile(PUBLIC_ID))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> assertThat(((UnauthorizedException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED));
    }

    @Test
    @DisplayName("정지(SUSPENDED) 유저의 프로필 수정은 403으로 차단되고 도메인 변경이 일어나지 않는다")
    void updateProfile_withSuspendedUser_throwsAccountBlocked() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);
        when(userRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(user));
        UserUpdateRequest request = mock(UserUpdateRequest.class);

        assertThatThrownBy(() -> userService.updateProfile(PUBLIC_ID, request))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> assertThat(((UnauthorizedException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED));

        verify(user, never()).updateProfile(any(), any(), any());
    }

    @Test
    @DisplayName("이미 탈퇴(DELETED)한 유저의 재삭제 시도는 403으로 차단된다")
    void deleteUser_withDeletedUser_throwsAccountBlocked() {
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.DELETED);
        when(userRepository.findByPublicId(PUBLIC_ID)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.deleteUser(PUBLIC_ID))
                .isInstanceOf(UnauthorizedException.class)
                .satisfies(e -> assertThat(((UnauthorizedException) e).getErrorCode())
                        .isEqualTo(ErrorCode.AUTH_ACCOUNT_BLOCKED));

        verify(user, never()).delete();
    }
}
