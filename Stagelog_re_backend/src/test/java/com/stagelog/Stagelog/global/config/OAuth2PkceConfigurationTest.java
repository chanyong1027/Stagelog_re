package com.stagelog.Stagelog.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring Security가 카카오 OAuth authorize URL에 PKCE 파라미터를 추가하는지 검증.
 *  Story 1.4 AC: "PKCE code_challenge 포함 카카오 인증 화면으로 리다이렉트"
 *  NFR42: "카카오 OAuth 2.0 + PKCE"
 */
@SpringBootTest
@ActiveProfiles("test")
class OAuth2PkceConfigurationTest {

    @Autowired
    OAuth2AuthorizationRequestResolver resolver;

    @Test
    void resolves_kakao_authorization_with_pkce_S256() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/kakao");
        request.setServletPath("/oauth2/authorization/kakao");

        // when
        OAuth2AuthorizationRequest authRequest = resolver.resolve(request);

        // then
        assertThat(authRequest)
                .as("kakao 등록 정보로 authorize request가 생성돼야 함")
                .isNotNull();

        assertThat(authRequest.getAdditionalParameters())
                .as("PKCE code_challenge가 authorize URL 파라미터에 포함되어야 함")
                .containsKey(PkceParameterNames.CODE_CHALLENGE)
                .containsEntry(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");

        assertThat(authRequest.getAttributes())
                .as("code_verifier가 server-side 저장 attribute에 포함되어야 함 (콜백 검증용)")
                .containsKey(PkceParameterNames.CODE_VERIFIER);
    }
}
