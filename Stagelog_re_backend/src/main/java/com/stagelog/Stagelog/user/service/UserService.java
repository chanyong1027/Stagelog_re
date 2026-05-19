package com.stagelog.Stagelog.user.service;

import com.stagelog.Stagelog.global.exception.EntityNotFoundException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.dto.UserProfileResponse;
import com.stagelog.Stagelog.user.dto.UserUpdateRequest;
import com.stagelog.Stagelog.user.repository.UserRepository;
import java.util.Optional;
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

    public UserProfileResponse getMyProfile(Long userId) {
        User user = getUserById(userId);
        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);
        user.updateProfile(
                request.getNickname(),
                request.getProfileImageUrl(),
                request.getEmailNotificationEnabled()
        );
        return UserProfileResponse.from(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
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
