package com.example.htmlmud.infra.persistence.json;

import java.io.IOException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

public class RoomDescriptionDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.currentToken() == JsonToken.VALUE_STRING) {
      return p.getText();
    } else if (p.currentToken() == JsonToken.START_OBJECT) {
      JsonNode node = p.getCodec().readTree(p);
      if (node.has("default")) {
        return node.get("default").asText();
      }
      return node.toString();
    }
    return p.getValueAsString();
  }
}
