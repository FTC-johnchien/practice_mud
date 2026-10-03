package com.example.htmlmud.domain.save.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.save.model.SaveData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

/**
 * 存檔版本遷移管線
 * 負責將舊版本存檔逐步升級至當前系統的 CURRENT_SCHEMA_VERSION
 */
@Slf4j
@Component
public class SaveMigrationPipeline {

  private final List<SaveMigration> migrations = new ArrayList<>();

  public SaveMigrationPipeline() {
    registerDefaultMigrations();
  }

  public SaveMigrationPipeline(List<SaveMigration> customMigrations) {
    if (customMigrations != null) {
      migrations.addAll(customMigrations);
    }
    registerDefaultMigrations();
  }

  private void registerDefaultMigrations() {
    // 0 -> 1: Legacy (無 schemaVersion) 遷移至 Version 1
    if (migrations.stream().noneMatch(m -> m.getFromVersion() == 0 && m.getToVersion() == 1)) {
      migrations.add(new SaveMigration() {
        @Override
        public int getFromVersion() {
          return 0;
        }

        @Override
        public int getToVersion() {
          return 1;
        }

        @Override
        public JsonNode migrate(JsonNode rootNode) {
          if (rootNode instanceof ObjectNode objNode) {
            log.info("Migrating legacy save (v0) to schemaVersion 1");
            objNode.put("schemaVersion", 1);
            if (!objNode.has("openedChests")) {
              objNode.putArray("openedChests");
            }
            if (!objNode.has("playtimeSeconds")) {
              objNode.put("playtimeSeconds", 0L);
            }
          }
          return rootNode;
        }
      });
    }

    migrations.sort(Comparator.comparingInt(SaveMigration::getFromVersion));
  }

  /**
   * 取得存檔 JsonNode 的版本號 (若無 schemaVersion 欄位則回傳 0)
   */
  public int extractVersion(JsonNode rootNode) {
    if (rootNode != null && rootNode.has("schemaVersion")) {
      return rootNode.get("schemaVersion").asInt(0);
    }
    return 0;
  }

  /**
   * 依序流經遷移器，升級至 targetVersion
   */
  public JsonNode migrateToLatest(JsonNode rootNode) {
    if (rootNode == null) return null;

    int currentVer = extractVersion(rootNode);
    int targetVer = SaveData.CURRENT_SCHEMA_VERSION;

    if (currentVer >= targetVer) {
      return rootNode;
    }

    log.info("Starting save data migration from version {} to {}", currentVer, targetVer);
    JsonNode currentNode = rootNode;

    while (currentVer < targetVer) {
      final int ver = currentVer;
      SaveMigration step = migrations.stream()
          .filter(m -> m.getFromVersion() == ver)
          .findFirst()
          .orElse(null);

      if (step == null) {
        log.warn("No migration path found for save version {} -> {}. Halting migration.", currentVer, targetVer);
        break;
      }

      currentNode = step.migrate(currentNode);
      currentVer = step.getToVersion();
      log.info("Successfully migrated save data to version {}", currentVer);
    }

    return currentNode;
  }
}

