package com.example.htmlmud.domain.service;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.ClassTemplate;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.ShopTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.repository.TemplateReader;

@Primary
@Service
public class TemplateCatalog implements TemplateReader {

  private static volatile Supplier<TemplateReader> defaultReaderSupplier;

  public static void setDefaultReaderSupplier(Supplier<TemplateReader> supplier) {
    defaultReaderSupplier = supplier;
  }

  private static TemplateReader createDefaultReader() {
    if (defaultReaderSupplier != null) {
      return defaultReaderSupplier.get();
    }
    try {
      Class<?> clazz = Class.forName("com.example.htmlmud.infra.persistence.repository.TemplateRepository");
      return (TemplateReader) clazz.getDeclaredConstructor().newInstance();
    } catch (Exception e) {
      return null;
    }
  }

  private final TemplateReader templateReader;

  @org.springframework.beans.factory.annotation.Autowired
  public TemplateCatalog(@org.springframework.beans.factory.annotation.Qualifier("templateRepository") TemplateReader templateReader) {
    this.templateReader = templateReader != null ? templateReader : createDefaultReader();
  }

  public TemplateCatalog() {
    this(createDefaultReader());
  }

  @Override
  public Optional<ItemTemplate> findItem(String itemId) {
    if (templateReader == null) return Optional.empty();
    return normalize(itemId).flatMap(templateReader::findItem);
  }

  public ItemTemplate resolveItem(String itemId) {
    return findItem(itemId).orElse(null);
  }

  @Override
  public Optional<SkillTemplate> findSkill(String skillId) {
    if (templateReader == null) return Optional.empty();
    return normalize(skillId).flatMap(templateReader::findSkill);
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
    return templateReader != null ? templateReader.findDefaultSkill(category) : null;
  }

  @Override
  public String findDefaultSkillId(SkillCategory category) {
    return templateReader != null ? templateReader.findDefaultSkillId(category) : null;
  }

  @Override
  public Optional<RaceTemplate> findRace(String raceId) {
    return templateReader != null ? templateReader.findRace(raceId) : Optional.empty();
  }

  @Override
  public Optional<MobTemplate> findMob(String mobId) {
    return templateReader != null ? templateReader.findMob(mobId) : Optional.empty();
  }

  @Override
  public Optional<CompanionTemplate> findCompanion(String companionId) {
    return templateReader != null ? templateReader.findCompanion(companionId) : Optional.empty();
  }

  @Override
  public Optional<PartyMemberSkill> findPartySkill(String skillId) {
    return templateReader != null ? templateReader.findPartySkill(skillId) : Optional.empty();
  }

  @Override
  public Optional<FormationTemplate> findFormation(String formationId) {
    return templateReader != null ? templateReader.findFormation(formationId) : Optional.empty();
  }

  @Override
  public Optional<ZoneTemplate> findZone(String zoneId) {
    return templateReader != null ? templateReader.findZone(zoneId) : Optional.empty();
  }

  @Override
  public Optional<RoomTemplate> findRoom(String roomId) {
    return templateReader != null ? templateReader.findRoom(roomId) : Optional.empty();
  }

  @Override
  public Optional<ShopTemplate> findShop(String shopId) {
    return templateReader != null ? templateReader.findShop(shopId) : Optional.empty();
  }

  @Override
  public Optional<ShopTemplate> findShopByRoomId(String roomId) {
    return templateReader != null ? templateReader.findShopByRoomId(roomId) : Optional.empty();
  }

  @Override
  public Optional<ClassTemplate> findClass(String classId) {
    return templateReader != null ? templateReader.findClass(classId) : Optional.empty();
  }

  @Override
  public Map<String, FormationTemplate> getAllFormations() {
    return templateReader != null ? templateReader.getAllFormations() : Collections.emptyMap();
  }

  @Override
  public Map<String, SkillTemplate> getAllSkills() {
    return templateReader != null ? templateReader.getAllSkills() : Collections.emptyMap();
  }

  @Override
  public Map<String, CompanionTemplate> getAllCompanions() {
    return templateReader != null ? templateReader.getAllCompanions() : Collections.emptyMap();
  }

  private Optional<String> normalize(String rawId) {
    if (rawId == null) {
      return Optional.empty();
    }

    String trimmed = rawId.trim();
    if (trimmed.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(trimmed.toLowerCase());
  }
}