package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.application.command.impl.RestCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.GameStateBroadcastService;

class PhaseOneMechanicsTest {

  private PartyService partyService;
  private DungeonManager dungeonManager;
  private DrpgBattleService battleService;
  private GameStateBroadcastService broadcastService;
  private RestCommand restCommand;
  private Player dummyPlayer;
  private DungeonPosition mozhuPos;

  @BeforeEach
  void setUp() {
    dungeonManager = new DungeonManager();
    dungeonManager.init();

    partyService = new PartyService();
    partyService.initDefaultFormations();

    battleService = new DrpgBattleService(partyService, dungeonManager, new DungeonNavigator());
    broadcastService = mock(GameStateBroadcastService.class);
    restCommand = new RestCommand(partyService, dungeonManager, new DungeonNavigator(), battleService, broadcastService);

    dummyPlayer = mock(Player.class);
    when(dummyPlayer.getName()).thenReturn("tester");
    when(dummyPlayer.isValid()).thenReturn(true);
    when(dummyPlayer.getCurrentRoomId()).thenReturn("mozhu_mines:entrance");

    // 墨竹礦坑起點在 (1, 8)
    mozhuPos = new DungeonPosition("mozhu_mines_b1f", 3, 3, Direction.NORTH, 10, 10);
    dungeonManager.setPlayerPosition("tester", mozhuPos);
  }

  @Test
  @DisplayName("測試滅團重生坐標動態化：正確回歸當前地牢起點 (1, 8)，而非硬編碼 (1, 1)")
  void testDynamicDefeatRespawnCoord() throws Exception {
    battleService.startBattle(dummyPlayer, mozhuPos, List.of("taiyin_tomb:corpse_doll"));
    BattleContext ctx = battleService.getBattle("tester");
    assertThat(ctx).isNotNull();

    // 模擬小隊滅團調用 resolveDefeat
    Method resolveDefeatMethod = DrpgBattleService.class.getDeclaredMethod(
        "resolveDefeat", Player.class, BattleContext.class, DungeonPosition.class);
    resolveDefeatMethod.setAccessible(true);
    resolveDefeatMethod.invoke(battleService, dummyPlayer, ctx, mozhuPos);

    DungeonFloor floor = dungeonManager.getFloor("mozhu_mines_b1f");
    assertThat(floor).isNotNull();
    assertThat(floor.getStartCoord()).isNotNull();

    // 斷言坐標回歸至起點 (1, 8)
    assertThat(mozhuPos.getX()).isEqualTo(floor.getStartCoord().x());
    assertThat(mozhuPos.getY()).isEqualTo(floor.getStartCoord().y());
    assertThat(mozhuPos.getX()).isEqualTo(1);
    assertThat(mozhuPos.getY()).isEqualTo(8);
  }

  @Test
  @DisplayName("測試地牢安全節點 (墨家殘碑 EVENT) 調息：免除靈壓累加且不觸發伏擊")
  void testSafePointRestInDungeon() {
    // 墨竹礦坑 B1F (4, 4) 為 EVENT 墨家十戒非攻殘碑
    mozhuPos.setCoord(4, 4);
    mozhuPos.setDangerLevel(20);

    Party party = partyService.getOrCreateParty("tester");
    PartyMember member = party.getMembers().get(0);
    member.getStats().setHp(50);
    member.consumeSan(30);

    ScopedValue.where(MudContext.CURRENT_PLAYER, dummyPlayer).run(() -> {
      restCommand.execute("");
    });

    // 氣血與道心有回復
    assertThat(member.getStats().getHp()).isEqualTo(80);
    assertThat(member.getCurrentSan()).isEqualTo(80);
    // 安全點調息不增加危險警戒靈壓
    assertThat(mozhuPos.getDangerLevel()).isEqualTo(20);
    // 未觸發戰鬥
    assertThat(battleService.isInBattle("tester")).isFalse();
  }

  @Test
  @DisplayName("測試同伴戰術 AI：隊友殘血時醫修自動施展【神聖治癒】急救")
  void testCompanionHealingTactics() throws Exception {
    battleService.startBattle(dummyPlayer, mozhuPos, List.of("taiyin_tomb:corpse_doll"));
    BattleContext ctx = battleService.getBattle("tester");

    // 取得鐵牛 (主角之後的隊友) 與 凌霜 (醫修)
    PartyMember iron = ctx.getParty().getMembers().get(1);
    PartyMember ling = ctx.getParty().getMembers().get(3);

    // 確保凌霜具有治療招式且 MP 充足
    PartyMemberSkill healSkill = ling.getSkills().stream().filter(PartyMemberSkill::isHeal).findFirst().orElse(null);
    if (healSkill == null) {
      healSkill = PartyMemberSkill.builder()
          .id("divine_healing")
          .name("神聖治癒")
          .heal(true)
          .healAmount(60)
          .costType(ResourceType.MP)
          .costValue(20)
          .cooldownMs(5000)
          .build();
      ling.getSkills().add(healSkill);
    }
    ling.getStats().setMp(100);

    // 將鐵牛生命值壓低至 30% (<= 45%)
    iron.getStats().setHp((int) (iron.getStats().getMaxHp() * 0.3));
    int hpBefore = iron.getStats().getHp();

    // 透過反射調用 tryTriggerCompanionTactics
    Method tacticsMethod = DrpgBattleService.class.getDeclaredMethod(
        "tryTriggerCompanionTactics", Player.class, BattleContext.class, PartyMember.class, DungeonPosition.class);
    tacticsMethod.setAccessible(true);
    boolean triggered = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, ling, mozhuPos);

    assertThat(triggered).isTrue();
    // 斷言鐵牛血量得到回復
    assertThat(iron.getStats().getHp()).isGreaterThan(hpBefore);
    // 斷言凌霜治療技能進入冷卻
    assertThat(ling.isOnCooldown(healSkill.getId())).isTrue();
  }

  @Test
  @DisplayName("測試同伴戰術 AI：多隻怪物時力士自動施展【金剛怒目】群體嘲諷")
  void testCompanionTauntTactics() throws Exception {
    // 召喚 2 隻怪物
    battleService.startBattle(dummyPlayer, mozhuPos, List.of("taiyin_tomb:corpse_doll", "taiyin_tomb:bone_bat"));
    BattleContext ctx = battleService.getBattle("tester");

    PartyMember iron = ctx.getParty().getMembers().get(1);
    PartyMemberSkill tauntSkill = iron.getSkills().stream().filter(PartyMemberSkill::isTaunt).findFirst().orElse(null);
    if (tauntSkill == null) {
      tauntSkill = PartyMemberSkill.builder()
          .id("lion_roar")
          .name("獅子吼")
          .taunt(true)
          .costType(ResourceType.RAGE)
          .costValue(10)
          .cooldownMs(6000)
          .build();
      iron.getSkills().add(tauntSkill);
    }
    iron.setCurrentRage(30);

    assertThat(ctx.isTaunted()).isFalse();

    Method tacticsMethod = DrpgBattleService.class.getDeclaredMethod(
        "tryTriggerCompanionTactics", Player.class, BattleContext.class, PartyMember.class, DungeonPosition.class);
    tacticsMethod.setAccessible(true);
    boolean triggered = (boolean) tacticsMethod.invoke(battleService, dummyPlayer, ctx, iron, mozhuPos);

    assertThat(triggered).isTrue();
    // 斷言戰鬥被嘲諷鎖定
    assertThat(ctx.isTaunted()).isTrue();
    assertThat(ctx.getTauntedByMemberId()).isEqualTo(iron.getId());
    assertThat(iron.isOnCooldown(tauntSkill.getId())).isTrue();
  }
}
