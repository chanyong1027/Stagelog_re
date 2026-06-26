package com.stagelog.Stagelog.global.jwt;

import com.stagelog.Stagelog.global.security.AuthUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    /**
     * 검증 결과 + (VALID일 때) 파싱된 클레임을 함께 반환한다.
     * 필터가 요청당 토큰을 한 번만 파싱하도록 하는 단일 진입점 — 서명·만료·iss/aud 검증을 여기서 끝낸다.
     */
    public TokenInspection inspect(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .requireIssuer(jwtProperties.getIssuer())
                    .requireAudience(jwtProperties.getAudience())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // principal 조립에 필수인 pid/role이 없으면 INVALID로 처리한다.
            // (서명은 유효하나 클레임이 누락된 토큰이 getAuthUser에서 NPE→500 나는 것을 401로 일관화)
            String pid = claims.get("pid", String.class);
            if (pid == null || claims.get("role", String.class) == null) {
                log.info("필수 클레임(pid/role) 누락 — 유효하지 않은 토큰으로 처리합니다.");
                return TokenInspection.of(TokenValidationResult.INVALID);
            }
            // pid가 UUID 형식이 아니면 getAuthUser의 UUID.fromString이 500을 던지므로 여기서 401로 일관화.
            try {
                UUID.fromString(pid);
            } catch (IllegalArgumentException e) {
                log.info("pid 클레임이 UUID 형식이 아님 — 유효하지 않은 토큰으로 처리합니다.");
                return TokenInspection.of(TokenValidationResult.INVALID);
            }
            return new TokenInspection(TokenValidationResult.VALID, claims);
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다.");
            return TokenInspection.of(TokenValidationResult.EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            // iss/aud 불일치(IncorrectClaimException)·부재(MissingClaimException)도
            // JwtException 하위이므로 여기서 INVALID로 처리된다.
            log.info("유효하지 않은 JWT 토큰입니다.");
            return TokenInspection.of(TokenValidationResult.INVALID);
        }
    }

    /** 검증 결과만 필요할 때(테스트 등). 내부적으로 inspect()에 위임한다. */
    public TokenValidationResult getTokenValidationResult(String token) {
        return inspect(token).result();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type", String.class));
    }

    /**
     * 클레임만으로 principal 조립 — stateless 전환의 핵심 (DB 조회 없음).
     * inspect()가 검증 + pid/role 존재를 이미 보장하므로 그 Claims를 그대로 받는다(재파싱 없음).
     */
    public AuthUser getAuthUser(Claims claims) {
        return new AuthUser(
                UUID.fromString(claims.get("pid", String.class)),
                claims.getSubject(),
                claims.get("role", String.class));
    }

    public record TokenInspection(TokenValidationResult result, Claims claims) {
        static TokenInspection of(TokenValidationResult result) {
            return new TokenInspection(result, null);
        }
    }
}
