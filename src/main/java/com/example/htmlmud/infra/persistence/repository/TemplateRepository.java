package com.example.htmlmud.infra.persistence.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.ZoneTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateRepository {

  // 儲存所有的靜態資料
  // Key: 絕對 ID (e.g., "newbie_village:sword")
  private final Map<String, ZoneTemplate> zoneTemplates = new ConcurrentHashMap<>();

  @Getter
  private final Map<String, RoomTemplate> roomTemplates = new ConcurrentHashMap<>();
  private final Map<String, MobTemplate> mobTemplates = new ConcurrentHashMap<>();
  private final Map<String, ItemTemplate> itemTemplates = new ConcurrentHashMap<>();

  // 註冊方法
  public void registerZone(ZoneTemplate tpl) {
    zoneTemplates.put(tpl.id(), tpl);
  }

  // 查詢方法
  public Optional<ZoneTemplate> findZone(String id) {
    return Optional.ofNullable(zoneTemplates.get(id));
  }

  // 這是給 MapLoader 呼叫的，用來填入資料
  public void registerRoom(RoomTemplate tpl) {
    roomTemplates.put(tpl.id(), tpl);
  }

  // 查詢方法
  public Optional<RoomTemplate> findRoom(String id) {
    return Optional.ofNullable(roomTemplates.get(id));
  }

  public void registerMob(MobTemplate tpl) {
    mobTemplates.put(tpl.id(), tpl);
  }

  public Optional<MobTemplate> findMob(String id) {
    return Optional.ofNullable(mobTemplates.get(id));
  }

  public void registerItem(ItemTemplate tpl) {
    itemTemplates.put(tpl.id(), tpl);
  }

  public Optional<ItemTemplate> findItem(String id) {
    return Optional.ofNullable(itemTemplates.get(id));
  }


  // 檢查資料完整性 (Server 啟動時檢查)
  public void validate() {
    // 檢查 room 的 exit 是否指向存在的 room id
    // 檢查 mob 的 loot table 是否指向存在的 item id
  }
}
