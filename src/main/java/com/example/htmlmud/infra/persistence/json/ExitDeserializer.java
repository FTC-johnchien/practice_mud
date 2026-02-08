package com.example.htmlmud.infra.persistence.json;

import java.io.IOException;
import com.example.htmlmud.domain.model.template.RoomExit;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class ExitDeserializer extends JsonDeserializer<RoomExit> {

  @Override
  public RoomExit deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    // 1. 判斷當前 JSON Token 是字串還是物件
    if (p.currentToken() == JsonToken.VALUE_STRING) {
      // 【簡寫模式】 "north": "square"
      // 直接把字串當成 targetId
      return RoomExit.of(p.getText());
    } else if (p.currentToken() == JsonToken.START_OBJECT) {
      // 【詳寫模式】 "east": { "targetId": "village_elder_house", "doorName": "厚重的鐵門", "isLocked": true,
      // "isHidden": false, "keyId": "village_elder_key", "pickProof": true }
      // 讀取整個物件節點
      JsonNode node = p.getCodec().readTree(p);

      // 優先讀取 "targetId" (與 RoomExit 的 JsonProperty 一致)，若無則讀取 "targetId"
      String targetId = node.has("targetId") ? node.get("targetId").asText() : null;

      // 讀取選填欄位 (處理 null)
      String doorName = node.has("doorName") ? node.get("doorName").asText() : null;
      boolean isLocked = node.has("isLocked") && node.get("isLocked").asBoolean();
      String keyId = node.has("keyId") ? node.get("keyId").asText() : null;
      boolean isHidden = node.has("isHidden") && node.get("isHidden").asBoolean();
      boolean pickProof = node.has("pickProof") && node.get("pickProof").asBoolean();

      return new RoomExit(targetId, doorName, isLocked, keyId, isHidden, pickProof);
    }

    throw new IOException("Invalid exit format: expected String or Object");
  }
}
