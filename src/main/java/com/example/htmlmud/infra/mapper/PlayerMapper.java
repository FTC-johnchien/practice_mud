package com.example.htmlmud.infra.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import com.example.htmlmud.domain.model.PlayerRecord;
import com.example.htmlmud.infra.persistence.entity.PlayerEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PlayerMapper {

  // 1. Entity -> Record (載入時用)
  // MapStruct 會自動對應相同名稱的欄位 (nickname -> nickname)
  PlayerRecord toRecord(PlayerEntity entity);

  // 2. Record -> Entity (存檔時用)
  // 這裡我們通常是用 "更新" 模式，而不是 "新建"
  // @MappingTarget 代表要把 source 的值填入 target
  void updateEntityFromRecord(PlayerRecord source, @MappingTarget PlayerEntity target);
}
