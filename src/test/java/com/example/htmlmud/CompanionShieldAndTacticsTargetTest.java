package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleContext;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.battle.DefenseResolver;
import com.example.htmlmud.domain.dungeon.battle.DrpgCombatLoop;
import com.example.htmlmud.domain.dungeon.battle.DrpgEnemyTacticsService;
import com.example.htmlmud.domain.dungeon.battle.DrpgRewardService;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.TacticsTarget;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;

@SpringBootTest
class CompanionShieldAndTacticsTargetTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private TemplateReader templateReader;

  @Test
  @DisplayName("驗證 4 大問題修復：技能不重複、護盾加持給前衛 Tank、傷害由護盾優先吸收、不誤打敵怪")
  void testShieldMechanicsAndTacticsTarget() {
    // 1. 驗證同伴技能去重 (Issue 4)
    Party party = partyService.getOrCreateParty("tester_shield");
    partyService.recruitCompanion(party, "tie_niu");
    partyService.recruitCompanion(party, "ling_shuang");

    PartyMember tieNiu = party.getMembers().stream()
        .filter(m -> m.getName().contains("鐵牛"))
        .findFirst().orElseThrow();
    PartyMember lingShuang = party.getMembers().stream()
        .filter(m -> m.getName().contains("凌霜"))
        .findFirst().orElseThrow();

    // 檢查同伴絕無重複名稱技能
    long distinctTieNiuSkills = tieNiu.getSkills().stream().map(PartyMemberSkill::getName).distinct().count();
    assertThat(tieNiu.getSkills().size()).isEqualTo((int) distinctTieNiuSkills);

    long distinctLingShuangSkills = lingShuang.getSkills().stream().map(PartyMemberSkill::getName).distinct().count();
    assertThat(lingShuang.getSkills().size()).isEqualTo((int) distinctLingShuangSkills);

    // 2. 驗證戰術方針支援 FRONT_ROW_ALLY (Tank) 與 指定隊員 (Issue 1)
    DrpgEnemyTacticsService tacticsService = new DrpgEnemyTacticsService(templateReader);
    BattleEnemy dummyEnemy = BattleEnemy.builder()
        .id("test-dummy")
        .name("測試傀儡")
        .maxHp(200)
        .hp(200)
        .minDamage(10)
        .maxDamage(15)
        .defense(5)
        .alive(true)
        .build();

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-1")
        .playerId("tester_shield")
        .party(party)
        .enemies(new ArrayList<>(List.of(dummyEnemy)))
        .build();

    // 解析前衛肉盾目標：應選中前衛且血量最高的 鐵牛
    PartyMember tankTarget = tacticsService.resolveAllyTarget(ctx, lingShuang, TacticsTarget.FRONT_ROW_ALLY, -1);
    assertThat(tankTarget.getName()).contains("鐵牛");

    // 解析隊長目標：應選中第 1 位隊長
    PartyMember leaderTarget = tacticsService.resolveAllyTarget(ctx, lingShuang, TacticsTarget.LEADER, -1);
    assertThat(leaderTarget).isEqualTo(party.getMembers().get(0));

    // 解析指定隊員 MEMBER_2：應選中 鐵牛
    PartyMember m2Target = tacticsService.resolveAllyTarget(ctx, lingShuang, TacticsTarget.MEMBER_2, -1);
    assertThat(m2Target.getName()).contains("鐵牛");

    // 3. 驗證金光辟邪護體技能與不動明王機制 (Issue 2)
    PartyMemberSkill blessSkill = lingShuang.getSkills().stream()
        .filter(s -> s.getName().contains("金光辟邪護體") || s.getId().contains("bless"))
        .findFirst().orElseThrow();

    assertThat(blessSkill.isShield()).isTrue();

    // 執行護盾技能
    DrpgCombatLoop combatLoop = new DrpgCombatLoop(tacticsService, new DrpgRewardService(), new DefenseResolver());
    Player player = mock(Player.class);
    when(player.getName()).thenReturn("tester_shield");
    when(player.isValid()).thenReturn(true);

    int initialEnemyHp = dummyEnemy.getHp();
    int initialTankHp = tankTarget.getStats().getHp();
    assertThat(tankTarget.getCurrentShield()).isEqualTo(0);

    // 凌霜對 FRONT_ROW_ALLY (鐵牛) 施展金光辟邪護體
    combatLoop.applySkillEffects(player, ctx, lingShuang, blessSkill, -1, TacticsTarget.FRONT_ROW_ALLY, "【戰術方針】");

    // 驗證：敵怪血量未受到任何減少 (絕不可誤傷敵怪)
    assertThat(dummyEnemy.getHp()).isEqualTo(initialEnemyHp);
    // 驗證：鐵牛獲得辟邪護盾 (25% 最大生命 = 180 * 0.25 = 45 護盾)
    assertThat(tankTarget.getCurrentShield()).isGreaterThanOrEqualTo(45);

    // 4. 驗證護盾吸收傷害機制 (Issue 2)
    int shieldBeforeHit = tankTarget.getCurrentShield();
    tankTarget.takeDamage(20);
    // 受到 20 點傷害後，血量無損，護盾吸收 20 點
    assertThat(tankTarget.getStats().getHp()).isEqualTo(initialTankHp);
    assertThat(tankTarget.getCurrentShield()).isEqualTo(shieldBeforeHit - 20);

    // 受到超過護盾剩餘值的傷害 (例如 50 點，此時剩餘護盾為 25)
    tankTarget.takeDamage(50);
    assertThat(tankTarget.getCurrentShield()).isEqualTo(0);
    assertThat(tankTarget.getStats().getHp()).isEqualTo(initialTankHp - 25);
  }
}
