package com.stagelog.Stagelog.auth.oauth2.userinfo;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 팩토리는 미지원 provider를 예외가 아니라 빈 Optional로 표현한다 (P1-2).
 * 예외 종류(→ OAuth2 실패 핸들러 라우팅) 결정은 호출자(CustomOAuth2UserService)가 맡는다.
 */
class OAuth2UserInfoFactoryTest {

    @Test
    void supported_provider_returns_userinfo() {
        assertThat(OAuth2UserInfoFactory.find("kakao", Map.of("id", 1L)))
                .get()
                .isInstanceOf(KakaoOAuth2UserInfo.class);
    }

    @Test
    void provider_id_is_case_insensitive() {
        assertThat(OAuth2UserInfoFactory.find("KAKAO", Map.of("id", 1L))).isPresent();
    }

    @Test
    void unsupported_provider_returns_empty() {
        assertThat(OAuth2UserInfoFactory.find("google", Map.of())).isEmpty();
    }
}
