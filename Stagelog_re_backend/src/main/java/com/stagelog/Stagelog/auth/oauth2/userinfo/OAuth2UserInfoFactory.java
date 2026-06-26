package com.stagelog.Stagelog.auth.oauth2.userinfo;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * registrationId(카카오·구글·네이버)에 따라 OAuth2UserInfo 구현체를 반환하는 정적 팩토리.
 *
 * 새 provider 추가 시 REGISTRY에 항목 하나만 등록하면 되며,
 * CustomOAuth2UserService·핸들러 코드는 수정하지 않아도 된다 (OCP).
 */
public final class OAuth2UserInfoFactory {

    private static final Map<String, Function<Map<String, Object>, OAuth2UserInfo>> REGISTRY =
            Map.of(
                    "kakao", KakaoOAuth2UserInfo::new
                    // Step 7: "google", GoogleOAuth2UserInfo::new
                    // Step 8: "naver",  NaverOAuth2UserInfo::new
            );

    private OAuth2UserInfoFactory() {
    }

    /**
     * 미지원 provider는 예외 대신 빈 Optional로 반환한다 (P1-2).
     * 예외 변환은 호출자가 OAuth2AuthenticationException으로 수행해야 Spring Security가
     * 실패 핸들러로 라우팅한다 — 여기서 BusinessException을 던지면 500이 떨어진다.
     *
     * @param registrationId Spring Security OAuth2 Client의 registration ID (예: "kakao")
     * @param attributes     provider userinfo 엔드포인트 응답 attributes
     */
    public static Optional<OAuth2UserInfo> find(String registrationId, Map<String, Object> attributes) {
        Function<Map<String, Object>, OAuth2UserInfo> creator =
                REGISTRY.get(registrationId.toLowerCase());
        return Optional.ofNullable(creator).map(c -> c.apply(attributes));
    }
}
