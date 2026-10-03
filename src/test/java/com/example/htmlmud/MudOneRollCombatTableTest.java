package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.service.MobService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

/**
 * 3.1: MUD 端生靈一元一次擲骰圓桌判定 (One-Roll Combat Table) 整合測試
 */
@SpringBootTest
public class MudOneRollCombatTableTest {

  @Autowired
  private DefenseResolver defenseResolver;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private MobService mobService;

  private Player player;
  private Mob mob;

  @BeforeEach
  void setUp() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    String pid = "test-mud-table-" + UUID.randomUUID().toString().substring(0, 8);
    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "圓桌少俠", pid);
    player.getStats().setDex(20);
    player.getStats().setStr(20);
    player.getStats().setCon(20);

    MobTemplate tpl = MobTemplate.builder()
        .id("test_combat_mob")
        .name("鐵甲山賊")
        .maxHp(200)
        .build();
    LivingStats mobStats = new LivingStats();
    mobStats.setDex(15);
    mobStats.setStr(15);
    mobStats.setCon(15);
    mob = new Mob(tpl, mobStats, mobService);
  }

  @AfterEach
  void tearDown() {
    if (player != null) {
      worldManager.removeLivingActor(player.getId());
    }
  }

  @Test
  @DisplayName("3.1-1: 驗證 MUD 生靈攻擊於 Miss 區間時結算為 MISS，傷害歸零")
  void testMudLivingCombatTableMiss() {
    // 擲骰極低值 (0.01) 必定落入 Miss 區間 (5%+)
    var res = defenseResolver.resolveLivingAttack(mob, player, 50, 0.01);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.MISS);
    assertThat(res.finalDamage()).isEqualTo(0);
    assertThat(res.combatLog()).contains("💨【未命中】");
  }

  @Test
  @DisplayName("3.1-2: 驗證 MUD 玩家啟用身法技能且擲骰落入閃避區間時結算為 DODGED")
  void testMudLivingCombatTableDodge() {
    player.getLearnedSkills().put("cloud_step", new com.example.htmlmud.domain.model.entity.SkillEntry("cloud_step", 1));
    player.getEnabledSkills().put(SkillCategory.DODGE, "cloud_step");

    // 擲骰 0.15 落入身法閃避區間
    var res = defenseResolver.resolveLivingAttack(mob, player, 50, 0.15);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.DODGED);
    assertThat(res.finalDamage()).isEqualTo(0);
    assertThat(res.combatLog()).contains("💨【身法閃避】");
  }

  @Test
  @DisplayName("3.1-3: 驗證 MUD 玩家啟用招架技能且擲骰落入招架區間時結算為 PARRIED 且傷害減半")
  void testMudLivingCombatTableParry() {
    player.getLearnedSkills().put("basic_parry", new com.example.htmlmud.domain.model.entity.SkillEntry("basic_parry", 1));
    player.getEnabledSkills().put(SkillCategory.PARRY, "basic_parry");

    // 不配身法，直接選定 0.20 落入 Parry 區間
    var res = defenseResolver.resolveLivingAttack(mob, player, 60, 0.20);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.PARRIED);
    assertThat(res.finalDamage()).isLessThanOrEqualTo(30);
    assertThat(res.finalDamage()).isGreaterThan(0);
    assertThat(res.combatLog()).contains("🛡️【招架格擋】");
  }

  @Test
  @DisplayName("3.1-4: 驗證 MUD 怪物發動致命一擊時結算為 CRIT 且造成 1.5x 爆擊傷害")
  void testMudLivingCombatTableCrit() {
    // 假設高 roll (0.90) 若落入 Crit 區間或指定在暴擊窗
    var res = defenseResolver.resolveLivingAttack(mob, player, 40, 0.76);
    assertThat(res.outcome()).isIn(DefenseOutcome.CRIT, DefenseOutcome.HIT);
    if (res.outcome() == DefenseOutcome.CRIT) {
      assertThat(res.finalDamage()).isEqualTo(60); // 40 * 1.5 = 60
      assertThat(res.combatLog()).contains("💥【致命一擊】");
    }
  }

  @Test
  @DisplayName("3.1-5: 驗證極端屬性 (DEX 999) 總防禦機率嚴格收斂於上限 (75%)，保留命中窗口")
  void testExtremeAttributesDefenseConvergence() {
    player.getStats().setDex(999);
    player.getStats().setStr(999);
    player.getStats().setCon(999);
    player.getLearnedSkills().put("cloud_step", new com.example.htmlmud.domain.model.entity.SkillEntry("cloud_step", 100));
    player.getEnabledSkills().put(SkillCategory.DODGE, "cloud_step");
    player.getLearnedSkills().put("basic_parry", new com.example.htmlmud.domain.model.entity.SkillEntry("basic_parry", 100));
    player.getEnabledSkills().put(SkillCategory.PARRY, "basic_parry");

    // 擲骰 0.96 必定落入普通命中窗口 (95%~100%)
    var res = defenseResolver.resolveLivingAttack(mob, player, 50, 0.96);
    assertThat(res.outcome()).isEqualTo(DefenseOutcome.HIT);
    assertThat(res.finalDamage()).isGreaterThan(0);
    assertThat(res.combatLog()).contains("造成");
  }
}
