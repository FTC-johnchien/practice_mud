package com.example.htmlmud.infra.persistence.repository;

import java.util.Collections;
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
import lombok.extern.slf4j.Slf4j;

/**
 * 模板資料倉儲 (Template Repository)
 * 由 Spring 容器管理單例生命週期，內部持有實例化 Map 快取，
 * 同時提供 static delegate 方法確保既有靜態存取與各類測試 100% 向下相容。
 */
@Component
@Slf4j
public class TemplateRepository {

  // 快取映射 (static final 確保多實例與靜態存取資料一致性，必須在 INSTANCE 前初始化)
  private static final Map<String, ZoneTemplate> zoneTemplates = new ConcurrentHashMap<>();
  private static final Map<String, RoomTemplate> roomTemplates = new ConcurrentHashMap<>();
  private static final Map<String, MobTemplate> mobTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ItemTemplate> itemTemplates = new ConcurrentHashMap<>();
  private static final Map<String, SkillTemplate> skillTemplates = new ConcurrentHashMap<>();
  private static final Map<String, RaceTemplate> raceTemplates = new ConcurrentHashMap<>();
  private static final Map<String, CompanionTemplate> companionTemplates = new ConcurrentHashMap<>();
  private static final Map<String, FormationTemplate> formationTemplates = new ConcurrentHashMap<>();
  private static final Map<String, PartyMemberSkill> partySkillTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ClassTemplate> classTemplates = new ConcurrentHashMap<>();
  private static final Map<String, ShopTemplate> shopTemplates = new ConcurrentHashMap<>();

  // 靜態持有單例實例，確保向下相容靜態呼叫與純單元測試
  private static final TemplateRepository INSTANCE = new TemplateRepository();

  public static TemplateRepository getInstance() {
    return INSTANCE;
  }

  // 預先計算基礎技能的 ID
  private static final Map<SkillCategory, String> BASIC_SKILL_IDS = new EnumMap<>(SkillCategory.class);
  private static final Map<SkillCategory, String> MOB_BASIC_SKILL_IDS = new EnumMap<>(SkillCategory.class);

  static {
    for (SkillCategory cat : SkillCategory.values()) {
      BASIC_SKILL_IDS.put(cat, ("basic_" + cat.name()).toLowerCase());
    }
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
  }

  public TemplateRepository() {
    initDefaults();
  }

  public void clear() {
    zoneTemplates.clear();
    roomTemplates.clear();
    mobTemplates.clear();
    itemTemplates.clear();
    skillTemplates.clear();
    raceTemplates.clear();
    companionTemplates.clear();
    formationTemplates.clear();
    partySkillTemplates.clear();
    classTemplates.clear();
    shopTemplates.clear();
    initDefaults();
  }

  public static void clearAll() {
    INSTANCE.clear();
  }

  public synchronized void initDefaults() {
    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
    if (partySkillTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/party/party_skills.json")) {
        if (is != null) {
          java.util.List<PartyMemberSkill> skills = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<PartyMemberSkill>>() {});
          for (PartyMemberSkill s : skills) addPartySkill(s);
        }
      } catch (Exception e) {
        log.error("Failed to load data/party/party_skills.json", e);
      }
    }
    if (companionTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/companions/default_companions.json")) {
        if (is != null) {
          java.util.List<CompanionTemplate> companions = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<CompanionTemplate>>() {});
          for (CompanionTemplate c : companions) addCompanion(c);
        }
      } catch (Exception e) {
        log.error("Failed to load data/companions/default_companions.json", e);
      }
    }
    if (formationTemplates.isEmpty()) {
      try (var is = TemplateRepository.class.getClassLoader().getResourceAsStream("data/formations/formations.json")) {
        if (is != null) {
          java.util.List<FormationTemplate> formations = mapper.readValue(is, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<FormationTemplate>>() {});
          for (FormationTemplate f : formations) addFormation(f);
        }
      } catch (Exception e) {
        log.error("Failed to load data/formations/formations.json", e);
      }
    }
  }

  public static synchronized void initDataDrivenDefaults() {
    INSTANCE.initDefaults();
  }

  // --- Zone ---
  public void addZone(ZoneTemplate tpl) {
    if (tpl != null && tpl.id() != null) zoneTemplates.put(tpl.id(), tpl);
  }
  public Optional<ZoneTemplate> getZone(String id) {
    return Optional.ofNullable(zoneTemplates.get(id));
  }
  public static void registerZone(ZoneTemplate tpl) { INSTANCE.addZone(tpl); }
  public static Optional<ZoneTemplate> findZone(String id) { return INSTANCE.getZone(id); }

  // --- Room ---
  public void addRoom(RoomTemplate tpl) {
    if (tpl != null && tpl.id() != null) roomTemplates.put(tpl.id(), tpl);
  }
  public Optional<RoomTemplate> getRoom(String id) {
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
  public Map<String, RoomTemplate> getRoomTemplateMap() { return roomTemplates; }
  public static void registerRoom(RoomTemplate tpl) { INSTANCE.addRoom(tpl); }
  public static Optional<RoomTemplate> findRoom(String id) { return INSTANCE.getRoom(id); }
  public static Map<String, RoomTemplate> getRoomTemplates() { return INSTANCE.getRoomTemplateMap(); }

  // --- Mob ---
  public void addMob(MobTemplate tpl) {
    if (tpl != null && tpl.id() != null) mobTemplates.put(tpl.id(), tpl);
  }
  public Optional<MobTemplate> getMob(String id) {
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
    String cleanId = id.contains(":") ? id.substring(id.indexOf(":") + 1) : id;
    Optional<CompanionTemplate> compOpt = getCompanion(cleanId);
    if (compOpt.isPresent()) {
      String zoneId = id.contains(":") ? id.substring(0, id.indexOf(":")) : "newbie_village";
      return Optional.of(compOpt.get().toMobTemplate(zoneId));
    }
    return Optional.empty();
  }
  public static void registerMob(MobTemplate tpl) { INSTANCE.addMob(tpl); }
  public static Optional<MobTemplate> findMob(String id) { return INSTANCE.getMob(id); }

  // --- Item ---
  public void addItem(ItemTemplate tpl) {
    if (tpl != null && tpl.id() != null) itemTemplates.put(tpl.id(), tpl);
  }
  public Optional<ItemTemplate> getItem(String id) {
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
  public static void registerItem(ItemTemplate tpl) { INSTANCE.addItem(tpl); }
  public static Optional<ItemTemplate> findItem(String id) { return INSTANCE.getItem(id); }

  // --- Skill ---
  public void addSkill(SkillTemplate tpl) {
    if (tpl != null && tpl.getId() != null) skillTemplates.put(tpl.getId(), tpl);
  }
  public Optional<SkillTemplate> getSkillTemplate(String id) {
    return Optional.ofNullable(skillTemplates.get(id));
  }
  public Map<String, SkillTemplate> getAllSkillMap() { return skillTemplates; }
  public SkillTemplate getSkillById(String id) {
    SkillTemplate tpl = skillTemplates.get(id);
    if (tpl == null) throw new MudException("Skill not found id:" + id);
    return tpl;
  }
  public SkillTemplate getDefaultSkillByCategory(SkillCategory category) {
    return skillTemplates.get(BASIC_SKILL_IDS.get(category));
  }
  public String getDefaultSkillIdByCategory(SkillCategory category) {
    SkillTemplate defaultSkill = getDefaultSkillByCategory(category);
    return defaultSkill != null ? defaultSkill.getId() : BASIC_SKILL_IDS.get(category);
  }
  public SkillTemplate getMobDefaultSkillByCategory(SkillCategory category) {
    return skillTemplates.get(MOB_BASIC_SKILL_IDS.get(category));
  }
  public String getMobDefaultSkillIdByCategory(SkillCategory category) {
    SkillTemplate mobDefaultSkill = getMobDefaultSkillByCategory(category);
    return mobDefaultSkill != null ? mobDefaultSkill.getId() : MOB_BASIC_SKILL_IDS.get(category);
  }
  public static void registerSkill(SkillTemplate tpl) { INSTANCE.addSkill(tpl); }
  public static Optional<SkillTemplate> findSkill(String id) { return INSTANCE.getSkillTemplate(id); }
  public static Map<String, SkillTemplate> getAllSkills() { return INSTANCE.getAllSkillMap(); }
  public static SkillTemplate getSkill(String id) { return INSTANCE.getSkillById(id); }
  public static SkillTemplate getDefaultSkill(SkillCategory category) { return INSTANCE.getDefaultSkillByCategory(category); }
  public static String getDefaultSkillId(SkillCategory category) { return INSTANCE.getDefaultSkillIdByCategory(category); }
  public static SkillTemplate getMobDefaultSkill(SkillCategory category) { return INSTANCE.getMobDefaultSkillByCategory(category); }
  public static String getMobDefaultSkillId(SkillCategory category) { return INSTANCE.getMobDefaultSkillIdByCategory(category); }

  // --- Race ---
  public void addRace(RaceTemplate tpl) {
    if (tpl != null && tpl.id() != null) raceTemplates.put(tpl.id(), tpl);
  }
  public Optional<RaceTemplate> getRace(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(raceTemplates.get(id));
  }
  public Map<String, RaceTemplate> getRaceTemplateMap() { return raceTemplates; }
  public static void registerRace(RaceTemplate tpl) { INSTANCE.addRace(tpl); }
  public static Optional<RaceTemplate> findRace(String id) { return INSTANCE.getRace(id); }
  public static Map<String, RaceTemplate> getRaceTemplates() { return INSTANCE.getRaceTemplateMap(); }

  // --- Companion ---
  public void addCompanion(CompanionTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      companionTemplates.put(tpl.id(), tpl);
      if (tpl.aliases() != null) {
        for (String alias : tpl.aliases()) {
          companionTemplates.putIfAbsent(alias, tpl);
        }
      }
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
  public Optional<CompanionTemplate> getCompanion(String id) {
    if (id == null) return Optional.empty();
    CompanionTemplate t = companionTemplates.get(id);
    if (t != null) return Optional.of(t);
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
  public Map<String, CompanionTemplate> getAllCompanionMap() { return Collections.unmodifiableMap(companionTemplates); }
  public static void registerCompanion(CompanionTemplate tpl) { INSTANCE.addCompanion(tpl); }
  public static Optional<CompanionTemplate> findCompanion(String id) { return INSTANCE.getCompanion(id); }
  public static Map<String, CompanionTemplate> getAllCompanions() { return INSTANCE.getAllCompanionMap(); }

  // --- Formation ---
  public void addFormation(FormationTemplate tpl) {
    if (tpl != null && tpl.getId() != null) formationTemplates.put(tpl.getId(), tpl);
  }
  public Optional<FormationTemplate> getFormation(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(formationTemplates.get(id));
  }
  public Map<String, FormationTemplate> getAllFormationMap() { return Collections.unmodifiableMap(formationTemplates); }
  public static void registerFormation(FormationTemplate tpl) { INSTANCE.addFormation(tpl); }
  public static Optional<FormationTemplate> findFormation(String id) { return INSTANCE.getFormation(id); }
  public static Map<String, FormationTemplate> getAllFormations() { return INSTANCE.getAllFormationMap(); }

  // --- PartySkill ---
  public void addPartySkill(PartyMemberSkill skill) {
    if (skill != null && skill.getId() != null) partySkillTemplates.put(skill.getId(), skill);
  }
  public Optional<PartyMemberSkill> getPartySkill(String id) {
    if (id == null) return Optional.empty();
    PartyMemberSkill pSkill = partySkillTemplates.get(id);
    if (pSkill != null) return Optional.of(pSkill);

    // 統一 Data-Driven 唯一真相源：降級查詢 SkillTemplate 並動態適配為 PartyMemberSkill
    SkillTemplate sTpl = skillTemplates.get(id);
    if (sTpl != null) {
      return Optional.of(PartyMemberSkill.fromSkillTemplate(sTpl));
    }
    // 支援舊名稱與新職業技能別名映射
    String alias = switch (id.toLowerCase()) {
      case "tank_taunt" -> "class_warrior_taunt";
      case "heal_single" -> "class_cleric_heal";
      case "heal_all_purify" -> "class_cleric_purify";
      case "taoist_seal" -> "class_taoist_seal";
      default -> null;
    };
    if (alias != null) {
      SkillTemplate aliasTpl = skillTemplates.get(alias);
      if (aliasTpl != null) {
        PartyMemberSkill adapted = PartyMemberSkill.fromSkillTemplate(aliasTpl);
        adapted.setId(id);
        return Optional.of(adapted);
      }
    }
    return Optional.empty();
  }
  public Map<String, PartyMemberSkill> getAllPartySkillMap() { return Collections.unmodifiableMap(partySkillTemplates); }
  public static void registerPartySkill(PartyMemberSkill skill) { INSTANCE.addPartySkill(skill); }
  public static Optional<PartyMemberSkill> findPartySkill(String id) { return INSTANCE.getPartySkill(id); }
  public static Map<String, PartyMemberSkill> getAllPartySkills() { return INSTANCE.getAllPartySkillMap(); }

  // --- Class ---
  public void addClass(ClassTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      classTemplates.put(tpl.id(), tpl);
      classTemplates.put(tpl.id().toLowerCase(), tpl);
    }
  }
  public Optional<ClassTemplate> getClassTemplate(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(classTemplates.get(id));
  }
  public Map<String, ClassTemplate> getAllClassMap() { return Collections.unmodifiableMap(classTemplates); }
  public static void registerClass(ClassTemplate tpl) { INSTANCE.addClass(tpl); }
  public static Optional<ClassTemplate> findClass(String id) { return INSTANCE.getClassTemplate(id); }
  public static Map<String, ClassTemplate> getAllClasses() { return INSTANCE.getAllClassMap(); }

  // --- Shop ---
  public void addShop(ShopTemplate tpl) {
    if (tpl != null && tpl.id() != null) {
      shopTemplates.put(tpl.id(), tpl);
      if (tpl.roomId() != null) {
        shopTemplates.put("room:" + tpl.roomId(), tpl);
      }
    }
  }
  public Optional<ShopTemplate> getShop(String id) {
    if (id == null) return Optional.empty();
    return Optional.ofNullable(shopTemplates.get(id));
  }
  public Optional<ShopTemplate> getShopByRoom(String roomId) {
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
  public Map<String, ShopTemplate> getAllShopMap() { return Collections.unmodifiableMap(shopTemplates); }
  public static void registerShop(ShopTemplate tpl) { INSTANCE.addShop(tpl); }
  public static Optional<ShopTemplate> findShop(String id) { return INSTANCE.getShop(id); }
  public static Optional<ShopTemplate> findShopByRoomId(String roomId) { return INSTANCE.getShopByRoom(roomId); }
  public static Map<String, ShopTemplate> getAllShops() { return INSTANCE.getAllShopMap(); }

  // --- Validate ---
  public void validateData() {
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

  public static void validate() {
    INSTANCE.validateData();
  }
}
