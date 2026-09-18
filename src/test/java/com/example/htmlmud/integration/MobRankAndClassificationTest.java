package com.example.htmlmud.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import com.example.htmlmud.domain.dungeon.battle.BattleEnemy;
import com.example.htmlmud.domain.model.enums.MobKind;
import com.example.htmlmud.domain.model.enums.MobRank;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
@ActiveProfiles("test")
public class MobRankAndClassificationTest {

  @Test
  @DisplayName("1. 驗證 MobRank 枚舉屬性與前綴設定")
  void testMobRankEnums() {
    assertThat(MobRank.NORMAL.getDisplayName()).isEqualTo("普通");
    assertThat(MobRank.NORMAL.getPrefix()).isEmpty();
    assertThat(MobRank.NORMAL.getHpMultiplier()).isEqualTo(1.0);

    assertThat(MobRank.ELITE.getDisplayName()).isEqualTo("精英");
    assertThat(MobRank.ELITE.getPrefix()).isEqualTo("【精英】");
    assertThat(MobRank.ELITE.getHpMultiplier()).isGreaterThan(1.0);

    assertThat(MobRank.BOSS.getDisplayName()).isEqualTo("首領");
    assertThat(MobRank.BOSS.getPrefix()).isEqualTo("【首領】");
    assertThat(MobRank.BOSS.getHpMultiplier()).isGreaterThanOrEqualTo(3.0);
  }

  @Test
  @DisplayName("2. 驗證 MobTemplate 建構預設值與舊版 BOSS 態度向下相容轉換")
  void testMobTemplateDefaultsAndBackwardCompatibility() {
    // 預設情況
    MobTemplate defaultTpl = MobTemplate.builder()
        .id("test_mob")
        .name("測試怪物")
        .build();
    assertThat(defaultTpl.rank()).isEqualTo(MobRank.NORMAL);
    assertThat(defaultTpl.isUnique()).isFalse();
    assertThat(defaultTpl.kind()).isEqualTo(MobKind.NEUTRAL);

    // 舊版寫法 kind = BOSS -> 自動轉為 rank = BOSS 且 kind = AGGRESSIVE
    MobTemplate legacyBossTpl = MobTemplate.builder()
        .id("legacy_boss")
        .name("舊版首領")
        .kind(MobKind.BOSS)
        .build();
    assertThat(legacyBossTpl.rank()).isEqualTo(MobRank.BOSS);
    assertThat(legacyBossTpl.kind()).isEqualTo(MobKind.AGGRESSIVE);
    assertThat(legacyBossTpl.isAggressive()).isTrue();
  }

  @Test
  @DisplayName("3. 驗證真實資料檔中 BOSS 與 UNIQUE 唯一命名生靈之正交標籤")
  void testRealWorldDataClassification() {
    // 畸變大師兄·宋天衡 (墨竹礦坑 BOSS, 唯一角色)
    var songOpt = TemplateRepository.findMob("boss_song_tianheng");
    assertThat(songOpt).isPresent();
    MobTemplate song = songOpt.get();
    assertThat(song.rank()).isEqualTo(MobRank.BOSS);
    assertThat(song.isUnique()).isTrue();
    assertThat(song.kind()).isEqualTo(MobKind.AGGRESSIVE);

    // 哥布林王 (銀葉村 BOSS, 唯一角色)
    var goblinKingOpt = TemplateRepository.findMob("goblin_king");
    assertThat(goblinKingOpt).isPresent();
    MobTemplate goblinKing = goblinKingOpt.get();
    assertThat(goblinKing.rank()).isEqualTo(MobRank.BOSS);
    assertThat(goblinKing.isUnique()).isTrue();
    assertThat(goblinKing.kind()).isEqualTo(MobKind.AGGRESSIVE);

    // 客棧掌櫃·福伯 (新手村 NPC, 唯一角色, 友善)
    var innkeeperOpt = TemplateRepository.findMob("innkeeper");
    assertThat(innkeeperOpt).isPresent();
    MobTemplate innkeeper = innkeeperOpt.get();
    assertThat(innkeeper.rank()).isEqualTo(MobRank.NORMAL);
    assertThat(innkeeper.isUnique()).isTrue();
    assertThat(innkeeper.kind()).isEqualTo(MobKind.FRIENDLY);

    // 新手村長·白石老人 (新手村 NPC, 唯一角色, 友善)
    var elderOpt = TemplateRepository.findMob("village_elder");
    assertThat(elderOpt).isPresent();
    MobTemplate elder = elderOpt.get();
    assertThat(elder.rank()).isEqualTo(MobRank.NORMAL);
    assertThat(elder.isUnique()).isTrue();
    assertThat(elder.kind()).isEqualTo(MobKind.FRIENDLY);

    // 普通野鼠 (大眾量產怪物, 非唯一, 敵對)
    var ratOpt = TemplateRepository.findMob("wild_rat");
    assertThat(ratOpt).isPresent();
    MobTemplate rat = ratOpt.get();
    assertThat(rat.rank()).isEqualTo(MobRank.NORMAL);
    assertThat(rat.isUnique()).isFalse();
    assertThat(rat.kind()).isEqualTo(MobKind.AGGRESSIVE);
  }

  @Test
  @DisplayName("4. 驗證 BattleEnemy 正確繼承 rank 與 isUnique，並注入【首領】前綴")
  void testBattleEnemyRankInheritance() {
    var songOpt = TemplateRepository.findMob("boss_song_tianheng");
    assertThat(songOpt).isPresent();

    BattleEnemy bossEnemy = BattleEnemy.fromTemplate("enemy_song", songOpt.get(), RowPosition.FRONT, null);
    assertThat(bossEnemy).isNotNull();
    assertThat(bossEnemy.getRank()).isEqualTo(MobRank.BOSS);
    assertThat(bossEnemy.isUnique()).isTrue();
    assertThat(bossEnemy.getName()).startsWith("【首領】");

    var ratOpt = TemplateRepository.findMob("wild_rat");
    assertThat(ratOpt).isPresent();

    BattleEnemy ratEnemy = BattleEnemy.fromTemplate("enemy_rat", ratOpt.get(), RowPosition.FRONT, null);
    assertThat(ratEnemy).isNotNull();
    assertThat(ratEnemy.getRank()).isEqualTo(MobRank.NORMAL);
    assertThat(ratEnemy.isUnique()).isFalse();
    assertThat(ratEnemy.getName()).doesNotContain("【首領】");
  }

  @Test
  @DisplayName("5. 驗證招募夥伴模板自動標記為 isUnique = true")
  void testCompanionTemplateIsUnique() {
    CompanionTemplate companion = CompanionTemplate.builder()
        .id("test_iron_cow")
        .name("鐵牛")
        .roleTitle("鐵牛力士")
        .build();

    MobTemplate mobTpl = companion.toMobTemplate();
    assertThat(mobTpl.isUnique()).isTrue();
    assertThat(mobTpl.rank()).isEqualTo(MobRank.NORMAL);
    assertThat(mobTpl.kind()).isEqualTo(MobKind.FRIENDLY);
  }
}
