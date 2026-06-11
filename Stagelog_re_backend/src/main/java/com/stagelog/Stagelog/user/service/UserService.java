package com.stagelog.Stagelog.user.service;

import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.User;
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
        return userRepository.findByEmail(email);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    public User getUserByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.USER_NOT_FOUND));
    }

    public UserProfileResponse getMyProfile(UUID publicId) {
        User user = getUserByPublicId(publicId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID publicId, UserUpdateRequest request) {
        User user = getUserByPublicId(publicId);
        user.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getEmailNotificationEnabled()
        );
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUser(UUID publicId) {
        User user = getUserByPublicId(publicId);
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
