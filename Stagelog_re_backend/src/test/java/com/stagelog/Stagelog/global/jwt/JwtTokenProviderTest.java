package com.stagelog.Stagelog.global.jwt;

import static org.assertj.core.api.Assertions.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stateless 전환 핵심 검증 (spec 2.1~2.3):
 *  - 신규 클레임(pid/iss/aud) 존재
 *  - iss/aud 불일치·부재 토큰은 INVALID (구버전 토큰 차단)
 *  - 만료는 여전히 EXPIRED로 구분 (refresh 401 분기 회귀 방지)
 */
class JwtTokenProviderTest {

    private static final String SECRET =
            "dGVzdC1qd3Qtc2VjcmV0LWtleS1mb3ItdW5pdC10ZXN0cy1tdXN0LWJlLWxvbmctZW5vdWdoLWZvci1ITUFD";

    private JwtTokenProvider provider;
    private JwtProperties props;
    private final UUID publicId = UUID.randomUUID();

    private JwtTokenProvider buildProvider(String issuer, String audience, long accessValidity) {
        JwtProperties p = new JwtProperties();
        p.setSecret(SECRET);
        p.setAccessTokenValidity(accessValidity);
        p.setRefreshTokenValidity(1209600000L);
        p.setIssuer(issuer);
        p.setAudience(audience);
        JwtTokenProvider tp = new JwtTokenProvider(p);
        tp.init();
        return tp;
    }

    @BeforeEach
    void setUp() {
        props = new JwtProperties();
        props.setSecret(SECRET);
        props.setAccessTokenValidity(900000L);
        props.setRefreshTokenValidity(1209600000L);
        props.setIssuer("stagelog-test");
        props.setAudience("stagelog-api");
        provider = new JwtTokenProvider(props);
        provider.init();
    }

    @Test
    void access_token_contains_pid_iss_aud_role_type_claims() {
        String token = provider.createAccessToken("a@b.com", "ROLE_USER", publicId);

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("a@b.com");
        assertThat(claims.get("pid", String.class)).isEqualTo(publicId.toString());
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_USER");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(claims.getIssuer()).isEqualTo("stagelog-test");
        assertThat(claims.getAudience()).contains("stagelog-api");
    }

    @Test
    void valid_token_passes_require_issuer_audience_validation() {
        // happy path — require* 배선이 정상 토큰을 거부하지 않는지 보증 (품질 리뷰 지적 반영)
        String token = provider.createAccessToken("a@b.com", "ROLE_USER", publicId);

        assertThat(provider.getTokenValidationResult(token))
                .isEqualTo(JwtTokenProvider.TokenValidationResult.VALID);
    }

    @Test
    void token_with_wrong_issuer_is_invalid() {
        String foreign = buildProvider("stagelog-other", "stagelog-api", 900000L)
                .createAccessToken("a@b.com", "ROLE_USER", publicId);

        assertThat(provider.getTokenValidationResult(foreign))
                .isEqualTo(JwtTokenProvider.TokenValidationResult.INVALID);
    }

    @Test
    void token_with_wrong_audience_is_invalid() {
        String foreign = buildProvider("stagelog-test", "other-api", 900000L)
                .createAccessToken("a@b.com", "ROLE_USER", publicId);

        assertThat(provider.getTokenValidationResult(foreign))
                .isEqualTo(JwtTokenProvider.TokenValidationResult.INVALID);
    }

    @Test
    void legacy_token_without_iss_aud_is_invalid() {
        // 구버전 포맷 시뮬레이션 — 같은 키로 서명했지만 iss/aud 없음.
        // 구 access 토큰은 필터에서 이렇게 차단되고, 세션은 refresh(DB 해시 권위)로 자동 전환된다 (spec 8장)
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        String legacy = Jwts.builder()
                .subject("a@b.com")
                .claim("role", "ROLE_USER")
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900000L))
                .signWith(key)
                .compact();

        assertThat(provider.getTokenValidationResult(legacy))
                .isEqualTo(JwtTokenProvider.TokenValidationResult.INVALID);
    }

    @Test
    void expired_token_with_correct_iss_aud_is_expired() {
        String expired = buildProvider("stagelog-test", "stagelog-api", -1000L)
                .createAccessToken("a@b.com", "ROLE_USER", publicId);

        assertThat(provider.getTokenValidationResult(expired))
                .isEqualTo(JwtTokenProvider.TokenValidationResult.EXPIRED);
    }
}
