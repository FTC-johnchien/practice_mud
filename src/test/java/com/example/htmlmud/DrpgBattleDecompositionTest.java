package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.BattleState;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.dungeon.battle.DrpgRewardService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.TemplateCatalog;

class DrpgBattleDecompositionTest {

  private PartyService partyService;
  private DungeonManager dungeonManager;
  private DrpgEnemyTacticsService tacticsService;
  private DrpgRewardService rewardService;
  private DrpgCombatLoop combatLoop;
  private DrpgBattleService battleService;
  private Player dummyPlayer;
  private DungeonPosition dungeonPos;

  @BeforeEach
  void setUp() {
    dungeonManager = new DungeonManager();
    dungeonManager.init();

    partyService = new PartyService();
    partyService.initDefaultFormations();

    tacticsService = new DrpgEnemyTacticsService(new TemplateCatalog());
    rewardService = new DrpgRewardService(new TemplateCatalog(), dungeonManager);
    combatLoop = new DrpgCombatLoop(tacticsService, rewardService);

    battleService = new DrpgBattleService(
        partyService, dungeonManager, null, new TemplateCatalog(),
        combatLoop, tacticsService, rewardService, null
    );

    dummyPlayer = mock(Player.class);
    when(dummyPlayer.getName()).thenReturn("tester");
    when(dummyPlayer.getId()).thenReturn("p-tester");
    when(dummyPlayer.isValid()).thenReturn(true);

    dungeonPos = new DungeonPosition("taiyin_tomb_b1f", 1, 1, Direction.NORTH, 10, 10);
  }

  @Test
  @DisplayName("測試 DrpgEnemyTacticsService：前排 1.3 倍仇恨權重與嘲諷覆蓋鎖定")
  void testEnemyTacticsTargetSelection() {
    Party party = partyService.createFullParty("tester");
    PartyMember frontMember = party.getMembers().stream()
        .filter(m -> m.getRow() == RowPosition.FRONT).findFirst().orElseThrow();
    PartyMember backMember = party.getMembers().stream()
        .filter(m -> m.getRow() == RowPosition.BACK).findFirst().orElseThrow();

    BattleEnemy enemy = BattleEnemy.builder().id("mob-1").name("妖邪").build();

    BattleContext ctx = BattleContext.builder()
        .battleId("b-1")
        .party(party)
        .enemies(List.of(enemy))
        .build();

    // 1. 初始無仇恨，優先鎖定前排成員
    PartyMember picked1 = tacticsService.selectPartyTarget(ctx);
    assertThat(picked1.getRow()).isEqualTo(RowPosition.FRONT);

    // 2. 後排建立大量仇恨
    backMember.addThreat(1000);
    PartyMember picked2 = tacticsService.selectPartyTarget(ctx);
    assertThat(picked2.getId()).isEqualTo(backMember.getId());

    // 3. 嘲諷狀態覆蓋：前排開啟嘲諷，即便後排仇恨高，怪物仍強制鎖定嘲諷者
    ctx.setTaunt(frontMember.getId(), 5000);
    PartyMember picked3 = tacticsService.selectPartyTarget(ctx);
    assertThat(picked3.getId()).isEqualTo(frontMember.getId());
  }

  @Test
  @DisplayName("測試 DrpgRewardService：深淵畸變體擊殺掉落【血肉道核】且包含遺留絕學")
  void testAberrationDaoCoreDrop() {
    Party party = partyService.createFullParty("tester");

    BattleEnemy aberration = BattleEnemy.builder()
        .id("aberration-m-iron")
        .name("【深淵畸變體・鐵牛】")
        .alive(false)
        .droppedDaoSkillId("tank_smash")
        .droppedDaoSkillName("裂地崩山")
        .droppedDaoMemberName("鐵牛")
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("b-aberration")
        .party(party)
        .enemies(List.of(aberration))
        .state(BattleState.VICTORY)
        .build();

    int initialSlots = party.getInventory().getSlots().size();
    battleService.resolveVictory(dummyPlayer, ctx, dungeonPos);

    assertThat(party.getInventory().getSlots().size()).isEqualTo(initialSlots + 1);
    var coreSlot = party.getInventory().getSlots().stream()
        .filter(s -> s.getItemId().equals("dao_core_tank_smash"))
        .findFirst().orElseThrow();

    assertThat(coreSlot.getGrantedSkillId()).isEqualTo("tank_smash");
    assertThat(coreSlot.getGrantedSkillName()).isEqualTo("裂地崩山");
  }

  @Test
  @DisplayName("測試 DrpgRewardService：戰後清理鎮魔封印狀態 (恢復道心 +20 SAN，清除異變計數)")
  void testPostBattleUnsealing() {
    Party party = partyService.createFullParty("tester");
    PartyMember iron = party.getMembers().get(1);

    iron.setMadnessState(PartyMember.MadnessState.SEALED);
    iron.setAberrationCounter(65);
    iron.setCurrentSan(10);

    BattleContext ctx = BattleContext.builder()
        .battleId("b-clean")
        .party(party)
        .enemies(List.of(BattleEnemy.builder().id("m").alive(false).build()))
        .build();

    rewardService.postBattleCleanup(dummyPlayer, ctx);

    assertThat(iron.getMadnessState()).isEqualTo(PartyMember.MadnessState.SANE);
    assertThat(iron.getAberrationCounter()).isZero();
    assertThat(iron.getCurrentSan()).isEqualTo(30);
  }

  @Test
  @DisplayName("測試 DrpgBattleService 門面與子服務協同：啟動戰鬥、查詢狀態與戰鬥視圖")
  void testFacadeCoordination() {
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll"));

    assertThat(battleService.isInBattle("tester")).isTrue();
    assertThat(battleService.isInBattle(dummyPlayer)).isTrue();

    var view = battleService.createBattleView("tester");
    assertThat(view).isNotNull();
    assertThat(view.enemies()).hasSize(1);
    assertThat(view.inBattle()).isTrue();
    assertThat(view.battleState()).isEqualTo("FIGHTING");

    assertThat(battleService.getCombatLoop()).isNotNull();
    assertThat(battleService.getTacticsService()).isNotNull();
    assertThat(battleService.getRewardService()).isNotNull();
  }
}
