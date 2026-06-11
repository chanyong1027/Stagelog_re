package com.stagelog.Stagelog.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리 EntityNotFoundException, DuplicateEntityException 등
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        // 4xx 에러는 WARN 레벨 (클라이언트 오류)
        if (errorCode.getHttpStatus().is4xxClientError()) {
            log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), e.getMessage());
        }
        // 5xx 에러는 ERROR 레벨 (서버 오류)
        else {
            log.error("[BusinessException] code={}, message={}", errorCode.getCode(), e.getMessage(), e);
        }

        ErrorResponse response = ErrorResponse.of(errorCode);
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(response);
    }

    /**
     * @Valid, @Validated 유효성 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("[Validation Failed] errors={}", e.getBindingResult().getAllErrors());

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult().getFieldErrors());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * @ModelAttribute 바인딩 실패
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.warn("[Binding Failed] errors={}", e.getBindingResult().getAllErrors());

        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult().getFieldErrors());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * 존재하지 않는 경로 (미매핑 URL·정적 리소스 부재) — 404.
     * 이 핸들러가 없으면 아래 catch-all(Exception → 500)이 NoResourceFoundException을
     * 가로채 모든 오타 URL이 500 + error 로그로 응답된다. (stateless 전환 E2E가 발견)
     */
    @ExceptionHandler(NoResourceFoundException.class)
    protected ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        ErrorResponse response = ErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND);
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * 그 외 모든 예외 (Fallback) 예상하지 못한 서버 오류
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        // 스택 트레이스 전체를 로그에 기록
        log.error("[Unexpected Exception] message={}", e.getMessage(), e);

        // 클라이언트에게는 간단한 메시지만 전달 (보안)
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }
}
