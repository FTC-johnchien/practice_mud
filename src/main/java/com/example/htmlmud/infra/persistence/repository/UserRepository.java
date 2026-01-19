package com.example.htmlmud.infra.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

  Optional<UserEntity> findByUsername(String username);

  boolean existsByUsername(String username);

}
