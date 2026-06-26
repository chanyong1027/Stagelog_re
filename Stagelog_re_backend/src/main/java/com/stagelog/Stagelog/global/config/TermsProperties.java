package com.stagelog.Stagelog.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.terms")
@Getter
@Setter
public class TermsProperties {
    /** 현재 적용 중인 약관 버전. */
    private String currentVersion = "2026-05-16";
}
