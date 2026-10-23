package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.KillCommand;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.combat.CombatPresentation;
import com.example.htmlmud.domain.combat.CombatRules;
import com.example.htmlmud.domain.combat.Combatant;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseResolution;
import com.example.htmlmud.domain.dungeon.battle.event.BattleEvent;
import com.example.htmlmud.domain.dungeon.battle.event.HitOutcome;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.DamageType;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.party.CombatResourceType;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.domain.combat.service.CombatService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;
import com.example.htmlmud.infra.server.WorldPulse;

@SpringBootTest
@ActiveProfiles("test")
class UnifiedCombatEngineAndPresentationTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private CombatService combatService;

  @Autowired
  private WorldPulse worldPulse;

  @Autowired
  private KillCommand killCommand;

  @Autowired
  private com.example.htmlmud.application.factory.WorldFactory worldFactory;

  private Player player;
  private List<String> capturedMessages;

  @BeforeEach
  void setUp() {
    capturedMessages = new CopyOnWriteArrayList<>();
    MessageOutput mockOutput = new MessageOutput() {
      @Override
      public void sendJson(Object payload) {
        if (payload != null) {
          capturedMessages.add(payload.toString());
        }
      }

      @Override
      public void close() {}

      @Override
      public WebSocketSession getSession() {
        return null;
      }
    };

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "獨孤劍客");
    player.setCurrentRoomId("newbie_village:forest_entrance");
    player.setInDungeon(false);
    player.start();
    worldManager.addLivingActor(player);

    Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
    room.enter(player, com.example.htmlmud.domain.model.enums.Direction.SOUTH);
  }

  @Test
  @DisplayName("1. 純函式數學檢驗：CombatRules.calculateRawDamage 與抗性折減完全正確")
  void testCombatRulesMathAndResistance() {
    // 基礎傷害：min=20, max=20, def=10 -> net=10, variance=1.0 -> final=10
    int dmg = CombatRules.calculateRawDamage(20, 20, 10, MobRank.NORMAL, 1.0);
    assertEquals(10, dmg);

    // 菁英首領 (ELITE / BOSS) 階級倍率加成
    int bossDmg = CombatRules.calculateRawDamage(20, 20, 10, MobRank.BOSS, 1.0);
    assertEquals((int) Math.round(10 * MobRank.BOSS.getDamageMultiplier()), bossDmg);

    // 傷害保底為 1
    int minGuaranteedDmg = CombatRules.calculateRawDamage(5, 5, 100, MobRank.NORMAL, 1.0);
    assertEquals(1, minGuaranteedDmg);

    // 抗性結算：50% 抗性 -> 減半
    int resDmg = CombatRules.applyResistance(100, DamageType.FIRE, 0.5);
    assertEquals(50, resDmg);

    // 弱點加成：-50% 弱點 -> 增傷 50%
    int weakDmg = CombatRules.applyResistance(100, DamageType.ICE, -0.5);
    assertEquals(150, weakDmg);

    // 免疫：1.0 抗性 -> 保底 1 點
    int immuneDmg = CombatRules.applyResistance(100, DamageType.PHYSICAL, 1.0);
    assertEquals(10, immuneDmg); // 0.10 clamp -> 10 點
  }

  @Test
  @DisplayName("2. 戰鬥參與者大一統抽象：Living, PartyMember 與 BattleEnemy 皆實現 Combatant 契約")
  void testCombatantPolymorphicContract() {
    // 1. Living (Player) 作為 Combatant
    Combatant playerCombatant = player;
    assertNotNull(playerCombatant.getId());
    assertEquals("獨孤劍客", playerCombatant.getName());
    assertTrue(playerCombatant.getEffectiveMinDamage() > 0);
    assertTrue(playerCombatant.getEffectiveMaxDamage() >= playerCombatant.getEffectiveMinDamage());

    // 2. PartyMember 作為 Combatant
    Party party = partyService.resetParty("獨孤劍客", "獨孤劍客");
    PartyMember leader = party.getMembers().get(0);
    Combatant leaderCombatant = leader;
    assertEquals(leader.getEffectiveMinDamage(), leaderCombatant.getEffectiveMinDamage());
    assertEquals(leader.getEffectiveDefense(), leaderCombatant.getEffectiveDefense());
    assertEquals(10, leaderCombatant.getEffectiveDex());

    // 3. BattleEnemy 作為 Combatant
    BattleEnemy enemy = BattleEnemy.builder()
        .id("test-wolf")
        .name("太陰魔狼")
        .hp(120)
        .maxHp(120)
        .minDamage(15)
        .maxDamage(25)
        .defense(6)
        .dex(14)
        .rank(MobRank.ELITE)
        .build();
    Combatant enemyCombatant = enemy;
    assertEquals(15, enemyCombatant.getEffectiveMinDamage());
    assertEquals(25, enemyCombatant.getEffectiveMaxDamage());
    assertEquals(6, enemyCombatant.getEffectiveDefense());
    assertEquals(14, enemyCombatant.getEffectiveDex());
    assertEquals(MobRank.ELITE, enemyCombatant.getRank());

    // 4. 基於 Combatant 呼叫 CombatRules
    int crossDmg = CombatRules.calculateRawDamage(leaderCombatant, enemyCombatant, 1.0);
    assertTrue(crossDmg > 0);
  }

  @Test
  @DisplayName("3. 雙模呈現投影：CombatPresentation 正確生成 MUD 武俠文字長文與 GUI BattleEvent")
  void testCombatPresentationDualMode() {
    DefenseResolution resHit = new DefenseResolution(
        DefenseOutcome.HIT, 35, 10, 15, "普通命中", false, 0, 0, false);

    // GUI 投影事件
    BattleEvent guiEvent = CombatPresentation.projectGuiCombatEvent(
        1001L, player, player, "獨孤九劍・破劍式", resHit, 35, "SWORD", "PHYSICAL");

    assertEquals(1001L, guiEvent.seq());
    assertEquals("獨孤九劍・破劍式", guiEvent.skillName());
    assertEquals(HitOutcome.HIT, guiEvent.hits().get(0).outcome());
    assertEquals(35, guiEvent.hits().get(0).amount());

    // 暴擊 GUI 投影
    DefenseResolution resCrit = new DefenseResolution(
        DefenseOutcome.CRIT, 70, 10, 15, "致命一擊", false, 0, 0, false);
    BattleEvent critEvent = CombatPresentation.projectGuiCombatEvent(
        1002L, player, player, "天外飛仙", resCrit, 70, "SWORD", "PHYSICAL");
    assertEquals(HitOutcome.CRIT, critEvent.hits().get(0).outcome());
    assertEquals(70, critEvent.hits().get(0).amount());
  }

  @Test
  @DisplayName("4. 城鎮遭遇戰整合驗證：kill 指令由統一戰鬥流程驅動並同時廣播 MUD 敘事與 GUI 結構化事件")
  void testTownCombatDualPresentationIntegration() throws Exception {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
      if (room.getMobs().isEmpty()) {
        Mob mob = worldFactory.createMob("wild_rat");
        mob.setCurrentRoomId(room.getId());
        mob.start();
        worldManager.addLivingActor(mob);
        room.addMob(mob);
      }
      List<Mob> mobs = room.getMobs();
      assertThat(mobs).isNotEmpty();

      killCommand.execute("wild rat");
    });

    // 模擬 40 個心跳 (4秒)
    for (int i = 0; i < 40; i++) {
      worldPulse.pulse();
      Thread.sleep(100);
    }

    assertThat(capturedMessages).isNotEmpty();

    // 驗證 1: MUD 文字敘事輸出
    boolean hasMudWuxiaLog = capturedMessages.stream().anyMatch(m ->
        m.contains("野鼠") && (m.contains("咬") || m.contains("抓") || m.contains("撞") || m.contains("傷害") || m.contains("撲擊")));
    assertTrue(hasMudWuxiaLog, "MUD 終端視窗應捕獲到武俠文字長篇戰鬥日誌");

    // 驗證 2: GUI 結構化 BattleEventsDto 同步輸出
    boolean hasGuiBattleEvents = capturedMessages.stream().anyMatch(m ->
        m.contains("town-combat-") || m.contains("BattleEventsDto") || m.contains("events"));
    assertTrue(hasGuiBattleEvents, "Web 前端視窗應同步捕獲到結構化 BattleEventsDto 戰鬥事件");
  }
}
