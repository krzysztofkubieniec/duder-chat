package org.duder.user.repository;

import org.duder.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findBySessionToken(String sessionToken);

    Optional<User> findByLoginIgnoreCase(String login);

    Optional<User> findByLoginIgnoreCaseAndPasswordIgnoreCase(String login, String password);

    Page<User> findAllByIdNot(Long id, Pageable pageable);
}
