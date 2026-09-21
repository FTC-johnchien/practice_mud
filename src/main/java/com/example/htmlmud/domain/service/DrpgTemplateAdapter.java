package com.example.htmlmud.domain.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.repository.TemplateReader;

/**
 * 負責 DRPG 運行時實體（PartyItemSlot, BattleEnemy, PartyMemberSkill）與靜態模板之間的轉換與適配。
 */
@Service
public class DrpgTemplateAdapter {

  private final TemplateReader templateReader;

  @Autowired
  public DrpgTemplateAdapter(TemplateReader templateReader) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
  }

  public DrpgTemplateAdapter() {
    this(new TemplateCatalog());
  }

  public Optional<ItemTemplate> findItem(String itemId) {
    return templateReader.findItem(itemId);
  }

  public Optional<SkillTemplate> findSkill(String skillId) {
    return templateReader.findSkill(skillId);
  }

  public Optional<PartyMemberSkill> findPartySkill(String skillId) {
    return templateReader.findPartySkill(skillId);
  }

  public Optional<MobTemplate> findMob(String mobTemplateId) {
    return templateReader.findMob(mobTemplateId);
  }

  /**
   * 根據物品模板 ID 實例化為 DRPG 的 PartyItemSlot 隊伍行囊插槽
   */
  public Optional<PartyItemSlot> instantiatePartySlot(String itemId, int count) {
    return templateReader.findItem(itemId).map(tpl -> PartyItemSlot.fromItemTemplate(tpl, Math.max(1, count)));
  }

  public Optional<PartyItemSlot> instantiatePartySlot(String itemId) {
    return instantiatePartySlot(itemId, 1);
  }

  /**
   * 根據怪物模板 ID 實例化為 DRPG 的 BattleEnemy 戰鬥怪物實體
   */
  public Optional<BattleEnemy> instantiateBattleEnemy(String mobTemplateId, String instanceId, RowPosition row, String dropId) {
    return templateReader.findMob(mobTemplateId).map(tpl -> {
      String id = (instanceId != null && !instanceId.isBlank()) ? instanceId : "mob-" + UUID.randomUUID().toString().substring(0, 8);
      RowPosition r = row != null ? row : RowPosition.FRONT;
      return BattleEnemy.fromTemplate(id, tpl, r, dropId, templateReader);
    });
  }
}