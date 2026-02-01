package com.example.htmlmud.infra.mapper;

import com.example.htmlmud.domain.model.GameItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.infra.persistence.entity.ItemTemplateEntity;

@Mapper(componentModel = "spring",
    // 忽略沒對應到的欄位 (重要！防止 MapStruct 報錯說 record 缺欄位)
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    // 當來源是 null 時不覆蓋目標 (選用，視需求而定)
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ItemTemplateMapper {

  // 1. Entity -> Record (載入時用)
  // MapStruct 會自動對應相同名稱的欄位 (nickname -> nickname)
  ItemTemplate toRecord(ItemTemplateEntity entity);

  // 2. Record -> Entity (存檔時用)
  // 這裡我們通常是用 "更新" 模式，而不是 "新建"
  // @MappingTarget 代表要把 source 的值填入 target
  void updateEntityFromRecord(ItemTemplate source, @MappingTarget ItemTemplateEntity target);

  // 3. Template -> GameItem (實例化時用)
  @Mapping(target = "id", ignore = true) // ID 由 Factory 生成 UUID，不從 Template 複製
  @Mapping(target = "template", source = "template") // 保留模板引用
  @Mapping(target = "currentDurability", source = "equipmentProp.maxDurability")
  @Mapping(target = "maxDurability", source = "equipmentProp.maxDurability")
  GameItem toGameItem(ItemTemplate template);
}
