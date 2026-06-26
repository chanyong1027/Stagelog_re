package com.stagelog.Stagelog.global.jwt;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.auth.service.AuthService;
import com.stagelog.Stagelog.user.domain.User;
import com.stagelog.Stagelog.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stateless 인증 E2E (spec 6장 에러 처리 + 7.4 풀사이클):
 *  - 유효 access token으로 보호 API 200
 *  - refresh token으로 보호 API 401 (필터 attribute → EntryPoint)
 *  - 무토큰 보호 API 401 / 무토큰 permitAll 통과
 *  - 차단 유저 refresh → 403 AUTH_004 (BLOCKED 매핑의 HTTP 계층 보증)
 *
 * [정적 검증 조정 내역]
 *  - $.email: UserProfileResponse.email 필드 (Lombok @Getter) → jsonPath 그대로 사용
 *  - $.code: ErrorResponse.code 필드 → jsonPath 그대로 사용
 *  - permitAll 경로: GET /api/performances 는 permitAll 이지만 컨트롤러 미존재.
 *    보안 필터 통과(인증 불필요) 확인 목적이므로 status().isNotFound() 로 단언.
 *  - 쿠키 이름: RefreshTokenCookieManager.REFRESH_TOKEN_COOKIE_NAME = "refresh_token" 확인
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class JwtAuthenticationFlowIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private AuthTokenResult tokens;

    @BeforeEach
    void seed() {
        OffsetDateTime now = OffsetDateTime.now();
        userRepository.save(User.createLocalUser(
                "flow@test.com", passwordEncoder.encode("Pwd!1234"), "플로우테스터",
                now, now, "2026-05-16"));
        tokens = authService.login(
                LoginRequest.builder().email("flow@test.com").password("Pwd!1234").build(),
                "127.0.0.1");
    }

    @Test
    void valid_access_token_returns_my_profile() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("flow@test.com"));
    }

    @Test
    void refresh_token_on_protected_api_returns_401_via_entrypoint() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokens.refreshToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
    }

    @Test
    void no_token_on_protected_api_returns_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * GET /api/performances 는 permitAll 로 Security 필터를 통과하지만,
     * 해당 경로에 매핑된 컨트롤러가 없으므로 404를 반환한다.
     * 401/403이 아닌 점(= 인증 없이 진입 가능)으로 permitAll 동작을 단언한다.
     */
    @Test
    void no_token_on_permit_all_api_passes() throws Exception {
        mockMvc.perform(get("/api/performances"))
                .andExpect(status().isNotFound());
    }

    @Test
    void suspended_user_refresh_returns_403_account_blocked() throws Exception {
        // BLOCKED 매핑의 HTTP 계층 보증 — "403"이 ErrorCode 추론이 아니라 실제 응답 단언으로 고정
        User user = userRepository.findByEmail("flow@test.com").orElseThrow();
        user.suspend();
        userRepository.saveAndFlush(user);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refresh_token", tokens.refreshToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_004"));
    }
}
