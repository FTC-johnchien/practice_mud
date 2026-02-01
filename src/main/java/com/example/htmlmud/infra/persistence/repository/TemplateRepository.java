package com.example.htmlmud.infra.persistence.repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.SkillCategory;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.map.RaceTemplate;
import com.example.htmlmud.domain.model.map.RoomTemplate;
import com.example.htmlmud.domain.model.map.SkillTemplate;
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

  private final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();

  @Getter
  private final Map<String, RaceTemplate> raceTemplates = new ConcurrentHashMap<>();



  // 預先計算基礎技能的 ID，避免在戰鬥等高頻率呼叫中重複進行字串拼接與轉換
  private static final Map<SkillCategory, String> BASIC_SKILL_IDS =
      new EnumMap<>(SkillCategory.class);
  private static final Map<SkillCategory, String> MOB_BASIC_SKILL_IDS =
      new EnumMap<>(SkillCategory.class);

  static {
    for (SkillCategory cat : SkillCategory.values()) {
      BASIC_SKILL_IDS.put(cat, ("basic_" + cat.name()).toLowerCase());
    }

    MOB_BASIC_SKILL_IDS.put(SkillCategory.UNARMED, "mob_hit");
    MOB_BASIC_SKILL_IDS.put(SkillCategory.DODGE, "mob_dodge");
    MOB_BASIC_SKILL_IDS.put(SkillCategory.PARRY, "mob_parry");
    // MOB_BASIC_SKILL_IDS.put(SkillCategory.FORCE, "mob_force");
  }



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

  public void registerSkill(SkillTemplate tpl) {
    skillTemplates.put(tpl.getId(), tpl);
  }

  public SkillTemplate getSkill(String id) {
    SkillTemplate tpl = skillTemplates.get(id);
    if (tpl == null) {
      throw new MudException("Skill not found id:" + id);
    }
    return tpl;
  }

  public SkillTemplate getDefaultSkill(SkillCategory category) {
    return skillTemplates.get(BASIC_SKILL_IDS.get(category));
  }

  public String getDefaultSkillId(SkillCategory category) {
    return getDefaultSkill(category).getId();
  }

  public SkillTemplate getMobDefaultSkill(SkillCategory category) {
    return skillTemplates.get(MOB_BASIC_SKILL_IDS.get(category));
  }

  public String getMobDefaultSkillId(SkillCategory category) {
    return getMobDefaultSkill(category).getId();
  }


  public void registerRace(RaceTemplate tpl) {
    raceTemplates.put(tpl.id(), tpl);
  }

  public Optional<RaceTemplate> findRace(String id) {
    return Optional.ofNullable(raceTemplates.get(id));
  }



  // 檢查資料完整性 (Server 啟動時檢查)
  public void validate() {
    // 檢查 room 的 exit 是否指向存在的 room id
    // 檢查 mob 的 loot table 是否指向存在的 item id
  }
}
