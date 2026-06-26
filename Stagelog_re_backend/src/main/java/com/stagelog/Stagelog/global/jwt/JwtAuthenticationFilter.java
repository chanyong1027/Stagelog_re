package com.stagelog.Stagelog.global.jwt;

import com.stagelog.Stagelog.global.security.AuthUser;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stateless JWT 인증 필터 — 서명·만료·iss/aud 검증 + 클레임 파싱만으로 인증을 구성한다.
 * 요청별 DB 조회 없음. UserStatus 차단은 login/refresh 시점에서 수행 (ADR 2026-06-10).
 * 에러 응답은 직접 작성하지 않고 attribute 세팅 후 EntryPoint에 위임한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    public static final String TOKEN_ERROR_CODE_ATTRIBUTE = "jwtTokenErrorCode";
    public static final String TOKEN_EXPIRED_CODE = "TOKEN_EXPIRED";
    public static final String TOKEN_INVALID_CODE = "TOKEN_INVALID";

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * OAuth2 진입/콜백 경로와 /error 경로는 JWT 필터를 명시적으로 건너뛴다.
     * "토큰이 null이면 통과"하는 암묵 동작에 의존하지 않고 명시 규칙으로 고정한다.
     * /error 포함 이유: OAuth2 실패 핸들러 → Spring /error forward 시 이중 처리 방지
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        JwtTokenProvider.TokenInspection inspection = jwtTokenProvider.inspect(token);
        if (inspection.result() != JwtTokenProvider.TokenValidationResult.VALID) {
            request.setAttribute(
                    TOKEN_ERROR_CODE_ATTRIBUTE,
                    inspection.result() == JwtTokenProvider.TokenValidationResult.EXPIRED
                            ? TOKEN_EXPIRED_CODE
                            : TOKEN_INVALID_CODE
            );
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims = inspection.claims();
        // refresh 토큰으로 API 접근 — invalid와 동일 취급, 보호 경로면 EntryPoint가 401.
        // permitAll 경로는 anonymous로 통과 (spec 3.2 — 의도된 동작 변화)
        if (!jwtTokenProvider.isAccessToken(claims)) {
            request.setAttribute(TOKEN_ERROR_CODE_ATTRIBUTE, TOKEN_INVALID_CODE);
            filterChain.doFilter(request, response);
            return;
        }

        AuthUser authUser = jwtTokenProvider.getAuthUser(claims);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                authUser, "", List.of(new SimpleGrantedAuthority(authUser.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
