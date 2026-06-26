package com.stagelog.Stagelog.auth.oauth2.service;

import com.stagelog.Stagelog.auth.oauth2.userinfo.OAuth2UserInfo;
import com.stagelog.Stagelog.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.domain.UserStatus;
import com.stagelog.Stagelog.user.service.UserService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OAuth2 로그인 시 provider에서 받은 사용자 정보를 도메인 User와 동기화한다.
 *
 * 에러는 반드시 OAuth2AuthenticationException으로 던져야 Spring Security가
 * OAuth2FailureHandler로 라우팅한다. BusinessException을 던지면 500이 된다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserService userService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String nameAttributeKey = userRequest.getClientRegistration()
                .getProviderDetails()
                .getUserInfoEndpoint()
                .getUserNameAttributeName();

        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.find(registrationId, oAuth2User.getAttributes())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error(ErrorCode.AUTH_OAUTH2_PROVIDER_ERROR.getCode())));
        return processUserInfo(userInfo, oAuth2User.getAttributes(), nameAttributeKey);
    }

    /**
     * provider 검증 + 이메일 충돌 확인 + 유저 동기화 + 계정 상태 확인.
     * HTTP 호출(super.loadUser) 이후 단계를 분리해 단위 테스트 가능하게 한다 (package-private).
     */
    OAuth2User processUserInfo(
            OAuth2UserInfo userInfo,
            Map<String, Object> attributes,
            String nameAttributeKey) {

        validateProviderId(userInfo);
        validateEmail(userInfo);

        User user = syncUser(userInfo);
        validateUserStatus(user);

        return new CustomOAuth2User(
                user,
                attributes,
                List.of(new SimpleGrantedAuthority(user.getRole().getValue())),
                nameAttributeKey
        );
    }

    private void validateProviderId(OAuth2UserInfo userInfo) {
        if (userInfo.providerId() == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ErrorCode.AUTH_OAUTH2_PROVIDER_ERROR.getCode()));
        }
    }

    private void validateEmail(OAuth2UserInfo userInfo) {
        if (userInfo.email() == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ErrorCode.AUTH_OAUTH2_MISSING_EMAIL.getCode()));
        }
    }

    /**
     * 이메일 충돌 확인 후 신규 가입 또는 기존 유저 반환.
     * 동일 이메일이 다른 provider로 이미 가입돼 있으면 AUTH_OAUTH2_EMAIL_CONFLICT.
     */
    private User syncUser(OAuth2UserInfo userInfo) {
        userService.findByEmail(userInfo.email()).ifPresent(existing -> {
            if (existing.getProvider() != userInfo.provider()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error(ErrorCode.AUTH_OAUTH2_EMAIL_CONFLICT.getCode()));
            }
        });

        return userService.getOrCreateUser(
                userInfo.email(),
                userInfo.nickname(),
                userInfo.profileImageUrl(),
                userInfo.provider(),
                userInfo.providerId()
        );
    }

    private void validateUserStatus(User user) {
        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.SUSPENDED) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(ErrorCode.AUTH_ACCOUNT_BLOCKED.getCode()));
        }
    }
}
