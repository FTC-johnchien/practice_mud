package com.example.htmlmud.infra.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;

public interface PlayerRepository extends JpaRepository<PlayerEntity, String> {
  Optional<PlayerEntity> findByAccountIdAndName(Integer accountId, String name);

  boolean existsByAccountIdAndName(Integer accountId, String name);

}
