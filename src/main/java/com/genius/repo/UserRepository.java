package com.genius.repo;

import com.genius.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
    Optional<User> findByUsername(String username);
    Optional<User> findByMatricNo(String matricNo);
    boolean existsByEmail(String email);
    boolean existsByMatricNo(String matricNo);
    boolean existsByUsername(String username);
}