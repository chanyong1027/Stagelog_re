package com.stagelog.Stagelog.global.security;

import java.util.UUID;

/**
 * JWT 클레임만으로 조립되는 경량 principal (JPA·UserDetails 비의존).
 * role은 "ROLE_USER" 형태 (Role.getValue()) — 그대로 authority 문자열로 사용한다.
 * 내부 PK는 절대 포함하지 않는다 — 사용자 키는 publicId.
 */
public record AuthUser(UUID publicId, String email, String role) {
}
