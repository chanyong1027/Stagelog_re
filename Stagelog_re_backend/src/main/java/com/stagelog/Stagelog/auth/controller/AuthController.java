package com.stagelog.Stagelog.auth.controller;

import com.stagelog.Stagelog.auth.cookie.RefreshTokenCookieManager;
import com.stagelog.Stagelog.auth.dto.AuthTokenResult;
import com.stagelog.Stagelog.auth.dto.LoginRequest;
import com.stagelog.Stagelog.auth.dto.SignupRequest;
import com.stagelog.Stagelog.auth.dto.TokenResponse;
import com.stagelog.Stagelog.auth.service.AuthService;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.UnauthorizedException;
import com.stagelog.Stagelog.global.security.AuthUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    @PostMapping("/signup")
    public ResponseEntity<Long> signup(@Valid @RequestBody SignupRequest request) {
        Long id = authService.signUp(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(id);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = resolveClientIp(httpRequest);
        AuthTokenResult result = authService.login(request, clientIp);
        refreshTokenCookieManager.addRefreshTokenCookie(httpResponse, result.refreshToken());
        return ResponseEntity.ok(result.toTokenResponse());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenCookieManager.resolveRefreshTokenFromCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new UnauthorizedException(ErrorCode.AUTH_REFRESH_INVALID);
        }
        AuthTokenResult result = authService.refresh(refreshToken);
        refreshTokenCookieManager.addRefreshTokenCookie(response, result.refreshToken());
        return ResponseEntity.ok(result.toTokenResponse());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AuthUser authUser,
            HttpServletResponse response
    ) {
        authService.logout(authUser.publicId());
        refreshTokenCookieManager.expireRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] candidates = forwardedFor.split(",");
            if (candidates.length > 0) {
                return candidates[0].trim();
            }
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return Objects.requireNonNullElse(request.getRemoteAddr(), "unknown");
    }
}
