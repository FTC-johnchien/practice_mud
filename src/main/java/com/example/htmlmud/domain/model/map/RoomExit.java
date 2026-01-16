package com.example.htmlmud.domain.model.map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

// 3. 出口 (RoomExit) - 支援單向、門鎖、隱藏
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomExit(@JsonProperty("to") int targetRoomId, // 目標房間 ID
    @JsonProperty("door") String doorName, // 門的名稱 (null 代表無門，直接通行)
    @JsonProperty("locked") boolean isLocked, // 預設是否上鎖
    @JsonProperty("key") String keyId, // 需要的鑰匙 Template ID
    @JsonProperty("hidden") boolean isHidden // 是否隱藏 (需 search)
) {
  // 為了方便 JSON 簡寫 (如果只有 targetId)，可以透過 Custom Deserializer 處理，
  // 或者在 Java 程式碼中提供一個簡易建構的靜態方法。
  public static RoomExit of(int targetId) {
    return new RoomExit(targetId, null, false, null, false);
  }
}
