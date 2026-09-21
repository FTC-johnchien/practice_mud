package com.example.htmlmud.domain.service;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.repository.TemplateReader;

/**
 * 負責 MUD 運行時實體與靜態模板之間的轉換與適配。
 */
@Service
public class MudTemplateAdapter {

  private final TemplateReader templateReader;

  @Autowired
  public MudTemplateAdapter(TemplateReader templateReader) {
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
  }

  public MudTemplateAdapter() {
    this(new TemplateCatalog());
  }

  public Optional<ItemTemplate> findItem(String itemId) {
    return templateReader.findItem(itemId);
  }

  public Optional<SkillTemplate> findSkill(String skillId) {
    return templateReader.findSkill(skillId);
  }

  /**
   * 根據物品模板 ID 實例化為 MUD 的 GameItem 實體
   */
  public Optional<GameItem> instantiateGameItem(String itemId, int amount) {
    return templateReader.findItem(itemId).map(tpl -> {
      PartyItemSlot slot = PartyItemSlot.fromItemTemplate(tpl, Math.max(1, amount));
      GameItem item = slot.toGameItem();
      item.setId(UUID.randomUUID().toString());
      return item;
    });
  }

  public Optional<GameItem> instantiateGameItem(String itemId) {
    return instantiateGameItem(itemId, 1);
  }
}