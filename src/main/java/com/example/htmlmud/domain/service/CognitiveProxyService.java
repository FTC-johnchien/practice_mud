package com.example.htmlmud.domain.service;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.enums.PerceptionTier;
import com.example.htmlmud.domain.model.vo.CognitiveLens;

/**
 * 靈識認知轉譯代理服務 (Cognitive Proxy Service)
 * 核心職責：依據主角特工晶片加載率 (0~100%)，動態將設施、裝備、技能與對話過濾為三階認知表象 (純仙俠 / Glitch / 科技真相)。
 * 對齊 Level 1《GAMEPLAY_CORE_LOOPS.md §5.5》與 Level 2《M1_TOWN_HUB_AND_FACILITIES.md §2.7》
 */
@Service
public class CognitiveProxyService {

  public record ResolvedCognitiveView(
      String name,
      String description,
      boolean glitch,
      PerceptionTier tier
  ) {}

  /**
   * 解析當前晶片加載率對應之認知位階
   */
  public PerceptionTier resolveTier(int chipRate) {
    return PerceptionTier.fromRate(chipRate);
  }

  /**
   * 動態解析名稱：若有匹配透鏡則取最高滿足門檻者，否則回傳 baseName
   */
  public String resolveName(String baseName, List<CognitiveLens> lenses, int chipRate) {
    if (lenses == null || lenses.isEmpty()) {
      return baseName;
    }
    return lenses.stream()
        .filter(l -> l.minProgress() <= chipRate && l.name() != null && !l.name().isBlank())
        .max(Comparator.comparingInt(CognitiveLens::minProgress))
        .map(CognitiveLens::name)
        .orElse(baseName);
  }

  /**
   * 動態解析說明文本：若有匹配透鏡則取最高滿足門檻者，否則回傳 baseDesc
   */
  public String resolveDescription(String baseDesc, List<CognitiveLens> lenses, int chipRate) {
    if (lenses == null || lenses.isEmpty()) {
      return baseDesc;
    }
    return lenses.stream()
        .filter(l -> l.minProgress() <= chipRate && l.desc() != null && !l.desc().isBlank())
        .max(Comparator.comparingInt(CognitiveLens::minProgress))
        .map(CognitiveLens::desc)
        .orElse(baseDesc);
  }

  /**
   * 判定當前條目是否觸發 CSS Glitch 故障光斑特效
   */
  public boolean isGlitch(List<CognitiveLens> lenses, int chipRate) {
    if (lenses == null || lenses.isEmpty()) {
      return chipRate >= 30 && chipRate <= 70;
    }
    return lenses.stream()
        .filter(l -> l.minProgress() <= chipRate)
        .max(Comparator.comparingInt(CognitiveLens::minProgress))
        .map(CognitiveLens::glitch)
        .orElse(false);
  }

  /**
   * 一鍵聚合解析投影檢視
   */
  public ResolvedCognitiveView resolve(String baseName, String baseDesc, List<CognitiveLens> lenses, int chipRate) {
    String resolvedName = resolveName(baseName, lenses, chipRate);
    String resolvedDesc = resolveDescription(baseDesc, lenses, chipRate);
    boolean glitch = isGlitch(lenses, chipRate);
    PerceptionTier tier = resolveTier(chipRate);
    return new ResolvedCognitiveView(resolvedName, resolvedDesc, glitch, tier);
  }
}
