package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketSession;

import com.example.htmlmud.application.command.impl.KillCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.combat.BattleContext;
import com.example.htmlmud.combat.BattleState;
import com.example.htmlmud.combat.DrpgBattleService;
import com.example.htmlmud.domain.model.enums.LivingPosture;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyService;
import com.example.htmlmud.combat.CombatService;
import com.example.htmlmud.domain.actor.service.PlayerService;
import com.example.htmlmud.domain.world.WorldManager;

/**
 * SP-08：戰鬥引擎大一統，野外戰鬥全面對齊 5 人小隊協同作戰整合測試
 */
@SpringBootTest
@ActiveProfiles("test")
class FieldCombatPartyUnificationTest {

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private DrpgBattleService drpgBattleService;

  @Autowired
  private CombatService combatService;

  @Autowired
  private KillCommand killCommand;

  @Autowired
  private WorldFactory worldFactory;

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

    player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "小隊俠士");
    player.setCurrentRoomId("newbie_village:forest_entrance");
    player.setInDungeon(false);
    player.start();
    worldManager.addLivingActor(player);

    Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
    room.enter(player, com.example.htmlmud.domain.model.enums.Direction.SOUTH);

    // 清理舊戰鬥狀態
    cleanupBattle("小隊俠士");
  }

  @AfterEach
  void tearDown() {
    cleanupBattle("小隊俠士");
  }

  private void cleanupBattle(String playerName) {
    if (drpgBattleService != null) {
      BattleContext ctx = drpgBattleService.getBattle(playerName);
      if (ctx != null) {
        ctx.cancelBattle();
        ctx.setState(BattleState.VICTORY);
      }
      drpgBattleService.getActiveBattles().remove(playerName);
    }
  }

  @Test
  @DisplayName("1. 野外遭遇戰啟動：kill 指令全面由 5 人小隊協同迎敵接管")
  void testFieldBattleInitiatesFullParty() {
    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
      Mob mob = room.getMobs().stream().filter(m -> m.isValid() && !m.isDead()).findFirst().orElseGet(() -> {
        Mob newMob = worldFactory.createMob("wild_rat");
        newMob.setCurrentRoomId(room.getId());
        newMob.start();
        worldManager.addLivingActor(newMob);
        room.addMob(newMob);
        return newMob;
      });

      killCommand.execute("wild rat");

      // 驗證 DRPG 戰鬥引擎已接管野外戰鬥
      assertTrue(drpgBattleService.isInBattle(player), "玩家應處於 DRPG 戰鬥引擎接管狀態");

      BattleContext ctx = drpgBattleService.getBattle(player.getName());
      assertNotNull(ctx, "戰鬥上下文不應為空");
      assertTrue(ctx.getBattleId().startsWith("town-combat-"), "戰鬥 ID 應具備 town-combat 前綴");

      // 驗證 5 人小隊全員列陣
      Party party = ctx.getParty();
      assertNotNull(party, "小隊不可為空");
      assertEquals(5, party.getMembers().size(), "小隊應滿編 5 人全員到齊迎敵");
      assertEquals("小隊俠士", party.getMembers().get(0).getName(), "隊長應為發起攻擊的主角");

      // 驗證敵群設定自大世界怪物正確映射
      assertEquals(1, ctx.getEnemies().size(), "應有一名敵方野怪");
      assertEquals(mob.getName(), ctx.getEnemies().get(0).getName());
      assertEquals(mob.getId(), ctx.getEnemies().get(0).getId());

      // 清理戰鬥
      ctx.cancelBattle();
      ctx.setState(BattleState.VICTORY);
    });
  }

  @Test
  @DisplayName("2. 戰鬥大捷連動：戰利品入小隊 40 格公共行囊，房間 Mob 倒地消散")
  void testVictoryRewardsIntoPartyInventoryAndCleansMob() throws Exception {
    Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
    Mob mob = room.getMobs().stream().filter(m -> m.isValid() && !m.isDead()).findFirst().orElseGet(() -> {
      Mob newMob = worldFactory.createMob("wild_rat");
      newMob.setCurrentRoomId(room.getId());
      newMob.start();
      worldManager.addLivingActor(newMob);
      room.addMob(newMob);
      return newMob;
    });

    ScopedValue.where(MudContext.CURRENT_PLAYER, player).run(() -> {
      killCommand.execute("wild rat");
    });

    BattleContext ctx = drpgBattleService.getBattle(player.getName());
    assertNotNull(ctx);

    // 模擬將敵人擊殺
    ctx.getEnemies().get(0).setHp(0);
    ctx.getEnemies().get(0).setAlive(false);

    // 觸發戰鬥結束回呼
    if (ctx.getOnBattleEnd() != null) {
      ctx.getOnBattleEnd().accept(BattleState.VICTORY);
    }

    // 驗證房間內生靈已清理
    assertFalse(room.getMobs().stream().anyMatch(m -> m.getId().equals(mob.getId())), "房間內應已移除該死亡怪物");
    assertEquals(LivingPosture.DEAD, mob.getPosture(), "怪物姿勢應標記為 DEAD");
    assertFalse(mob.isValid(), "怪物應標記為 invalid 停止心跳");

    // 驗證公共行囊具備預設容量
    Party party = partyService.getOrCreateParty(player.getName());
    assertThat(party.getInventory().getCapacity()).isEqualTo(40);
  }

  @Test
  @DisplayName("3. 向下相容通道：CombatService.startCombat 亦能平滑導向小隊戰鬥")
  void testCombatServiceDelegationToDrpgParty() {
    Room room = worldManager.getRoomActor("newbie_village:forest_entrance");
    Mob mob = room.getMobs().stream().filter(m -> m.isValid() && !m.isDead()).findFirst().orElseGet(() -> {
      Mob newMob = worldFactory.createMob("wild_rat");
      newMob.setCurrentRoomId(room.getId());
      newMob.start();
      worldManager.addLivingActor(newMob);
      room.addMob(newMob);
      return newMob;
    });

    // 透過歷史入口調用
    combatService.startCombat(player, mob.getId());

    assertTrue(drpgBattleService.isInBattle(player), "CombatService 應自動委派至 DrpgBattleService 啟動小隊戰鬥");
    BattleContext ctx = drpgBattleService.getBattle(player.getName());
    assertNotNull(ctx);
    assertEquals(5, ctx.getParty().getMembers().size(), "小隊應依然為 5 人滿編");

    ctx.cancelBattle();
    ctx.setState(BattleState.VICTORY);
  }
}
