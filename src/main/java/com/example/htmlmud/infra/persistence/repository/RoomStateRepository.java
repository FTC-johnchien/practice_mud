package com.example.htmlmud.infra.persistence.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;
import com.example.htmlmud.infra.persistence.entity.RoomStateEntity;

@Repository
public class RoomStateRepository {

  private final Map<String, RoomStateEntity> storage = new ConcurrentHashMap<>();

  private String toKey(String roomId, String zoneId) {
    return (zoneId != null ? zoneId : "") + ":" + (roomId != null ? roomId : "");
  }

  public Optional<RoomStateEntity> findByRoomIdAndZoneId(String roomId, String zoneId) {
    return Optional.ofNullable(storage.get(toKey(roomId, zoneId)));
  }

  public RoomStateEntity save(RoomStateEntity entity) {
    if (entity != null) {
      storage.put(toKey(entity.getRoomId(), entity.getZoneId()), entity);
    }
    return entity;
  }

  public void deleteAll() {
    storage.clear();
  }
}
