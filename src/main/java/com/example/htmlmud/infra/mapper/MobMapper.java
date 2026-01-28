package com.example.htmlmud.infra.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.map.MobTemplate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MobMapper {

  @Mapping(target = "hp", source = "maxHp")
  @Mapping(target = "mp", source = "maxMp")
  @Mapping(target = "stamina", source = "maxStamina")
  // 忽略 equipment 欄位，因為類型不相容且涉及業務邏輯 (建立物品實體)
  @Mapping(target = "equipment", ignore = true)
  LivingState toLivingState(MobTemplate template);

}
