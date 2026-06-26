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

    // Task 3에서 rotation chain + reuse detection을 위해 사용된다.
    //   - rotatedToId: 본 토큰이 rotate된 *후속* 토큰의 PK. ROTATED 상태에서만 채워짐.
    //   - revokedAt / revokedReason: 토큰이 더 이상 유효하지 않은 시점과 사유.
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

    public boolean isRotated() {
        return rotatedToId != null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * 본 토큰이 새 토큰으로 rotate됐음을 기록.
     *  rotation chain의 마디 — 후속 토큰의 PK를 rotated_to_id에 저장.
     */
    public void markRotated(Long newTokenId, OffsetDateTime when) {
        this.rotatedToId = newTokenId;
        this.revokedAt = when;
        this.revokedReason = "ROTATED";
    }

    /**
     * 본 토큰을 폐기 (reason: LOGOUT / REUSED / EXPIRED 등).
     *  rotated_to_id는 건드리지 않는다 — rotation이 아닌 일반 폐기.
     */
    public void markRevoked(String reason, OffsetDateTime when) {
        this.revokedAt = when;
        this.revokedReason = reason;
    }
}
