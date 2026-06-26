package com.stagelog.Stagelog.user.repository;

import com.stagelog.Stagelog.user.domain.Provider;
import com.stagelog.Stagelog.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    Optional<User> findByPublicId(UUID publicId);
}
