package com.example.htmlmud.domain.repository;

import java.util.Optional;
import java.util.Map;

import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;

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

  Optional<FormationTemplate> findFormation(String formationId);

  Optional<ZoneTemplate> findZone(String zoneId);

  Optional<RoomTemplate> findRoom(String roomId);

  Optional<ShopTemplate> findShop(String shopId);

  Optional<ShopTemplate> findShopByRoomId(String roomId);

  Optional<ClassTemplate> findClass(String classId);

  Map<String, FormationTemplate> getAllFormations();

  Map<String, SkillTemplate> getAllSkills();

  Map<String, CompanionTemplate> getAllCompanions();
}