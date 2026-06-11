package com.stagelog.Stagelog.global.jwt;

import static org.assertj.core.api.Assertions.*;

import com.stagelog.Stagelog.global.security.AuthUser;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Stateless 필터 검증 (spec 3.2):
 *  - 유효 access token → 클레임만으로 AuthUser principal + authority 구성
 *  - refresh token으로 API 접근 → TOKEN_INVALID attribute (직접 응답 작성 금지)
 *  - 만료/무효/무토큰 → 기존 attribute 패턴 유지
 */
class JwtAuthenticationFilterTest {

    private static final String SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0cy1tdXN0LWJlLWxvbmctZW5vdWdoLWZvci1ITUFD";

    private JwtTokenProvider provider;
    private JwtAuthenticationFilter filter;
    private final UUID publicId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenValidity(900000L);
        props.setRefreshTokenValidity(1209600000L);
        props.setIssuer("stagelog-test");
        props.setAudience("stagelog-api");
        provider = new JwtTokenProvider(props);
        provider.init();
        filter = new JwtAuthenticationFilter(provider);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest requestWithToken(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        return request;
    }

    @Test
    void valid_access_token_builds_authuser_principal_from_claims_only() throws Exception {
        String token = provider.createAccessToken("a@b.com", "ROLE_USER", publicId);
        MockHttpServletRequest request = requestWithToken(token);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AuthUser principal = (AuthUser) auth.getPrincipal();
        assertThat(principal.publicId()).isEqualTo(publicId);
        assertThat(principal.email()).isEqualTo("a@b.com");
        assertThat(auth.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    void refresh_token_sets_invalid_attribute_without_writing_response() throws Exception {
        String refresh = provider.createRefreshToken("a@b.com", "ROLE_USER", publicId);
        MockHttpServletRequest request = requestWithToken(refresh);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_CODE_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_INVALID_CODE);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        // 필터가 직접 응답을 쓰지 않음 — EntryPoint 위임 (체인은 계속됨)
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void invalid_token_sets_invalid_attribute() throws Exception {
        MockHttpServletRequest request = requestWithToken("not-a-jwt");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_CODE_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_INVALID_CODE);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void no_token_passes_through_as_anonymous() throws Exception {
        MockHttpServletRequest request = requestWithToken(null);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_CODE_ATTRIBUTE)).isNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void expired_token_sets_expired_attribute() throws Exception {
        // 프론트가 "refresh 시도 vs 재로그인"을 가르는 분기 — TOKEN_EXPIRED 보증 (품질 리뷰 지적 반영)
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(SECRET);
        expiredProps.setAccessTokenValidity(-1000L);
        expiredProps.setRefreshTokenValidity(1209600000L);
        expiredProps.setIssuer("stagelog-test");
        expiredProps.setAudience("stagelog-api");
        JwtTokenProvider expiredProvider = new JwtTokenProvider(expiredProps);
        expiredProvider.init();
        String expired = expiredProvider.createAccessToken("a@b.com", "ROLE_USER", publicId);
        MockHttpServletRequest request = requestWithToken(expired);

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(request.getAttribute(JwtAuthenticationFilter.TOKEN_ERROR_CODE_ATTRIBUTE))
                .isEqualTo(JwtAuthenticationFilter.TOKEN_EXPIRED_CODE);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
