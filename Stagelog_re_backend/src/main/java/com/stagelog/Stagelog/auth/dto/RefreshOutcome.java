package com.stagelog.Stagelog.auth.dto;

/**
 * Refresh 회전 처리 결과. 재사용/만료/무효는 예외가 아니라 예상된 결과로 본다.
 * RotationService(트랜잭션 내부)가 반환 → AuthService(매퍼)가 HTTP 예외로 변환.
 */
public record RefreshOutcome(Status status, AuthTokenResult tokens) {

    public enum Status { ROTATED, REUSED, EXPIRED, INVALID }

    public static RefreshOutcome rotated(AuthTokenResult tokens) {
        return new RefreshOutcome(Status.ROTATED, tokens);
    }

    public static RefreshOutcome reused() {
        return new RefreshOutcome(Status.REUSED, null);
    }

    public static RefreshOutcome expired() {
        return new RefreshOutcome(Status.EXPIRED, null);
    }

    public static RefreshOutcome invalid() {
        return new RefreshOutcome(Status.INVALID, null);
    }
}
