package com.stagelog.Stagelog.user.service;

import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public User getOrCreateUser(
            String email,
            String nickname,
            String profileImageUrl,
            Provider provider,
            String providerId
    ) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseGet(() -> userRepository.save(
                        User.createSocialUser(email, nickname, profileImageUrl, provider, providerId)
                ));
    }

    public Optional<User> findByEmail(String email) {
        // 저장 시 정규화된 email과 일치시킨다 (OAuth 이메일 충돌 검사 등 조회 경로 통일).
        return userRepository.findByEmail(User.normalizeEmail(email));
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    public User getUserByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 차단(SUSPENDED/DELETED) 계정의 일반 API 접근을 차단한다.
     * stateless 필터는 claims-only라 status를 모르므로, user를 이미 로드하는 이 계층에서
     * 추가 쿼리 없이 status를 검사한다 — login/refresh와 동일한 403 AUTH_ACCOUNT_BLOCKED.
     * (차단 후 ≤15분 잔존 access token으로 도달하는 창을, user를 로드하는 경로에 한해 즉시 닫음)
     */
    private User getActiveUserByPublicId(UUID publicId) {
        User user = getUserByPublicId(publicId);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException(ErrorCode.AUTH_ACCOUNT_BLOCKED);
        }
        return user;
    }

    public UserProfileResponse getMyProfile(UUID publicId) {
        User user = getActiveUserByPublicId(publicId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID publicId, UserUpdateRequest request) {
        User user = getActiveUserByPublicId(publicId);
        user.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getEmailNotificationEnabled()
        );
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUser(UUID publicId) {
        User user = getActiveUserByPublicId(publicId);
        user.delete();
    }

    @Transactional
    public void suspendUser(Long userId) {
        User user = getUserById(userId);
        user.suspend();
    }

    @Transactional
    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.activate();
    }
}
