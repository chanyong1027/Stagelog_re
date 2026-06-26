package com.stagelog.Stagelog.user.domain;

import com.stagelog.Stagelog.global.entity.BaseEntity;
import com.stagelog.Stagelog.global.exception.ErrorCode;
import com.stagelog.Stagelog.global.exception.InvalidInputException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Check(constraints = "(provider = 'LOCAL' AND password IS NOT NULL) OR "
                  + "(provider <> 'LOCAL' AND password IS NULL)")
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_provider_provider_id",
                          columnNames = {"provider", "provider_id"})
    }
)
public class User extends BaseEntity {

    private static final Pattern EMAIL_PATTERN =
        Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false, length = 20)
    private String nickname;

    @Column(length = 30, unique = true)
    private String handle;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false, length = 20)
    private UserStatus status;

    @Column(name = "age_confirmed_at")
    private OffsetDateTime ageConfirmedAt;

    @Column(name = "terms_agreed_at")
    private OffsetDateTime termsAgreedAt;

    @Column(name = "terms_version", length = 20)
    private String termsVersion;

    @Column(name = "email_notification_enabled", nullable = false)
    private Boolean emailNotificationEnabled;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public static User createLocalUser(
            String email, String encodedPassword, String nickname,
            OffsetDateTime ageConfirmedAt, OffsetDateTime termsAgreedAt, String termsVersion) {
        email = normalizeEmail(email);
        validateEmail(email);
        validateEncodedPassword(encodedPassword);
        validateNickname(nickname);
        validateConsent(ageConfirmedAt, termsAgreedAt, termsVersion);

        User u = new User();
        u.publicId = UUID.randomUUID();
        u.email = email;
        u.password = encodedPassword;
        u.nickname = nickname;
        u.provider = Provider.LOCAL;
        u.providerId = null;
        u.role = Role.USER;
        u.status = UserStatus.ACTIVE;
        u.ageConfirmedAt = ageConfirmedAt;
        u.termsAgreedAt = termsAgreedAt;
        u.termsVersion = termsVersion;
        u.emailNotificationEnabled = true;
        return u;
    }

    /**
     * MVP: 카카오 등 소셜 가입자는 consent를 수집하지 않는다.
     *  age_confirmed_at / terms_agreed_at / terms_version 모두 NULL로 INSERT.
     *  TODO v1.0: Kakao Sync 약관 동의 활용 또는 자체 1회 동의 화면 도입 결정.
     */
    public static User createSocialUser(
            String email, String nickname, String profileImageUrl,
            Provider provider, String providerId) {
        email = normalizeEmail(email);
        validateEmail(email);
        validateNickname(nickname);
        validateSocialProvider(provider, providerId);

        User u = new User();
        u.publicId = UUID.randomUUID();
        u.email = email;
        u.password = null;
        u.nickname = nickname;
        u.profileImageUrl = profileImageUrl;
        u.provider = provider;
        u.providerId = providerId;
        u.role = Role.USER;
        u.status = UserStatus.ACTIVE;
        u.ageConfirmedAt = null;
        u.termsAgreedAt = null;
        u.termsVersion = null;
        u.emailNotificationEnabled = true;
        return u;
    }

    public void updateLastLoginAt(OffsetDateTime when) {
        this.lastLoginAt = when;
    }

    public void changeNickname(String nickname) {
        validateNickname(nickname);
        this.nickname = nickname;
    }

    public void updateProfile(String nickname, String profileImageUrl, Boolean emailNotificationEnabled) {
        if (nickname != null) {
            validateNickname(nickname);
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (emailNotificationEnabled != null) {
            this.emailNotificationEnabled = emailNotificationEnabled;
        }
    }

    public void markDeleted(OffsetDateTime when) {
        this.status = UserStatus.DELETED;
        this.deletedAt = when;
    }

    public void delete() {
        markDeleted(OffsetDateTime.now());
    }

    public void suspend()  { this.status = UserStatus.SUSPENDED; }
    public void activate() { this.status = UserStatus.ACTIVE; }

    /**
     * 이메일 정규화 — 저장/조회 키를 일관화한다.
     * 대소문자·주변 공백 차이로 인한 중복가입('Foo@x.com' vs 'foo@x.com')과
     * 로그인 조회 불일치를 막는다. 저장 경로(팩토리)와 조회 경로(login/OAuth)에서 공유한다.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new InvalidInputException(ErrorCode.INVALID_EMAIL_FORMAT);
        }
    }

    private static void validateNickname(String nickname) {
        if (nickname == null || nickname.isBlank() || nickname.length() < 2 || nickname.length() > 20) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateEncodedPassword(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateSocialProvider(Provider provider, String providerId) {
        if (provider == null || provider == Provider.LOCAL) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (providerId == null || providerId.isBlank()) {
            throw new InvalidInputException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private static void validateConsent(
            OffsetDateTime ageConfirmedAt, OffsetDateTime termsAgreedAt, String termsVersion) {
        if (ageConfirmedAt == null) {
            throw new InvalidInputException(ErrorCode.AUTH_UNDER_AGE);
        }
        if (termsAgreedAt == null || termsVersion == null || termsVersion.isBlank()) {
            throw new InvalidInputException(ErrorCode.AUTH_TERMS_NOT_AGREED);
        }
    }
}
