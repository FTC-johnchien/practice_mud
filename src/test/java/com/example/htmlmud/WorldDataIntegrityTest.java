package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import com.example.htmlmud.domain.model.config.EquipmentProp;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ResourceType;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.template.ZoneTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;

@SpringBootTest
class WorldDataIntegrityTest {

  @Test
  @DisplayName("驗證 4 大核心區域 (newbie_village, mozhu_mines, snow, silverleaf) 均成功載入")
  void testAllZonesLoaded() {
    assertTrue(TemplateRepository.findZone("newbie_village").isPresent(), "newbie_village 應載入");
    assertTrue(TemplateRepository.findZone("mozhu_mines").isPresent(), "mozhu_mines 應載入");
    assertTrue(TemplateRepository.findZone("snow").isPresent(), "snow 應載入");
    assertTrue(TemplateRepository.findZone("silverleaf").isPresent(), "silverleaf 應載入");
  }

  @Test
  @DisplayName("驗證雪亭鎮 (snow) 與銀葉村 (silverleaf) 地圖拓撲與房間存在")
  void testSnowAndSilverleafRooms() {
    assertTrue(TemplateRepository.findRoom("snow:snow_gate").isPresent(), "snow_gate 應存在");
    assertTrue(TemplateRepository.findRoom("snow:snow_square").isPresent(), "snow_square 應存在");
    assertTrue(TemplateRepository.findRoom("snow:snow_hall").isPresent(), "snow_hall 應存在");
    assertTrue(TemplateRepository.findRoom("snow:snow_warehouse").isPresent(), "snow_warehouse 應存在");

    assertTrue(TemplateRepository.findRoom("silverleaf:sl_gate").isPresent(), "sl_gate 應存在");
    assertTrue(TemplateRepository.findRoom("silverleaf:sl_square").isPresent(), "sl_square 應存在");
    assertTrue(TemplateRepository.findRoom("silverleaf:sl_hidden_cellar").isPresent(), "sl_hidden_cellar 應存在");
  }

  @Test
  @DisplayName("驗證全地圖房間出口 (Exits) 指向的目標房間皆存在 (無懸空出口)")
  void testAllRoomExitsExist() {
    Map<String, RoomTemplate> rooms = TemplateRepository.getRoomTemplates();
    assertFalse(rooms.isEmpty(), "房間模板清單不應為空");

    for (RoomTemplate room : rooms.values()) {
      if (room.exits() != null) {
        for (Map.Entry<String, RoomExit> entry : room.exits().entrySet()) {
          String targetId = entry.getValue().targetRoomId();
          assertNotNull(targetId, "房間 " + room.id() + " 的出口 " + entry.getKey() + " targetRoomId 不得為 null");
          assertTrue(TemplateRepository.findRoom(targetId).isPresent(),
              "房間 [" + room.id() + "] 的出口 [" + entry.getKey() + "] 指向不存在的房間 [" + targetId + "]");
        }
      }
    }
  }

  @Test
  @DisplayName("驗證技能分類與怪物基礎技能映射完整性")
  void testSkillCategoryAndMobSkillsIntegrity() {
    // 預設基礎拳法
    assertEquals("basic_fist", TemplateRepository.getDefaultSkillId(SkillCategory.UNARMED));
    assertEquals("mob_basic_dodge", TemplateRepository.getMobDefaultSkillId(SkillCategory.DODGE));
    assertEquals("mob_basic_parry", TemplateRepository.getMobDefaultSkillId(SkillCategory.PARRY));

    assertTrue(TemplateRepository.findSkill("basic_fist").isPresent(), "basic_fist 技能模板應存在");
    assertTrue(TemplateRepository.findSkill("mob_basic_dodge").isPresent(), "mob_basic_dodge 技能模板應存在");
    assertTrue(TemplateRepository.findSkill("mob_basic_parry").isPresent(), "mob_basic_parry 技能模板應存在");
    assertTrue(TemplateRepository.findSkill("taichi_fist").isPresent(), "taichi_fist 技能模板應存在");
    assertTrue(TemplateRepository.findSkill("lion_roar").isPresent(), "lion_roar 技能模板應存在");
  }

  @Test
  @DisplayName("驗證種族 (Races) 完整性與自然戰鬥技能")
  void testRacesIntegrity() {
    Optional<RaceTemplate> human = TemplateRepository.findRace("human");
    assertTrue(human.isPresent(), "human 種族應存在");

    Optional<RaceTemplate> wolf = TemplateRepository.findRace("wolf");
    assertTrue(wolf.isPresent(), "wolf 種族應存在");
    assertNotNull(wolf.get().combat(), "wolf 應定義 combat");

    Optional<RaceTemplate> rat = TemplateRepository.findRace("rat");
    assertTrue(rat.isPresent(), "rat 種族應存在");

    Optional<RaceTemplate> humanoid = TemplateRepository.findRace("humanoid");
    assertTrue(humanoid.isPresent(), "humanoid 種族應存在");
  }

  @Test
  @DisplayName("驗證修復 Bug 1: ResourceType.COIN 扣除不會覆寫 age")
  void testCoinDeductionBugFix() {
    LivingStats stats = new LivingStats();
    stats.setAge(25);
    stats.setCoin(100);

    ResourceType.COIN.deduct(stats, 30);

    assertEquals(70, stats.getCoin(), "金幣應扣除為 70");
    assertEquals(25, stats.getAge(), "年齡 (age) 絕不能被金幣扣除覆寫！");
  }

  @Test
  @DisplayName("驗證修復 Bug 2: EquipmentProp.getDamageSource 正確傳入 hitRate")
  void testEquipmentPropHitRateBugFix() {
    EquipmentProp prop = EquipmentProp.builder()
        .slot(EquipmentSlot.MAIN_HAND)
        .attackVerb("劈")
        .minDamage(10)
        .maxDamage(20)
        .attackSpeed(1800)
        .hitRate(15)
        .maxDurability(100)
        .build();

    DamageSource ds = prop.getDamageSource("鋼刀");
    assertEquals(1800, ds.attackSpeed(), "攻速應為 1800ms");
    assertEquals(15, ds.hitRate(), "命中率應為 15，不得誤傳為 attackSpeed！");
  }

  @Test
  @DisplayName("驗證 MessageUtil 訊息佔位符 ($N 主語, $n 受詞) 視角動態替換與 Null-Safety")
  void testMessageUtilFormatting() {
    com.example.htmlmud.domain.actor.impl.Player player = org.mockito.Mockito.mock(com.example.htmlmud.domain.actor.impl.Player.class);
    com.example.htmlmud.domain.actor.impl.Player observer = org.mockito.Mockito.mock(com.example.htmlmud.domain.actor.impl.Player.class);
    com.example.htmlmud.domain.actor.impl.Mob mob = org.mockito.Mockito.mock(com.example.htmlmud.domain.actor.impl.Mob.class);

    LivingStats playerStats = new LivingStats();
    playerStats.gender = com.example.htmlmud.domain.model.enums.Gender.MALE;
    LivingStats observerStats = new LivingStats();
    LivingStats mobStats = new LivingStats();
    mobStats.gender = com.example.htmlmud.domain.model.enums.Gender.MALE;

    org.mockito.Mockito.when(player.getId()).thenReturn("p-1");
    org.mockito.Mockito.when(player.getName()).thenReturn("張無忌");
    org.mockito.Mockito.when(player.getStats()).thenReturn(playerStats);

    org.mockito.Mockito.when(observer.getId()).thenReturn("p-2");
    org.mockito.Mockito.when(observer.getName()).thenReturn("路人甲");
    org.mockito.Mockito.when(observer.getStats()).thenReturn(observerStats);

    org.mockito.Mockito.when(mob.getId()).thenReturn("m-1");
    org.mockito.Mockito.when(mob.getName()).thenReturn("少林武僧");
    org.mockito.Mockito.when(mob.getStats()).thenReturn(mobStats);

    String shoutTemplate = "$N對著$n喝道﹕「臭賊﹗今日不是你死就是我活﹗」";
    String attackTemplate = "$N像發瘋似地用身體猛力衝撞$n！";

    // 1. 玩家視角：玩家發起對武僧的攻擊
    String playerView = com.example.htmlmud.domain.service.MessageUtil.format(shoutTemplate, player, mob, player);
    assertEquals("你對著少林武僧喝道﹕「臭賊﹗今日不是你死就是我活﹗」", playerView, "$N 應替換為「你」，$n 替換為「少林武僧」");

    // 2. 玩家視角：武僧攻擊玩家
    String mobAttacksPlayerView = com.example.htmlmud.domain.service.MessageUtil.format(attackTemplate, mob, player, player);
    assertEquals("少林武僧像發瘋似地用身體猛力衝撞你！", mobAttacksPlayerView, "$N 應替換為「少林武僧」，$n 替換為「你」");

    // 3. 旁觀者視角 (路人甲看武僧打張無忌)
    String observerView = com.example.htmlmud.domain.service.MessageUtil.format(attackTemplate, mob, player, observer);
    assertEquals("少林武僧像發瘋似地用身體猛力衝撞張無忌！", observerView, "旁觀者視角下應全部顯示角色本名");

    // 4. Null-Safety 驗證：即使性別為 null 也不拋出異常，且不遺留 raw $N 或 $n
    playerStats.gender = null;
    String nullSafetyView = com.example.htmlmud.domain.service.MessageUtil.format(shoutTemplate, player, mob, player);
    assertEquals("你對著少林武僧喝道﹕「臭賊﹗今日不是你死就是我活﹗」", nullSafetyView, "性別為 null 時應安全 fallback 至「你」，不能漏出 $N");
  }
}
