package com.example.htmlmud.infra.persistence.repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import com.example.htmlmud.infra.persistence.entity.CharacterEntity;

@Repository
public class CharacterRepository {

  private final Map<String, CharacterEntity> storage = new ConcurrentHashMap<>();

  public Optional<CharacterEntity> findByUidAndName(String uid, String name) {
    if (uid == null || name == null) return Optional.empty();
    return storage.values().stream()
        .filter(c -> uid.equals(c.getUid()) && name.equalsIgnoreCase(c.getName()))
        .findFirst();
  }

  public boolean existsByUidAndName(String uid, String name) {
    return findByUidAndName(uid, name).isPresent();
  }

  public Optional<CharacterEntity> findById(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(storage.get(id));
  }

  public CharacterEntity save(CharacterEntity entity) {
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
