package com.example.htmlmud.domain.factory;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.model.definition.ItemDefinition;
import com.example.htmlmud.domain.model.entity.ItemInstance;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.repository.TemplateReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 集中化道具工廠 (Canonical Item Factory)
 * 統一管理所有道具實體 (ItemInstance) 與槽位 (PartyItemSlot) 生成，廢除硬編碼猜測與重複解析。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ItemFactory {

  private final TemplateReader templateReader;

  /**
   * 取得指定道具的規範化模板定義
   */
  public Optional<ItemDefinition> findDefinition(String definitionId) {
    if (definitionId == null || definitionId.isBlank()) return Optional.empty();
    return templateReader.findItem(definitionId).map(ItemDefinition::fromTemplate);
  }

  /**
   * 建立純運行期道具實例 ItemInstance
   */
  public ItemInstance createInstance(String definitionId, int quantity) {
    ItemDefinition def = findDefinition(definitionId).orElse(null);
    int maxDurability = 100;
    if (def != null && def.extraProps() != null && def.extraProps().get("maxDurability") instanceof Number n) {
      maxDurability = n.intValue();
    }
    return ItemInstance.builder()
        .instanceId(UUID.randomUUID().toString())
        .definitionId(definitionId)
        .quantity(Math.max(1, quantity))
        .currentDurability(maxDurability)
        .maxDurability(maxDurability)
        .build();
  }

  public ItemInstance createInstance(String definitionId) {
    return createInstance(definitionId, 1);
  }

  /**
   * 建立供小隊背包使用的 PartyItemSlot (完全數據驅動，由 ItemDefinition 注入)
   */
  public PartyItemSlot createPartySlot(String templateId, int count) {
    var defOpt = findDefinition(templateId);
    if (defOpt.isPresent()) {
      return PartyItemSlot.fromDefinition(defOpt.get(), count, null);
    }
    log.warn("Item definition not found for: {}, using standard fallback item", templateId);
    return PartyItemSlot.createFallback(templateId, count);
  }

  /**
   * 由 ItemTemplate 建立 PartyItemSlot
   */
  public PartyItemSlot createPartySlot(ItemTemplate template, int count) {
    if (template == null) return null;
    ItemDefinition def = ItemDefinition.fromTemplate(template);
    return PartyItemSlot.fromDefinition(def, count, null);
  }

  /**
   * 建立狂亂異化遺物【道果道核】法寶
   */
  public PartyItemSlot createDaoCore(String memberName, String skillId, String skillName) {
    String slotId = "slot-" + UUID.randomUUID().toString().substring(0, 8);
    return PartyItemSlot.builder()
        .slotId(slotId)
        .itemId("dao_core_" + skillId)
        .name("凝煞道核·【" + memberName + "】")
        .icon("🔮")
        .itemType(ItemType.CONSUMABLE)
        .subType("SKILL_CORE")
        .count(1)
        .quality("EPIC")
        .description("自異化同伴殘軀中凝練而成的血肉道核，隱隱散發出瘋狂的道法共鳴。使用後可令其他同伴習得【" + skillName + "】。")
        .effectType("LEARN_SKILL")
        .grantedSkillId(skillId)
        .grantedSkillName(skillName)
        .build();
  }
}
