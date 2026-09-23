package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.htmlmud.application.command.impl.PartyCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.model.TacticsCondition;
import com.example.htmlmud.domain.party.model.TacticsRule;
import com.example.htmlmud.domain.party.model.TacticsTarget;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;

class CustomCompanionTacticsTest {

  private PartyService partyService;
  private DungeonManager dungeonManager;
  private DrpgBattleService battleService;
  private GameStateBroadcastService broadcastService;
  private PartyCommand partyCommand;
  private Player dummyPlayer;
  private DungeonPosition dungeonPos;
  private Method tacticsMethod;

  @BeforeEach
  void setUp() throws Exception {
    dungeonManager = new DungeonManager();
    dungeonManager.init();

    partyService = new PartyService();
    partyService.initDefaultFormations();

    battleService = new DrpgBattleService(partyService, dungeonManager, new DungeonNavigator());
    broadcastService = mock(GameStateBroadcastService.class);
    partyCommand = new PartyCommand(partyService, dungeonManager, new DungeonNavigator(), battleService, broadcastService);

    dummyPlayer = mock(Player.class);
    when(dummyPlayer.getName()).thenReturn("tester");
    when(dummyPlayer.isValid()).thenReturn(true);
    when(dummyPlayer.getCurrentRoomId()).thenReturn("mozhu_mines:entrance");

    dungeonPos = new DungeonPosition("mozhu_mines_b1f", 3, 3, Direction.NORTH, 10, 10);
    dungeonManager.setPlayerPosition("tester", dungeonPos);

    tacticsMethod = DrpgBattleService.class.getDeclaredMethod(
        "tryTriggerCompanionTactics", Player.class, BattleContext.class, PartyMember.class, DungeonPosition.class);
    tacticsMethod.setAccessible(true);
  }

  @Test
  @DisplayName("測試雙補師戰術差異化：Healer A 在 60% 觸發小補，Healer B 在 35% 觸發大補")
  void testDualHealerDifferentThresholds() throws Exception {
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll"));
    BattleContext ctx = battleService.getBattle("tester");
    Party party = ctx.getParty();

    PartyMember iron = party.getMembers().get(1); // 鐵牛
    PartyMember ling = party.getMembers().get(2); // 凌霜 (Healer A)

    // 新增第二位治療者 (Healer B)
    LivingStats healerStats = new LivingStats();
    healerStats.setHp(100);
    healerStats.setMaxHp(100);
    healerStats.setMp(100);
    healerStats.setMaxMp(100);

    PartyMember healerB = PartyMember.builder()
        .id("miao_yin")
        .name("妙音仙子")
        .roleTitle("靈醫")
        .alive(true)
        .row(RowPosition.BACK)
        .stats(healerStats)
        .skills(new ArrayList<>())
        .tactics(new ArrayList<>())
        .build();
    party.getMembers().add(healerB);

    // 設定 Healer A (凌霜) 技能與方針：隊友 < 60% 施展 small_heal
    PartyMemberSkill smallHeal = PartyMemberSkill.builder()
        .id("small_heal")
        .name("春風化雨")
        .heal(true)
        .healAmount(30)
        .costType(CombatResourceType.MP)
        .costValue(15)
        .cooldownMs(3000)
        .build();
    ling.getSkills().clear();
    ling.getSkills().add(smallHeal);
    ling.getStats().setMp(100);
    ling.clearTactics();
    ling.addTacticsRule(TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ALLY_HP_LESS_THAN)
        .conditionValue(60)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .skillId("small_heal")
        .enabled(true)
        .build());

    // 設定 Healer B (妙音) 技能與方針：隊友 < 35% 施展 major_heal
    PartyMemberSkill majorHeal = PartyMemberSkill.builder()
        .id("major_heal")
        .name("起死回生")
        .heal(true)
        .healAmount(100)
        .costType(CombatResourceType.MP)
        .costValue(40)
        .cooldownMs(8000)
        .build();
    healerB.getSkills().add(majorHeal);
    healerB.clearTactics();
    healerB.addTacticsRule(TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ALLY_HP_LESS_THAN)
        .conditionValue(35)
        .target(TacticsTarget.LOWEST_HP_ALLY)
        .skillId("major_heal")
        .enabled(true)
        .build());

    // 階段 1：鐵牛血量降至 50% (<= 60%, 但 > 35%)
    iron.getStats().setMaxHp(200);
    iron.getStats().setHp(100);

    // Healer B 評估戰術：不應觸發 (50% > 35%)
    boolean bTriggered1 = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, healerB, dungeonPos);
    assertThat(bTriggered1).isFalse();
    assertThat(iron.getStats().getHp()).isEqualTo(100);

    // Healer A 評估戰術：應成功觸發 (50% <= 60%)
    boolean aTriggered1 = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, ling, dungeonPos);
    assertThat(aTriggered1).isTrue();
    assertThat(iron.getStats().getHp()).isEqualTo(130);
    assertThat(ling.isOnCooldown("small_heal")).isTrue();

    // 階段 2：鐵牛血量大降至 20% (<= 35%)
    iron.getStats().setHp(40);

    // Healer B 評估戰術：應成功觸發大補！
    boolean bTriggered2 = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, healerB, dungeonPos);
    assertThat(bTriggered2).isTrue();
    assertThat(iron.getStats().getHp()).isEqualTo(140);
    assertThat(healerB.isOnCooldown("major_heal")).isTrue();
  }

  @Test
  @DisplayName("測試雙坦戰術差異化：Tank 1 群嘲（怪物>=2），Tank 2 單嘲（遭遇首領）")
  void testDualTankDifferentConditions() throws Exception {
    // 召喚 2 隻普通怪物
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll", "taiyin_tomb:bone_bat"));
    BattleContext ctx = battleService.getBattle("tester");
    Party party = ctx.getParty();

    PartyMember tank1 = party.getMembers().get(1); // 鐵牛

    // 新增第二位坦克 Tank 2
    LivingStats tankStats = new LivingStats();
    tankStats.setHp(300);
    tankStats.setMaxHp(300);
    tankStats.setMp(50);
    tankStats.setMaxMp(50);

    PartyMember tank2 = PartyMember.builder()
        .id("yan_ba")
        .name("燕霸天")
        .roleTitle("金甲衛")
        .alive(true)
        .row(RowPosition.FRONT)
        .stats(tankStats)
        .currentRage(50)
        .maxRage(100)
        .resourceType(CombatResourceType.RAGE)
        .skills(new ArrayList<>())
        .tactics(new ArrayList<>())
        .build();
    party.getMembers().add(tank2);

    PartyMemberSkill groupTaunt = PartyMemberSkill.builder()
        .id("group_taunt")
        .name("獅子吼")
        .taunt(true)
        .costType(CombatResourceType.RAGE)
        .costValue(10)
        .cooldownMs(5000)
        .build();
    tank1.getSkills().clear();
    tank1.getSkills().add(groupTaunt);
    tank1.setCurrentRage(30);
    tank1.clearTactics();
    tank1.addTacticsRule(TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ENEMY_COUNT_GTE)
        .conditionValue(2)
        .target(TacticsTarget.ALL_ENEMIES)
        .skillId("group_taunt")
        .enabled(true)
        .build());

    PartyMemberSkill bossTaunt = PartyMemberSkill.builder()
        .id("boss_taunt")
        .name("鎮岳斷喝")
        .taunt(true)
        .costType(CombatResourceType.RAGE)
        .costValue(20)
        .cooldownMs(6000)
        .build();
    tank2.getSkills().add(bossTaunt);
    tank2.clearTactics();
    tank2.addTacticsRule(TacticsRule.builder()
        .priority(1)
        .condition(TacticsCondition.ENEMY_IS_BOSS)
        .conditionValue(0)
        .target(TacticsTarget.CURRENT_ENEMY)
        .skillId("boss_taunt")
        .enabled(true)
        .build());

    // 情境 A：當前有 2 隻普通怪物 (無首領)
    // Tank 1 條件符合 (怪物數量 >= 2)，應觸發
    boolean t1Triggered = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, tank1, dungeonPos);
    assertThat(t1Triggered).isTrue();

    // Tank 2 條件不符 (無首領)，不觸發
    boolean t2Triggered = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, tank2, dungeonPos);
    assertThat(t2Triggered).isFalse();

    // 情境 B：換成 1 隻 Boss 怪物
    ctx.getEnemies().clear();
    ctx.getEnemies().add(BattleEnemy.builder()
        .id("demon_lord")
        .name("九幽魔君")
        .templateId("taiyin_tomb:boss_demon")
        .hp(800)
        .maxHp(800)
        .alive(true)
        .build());
    tank1.resetCooldowns();
    tank2.resetCooldowns();

    // Tank 1 條件不符 (怪物數量只有 1 < 2)，不觸發
    boolean t1TriggeredBoss = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, tank1, dungeonPos);
    assertThat(t1TriggeredBoss).isFalse();

    // Tank 2 條件符合 (遭遇首領)，成功觸發嘲諷！
    boolean t2TriggeredBoss = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, tank2, dungeonPos);
    assertThat(t2TriggeredBoss).isTrue();
    assertThat(tank2.isOnCooldown("boss_taunt")).isTrue();
  }

  @Test
  @DisplayName("測試 PartyCommand 的 party tactics 指令完整互動流 (清單/清空/新增/重置)")
  void testPartyTacticsCommandExecution() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember iron = party.getMembers().get(1);

    // 1. 檢視戰術清單 (party tactics 1)
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1");
    });
    ArgumentCaptor<String> replyCaptor = ArgumentCaptor.forClass(String.class);
    verify(dummyPlayer, atLeastOnce()).reply(replyCaptor.capture());
    assertThat(replyCaptor.getValue()).contains("戰術方針設定").contains("鐵牛");

    // 2. 清空戰術 (party tactics 1 clear)
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1 clear");
    });
    assertThat(iron.getTactics()).isEmpty();

    // 3. 新增自訂戰術 (party tactics 1 add 1 ALLY_HP_LESS_THAN 60 LOWEST_HP_ALLY divine_healing)
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1 add 1 ALLY_HP_LESS_THAN 60 LOWEST_HP_ALLY divine_healing");
    });
    assertThat(iron.getTactics()).hasSize(1);
    TacticsRule rule = iron.getTactics().get(0);
    assertThat(rule.getPriority()).isEqualTo(1);
    assertThat(rule.getCondition()).isEqualTo(TacticsCondition.ALLY_HP_LESS_THAN);
    assertThat(rule.getConditionValue()).isEqualTo(60);
    assertThat(rule.getTarget()).isEqualTo(TacticsTarget.LOWEST_HP_ALLY);
    assertThat(rule.getSkillId()).isEqualTo("divine_healing");

    // 4. 重置為預設 (party tactics 1 reset)
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1 reset");
    });
    assertThat(iron.getTactics()).isNotEmpty();
  }

  @Test
  @DisplayName("測試 PartyCommand 的 party tactics toggle 與 delete 指令")
  void testPartyTacticsToggleAndDeleteCommand() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember iron = party.getMembers().get(1);

    iron.resetTactics();
    assertThat(iron.getTactics()).isNotEmpty();
    int firstPrio = iron.getTactics().get(0).getPriority();
    boolean initialEnabled = iron.getTactics().get(0).isEnabled();

    // 測試 toggle 切換啟用/停用
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1 toggle " + firstPrio);
    });
    assertThat(iron.getTactics().get(0).isEnabled()).isEqualTo(!initialEnabled);

    // 測試 delete 刪除方針
    int initialCount = iron.getTactics().size();
    java.lang.ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      partyCommand.execute("tactics 1 delete " + firstPrio);
    });
    assertThat(iron.getTactics()).hasSize(initialCount - 1);
  }
}
