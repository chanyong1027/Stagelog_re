package com.stagelog.Stagelog.auth.dto;

public record AuthTokenResult(
        String accessToken,
        String refreshToken,
        String publicId,
        String email,
        String nickname
) {
    public static AuthTokenResult of(
            String accessToken,
            String refreshToken,
            String publicId,
            String email,
            String nickname
    ) {
        return new AuthTokenResult(accessToken, refreshToken, publicId, email, nickname);
    }

    public TokenResponse toTokenResponse() {
        return TokenResponse.of(accessToken, publicId, email, nickname);
    }
}
