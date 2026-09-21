package com.example.htmlmud.domain.service;

import java.util.Optional;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@Service
public class TemplateCatalog implements TemplateReader {

  private final TemplateRepository templateRepository;

  @org.springframework.beans.factory.annotation.Autowired
  public TemplateCatalog(TemplateRepository templateRepository) {
    this.templateRepository = templateRepository != null ? templateRepository : TemplateRepository.getInstance();
  }

  public TemplateCatalog() {
    this(TemplateRepository.getInstance());
  }

  @Override
  public Optional<ItemTemplate> findItem(String itemId) {
    return normalize(itemId).flatMap(templateRepository::getItem);
  }

  public ItemTemplate resolveItem(String itemId) {
    return findItem(itemId).orElse(null);
  }

  @Override
  public Optional<SkillTemplate> findSkill(String skillId) {
    return normalize(skillId).flatMap(templateRepository::getSkillTemplate);
  }

  public SkillTemplate resolveSkill(String skillId) {
    return findSkill(skillId).orElse(null);
  }

  @Override
  public SkillTemplate requireSkill(String skillId) {
    return findSkill(skillId)
        .orElseThrow(() -> new MudException("Skill not found id:" + skillId));
  }

  @Override
  public SkillTemplate findDefaultSkill(SkillCategory category) {
    return templateRepository.getDefaultSkillByCategory(category);
  }

  @Override
  public String findDefaultSkillId(SkillCategory category) {
    return templateRepository.getDefaultSkillIdByCategory(category);
  }

  @Override
  public Optional<RaceTemplate> findRace(String raceId) {
    return templateRepository.getRace(raceId);
  }

  @Override
  public Optional<MobTemplate> findMob(String mobId) {
    return templateRepository.getMob(mobId);
  }

  @Override
  public Optional<CompanionTemplate> findCompanion(String companionId) {
    return templateRepository.getCompanion(companionId);
  }

  @Override
  public Optional<PartyMemberSkill> findPartySkill(String skillId) {
    return templateRepository.getPartySkill(skillId);
  }

  @Override
  public Optional<FormationTemplate> findFormation(String formationId) {
    return templateRepository.getFormation(formationId);
  }

  @Override
  public Optional<ZoneTemplate> findZone(String zoneId) {
    return templateRepository.getZone(zoneId);
  }

  @Override
  public Optional<RoomTemplate> findRoom(String roomId) {
    return templateRepository.getRoom(roomId);
  }

  @Override
  public Optional<ShopTemplate> findShop(String shopId) {
    return templateRepository.getShop(shopId);
  }

  @Override
  public Optional<ShopTemplate> findShopByRoomId(String roomId) {
    return templateRepository.getShopByRoom(roomId);
  }

  @Override
  public Optional<ClassTemplate> findClass(String classId) {
    return templateRepository.getClassTemplate(classId);
  }

  @Override
  public Map<String, FormationTemplate> getAllFormations() {
    return templateRepository.getAllFormationMap();
  }

  private Optional<String> normalize(String rawId) {
    if (rawId == null) {
      return Optional.empty();
    }

    String trimmed = rawId.trim();
    if (trimmed.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(trimmed);
  }
}