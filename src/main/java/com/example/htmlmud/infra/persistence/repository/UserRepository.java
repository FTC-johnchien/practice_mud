package com.example.htmlmud.infra.persistence.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import com.example.htmlmud.infra.persistence.entity.UserEntity;

@Repository
public class UserRepository {

  private final Map<String, UserEntity> storage = new ConcurrentHashMap<>();

  public Optional<UserEntity> findByUsername(String username) {
    if (username == null) return Optional.empty();
    return storage.values().stream()
        .filter(u -> username.equalsIgnoreCase(u.getUsername()))
        .findFirst();
  }

  public boolean existsByUsername(String username) {
    return findByUsername(username).isPresent();
  }

  public UserEntity save(UserEntity entity) {
    if (entity.getId() == null || entity.getId().isBlank()) {
      entity.setId(UUID.randomUUID().toString());
    }
    storage.put(entity.getId(), entity);
    return entity;
  }

  public void deleteAll() {
    storage.clear();
  }
}
