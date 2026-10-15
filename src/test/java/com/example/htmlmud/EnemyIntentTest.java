package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.domain.party.model.RowPosition;

class EnemyIntentTest {

  @Test
  @DisplayName("ENEMY-INTENT-01: 測試敵人意圖預告啟動與清除")
  void testEnemyIntentLifecycle() {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("mob-test-1")
        .name("幽冥狂屍")
        .hp(100)
        .maxHp(100)
        .row(RowPosition.FRONT)
        .build();

    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCurrentIntentName()).isNull();

    // 啟動意圖
    enemy.startIntent("⚔️", "屍爪猛擊", "HEAVY", "SINGLE", 1200, true);

    assertThat(enemy.getCurrentIntentIcon()).isEqualTo("⚔️");
    assertThat(enemy.getCurrentIntentName()).isEqualTo("屍爪猛擊");
    assertThat(enemy.getCurrentIntentType()).isEqualTo("HEAVY");
    assertThat(enemy.getIntentTargetScope()).isEqualTo("SINGLE");
    assertThat(enemy.isCasting()).isTrue();
    assertThat(enemy.getCastDurationMs()).isEqualTo(1200);
    assertThat(enemy.getCastRemainingMs()).isGreaterThan(0).isLessThanOrEqualTo(1200);
    assertThat(enemy.isInterruptible()).isTrue();

    // 清除意圖
    enemy.clearIntent();
    assertThat(enemy.isCasting()).isFalse();
    assertThat(enemy.getCurrentIntentName()).isNull();
    assertThat(enemy.getCastRemainingMs()).isEqualTo(0);
  }

  @Test
  @DisplayName("ENEMY-INTENT-01: 測試 BattleEnemyViewDto 向後相容與意圖欄位封裝")
  void testBattleEnemyViewDtoBackwardsCompatibility() {
    // 舊版 19 參數構造
    BattleEnemyViewDto oldDto = new BattleEnemyViewDto(
        0, "mob-1", "血蝠", 50, 50, "FRONT", true, false, false,
        "member-1", "劍修", 10, false, 1, "CENTER", 1, 2, 1, 1
    );

    assertThat(oldDto.intentIcon()).isNull();
    assertThat(oldDto.intentName()).isNull();
    assertThat(oldDto.intentType()).isEqualTo("PHYSICAL");
    assertThat(oldDto.isCasting()).isFalse();
    assertThat(oldDto.isInterruptible()).isTrue();

    // 全參構造
    BattleEnemyViewDto fullDto = new BattleEnemyViewDto(
        0, "mob-1", "血蝠", 50, 50, "FRONT", true, false, false,
        "member-1", "劍修", 10, false, 1, "CENTER", 1, 2, 1, 1,
        "🔥", "烈火咒", "SPELL", "SINGLE", true, 1500L, 1200L, true
    );

    assertThat(fullDto.intentIcon()).isEqualTo("🔥");
    assertThat(fullDto.intentName()).isEqualTo("烈火咒");
    assertThat(fullDto.intentType()).isEqualTo("SPELL");
    assertThat(fullDto.isCasting()).isTrue();
    assertThat(fullDto.castRemainingMs()).isEqualTo(1200L);
    assertThat(fullDto.currentAction()).isEqualTo("烈火咒");
    assertThat(fullDto.actionProgressPercent()).isEqualTo(20);
    assertThat(fullDto.interruptible()).isTrue();
  }

  @Test
  @DisplayName("MILESTONE-1 TASK-1: 驗證 BattleEnemy 與 BattleEnemyViewDto 之 currentAction、actionProgressPercent、interruptible")
  void testEnemyActionAndProgressPercent() throws InterruptedException {
    BattleEnemy enemy = BattleEnemy.builder()
        .id("boss-song")
        .name("畸變大師兄·宋天衡")
        .hp(450)
        .maxHp(450)
        .castDurationMs(2000L)
        .interruptible(true)
        .build();

    assertThat(enemy.getCurrentAction()).isEmpty();
    assertThat(enemy.getActionProgressPercent()).isEqualTo(0);
    assertThat(enemy.isInterruptible()).isTrue();
    assertThat(enemy.getInterruptible()).isTrue();

    // 啟動蓄力
    enemy.startIntent("⚔️", "泣血狂暴巨爪拍下", "HEAVY", "SINGLE", 2000L, true);

    assertThat(enemy.getCurrentAction()).isEqualTo("泣血狂暴巨爪拍下");
    assertThat(enemy.isCasting()).isTrue();
    assertThat(enemy.getActionProgressPercent()).isGreaterThanOrEqualTo(0).isLessThanOrEqualTo(100);

    // 驗證 BattleEnemyDto 與 CombatMobDto 轉化
    com.example.htmlmud.domain.dungeon.dto.BattleEnemyDto enemyDto =
        com.example.htmlmud.domain.dungeon.dto.BattleEnemyDto.from(0, enemy);
    assertThat(enemyDto.currentAction()).isEqualTo("泣血狂暴巨爪拍下");
    assertThat(enemyDto.actionProgressPercent()).isGreaterThanOrEqualTo(0);
    assertThat(enemyDto.interruptible()).isTrue();

    com.example.htmlmud.domain.dungeon.dto.CombatMobDto mobDto =
        com.example.htmlmud.domain.dungeon.dto.CombatMobDto.of("boss-song", "畸變大師兄·宋天衡", 450, 450, enemy.getCurrentAction(), enemy.getActionProgressPercent(), enemy.isInterruptible());
    assertThat(mobDto.currentAction()).isEqualTo("泣血狂暴巨爪拍下");
    assertThat(mobDto.interruptible()).isTrue();
  }

  @Test
  @DisplayName("MILESTONE-1 TASK-1: 驗證 MobTemplate 與 PartyMemberSkill 資料驅動加載 castTimeMs 與 interruptible")
  void testDataModelSchemaExtensions() {
    com.example.htmlmud.infra.persistence.repository.TemplateRepository repo =
        new com.example.htmlmud.infra.persistence.repository.TemplateRepository();
    repo.initDefaults();

    // 驗證 party_skills.json 中的 castTimeMs 與 interruptible 設定
    var healSkillOpt = repo.findPartySkill("heal_single");
    assertThat(healSkillOpt).isPresent();
    assertThat(healSkillOpt.get().getCastTimeMs()).isEqualTo(1500L);
    assertThat(healSkillOpt.get().isInterruptible()).isTrue();

    var sealSkillOpt = repo.findPartySkill("taoist_seal");
    assertThat(sealSkillOpt).isPresent();
    assertThat(sealSkillOpt.get().getTags()).contains("STUN", "SILENCE");

    var meteorSkillOpt = repo.findPartySkill("star_meteor");
    assertThat(meteorSkillOpt).isPresent();
    assertThat(meteorSkillOpt.get().getCastTimeMs()).isEqualTo(2500L);
    assertThat(meteorSkillOpt.get().isInterruptible()).isTrue();
  }

  @Test
  @DisplayName("MILESTONE-1 TASK-2: 驗證 CombatFsmService 怪物施法階段與 STUN / SILENCE 招式打斷判定")
  void testCombatFsmCastingAndInterrupt() {
    com.example.htmlmud.domain.dungeon.battle.CombatFsmService fsmService =
        new com.example.htmlmud.domain.dungeon.battle.CombatFsmService();

    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-song")
        .name("畸變大師兄·宋天衡")
        .hp(450)
        .maxHp(450)
        .poise(100)
        .maxPoise(100)
        .castDurationMs(2000L)
        .interruptible(true)
        .build();

    // 1. 啟動怪物蓄力施法階段 (CASTING)
    fsmService.startEnemyCast(boss, "⚡", "【天煞滅世轟】", "SPELL", "ALL", 2000L, true);
    assertThat(boss.isCasting()).isTrue();
    assertThat(boss.getCurrentAction()).isEqualTo("【天煞滅世轟】");
    assertThat(boss.isInterruptible()).isTrue();
    assertThat(boss.isPoiseBroken()).isFalse();

    // 2. 測試普通非控制技能無法打斷
    com.example.htmlmud.domain.party.model.PartyMember warrior =
        com.example.htmlmud.domain.party.model.PartyMember.builder()
            .id("warrior-1")
            .name("鐵面劍俠")
            .build();

    com.example.htmlmud.domain.party.model.PartyMemberSkill normalSlash =
        com.example.htmlmud.domain.party.model.PartyMemberSkill.builder()
            .id("sword_slash")
            .name("基礎劈砍")
            .tags(List.of("ATTACK", "PHYSICAL"))
            .build();

    var nonIntRes = fsmService.resolveSkillInterrupt(boss, warrior, normalSlash);
    assertThat(nonIntRes.interrupted()).isFalse();
    assertThat(boss.isCasting()).isTrue();

    // 3. 測試帶有 STUN / SILENCE 標籤的招式打斷判定
    com.example.htmlmud.domain.party.model.PartyMemberSkill sealSpell =
        com.example.htmlmud.domain.party.model.PartyMemberSkill.builder()
            .id("taoist_seal")
            .name("太陰定身符")
            .tags(List.of("SPELL", "CONTROL", "STUN", "SILENCE"))
            .stun(true)
            .stunDurationSeconds(3)
            .build();

    var intRes = fsmService.resolveSkillInterrupt(boss, warrior, sealSpell);
    assertThat(intRes.interrupted()).isTrue();
    assertThat(intRes.interruptedAction()).isEqualTo("【天煞滅世轟】");
    assertThat(intRes.combatLog()).contains("擊破【畸變大師兄·宋天衡】的【【天煞滅世轟】】蓄力");

    // 驗證怪物狀態機重置：蓄力中斷、進入破防力竭失衡狀態
    assertThat(boss.isCasting()).isFalse();
    assertThat(boss.getCurrentIntentName()).isNull();
    assertThat(boss.isPoiseBroken()).isTrue();
    assertThat(boss.isStunned()).isTrue();

    // 4. 測試不可被打斷之霸體招式 (interruptible = false)
    BattleEnemy unstoppableBoss = BattleEnemy.builder()
        .id("boss-song-2")
        .name("畸變大師兄·宋天衡")
        .hp(450)
        .maxHp(450)
        .poise(100)
        .maxPoise(100)
        .build();

    fsmService.startEnemyCast(unstoppableBoss, "💥", "【九幽滅神擊】", "HEAVY", "ALL", 3000L, false);
    assertThat(unstoppableBoss.isCasting()).isTrue();
    assertThat(unstoppableBoss.isInterruptible()).isFalse();

    var failedIntRes = fsmService.resolveSkillInterrupt(unstoppableBoss, warrior, sealSpell);
    assertThat(failedIntRes.interrupted()).isFalse();
    assertThat(unstoppableBoss.isCasting()).isTrue(); // 霸體招式無法被常規打斷
  }
}


