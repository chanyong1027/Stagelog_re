package com.stagelog.Stagelog.global.jwt.repository;

import com.stagelog.Stagelog.global.jwt.domain.RefreshToken;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserId(Long userId);

    /** logout / reuse detection 시 user의 활성 토큰만 일괄 폐기 대상 조회. */
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    /** rotation chain 검증용 — id 오름차순으로 user의 모든 토큰 이력 조회. */
    List<RefreshToken> findAllByUserIdOrderByIdAsc(Long userId);

    /**
     * 회전 권한 원자적 획득. revoked_at NULL + 미만료인 토큰만 ROTATED로 마킹.
     * @return 영향 행 수 (1=획득 성공, 0=이미 회전/폐기/만료 또는 동시 경합 패배)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
           SET r.revokedAt = :now, r.revokedReason = 'ROTATED'
         WHERE r.tokenHash = :hash
           AND r.revokedAt IS NULL
           AND r.expiresAt > :now
        """)
    int claimForRotation(@Param("hash") String hash, @Param("now") OffsetDateTime now);

    /** user의 모든 활성 토큰을 일괄 폐기 (재사용 탐지 시 family 무효화). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
           SET r.revokedAt = :now, r.revokedReason = :reason
         WHERE r.userId = :userId
           AND r.revokedAt IS NULL
        """)
    int revokeAllActiveByUserId(@Param("userId") Long userId,
                                @Param("reason") String reason,
                                @Param("now") OffsetDateTime now);

    /** 단일 토큰을 해시로 폐기 (자연 만료 등). 이미 폐기된 토큰은 건드리지 않음. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE RefreshToken r
           SET r.revokedAt = :now, r.revokedReason = :reason
         WHERE r.tokenHash = :hash
           AND r.revokedAt IS NULL
        """)
    int revokeByHashIfActive(@Param("hash") String hash,
                             @Param("reason") String reason,
                             @Param("now") OffsetDateTime now);

    /** 회전 체인 연결 — 방금 발급한 새 토큰 PK를 옛 토큰에 기록. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE RefreshToken r SET r.rotatedToId = :newId WHERE r.tokenHash = :hash")
    int linkRotatedTo(@Param("hash") String hash, @Param("newId") Long newId);
}
