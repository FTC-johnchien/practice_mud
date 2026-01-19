package com.example.htmlmud.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.RoomStateEntity;

public interface RoomStateRepository extends JpaRepository<RoomStateEntity, Long> {

}
