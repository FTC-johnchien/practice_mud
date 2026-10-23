package com.example.htmlmud.template;

import java.util.Optional;
import java.util.Map;

import com.example.htmlmud.progression.SkillCategory;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.MobTemplate;
import com.example.htmlmud.template.model.RaceTemplate;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.template.model.CompanionTemplate;
import com.example.htmlmud.template.model.RoomTemplate;
import com.example.htmlmud.template.model.ZoneTemplate;
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.template.model.ClassTemplate;
import com.example.htmlmud.party.FormationTemplate;
import com.example.htmlmud.party.PartyMemberSkill;

public interface TemplateReader {

  Optional<ItemTemplate> findItem(String itemId);

  Optional<SkillTemplate> findSkill(String skillId);

  SkillTemplate requireSkill(String skillId);

  SkillTemplate findDefaultSkill(SkillCategory category);

  String findDefaultSkillId(SkillCategory category);

  Optional<RaceTemplate> findRace(String raceId);

  Optional<MobTemplate> findMob(String mobId);

  Optional<CompanionTemplate> findCompanion(String companionId);

  Optional<PartyMemberSkill> findPartySkill(String skillId);

  default String resolveSkillAlias(String skillId) {
    return null;
  }

  Optional<FormationTemplate> findFormation(String formationId);

  Optional<ZoneTemplate> findZone(String zoneId);

  Optional<RoomTemplate> findRoom(String roomId);

  Optional<ShopTemplate> findShop(String shopId);

  Optional<ShopTemplate> findShopByRoomId(String roomId);

  Optional<ClassTemplate> findClass(String classId);

  Map<String, FormationTemplate> getAllFormations();

  Map<String, SkillTemplate> getAllSkills();

  Map<String, CompanionTemplate> getAllCompanions();

  default Map<String, MobTemplate> getAllMobs() {
    return Map.of();
  }

  default Map<String, ItemTemplate> getAllItems() {
    return Map.of();
  }

  default Map<String, RoomTemplate> getAllRooms() {
    return Map.of();
  }

  default Map<String, ShopTemplate> getAllShops() {
    return Map.of();
  }

  default Map<String, PartyMemberSkill> getAllPartySkills() {
    return Map.of();
  }

  default java.util.Optional<com.example.htmlmud.template.model.QuestTemplate> findQuestByTriggerMob(String mobId) {
    return java.util.Optional.empty();
  }

  default Map<String, com.example.htmlmud.template.model.QuestTemplate> getAllQuests() {
    return Map.of();
  }

  default java.util.Optional<com.example.htmlmud.party.GambitPreset> findGambitPreset(String presetId) {
    return java.util.Optional.empty();
  }

  default Map<String, com.example.htmlmud.party.GambitPreset> getAllGambitPresets() {
    return Map.of();
  }
}