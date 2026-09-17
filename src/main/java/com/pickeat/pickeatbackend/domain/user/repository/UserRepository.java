package com.pickeat.pickeatbackend.domain.user.repository;

import com.pickeat.pickeatbackend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
}
