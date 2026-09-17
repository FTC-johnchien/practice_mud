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
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.model.Direction;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.service.PartyService;

class PartyInventoryAndMadnessTest {

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
  @DisplayName("測試隊伍公共行囊：初始物品、裝備穿戴與卸下數值加成")
  void testInventoryEquipAndUnequip() {
    Party party = partyService.getOrCreateParty("tester");
    assertThat(party.getInventory()).isNotNull();
    assertThat(party.getInventory().getSlots()).isNotEmpty();

    // 放入一把青銅古劍
    party.getInventory().addItem("bronze_sword", 1);
    PartyItemSlot swordSlot = party.getInventory().getSlots().stream()
        .filter(PartyItemSlot::isWeapon)
        .findFirst().orElse(null);
    assertThat(swordSlot).isNotNull();

    PartyMember member = party.getMembers().get(0);
    int baseMin = member.getBaseMinDamage();
    int baseMax = member.getBaseMaxDamage();

    // 穿戴武器
    String equipMsg = party.equipItemOnMember(swordSlot.getSlotId(), 0);
    assertThat(equipMsg).contains("裝備了武器");
    assertThat(member.getEquippedWeapon()).isNotNull();
    assertThat(member.getEffectiveMinDamage()).isGreaterThan(baseMin);
    assertThat(member.getEffectiveMaxDamage()).isGreaterThan(baseMax);

    // 卸下武器
    String unequipMsg = party.unequipItemFromMember("weapon", 0);
    assertThat(unequipMsg).contains("卸下了武器");
    assertThat(member.getEquippedWeapon()).isNull();
    assertThat(member.getEffectiveMinDamage()).isEqualTo(baseMin);
  }

  @Test
  @DisplayName("測試隊伍公共行囊：丹藥服用恢復氣血與清心符回復道心")
  void testInventoryItemUse() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember member = party.getMembers().get(1);

    member.getStats().setHp(30);
    member.setCurrentSan(90);
    member.consumeSan(40);
    assertThat(member.getCurrentSan()).isEqualTo(50);

    // 服用培元丹 (HEAL_HP 50)
    PartyItemSlot pillSlot = party.getInventory().getSlots().stream()
        .filter(s -> "HEAL_HP".equals(s.getEffectType()))
        .findFirst().orElse(null);
    assertThat(pillSlot).isNotNull();

    party.useItemOnMember(pillSlot.getSlotId(), 1);
    assertThat(member.getStats().getHp()).isEqualTo(80);

    // 使用清心符 (RESTORE_SAN 25)
    PartyItemSlot talismanSlot = party.getInventory().getSlots().stream()
        .filter(s -> "RESTORE_SAN".equals(s.getEffectType()))
        .findFirst().orElse(null);
    assertThat(talismanSlot).isNotNull();

    party.useItemOnMember(talismanSlot.getSlotId(), 1);
    assertThat(member.getCurrentSan()).isEqualTo(75);
  }

  @Test
  @DisplayName("測試第一階段走火入魔 (CHAOS) 與清心符解除心魔")
  void testChaosMadnessAndPurifyRecovery() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember iron = party.getMembers().get(1); // 鐵牛 (非主角)

    iron.consumeSan(100);
    assertThat(iron.getCurrentSan()).isEqualTo(0);

    // 設定走火入魔第一階段
    iron.setMadnessState(PartyMember.MadnessState.CHAOS);
    iron.setAberrationCounter(25);
    assertThat(iron.getSanityStatus()).contains("走火入魔 25%");

    // 使用清心符回復道心，若 SAN > 0 解除走火入魔
    PartyItemSlot talisman = party.getInventory().getSlots().stream()
        .filter(s -> "RESTORE_SAN".equals(s.getEffectType()))
        .findFirst().orElse(null);
    assertThat(talisman).isNotNull();

    String res = party.useItemOnMember(talisman.getSlotId(), 1);
    assertThat(res).contains("走火入魔狀態解除");
    assertThat(iron.getMadnessState()).isEqualTo(PartyMember.MadnessState.SANE);
    assertThat(iron.getAberrationCounter()).isEqualTo(0);
    assertThat(iron.getCurrentSan()).isGreaterThan(0);
  }

  @Test
  @DisplayName("測試太上鎮魔封印術 (SEALED) 凍結異變進度")
  void testSealMemberAction() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember iron = party.getMembers().get(1);

    iron.consumeSan(100);
    iron.setMadnessState(PartyMember.MadnessState.CHAOS);
    iron.setAberrationCounter(50);

    battleService.sealTarget(dummyPlayer, 1, dungeonPos);
    assertThat(iron.getMadnessState()).isEqualTo(PartyMember.MadnessState.SEALED);
    assertThat(iron.getSanityStatus()).contains("鎮魔封印中");
  }

  @Test
  @DisplayName("測試血肉道核遺學吸收：隊友學會異變隊員之絕學")
  void testLearnSkillFromDaoCore() {
    Party party = partyService.getOrCreateParty("tester");
    PartyMember iron = party.getMembers().get(1); // 鐵牛
    int initialSkillCount = iron.getSkills().size();

    // 構造一枚血肉道種
    PartyItemSlot daoCore = PartyItemSlot.builder()
        .slotId("test-dao-core")
        .itemId("dao_core_sword_pierce")
        .name("【血肉道核・燕青】")
        .icon("🧿")
        .itemType(ItemType.CONSUMABLE)
        .subType("SKILL_CORE")
        .count(1)
        .effectType("LEARN_SKILL")
        .grantedSkillId("sword_pierce")
        .grantedSkillName("青元貫日劍")
        .build();
    party.getInventory().addSlot(daoCore);

    String learnResult = party.useItemOnMember("test-dao-core", 1);
    assertThat(learnResult).contains("成功領悟掌握新絕學");
    assertThat(iron.getSkills().size()).isEqualTo(initialSkillCount + 1);

    // 驗證學到的技能
    PartyMemberSkill learned = iron.getSkills().stream()
        .filter(s -> s.getId().equals("sword_pierce"))
        .findFirst().orElse(null);
    assertThat(learned).isNotNull();
    assertThat(learned.getName()).isEqualTo("青元貫日劍");
    assertThat(learned.getCostType()).isEqualTo(ResourceType.RAGE); // 自動適配為力士怒氣資源
  }

  @Test
  @DisplayName("測試背包物品去綴比對堆疊與 5+2 裝備欄格式化輸出")
  void testItemPrefixNormalizationAndEquipmentSlots() {
    Party party = partyService.getOrCreateParty("tester");

    // 1. 驗證消耗品去綴堆疊：初始行囊已有 taiyin_pill，再加入 taiyin_tomb:taiyin_pill 應合併堆疊而非建立新格位
    int initialSlotsCount = party.getInventory().getSlots().size();
    PartyItemSlot pillSlotBefore = party.getInventory().getSlots().stream()
        .filter(s -> s.getItemId().contains("taiyin_pill"))
        .findFirst().orElse(null);
    assertThat(pillSlotBefore).isNotNull();
    int countBefore = pillSlotBefore.getCount();

    boolean addedPill = party.getInventory().addItem("taiyin_tomb:taiyin_pill", 2);
    assertThat(addedPill).isTrue();
    // 總格數不變，數量增加 2
    assertThat(party.getInventory().getSlots().size()).isEqualTo(initialSlotsCount);
    assertThat(pillSlotBefore.getCount()).isEqualTo(countBefore + 2);

    // 2. 驗證非堆疊裝備（如青銅古劍）即便 ID 相同也會分別佔用獨立格位
    party.getInventory().addItem("bronze_sword", 1);
    party.getInventory().addItem("taiyin_tomb:bronze_sword", 1);
    long swordCount = party.getInventory().getSlots().stream()
        .filter(s -> s.getItemId().contains("bronze_sword"))
        .count();
    assertThat(swordCount).isGreaterThanOrEqualTo(2);

    // 3. 驗證 formatPartyStatus 包含 5+2 槽位資訊
    String status = partyService.formatPartyStatus(party);
    assertThat(status).contains("[主手:");
    assertThat(status).contains("[副手:");
    assertThat(status).contains("[頭部:");
    assertThat(status).contains("[身軀:");
    assertThat(status).contains("[靴履:");
    assertThat(status).contains("[飾品1:");
    assertThat(status).contains("[飾品2:");
  }
}
