package com.stagelog.Stagelog.user.controller;

import com.stagelog.Stagelog.global.security.AuthUser;
import com.stagelog.Stagelog.user.dto.UserProfileResponse;
import com.stagelog.Stagelog.user.dto.UserUpdateRequest;
import com.stagelog.Stagelog.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthUser authUser) {
        return ResponseEntity.ok(userService.getMyProfile(authUser.publicId()));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody UserUpdateRequest request) {
        return ResponseEntity.ok(userService.updateProfile(authUser.publicId(), request));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMyAccount(
            @AuthenticationPrincipal AuthUser authUser) {
        userService.deleteUser(authUser.publicId());
        return ResponseEntity.noContent().build();
    }
}
