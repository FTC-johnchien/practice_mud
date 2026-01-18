package com.example.htmlmud.infra.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.CharacterEntity;

public interface CharacterRepository extends JpaRepository<CharacterEntity, String> {
  Optional<CharacterEntity> findByAccountIdAndName(Integer accountId, String name);

  boolean existsByAccountIdAndName(Integer accountId, String name);

}
