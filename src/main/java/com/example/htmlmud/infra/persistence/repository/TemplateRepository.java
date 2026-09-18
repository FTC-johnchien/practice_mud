package com.example.htmlmud.infra.persistence.repository;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateRepository {

  // 儲存所有的靜態資料
  // Key: 絕對 ID (e.g., "newbie_village:sword")
  private static final Map<String, ZoneTemplate> zoneTemplates = new ConcurrentHashMap<>();

  @Getter
  private static final Map<String, RoomTemplate> roomTemplates = new ConcurrentHashMap<>();
  private static final Map<String, MobTemplate> mobTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ItemTemplate> itemTemplates = new ConcurrentHashMap<>();

  private static final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();

  @Getter
  private static final Map<String, RaceTemplate> raceTemplates = new ConcurrentHashMap<>();

  private static final Map<String, CompanionTemplate> companionTemplates = new ConcurrentHashMap<>();
  private static final Map<String, FormationTemplate> formationTemplates = new ConcurrentHashMap<>();
  private static final Map<String, PartyMemberSkill> partySkillTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ClassTemplate> classTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ShopTemplate> shopTemplates = new ConcurrentHashMap<>();



  // 預先計算基礎技能的 ID，避免在戰鬥等高頻率呼叫中重複進行字串拼接與轉換
  private static final Map<SkillCategory, String> BASIC_SKILL_IDS =
      new EnumMap<>(SkillCategory.class);
  private static final Map<SkillCategory, String> MOB_BASIC_SKILL_IDS =
      new EnumMap<>(SkillCategory.class);

  static {
    for (SkillCategory cat : SkillCategory.values()) {
      BASIC_SKILL_IDS.put(cat, ("basic_" + cat.name()).toLowerCase());
    }
    // 修正特例技能命名與檔案實際 ID 對齊
    BASIC_SKILL_IDS.put(SkillCategory.UNARMED, "basic_fist");
    BASIC_SKILL_IDS.put(SkillCategory.BOW, "basic_archery");
    BASIC_SKILL_IDS.put(SkillCategory.SPEAR, "basic_polearm");
    BASIC_SKILL_IDS.put(SkillCategory.POLEARM, "basic_polearm");
    BASIC_SKILL_IDS.put(SkillCategory.HAMMER, "basic_blunt");
    BASIC_SKILL_IDS.put(SkillCategory.FORCE, "basic_breathing");
    BASIC_SKILL_IDS.put(SkillCategory.MEDICAL, "basic_first_aid");
    BASIC_SKILL_IDS.put(SkillCategory.STAFF, "basic_magic_staff");
    BASIC_SKILL_IDS.put(SkillCategory.WAND, "basic_magic_staff");

    MOB_BASIC_SKILL_IDS.put(SkillCategory.UNARMED, "mob_hit");
    MOB_BASIC_SKILL_IDS.put(SkillCategory.DODGE, "mob_basic_dodge");
    MOB_BASIC_SKILL_IDS.put(SkillCategory.PARRY, "mob_basic_parry");

    initDataDrivenDefaults();
  }

  public static synchronized void initDataDrivenDefaults() {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    if (partySkillTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/party/party_skills.json")) {
        if (is != null) {
          java.util.List<PartyMemberSkill> skills = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<PartyMemberSkill>>() {});
          for (PartyMemberSkill s : skills) registerPartySkill(s);
        }
      } catch (Exception e) {
        log.error("Failed to load data/party/party_skills.json", e);
      }
    }
    if (companionTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/companions/default_companions.json")) {
        if (is != null) {
          java.util.List<CompanionTemplate> companions = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<CompanionTemplate>>() {});
          for (CompanionTemplate c : companions) registerCompanion(c);
        }
      } catch (Exception e) {
        log.error("Failed to load data/companions/default_companions.json", e);
      }
    }
    if (formationTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/formations/formations.json")) {
        if (is != null) {
          java.util.List<FormationTemplate> formations = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<FormationTemplate>>() {});
          for (FormationTemplate f : formations) registerFormation(f);
        }
      } catch (Exception e) {
        log.error("Failed to load data/formations/formations.json", e);
      }
    }
  }



  // 註冊方法
  public static void registerZone(ZoneTemplate tpl) {
    zoneTemplates.put(tpl.id(), tpl);
  }

  // 查詢方法
  public static Optional<ZoneTemplate> findZone(String id) {
    return Optional.ofNullable(zoneTemplates.get(id));
  }

  // 這是給 MapLoader 呼叫的，用來填入資料
  public static void registerRoom(RoomTemplate tpl) {
    roomTemplates.put(tpl.id(), tpl);
  }

  // 查詢方法
  public static Optional<RoomTemplate> findRoom(String id) {
    if (id == null) return Optional.empty();
    RoomTemplate tpl = roomTemplates.get(id);
    if (tpl != null) return Optional.of(tpl);
    if (!id.contains(":")) {
      for (Map.Entry<String, RoomTemplate> entry : roomTemplates.entrySet()) {
        if (entry.getKey().endsWith(":" + id)) {
          return Optional.of(entry.getValue());
        }
      }
    }
    return Optional.empty();
  }

  public static void registerMob(MobTemplate tpl) {
    mobTemplates.put(tpl.id(), tpl);
  }

  public static Optional<MobTemplate> findMob(String id) {
    if (id == null) return Optional.empty();
    MobTemplate tpl = mobTemplates.get(id);
    if (tpl != null) return Optional.of(tpl);
    if (!id.contains(":")) {
      for (Map.Entry<String, MobTemplate> entry : mobTemplates.entrySet()) {
        if (entry.getKey().endsWith(":" + id)) {
          return Optional.of(entry.getValue());
        }
      }
    }
    // 檢查是否為 Companion，動態轉為 MobTemplate (Single Source of Truth)
    String cleanId = id.contains(":") ? id.substring(id.indexOf(":") + 1) : id;
    Optional<CompanionTemplate> compOpt = findCompanion(cleanId);
    if (compOpt.isPresent()) {
      String zoneId = id.contains(":") ? id.substring(0, id.indexOf(":")) : "newbie_village";
      return Optional.of(compOpt.get().toMobTemplate(zoneId));
    }
    return Optional.empty();
  }

  public static void registerItem(ItemTemplate tpl) {
    itemTemplates.put(tpl.id(), tpl);
  }

  public static Optional<ItemTemplate> findItem(String id) {
    if (id == null) return Optional.empty();
    ItemTemplate tpl = itemTemplates.get(id);
    if (tpl != null) return Optional.of(tpl);

    if (id.contains(":")) {
      String cleanId = id.substring(id.indexOf(":") + 1);
      ItemTemplate globalTpl = itemTemplates.get(cleanId);
      if (globalTpl != null) return Optional.of(globalTpl);
    } else {
      for (Map.Entry<String, ItemTemplate> entry : itemTemplates.entrySet()) {
        if (entry.getKey().endsWith(":" + id)) {
          return Optional.of(entry.getValue());
        }
      }
    }
    return Optional.empty();
  }

  public static void registerSkill(SkillTemplate tpl) {
    skillTemplates.put(tpl.getId(), tpl);
  }

  public static Optional<SkillTemplate> findSkill(String id) {
    return Optional.ofNullable(skillTemplates.get(id));
  }

  public static Map<String, SkillTemplate> getAllSkills() {
    return skillTemplates;
  }

  public static SkillTemplate getSkill(String id) {
    SkillTemplate tpl = skillTemplates.get(id);
    if (tpl == null) {
      throw new MudException("Skill not found id:" + id);
    }
    return tpl;
  }

  public static SkillTemplate getDefaultSkill(SkillCategory category) {
    return skillTemplates.get(BASIC_SKILL_IDS.get(category));
  }

  public static String getDefaultSkillId(SkillCategory category) {
    SkillTemplate defaultSkill = getDefaultSkill(category);
    return defaultSkill != null ? defaultSkill.getId() : BASIC_SKILL_IDS.get(category);
  }

  public static SkillTemplate getMobDefaultSkill(SkillCategory category) {
    return skillTemplates.get(MOB_BASIC_SKILL_IDS.get(category));
  }

  public static String getMobDefaultSkillId(SkillCategory category) {
    SkillTemplate mobDefaultSkill = getMobDefaultSkill(category);
    return mobDefaultSkill != null ? mobDefaultSkill.getId() : MOB_BASIC_SKILL_IDS.get(category);
  }


  public static void registerRace(RaceTemplate tpl) {
    raceTemplates.put(tpl.id(), tpl);
  }

  public static Optional<RaceTemplate> findRace(String id) {
    if (id == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(raceTemplates.get(id));
  }

  public static void registerCompanion(CompanionTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      companionTemplates.put(tpl.id(), tpl);
      if (tpl.aliases() != null) {
        for (String alias : tpl.aliases()) {
          companionTemplates.putIfAbsent(alias, tpl);
        }
      }
      // 自動轉換為 MobTemplate 註冊至城鎮怪物表，供城鎮房間直接生成 (Single Source of Truth)
      String homeZone = "newbie_village";
      if (tpl.homeRoomId() != null && tpl.homeRoomId().contains(":")) {
        homeZone = tpl.homeRoomId().substring(0, tpl.homeRoomId().indexOf(":"));
      }
      MobTemplate mobTpl = tpl.toMobTemplate(homeZone);
      mobTemplates.put(homeZone + ":" + tpl.id(), mobTpl);
      mobTemplates.put(tpl.id(), mobTpl);
      if (tpl.aliases() != null) {
        for (String alias : tpl.aliases()) {
          mobTemplates.putIfAbsent(homeZone + ":" + alias, mobTpl);
          mobTemplates.putIfAbsent(alias, mobTpl);
        }
      }
    }
  }

  public static Optional<CompanionTemplate> findCompanion(String id) {
    if (id == null) return Optional.empty();
    CompanionTemplate t = companionTemplates.get(id);
    if (t != null) return Optional.of(t);
    // 檢查大小寫與別名
    for (CompanionTemplate c : companionTemplates.values()) {
      if (c.id().equalsIgnoreCase(id) || c.name().equalsIgnoreCase(id)) {
        return Optional.of(c);
      }
      if (c.aliases() != null) {
        for (String a : c.aliases()) {
          if (a.equalsIgnoreCase(id)) return Optional.of(c);
        }
      }
    }
    return Optional.empty();
  }

  public static Map<String, CompanionTemplate> getAllCompanions() {
    return java.util.Collections.unmodifiableMap(companionTemplates);
  }

  public static void registerFormation(FormationTemplate tpl) {
    if (tpl != null && tpl.getId() != null) {
      formationTemplates.put(tpl.getId(), tpl);
    }
  }

  public static Optional<FormationTemplate> findFormation(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(formationTemplates.get(id));
  }

  public static Map<String, FormationTemplate> getAllFormations() {
    return java.util.Collections.unmodifiableMap(formationTemplates);
  }

  public static void registerPartySkill(PartyMemberSkill skill) {
    if (skill != null && skill.getId() != null) {
      partySkillTemplates.put(skill.getId(), skill);
    }
  }

  public static Optional<PartyMemberSkill> findPartySkill(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(partySkillTemplates.get(id));
  }

  public static Map<String, PartyMemberSkill> getAllPartySkills() {
    return java.util.Collections.unmodifiableMap(partySkillTemplates);
  }

  public static void registerClass(ClassTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      classTemplates.put(tpl.id(), tpl);
      classTemplates.put(tpl.id().toLowerCase(), tpl);
    }
  }

  public static Optional<ClassTemplate> findClass(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(classTemplates.get(id));
  }

  public static Map<String, ClassTemplate> getAllClasses() {
    return java.util.Collections.unmodifiableMap(classTemplates);
  }

  public static void registerShop(ShopTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      shopTemplates.put(tpl.id(), tpl);
      if (tpl.roomId() != null) {
        shopTemplates.put("room:" + tpl.roomId(), tpl);
      }
    }
  }

  public static Optional<ShopTemplate> findShop(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(shopTemplates.get(id));
  }

  public static Optional<ShopTemplate> findShopByRoomId(String roomId) {
    if (roomId == null) return Optional.empty();
    ShopTemplate shop = shopTemplates.get("room:" + roomId);
    if (shop != null) return Optional.of(shop);
    for (ShopTemplate s : shopTemplates.values()) {
      if (roomId.equals(s.roomId()) || (s.roomId() != null && roomId.endsWith(":" + s.roomId()))) {
        return Optional.of(s);
      }
    }
    return Optional.empty();
  }

  public static Map<String, ShopTemplate> getAllShops() {
    return java.util.Collections.unmodifiableMap(shopTemplates);
  }



  // 檢查資料完整性 (Server 啟動時檢查)
  public static void validate() {
    // 1. 檢查 Room Exits
    for (RoomTemplate room : roomTemplates.values()) {
      if (room.exits() != null) {
        for (Map.Entry<String, RoomExit> entry : room.exits().entrySet()) {
          String targetId = entry.getValue().targetRoomId();
          if (targetId != null && !targetId.isBlank()) {
            String resolvedId = targetId.contains(":") ? targetId
                : (room.zoneId() != null ? room.zoneId() + ":" + targetId : targetId);
            if (!roomTemplates.containsKey(resolvedId) && !roomTemplates.containsKey(targetId)) {
              log.warn("Data Validation Warning: Room [{}] exit [{}] points to non-existent room [{}]",
                  room.id(), entry.getKey(), targetId);
            }
          }
        }
      }
    }

    // 2. 檢查基礎技能與怪物技能存在性
    for (Map.Entry<SkillCategory, String> entry : BASIC_SKILL_IDS.entrySet()) {
      if (!skillTemplates.containsKey(entry.getValue())) {
        log.warn("Data Validation Warning: Basic skill ID [{}] for category [{}] not found in skillTemplates",
            entry.getValue(), entry.getKey());
      }
    }
    for (Map.Entry<SkillCategory, String> entry : MOB_BASIC_SKILL_IDS.entrySet()) {
      if (!skillTemplates.containsKey(entry.getValue())) {
        log.warn("Data Validation Warning: Mob basic skill ID [{}] for category [{}] not found in skillTemplates",
            entry.getValue(), entry.getKey());
      }
    }

    log.info("TemplateRepository validation complete. Loaded: {} zones, {} rooms, {} mobs, {} items, {} skills, {} races",
        zoneTemplates.size(), roomTemplates.size(), mobTemplates.size(), itemTemplates.size(), skillTemplates.size(), raceTemplates.size());
  }
}
