package com.stagelog.Stagelog.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.InvalidInputException;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-05-16T12:00:00+09:00");
    private static final String TERMS_VERSION = "2026-05-16";

    @Test
    void createLocalUser_assigns_publicId_and_consent_timestamps() {
        User u = User.createLocalUser("a@b.com", "encoded", "닉네임", NOW, NOW, TERMS_VERSION);

        assertThat(u.getEmail()).isEqualTo("a@b.com");
        assertThat(u.getPublicId()).isNotNull();
        assertThat(u.getAgeConfirmedAt()).isEqualTo(NOW);
        assertThat(u.getTermsAgreedAt()).isEqualTo(NOW);
        assertThat(u.getTermsVersion()).isEqualTo(TERMS_VERSION);
        assertThat(u.getProvider()).isEqualTo(Provider.LOCAL);
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.getHandle()).isNull();
    }

    @Test
    void createLocalUser_rejects_missing_age_confirmation() {
        assertThatThrownBy(() ->
            User.createLocalUser("a@b.com", "encoded", "닉네임", null, NOW, TERMS_VERSION))
            .isInstanceOf(InvalidInputException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.AUTH_UNDER_AGE);
    }

    @Test
    void createLocalUser_rejects_missing_terms_agreement() {
        assertThatThrownBy(() ->
            User.createLocalUser("a@b.com", "encoded", "닉네임", NOW, null, TERMS_VERSION))
            .isInstanceOf(InvalidInputException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.AUTH_TERMS_NOT_AGREED);
    }

    @Test
    void createLocalUser_rejects_blank_terms_version() {
        assertThatThrownBy(() ->
            User.createLocalUser("a@b.com", "encoded", "닉네임", NOW, NOW, "  "))
            .isInstanceOf(InvalidInputException.class)
            .extracting("errorCode").isEqualTo(ErrorCode.AUTH_TERMS_NOT_AGREED);
    }

    @Test
    void createLocalUser_rejects_invalid_email() {
        assertThatThrownBy(() ->
            User.createLocalUser("not-an-email", "encoded", "닉네임", NOW, NOW, TERMS_VERSION))
            .isInstanceOf(InvalidInputException.class);
    }

    @Test
    void createSocialUser_assigns_publicId_and_no_password_and_no_consent() {
        User u = User.createSocialUser("k@b.com", "닉네임", "https://img/x", Provider.KAKAO, "kakao-123");

        assertThat(u.getEmail()).isEqualTo("k@b.com");
        assertThat(u.getPassword()).isNull();
        assertThat(u.getPublicId()).isNotNull();
        assertThat(u.getProvider()).isEqualTo(Provider.KAKAO);
        assertThat(u.getProviderId()).isEqualTo("kakao-123");
        assertThat(u.getProfileImageUrl()).isEqualTo("https://img/x");
        // 사용자가 동의하지 않은 시점을 timestamp로 위조하지 않는다.
        assertThat(u.getAgeConfirmedAt()).isNull();
        assertThat(u.getTermsAgreedAt()).isNull();
        assertThat(u.getTermsVersion()).isNull();
    }

    @Test
    void createSocialUser_rejects_LOCAL_provider() {
        assertThatThrownBy(() ->
            User.createSocialUser("k@b.com", "닉네임", null, Provider.LOCAL, "x"))
            .isInstanceOf(InvalidInputException.class);
    }
}
