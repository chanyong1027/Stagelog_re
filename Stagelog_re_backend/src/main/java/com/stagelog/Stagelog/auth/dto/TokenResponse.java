package com.stagelog.Stagelog.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TokenResponse {
    private String accessToken;
    private String tokenType;
    private String publicId;
    private String email;
    private String nickname;

    public static TokenResponse of(String accessToken, String publicId, String email, String nickname) {
        return new TokenResponse(accessToken, "Bearer", publicId, email, nickname);
    }
}
