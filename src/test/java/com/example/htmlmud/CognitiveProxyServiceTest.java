package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.example.htmlmud.domain.model.enums.PerceptionTier;
import com.example.htmlmud.domain.model.vo.CognitiveLens;
import com.example.htmlmud.domain.service.CognitiveProxyService;
import com.example.htmlmud.domain.service.CognitiveProxyService.ResolvedCognitiveView;

class CognitiveProxyServiceTest {

  private CognitiveProxyService service;

  @BeforeEach
  void setUp() {
    service = new CognitiveProxyService();
  }

  @Test
  @DisplayName("認知透鏡三階門檻邊界值校驗 (<30% 純仙俠, 30%~70% 微光Glitch, >70% 科技覺醒)")
  void testResolveTier_boundaries() {
    assertEquals(PerceptionTier.IGNORANT_XIANXIA, service.resolveTier(0));
    assertEquals(PerceptionTier.IGNORANT_XIANXIA, service.resolveTier(12));
    assertEquals(PerceptionTier.IGNORANT_XIANXIA, service.resolveTier(29));

    assertEquals(PerceptionTier.FRACTURED_GLITCH, service.resolveTier(30));
    assertEquals(PerceptionTier.FRACTURED_GLITCH, service.resolveTier(50));
    assertEquals(PerceptionTier.FRACTURED_GLITCH, service.resolveTier(70));

    assertEquals(PerceptionTier.TRANSCENDED_TECH, service.resolveTier(71));
    assertEquals(PerceptionTier.TRANSCENDED_TECH, service.resolveTier(99));
    assertEquals(PerceptionTier.TRANSCENDED_TECH, service.resolveTier(100));
  }

  @Test
  @DisplayName("三階透鏡動態投影：名稱、描述與 Glitch 特效標籤隨晶片加載率切換")
  void testResolve_threeTierProgression() {
    List<CognitiveLens> lenses = List.of(
        CognitiveLens.builder()
            .minProgress(0)
            .name("百煉堂")
            .desc("宗門神兵古鐵百煉堂，長老爐火純青，傳承神兵開光與法寶注靈之法。")
            .glitch(false)
            .build(),
        CognitiveLens.builder()
            .minProgress(30)
            .name("百煉堂 [微光擾動]")
            .desc("爐火深處隱約閃爍著冷色金屬電弧與幾丁質殘骸，傳承神兵開光與生化零件注靈。")
            .glitch(true)
            .build(),
        CognitiveLens.builder()
            .minProgress(70)
            .name("工程師生化兵器組裝台")
            .desc("【千星科技掃描】工程師遺留的自動化生化兵器組裝基座，可分解外神殘骸與重構軍用義體。")
            .glitch(true)
            .build()
    );

    // 1. 序章 12% (純仙俠表象，無 Glitch)
    ResolvedCognitiveView tier1 = service.resolve("未知設施", "無描述", lenses, 12);
    assertEquals("百煉堂", tier1.name());
    assertEquals("宗門神兵古鐵百煉堂，長老爐火純青，傳承神兵開光與法寶注靈之法。", tier1.description());
    assertFalse(tier1.glitch());
    assertEquals(PerceptionTier.IGNORANT_XIANXIA, tier1.tier());

    // 2. 中期 45% (知見初裂，觸發微光 Glitch)
    ResolvedCognitiveView tier2 = service.resolve("未知設施", "無描述", lenses, 45);
    assertEquals("百煉堂 [微光擾動]", tier2.name());
    assertEquals("爐火深處隱約閃爍著冷色金屬電弧與幾丁質殘骸，傳承神兵開光與生化零件注靈。", tier2.description());
    assertTrue(tier2.glitch());
    assertEquals(PerceptionTier.FRACTURED_GLITCH, tier2.tier());

    // 3. 後期 85% (科技覺醒，揭示真容)
    ResolvedCognitiveView tier3 = service.resolve("未知設施", "無描述", lenses, 85);
    assertEquals("工程師生化兵器組裝台", tier3.name());
    assertEquals("【千星科技掃描】工程師遺留的自動化生化兵器組裝基座，可分解外神殘骸與重構軍用義體。", tier3.description());
    assertTrue(tier3.glitch());
    assertEquals(PerceptionTier.TRANSCENDED_TECH, tier3.tier());
  }

  @Test
  @DisplayName("當無自訂透鏡配置時安全回退至 Base 預設值")
  void testFallback_whenNoLenses() {
    ResolvedCognitiveView view12 = service.resolve("回春客棧", "休息調息", null, 12);
    assertEquals("回春客棧", view12.name());
    assertEquals("休息調息", view12.description());
    assertFalse(view12.glitch());

    // 中期過渡期無配置時仍提供合理的 Glitch 提示
    ResolvedCognitiveView view40 = service.resolve("回春客棧", "休息調息", List.of(), 40);
    assertEquals("回春客棧", view40.name());
    assertEquals("休息調息", view40.description());
    assertTrue(view40.glitch());
  }
}
