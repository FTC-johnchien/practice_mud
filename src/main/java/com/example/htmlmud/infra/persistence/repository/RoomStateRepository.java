package com.example.htmlmud.infra.persistence.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.RoomStateEntity;
import com.example.htmlmud.infra.persistence.entity.RoomStateId;

public interface RoomStateRepository extends JpaRepository<RoomStateEntity, RoomStateId> {

  Optional<RoomStateEntity> findByRoomIdAndZoneId(String roomId, String zoneId);

}
