package com.stagelog.Stagelog.global.jwt.domain;

import com.stagelog.Stagelog.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private OffsetDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    // 이하 3컬럼은 Task 3에서 의미 부여. Task 2 동안엔 항상 NULL.
    @Column(name = "rotated_to_id")
    private Long rotatedToId;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_reason", length = 50)
    private String revokedReason;

    public static RefreshToken issue(Long userId, String tokenHash,
                                     OffsetDateTime issuedAt, OffsetDateTime expiresAt) {
        RefreshToken t = new RefreshToken();
        t.userId = userId;
        t.tokenHash = tokenHash;
        t.issuedAt = issuedAt;
        t.expiresAt = expiresAt;
        return t;
    }

    public boolean isExpired(OffsetDateTime now) {
        return now.isAfter(this.expiresAt);
    }
}
