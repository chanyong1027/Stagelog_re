package com.stagelog.Stagelog.auth.repository;

import com.stagelog.Stagelog.auth.domain.LoginAttempt;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    Optional<LoginAttempt> findByEmailAndClientIp(String email, String clientIp);

    void deleteByEmailAndClientIp(String email, String clientIp);
}
