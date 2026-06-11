package com.stagelog.Stagelog.global.jwt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private Long accessTokenValidity;
    private Long refreshTokenValidity;
    private String refreshTokenPepper;
    private String issuer;
    private String audience;
}
