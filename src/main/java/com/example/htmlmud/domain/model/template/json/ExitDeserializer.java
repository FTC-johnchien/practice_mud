package com.example.htmlmud.domain.model.template.json;

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
    if (p.currentToken() == JsonToken.VALUE_STRING) {
      return RoomExit.of(p.getText());
    } else if (p.currentToken() == JsonToken.START_OBJECT) {
      JsonNode node = p.getCodec().readTree(p);

      String targetId = node.has("targetId") ? node.get("targetId").asText()
          : (node.has("targetRoomId") ? node.get("targetRoomId").asText() : null);

      String doorName = node.has("doorName") ? node.get("doorName").asText() : null;
      boolean isLocked = node.has("isLocked") && node.get("isLocked").asBoolean();
      String keyId = node.has("keyId") ? node.get("keyId").asText() : null;
      boolean isHidden = node.has("isHidden") && node.get("isHidden").asBoolean();
      boolean pickProof = node.has("pickProof") && node.get("pickProof").asBoolean();
      String actionCommand = node.has("actionCommand") ? node.get("actionCommand").asText() : null;

      return new RoomExit(targetId, doorName, isLocked, keyId, isHidden, pickProof, actionCommand);
    }

    throw new IOException("Invalid exit format: expected String or Object");
  }
}
