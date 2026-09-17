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
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.service.PartyService;

class DrpgBattleServiceTest {

  private PartyService partyService;
  private DungeonManager dungeonManager;
  private DrpgBattleService battleService;
  private Player dummyPlayer;
  private DungeonPosition dungeonPos;

  @BeforeEach
  void setUp() {
    dungeonManager = new DungeonManager();
    dungeonManager.init();

    partyService = new PartyService();
    partyService.initDefaultFormations();

    battleService = new DrpgBattleService(partyService, dungeonManager);

    dummyPlayer = mock(Player.class);
    when(dummyPlayer.getName()).thenReturn("tester");
    when(dummyPlayer.isValid()).thenReturn(true);

    dungeonPos = new DungeonPosition("taiyin_tomb_b1f", 1, 1, Direction.NORTH, 10, 10);
  }

  @Test
  @DisplayName("測試戰鬥初始化與前後排敵人生成")
  void testBattleInitialization() {
    List<String> mobIds = List.of("taiyin_tomb:corpse_doll", "taiyin_tomb:bone_bat");
    battleService.startBattle(dummyPlayer, dungeonPos, mobIds);

    assertThat(battleService.isInBattle("tester")).isTrue();
    BattleContext ctx = battleService.getBattle("tester");
    assertThat(ctx).isNotNull();
    assertThat(ctx.getEnemies()).hasSize(2);
    assertThat(ctx.getParty().getMembers()).hasSize(5);

    // 驗證主角為 COMBO 資源，鐵牛為 RAGE 資源，凌霜為 MP 資源
    PartyMember leader = ctx.getParty().getMembers().get(0);
    assertThat(leader.getResourceType()).isEqualTo(ResourceType.COMBO);
    assertThat(leader.getSkills()).isNotEmpty();

    PartyMember iron = ctx.getParty().getMembers().get(1);
    assertThat(iron.getResourceType()).isEqualTo(ResourceType.RAGE);

    PartyMember ling = ctx.getParty().getMembers().get(3);
    assertThat(ling.getResourceType()).isEqualTo(ResourceType.MP);
  }

  @Test
  @DisplayName("測試三態資源招式施展：MP、怒氣、連擊點")
  void testSkillCastingResources() {
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll"));
    BattleContext ctx = battleService.getBattle("tester");

    PartyMember leader = ctx.getParty().getMembers().get(0);
    PartyMember iron = ctx.getParty().getMembers().get(1);
    PartyMember ling = ctx.getParty().getMembers().get(3);

    // 1. 燕青/主角 COMBO 技能 (先裝備劍)
    leader.getEquipment().put(com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND,
        com.example.htmlmud.domain.party.model.PartyItemSlot.builder()
            .slotId("slot-sword")
            .itemId("iron_sword")
            .name("鐵劍")
            .itemType(com.example.htmlmud.domain.model.enums.ItemType.WEAPON)
            .equipSlot(com.example.htmlmud.domain.model.enums.EquipmentSlot.MAIN_HAND)
            .subType("SWORD")
            .build());
    leader.setCurrentCombo(0);
    battleService.castSkill(dummyPlayer, 0, "sword_pierce", 0, dungeonPos);
    // 未達 2 連擊點，施放失敗，技能不進入冷卻
    assertThat(leader.isOnCooldown("sword_pierce")).isFalse();

    leader.setCurrentCombo(3);
    battleService.castSkill(dummyPlayer, 0, "sword_pierce", 0, dungeonPos);
    assertThat(leader.getCurrentCombo()).isEqualTo(1); // 扣 2
    assertThat(leader.isOnCooldown("sword_pierce")).isTrue();

    // 2. 鐵牛 RAGE 技能
    iron.setCurrentRage(10);
    battleService.castSkill(dummyPlayer, 1, "tank_taunt", 0, dungeonPos);
    assertThat(iron.isOnCooldown("tank_taunt")).isFalse();

    iron.setCurrentRage(50);
    battleService.castSkill(dummyPlayer, 1, "tank_taunt", 0, dungeonPos);
    assertThat(iron.getCurrentRage()).isEqualTo(20); // 扣 30
    assertThat(iron.isOnCooldown("tank_taunt")).isTrue();
    assertThat(ctx.isTaunted()).isTrue();

    // 3. 凌霜 MP 技能 (回春)
    int initialMp = ling.getStats().getMp();
    battleService.castSkill(dummyPlayer, 3, "heal_single", 0, dungeonPos);
    assertThat(ling.getStats().getMp()).isEqualTo(initialMp - 30);
    assertThat(ling.isOnCooldown("heal_single")).isTrue();
  }

  @Test
  @DisplayName("測試陣法靈威達標釋放陣法奧義")
  void testPartyUltimate() {
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll"));
    BattleContext ctx = battleService.getBattle("tester");

    Party party = ctx.getParty();
    party.setFormationEnergy(100);

    battleService.castPartyUltimate(dummyPlayer, dungeonPos);
    assertThat(party.getFormationEnergy()).isEqualTo(0);
  }

  @Test
  @DisplayName("測試主動迎戰與戰鬥視圖持久化建立")
  void testFightActionAndBattleView() {
    battleService.startBattle(dummyPlayer, dungeonPos, List.of("taiyin_tomb:corpse_doll"));
    var view = battleService.createBattleView("tester");
    assertThat(view).isNotNull();
    assertThat(view.inBattle()).isTrue();
    assertThat(view.enemies()).hasSize(1);

    // 主動迎戰加速攻擊
    battleService.fight(dummyPlayer, dungeonPos);
    assertThat(battleService.isInBattle("tester")).isTrue();
  }
}