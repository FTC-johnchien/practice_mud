package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.config.MoveMessage;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.enums.SkillType;
import com.example.htmlmud.template.model.SkillTemplate;
import com.example.htmlmud.party.CombatResourceType;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyMemberSkill;
import com.example.htmlmud.party.UnifiedSkill;

/**
 * 專屬驗收測試：SP-06 技能模型合一 (UnifiedSkill Model)
 * 驗證修煉境界 (Level/XP/MaxXP)、動態數值縮放、敘事招式文本與主角/隊友大一統持有
 */
public class UnifiedSkillModelTest {

  @Test
  @DisplayName("SP-06-1: PartyMemberSkill 實作 UnifiedSkill，驗證熟練度積累、境界突破與動態數值縮放")
  void testUnifiedSkillProgressionAndScaling() {
    PartyMemberSkill skill = PartyMemberSkill.builder()
        .id("taiyin_sword")
        .name("太陰劍法")
        .level(1)
        .xp(0)
        .maxXp(100)
        .damageMultiplier(1.5)
        .cooldownMs(5000L)
        .healAmount(50)
        .sanRestore(20)
        .costType(CombatResourceType.MP)
        .costValue(20)
        .build();

    // 1. 初始境界驗證 (Level 1)
    assertThat(skill.getLevel()).isEqualTo(1);
    assertThat(skill.getEffectiveDamageMultiplier()).isEqualTo(1.50);
    assertThat(skill.getEffectiveCooldownMs()).isEqualTo(5000L);
    assertThat(skill.getEffectiveHealAmount()).isEqualTo(50);
    assertThat(skill.getEffectiveSanRestore()).isEqualTo(20);
    assertThat(skill.isMaxLevel()).isFalse();

    // 2. 獲取熟練度並突破至 Level 2
    skill.addXp(120); // 超過 100 門檻
    assertThat(skill.getLevel()).isEqualTo(2);
    assertThat(skill.getXp()).isEqualTo(20); // 餘額保留
    assertThat(skill.getMaxXp()).isGreaterThan(100L);

    // 3. 驗證動態縮放生效：傷害提升 5%，CD 縮減 100ms
    // 1.5 * (1 + 0.05) = 1.575 -> 1.58
    assertThat(skill.getEffectiveDamageMultiplier()).isEqualTo(1.58);
    assertThat(skill.getEffectiveCooldownMs()).isEqualTo(4900L);
    assertThat(skill.getEffectiveHealAmount()).isEqualTo((int) Math.round(50 * 1.08));

    // 4. 連續修煉直至圓滿境 (Level 10)
    for (int i = 0; i < 20; i++) {
      skill.addXp(5000);
    }
    assertThat(skill.getLevel()).isEqualTo(PartyMemberSkill.MAX_SKILL_LEVEL);
    assertThat(skill.isMaxLevel()).isTrue();
    assertThat(skill.getProficiencyProgress()).isEqualTo(1.0);
    // Level 10 增益：1.5 * (1 + 9 * 0.05) = 1.5 * 1.45 = 2.175 -> 2.18
    assertThat(skill.getEffectiveDamageMultiplier()).isEqualTo(2.18);
    assertThat(skill.getEffectiveCooldownMs()).isEqualTo(4100L); // 5000 - 900
  }

  @Test
  @DisplayName("SP-06-2: 驗證 SkillTemplate 轉為 UnifiedSkill 時招式敘事文本 (moves) 完整提取")
  void testUnifiedSkillNarrativeMoves() {
    SkillTemplate template = new SkillTemplate();
    template.setId("basic_sword");
    template.setName("基本劍法");
    template.setType(SkillType.WEAPON);
    template.setDescription("道門基礎入門劍訣。");

    MoveAction move1 = new MoveAction("白鶴亮翅", 1, 50, 1.2, 1.0, 1.0, null, 2000,
        new MoveMessage("你劍隨身走，一式「白鶴亮翅」點向敵身！", null, null, null, null), null);
    MoveAction move2 = new MoveAction("撥雲見日", 3, 50, 1.5, 1.0, 1.0, null, 2500,
        new MoveMessage("你氣沉丹田，長劍由下而上一招「撥雲見日」挑出！", null, null, null, null), null);

    template.setMoves(List.of(move1, move2));

    PartyMemberSkill unified = PartyMemberSkill.fromSkillTemplate(template);
    assertThat(unified).isNotNull();
    assertThat(unified.getMoves())
        .as("招式名稱敘事文本應完整保留")
        .containsExactly("白鶴亮翅", "撥雲見日");
  }

  @Test
  @DisplayName("SP-06-3: 驗證 SkillEntry 與 UnifiedSkill 雙向無縫互轉")
  void testSkillEntryAndPartyMemberSkillBidirectionalBridge() {
    SkillEntry entry = new SkillEntry("cloud_step", 5);
    entry.setXp(350);
    entry.setMaxXp(500);

    // 1. 轉為 UnifiedSkill
    PartyMemberSkill unified = entry.toUnifiedSkill(null);
    assertThat(unified.getId()).isEqualTo("cloud_step");
    assertThat(unified.getLevel()).isEqualTo(5);
    assertThat(unified.getXp()).isEqualTo(350);
    assertThat(unified.getMaxXp()).isEqualTo(500);

    // 2. 升級後反向同步為 SkillEntry
    unified.levelUp();
    SkillEntry syncedEntry = SkillEntry.fromUnifiedSkill(unified);
    assertThat(syncedEntry.getSkillId()).isEqualTo("cloud_step");
    assertThat(syncedEntry.getLevel()).isEqualTo(6);
  }

  @Test
  @DisplayName("SP-06-4: 驗證 PartyMember 小隊成員統一持有 UnifiedSkill，並具備境界屬性")
  void testPartyMemberUnifiedSkillHolding() {
    PartyMemberSkill fireSkill = PartyMemberSkill.builder()
        .id("mage_fireball")
        .name("三昧真火")
        .level(3)
        .xp(50)
        .maxXp(200)
        .damageMultiplier(2.0)
        .build();

    PartyMember companion = PartyMember.builder()
        .id("c-ling-shuang")
        .name("凌霜")
        .skills(List.of(fireSkill))
        .build();

    UnifiedSkill memberSkill = companion.getSkills().get(0);
    assertThat(memberSkill).isNotNull();
    assertThat(memberSkill.getName()).isEqualTo("三昧真火");
    assertThat(memberSkill.getLevel()).isEqualTo(3);
    // 2.0 * (1 + 2 * 0.05) = 2.0 * 1.1 = 2.2
    assertThat(memberSkill.getEffectiveDamageMultiplier()).isEqualTo(2.2);
  }
}
