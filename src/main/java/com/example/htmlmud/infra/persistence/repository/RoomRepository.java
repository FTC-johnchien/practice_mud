package com.example.htmlmud.infra.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.htmlmud.infra.persistence.entity.RoomEntity;

public interface RoomRepository extends JpaRepository<RoomEntity, Integer> {

}
