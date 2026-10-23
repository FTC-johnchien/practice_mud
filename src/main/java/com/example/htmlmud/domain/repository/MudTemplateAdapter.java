package com.example.htmlmud.domain.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.htmlmud.template.TemplateReader;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.party.PartyItemSlot;

/**
 * 負責 MUD 運行時實體與靜態模板之間的轉換與適配。
 */
@Service
public class MudTemplateAdapter {

  private final TemplateReader templateReader;

  @Autowired
  public MudTemplateAdapter(TemplateReader templateReader) {
    this.templateReader = templateReader;
  }

  public MudTemplateAdapter() {
    this((TemplateReader) null);
  }

  public Optional<ItemTemplate> findItem(String itemId) {
    return templateReader != null ? templateReader.findItem(itemId) : Optional.empty();
  }

  public Optional<SkillTemplate> findSkill(String skillId) {
    return templateReader != null ? templateReader.findSkill(skillId) : Optional.empty();
  }

  /**
   * 根據物品模板 ID 實例化為 MUD 的 GameItem 實體
   */
  public Optional<GameItem> instantiateGameItem(String itemId, int amount) {
    return templateReader.findItem(itemId).map(tpl -> {
      PartyItemSlot slot = PartyItemSlot.fromItemTemplate(tpl, Math.max(1, amount));
      GameItem item = slot.toGameItem(templateReader);
      item.setId(UUID.randomUUID().toString());
      return item;
    });
  }

  public Optional<GameItem> instantiateGameItem(String itemId) {
    return instantiateGameItem(itemId, 1);
  }
}
