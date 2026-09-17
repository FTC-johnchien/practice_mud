package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleEnemy {
  private String id;
  private String templateId;
  private String name;
  private int hp;
  private int maxHp;
  @Builder.Default
  private int minDamage = 12;
  @Builder.Default
  private int maxDamage = 20;
  @Builder.Default
  private int defense = 5;
  @Builder.Default
  private int dex = 10;
  @Builder.Default
  private RowPosition row = RowPosition.FRONT;
  @Builder.Default
  private long attackIntervalMs = 2200;
  @Builder.Default
  private long nextAttackTime = 0;
  @Builder.Default
  private long stunnedUntil = 0;
  @Builder.Default
  private boolean alive = true;
  @Builder.Default
  private int xp = 30;
  private String dropItemId;
  private String droppedDaoSkillId;
  private String droppedDaoSkillName;
  private String droppedDaoMemberName;

  @Builder.Default
  private Map<EquipmentSlot, String> equipment = new HashMap<>();
  @Builder.Default
  private List<String> skills = new ArrayList<>();

  public static BattleEnemy fromTemplate(String id, MobTemplate tpl, RowPosition row, String dropId) {
    if (tpl == null) return null;
    int hp = tpl.maxHp() > 0 ? tpl.maxHp() : 100;
    int minD = tpl.minDamage() > 0 ? tpl.minDamage() : 10;
    int maxD = tpl.maxDamage() > 0 ? tpl.maxDamage() : 18;
    int def = tpl.defense();
    int dex = tpl.dex() > 0 ? tpl.dex() : 10;
    long attackInterval = tpl.attackSpeed() > 0 ? tpl.attackSpeed() : 2200;

    Map<EquipmentSlot, String> equipMap = new HashMap<>();
    if (tpl.equipment() != null) {
      for (var entry : tpl.equipment().entrySet()) {
        try {
          EquipmentSlot slot = EquipmentSlot.valueOf(entry.getKey().toUpperCase());
          equipMap.put(slot, entry.getValue());
          var itOpt = TemplateRepository.findItem(entry.getValue());
          if (itOpt.isPresent()) {
            var it = itOpt.get();
            if (it.equipmentProp() != null) {
              minD += it.equipmentProp().minDamage();
              maxD += it.equipmentProp().maxDamage();
              def += it.equipmentProp().defense();
            }
          }
        } catch (Exception ignored) {}
      }
    }

    List<String> mobSkills = new ArrayList<>();
    if (tpl.enabledSkills() != null) {
      mobSkills.addAll(tpl.enabledSkills().values());
    }

    return BattleEnemy.builder()
        .id(id)
        .templateId(tpl.id())
        .name(tpl.name())
        .hp(hp)
        .maxHp(hp)
        .minDamage(minD)
        .maxDamage(maxD)
        .defense(def)
        .dex(dex)
        .row(row != null ? row : RowPosition.FRONT)
        .attackIntervalMs(attackInterval)
        .equipment(equipMap)
        .skills(mobSkills)
        .dropItemId(dropId)
        .alive(true)
        .xp(tpl.expReward() > 0 ? tpl.expReward() : 30)
        .build();
  }

  public void takeDamage(int dmg) {
    this.hp = Math.max(0, this.hp - dmg);
    if (this.hp <= 0) {
      this.alive = false;
    }
  }

  public boolean isStunned() {
    return stunnedUntil > System.currentTimeMillis();
  }

  public void applyStun(long durationMs) {
    this.stunnedUntil = Math.max(this.stunnedUntil, System.currentTimeMillis() + durationMs);
  }
}
