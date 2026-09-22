package com.example.htmlmud.infra.util;

public class IdUtils {

  /**
   * 解析 ID：將相對 ID 轉為絕對 ID * @param currentZoneId 當前所在的區域 ID (e.g., "newbie_village")
   * 
   * @param rawId 原始 ID (可能是 "square" 或 "dark_forest:clearing")
   * @return 完整的絕對 ID
   */
  public static String resolveId(String currentZoneId, String rawId) {
    if (rawId == null || rawId.isBlank()) {
      return null;
    }
    // 如果已經有冒號，就回傳原始值；否則加上前綴
    if (rawId.contains(":")) {
      return rawId;
    }
    return currentZoneId + ":" + rawId;
  }

  /**
   * Resolves an item reference from zone-authored data.
   *
   * <p>Global item definitions are stored without a runtime prefix, while zone-local definitions
   * are registered as {@code zoneId:itemId}. Source JSON must therefore explicitly use either
   * {@code global:itemId} or {@code zoneId:itemId}; unqualified IDs remain supported here only for
   * the zone-local fields which predate that convention.</p>
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
