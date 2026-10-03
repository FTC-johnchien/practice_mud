package com.example.htmlmud.domain.save.migration;

import com.fasterxml.jackson.databind.JsonNode;

public interface SaveMigration {
  int getFromVersion();
  int getToVersion();
  JsonNode migrate(JsonNode rootNode);
}
