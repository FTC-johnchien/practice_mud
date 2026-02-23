package com.example.htmlmud.protocol;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MudMessage<T> {
  private String type; // 訊息分類 (ROOM_DESC, COMBAT_LOG, etc.)
  private T payload; // 結構化數據
  private String rawText; // 傳統文字 (備用，給不支援 UI 的地方顯示)
  @Builder.Default
  private long timestamp = System.currentTimeMillis();
}
