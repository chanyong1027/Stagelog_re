package com.stagelog.Stagelog.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stagelog.Stagelog.auth.dto.SignupRequest;
import com.stagelog.Stagelog.global.exception.DuplicateEntityException;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.InvalidInputException;
import com.stagelog.Stagelog.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceSignupTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    private SignupRequest valid(String email) {
        return SignupRequest.builder()
            .email(email).password("Pwd!1234").nickname("닉네임")
            .agreedToTerms(true)
            .build();
    }

    @Test
    void signUp_creates_user_with_publicId_and_consent_from_server_terms_version() {
        Long id = authService.signUp(valid("a@b.com"));

        var saved = userRepository.findById(id).orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("a@b.com");
        assertThat(saved.getPublicId()).isNotNull();
        assertThat(saved.getAgeConfirmedAt()).isNotNull();
        assertThat(saved.getTermsAgreedAt()).isNotNull();
        assertThat(saved.getTermsVersion()).isNotBlank();
    }

    @Test
    void signUp_rejects_when_terms_not_agreed() {
        SignupRequest req = SignupRequest.builder()
            .email("a@b.com").password("Pwd!1234").nickname("닉네임")
            .agreedToTerms(false)
            .build();

        assertThatThrownBy(() -> authService.signUp(req))
            .isInstanceOf(InvalidInputException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.AUTH_TERMS_NOT_AGREED);
    }

    /**
     * AC: try-find-then-insert 금지 — DB UNIQUE 위반을 catch해 USER_EMAIL_DUPLICATE로 변환.
     */
    @Test
    void signUp_duplicate_email_throws_USER_EMAIL_DUPLICATE() {
        authService.signUp(valid("dup@b.com"));

        assertThatThrownBy(() -> authService.signUp(valid("dup@b.com")))
            .isInstanceOf(DuplicateEntityException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.USER_EMAIL_DUPLICATE);
    }
}
