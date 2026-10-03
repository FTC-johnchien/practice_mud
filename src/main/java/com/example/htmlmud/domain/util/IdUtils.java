package com.example.htmlmud.domain.util;

/**
 * 領域層 ID 解析工具
 * 負責 Zone 作用域與全域 ID 的解析與拼接，無外部依賴。
 */
public class IdUtils {

  /**
   * 解析 ID：將相對 ID 轉為絕對 ID
   *
   * @param currentZoneId 當前所在的區域 ID (e.g., "newbie_village")
   * @param rawId 原始 ID (可能是 "square" 或 "dark_forest:clearing")
   * @return 完整的絕對 ID
   */
  public static String resolveId(String currentZoneId, String rawId) {
    if (rawId == null || rawId.isBlank()) {
      return null;
    }
    if (rawId.contains(":")) {
      return rawId;
    }
    return currentZoneId + ":" + rawId;
  }

  /**
   * 解析作者定義的物品 ID 參照
   */
  public static String resolveItemReference(String currentZoneId, String rawId) {
    if (rawId == null || rawId.isBlank()) {
      return null;
    }
    if (rawId.startsWith("global:")) {
      return rawId.substring("global:".length());
    }
    return resolveId(currentZoneId, rawId);
  }
}
