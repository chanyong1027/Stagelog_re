package com.stagelog.Stagelog.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SignupRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Pattern(
        regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,20}$",
        message = "비밀번호는 8-20자의 영문, 숫자, 특수문자를 포함해야 합니다."
    )
    private String password;

    @NotBlank
    @Size(min = 2, max = 20)
    @Pattern(regexp = "^[가-힣a-zA-Z0-9_]+$")
    private String nickname;

    /**
     * 단일 통합 체크박스: "만 14세 이상이며 약관 및 개인정보 처리방침에 동의합니다"
     *  termsVersion은 서버 app.terms.current-version에서 주입.
     */
    @AssertTrue(message = "만 14세 이상이며 약관 및 개인정보 처리방침에 동의가 필요해요")
    private boolean agreedToTerms;
}
