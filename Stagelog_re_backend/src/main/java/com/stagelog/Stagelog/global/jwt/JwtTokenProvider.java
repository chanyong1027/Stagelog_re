package com.stagelog.Stagelog.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {
    public enum TokenValidationResult {
        VALID,
        EXPIRED,
        INVALID
    }

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(String email, String role, UUID publicId) {
        return createToken(email, role, "access", jwtProperties.getAccessTokenValidity(), publicId);
    }

    public String createRefreshToken(String email, String role, UUID publicId) {
        return createToken(email, role, "refresh", jwtProperties.getRefreshTokenValidity(), publicId);
    }

    private String createToken(String email, String role, String type, Long validity, UUID publicId) {
        long now = System.currentTimeMillis();

        // JWT iat/exp는 RFC 7519상 초 단위로 truncate된다.
        // 같은 1초 안에 발급된 토큰이 완전히 동일해지지 않도록 jti(JWT ID)를 매번 UUID로 부여한다.
        // → refresh_tokens.token_hash UNIQUE 충돌 방지 + 토큰 추적성(JTI revocation list 등) 확보.
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(email)
                .claim("pid", publicId.toString())
                .claim("role", role)
                .claim("type", type)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(new Date(now))
                .expiration(new Date(now + validity))
                .signWith(secretKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        String email = getEmail(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getType(String token) {
        return parseClaims(token).get("type", String.class);
    }

    public boolean isAccessToken(String token) {
        return "access".equals(getType(token));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(getType(token));
    }

    public boolean validateToken(String token) {
        return getTokenValidationResult(token) == TokenValidationResult.VALID;
    }

    public TokenValidationResult getTokenValidationResult(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseSignedClaims(token);
            return TokenValidationResult.VALID;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            return TokenValidationResult.EXPIRED;
        } catch (JwtException | IllegalArgumentException e) {
            // iss/aud 불일치(IncorrectClaimException)·부재(MissingClaimException)도
            // JwtException 하위이므로 여기서 INVALID로 처리된다.
            log.info("유효하지 않은 JWT 토큰입니다.");
            return TokenValidationResult.INVALID;
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 클레임 추출 전용 — require(iss/aud) 검증을 일부러 넣지 않는다.
     * 필터 흐름상 getTokenValidationResult()의 검증이 항상 선행하며,
     * 여기 추가하면 IncorrectClaimException 처리가 중복된다.
     */
    private Claims parseClaims(String token) {
        try {
            return Jwts.parser().verifyWith(secretKey).build()
                    .parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}
