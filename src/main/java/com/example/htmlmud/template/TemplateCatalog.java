package com.example.htmlmud.template;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.template.model.ClassTemplate;
import com.example.htmlmud.template.model.CompanionTemplate;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.MobTemplate;
import com.example.htmlmud.template.model.RaceTemplate;
import com.example.htmlmud.template.model.RoomTemplate;
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.template.model.ZoneTemplate;
import com.example.htmlmud.party.FormationTemplate;
import com.example.htmlmud.party.PartyMemberSkill;

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
      Class<?> clazz = Class.forName("com.example.htmlmud.template.TemplateRepository");
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

  @Override
  public Map<String, MobTemplate> getAllMobs() {
    return templateReader != null ? templateReader.getAllMobs() : Collections.emptyMap();
  }

  @Override
  public Map<String, ItemTemplate> getAllItems() {
    return templateReader != null ? templateReader.getAllItems() : Collections.emptyMap();
  }

  @Override
  public Map<String, RoomTemplate> getAllRooms() {
    return templateReader != null ? templateReader.getAllRooms() : Collections.emptyMap();
  }

  @Override
  public Map<String, ShopTemplate> getAllShops() {
    return templateReader != null ? templateReader.getAllShops() : Collections.emptyMap();
  }

  @Override
  public Map<String, PartyMemberSkill> getAllPartySkills() {
    return templateReader != null ? templateReader.getAllPartySkills() : Collections.emptyMap();
  }

  @Override
  public String resolveSkillAlias(String skillId) {
    return templateReader != null ? templateReader.resolveSkillAlias(skillId) : null;
  }

  @Override
  public Optional<com.example.htmlmud.party.GambitPreset> findGambitPreset(String presetId) {
    return templateReader != null ? templateReader.findGambitPreset(presetId) : Optional.empty();
  }

  @Override
  public Map<String, com.example.htmlmud.party.GambitPreset> getAllGambitPresets() {
    return templateReader != null ? templateReader.getAllGambitPresets() : Collections.emptyMap();
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
