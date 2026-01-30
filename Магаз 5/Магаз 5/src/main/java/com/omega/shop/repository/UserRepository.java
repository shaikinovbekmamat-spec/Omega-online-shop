package com.omega.shop.repository;

import com.omega.shop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Найти пользователя по username
     */
    Optional<User> findByUsername(String username);

    /**
     * Найти пользователя по email
     */
    Optional<User> findByEmail(String email);

    /**
     * Проверить существует ли пользователь с таким username
     */
    boolean existsByUsername(String username);

    /**
     * Проверить существует ли пользователь с таким email
     */
    boolean existsByEmail(String email);
}
