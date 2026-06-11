package com.stagelog.Stagelog.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ===== User 관련 (4001~4099) =====
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER_002", "이미 존재하는 아이디입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_003", "비밀번호가 일치하지 않습니다."),
    INVALID_USER_ID(HttpStatus.BAD_REQUEST, "USER_004", "로그인 ID는 필수입니다."),
    INVALID_EMAIL_FORMAT(HttpStatus.BAD_REQUEST, "USER_005", "올바르지 않은 이메일 형식입니다."),
    USER_EMAIL_DUPLICATE(HttpStatus.CONFLICT, "USER_006", "이미 가입된 이메일이에요"),

    // ===== Auth 관련 =====
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_001", "유효하지 않은 Refresh Token입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_002", "만료된 Refresh Token입니다."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_003", "아이디/비밀번호가 올바르지 않습니다."),
    AUTH_ACCOUNT_BLOCKED(HttpStatus.FORBIDDEN, "AUTH_004", "사용이 제한된 계정입니다."),
    AUTH_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "AUTH_005", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
    AUTH_OAUTH2_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "AUTH_006", "소셜 로그인 처리 중 오류가 발생했습니다."),
    AUTH_OAUTH2_MISSING_EMAIL(HttpStatus.BAD_REQUEST, "AUTH_007", "소셜 계정에서 이메일을 제공받지 못했습니다."),
    AUTH_OAUTH2_EMAIL_CONFLICT(HttpStatus.CONFLICT, "AUTH_008", "이미 가입된 계정입니다."),
    AUTH_UNDER_AGE(HttpStatus.BAD_REQUEST, "AUTH_009", "만 14세 이상만 가입할 수 있어요"),
    AUTH_TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "AUTH_010", "약관 동의가 필요해요"),
    AUTH_REFRESH_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_011", "다시 로그인해주세요"),
    AUTH_REFRESH_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_012", "세션이 만료되었어요"),
    AUTH_REFRESH_REUSED(HttpStatus.UNAUTHORIZED, "AUTH_013", "보안을 위해 모든 세션을 종료했어요"),

    // ===== Performance 관련 =====
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PERFORMANCE_001", "공연을 찾을 수 없습니다."),
    PERFORMANCE_DETAIL_NULL(HttpStatus.BAD_REQUEST, "PERFORMANCE_003", "공연 상세 정보가 null일 수 없습니다."),

    // ===== Batch 관련 =====
    BATCH_EXECUTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "BATCH_001", "배치 실행 중 오류가 발생했습니다."),
    BATCH_INTERRUPTED(HttpStatus.INTERNAL_SERVER_ERROR, "BATCH_002", "배치 처리가 중단되었습니다."),

    // ===== 공통 =====
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 내부 오류가 발생했습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_003", "요청한 리소스를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
