package com.stagelog.Stagelog.global.jwt.repository;

import com.stagelog.Stagelog.global.jwt.domain.RefreshToken;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserId(Long userId);

    /** logout / reuse detection 시 user의 활성 토큰만 일괄 폐기 대상 조회. */
    List<RefreshToken> findAllByUserIdAndRevokedAtIsNull(Long userId);

    /** rotation chain 검증용 — id 오름차순으로 user의 모든 토큰 이력 조회. */
    List<RefreshToken> findAllByUserIdOrderByIdAsc(Long userId);
}
