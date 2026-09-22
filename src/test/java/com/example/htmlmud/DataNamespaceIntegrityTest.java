package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Guards the source-data ID contract before WorldManager normalizes IDs at load time. */
class DataNamespaceIntegrityTest {

  private static final Path DATA_ROOT = Path.of("src", "main", "resources", "data");
  private static final ObjectMapper JSON = new ObjectMapper();

  @Test
  @DisplayName("物品定義 ID 在各自作用域中唯一，且不得自行包含 namespace")
  void itemDefinitionIdsAreUniqueWithinTheirScope() throws IOException {
    Set<String> globalIds = readItemIds(DATA_ROOT.resolve("global/items"), "global");
    assertFalse(globalIds.isEmpty(), "global item definitions must not be empty");

    try (Stream<Path> zones = Files.list(DATA_ROOT.resolve("zones"))) {
      for (Path zone : zones.filter(Files::isDirectory).toList()) {
        readItemIds(zone.resolve("items.json"), zone.getFileName().toString());
      }
    }
  }

  @Test
  @DisplayName("區域物品參照必須明確使用 global:<id> 或 <zone>:<id>，並指向已定義物品")
  void zoneItemReferencesUseExplicitResolvableNamespaces() throws IOException {
    Set<String> globalIds = readItemIds(DATA_ROOT.resolve("global/items"), "global");
    Map<String, Set<String>> zoneItemIds = readZoneItemIds();

    try (Stream<Path> zones = Files.list(DATA_ROOT.resolve("zones"))) {
      for (Path zone : zones.filter(Files::isDirectory).toList()) {
        String zoneId = zone.getFileName().toString();
        validateMobItemReferences(zoneId, zone.resolve("mobs.json"), globalIds, zoneItemIds);
        validateShopItemReferences(zoneId, zone.resolve("shops.json"), globalIds, zoneItemIds);
        validateLockedExitKeys(zoneId, zone.resolve("rooms.json"), globalIds, zoneItemIds);
      }
    }
  }

  private static Set<String> readItemIds(Path path, String scope) throws IOException {
    Set<String> ids = new HashSet<>();
    if (!Files.exists(path)) return ids;

    try (Stream<Path> files = Files.isDirectory(path) ? Files.walk(path) : Stream.of(path)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
        JsonNode root = JSON.readTree(file.toFile());
        assertTrue(root.isArray(), () -> file + " must contain an item array");
        for (JsonNode item : root) {
          String id = requiredText(item, "id", file);
          assertFalse(id.contains(":"), () -> file + " definition ID must be local: " + id);
          assertTrue(ids.add(id), () -> "duplicate item ID in " + scope + ": " + id);
        }
      }
    }
    return ids;
  }

  private static Map<String, Set<String>> readZoneItemIds() throws IOException {
    Map<String, Set<String>> ids = new HashMap<>();
    try (Stream<Path> zones = Files.list(DATA_ROOT.resolve("zones"))) {
      for (Path zone : zones.filter(Files::isDirectory).toList()) {
        ids.put(zone.getFileName().toString(), readItemIds(zone.resolve("items.json"), zone.getFileName().toString()));
      }
    }
    return ids;
  }

  private static void validateMobItemReferences(String zoneId, Path file, Set<String> globalIds,
      Map<String, Set<String>> zoneItemIds) throws IOException {
    if (!Files.exists(file)) return;
    for (JsonNode mob : JSON.readTree(file.toFile())) {
      for (JsonNode loot : mob.path("loot")) {
        assertItemReference(zoneId, requiredText(loot, "itemId", file), file, globalIds, zoneItemIds);
      }
      JsonNode equipment = mob.path("equipment");
      if (equipment.isObject()) {
        for (JsonNode itemId : equipment) {
          assertItemReference(zoneId, itemId.asText(), file, globalIds, zoneItemIds);
        }
      }
    }
  }

  private static void validateShopItemReferences(String zoneId, Path file, Set<String> globalIds,
      Map<String, Set<String>> zoneItemIds) throws IOException {
    if (!Files.exists(file)) return;
    for (JsonNode shop : JSON.readTree(file.toFile())) {
      for (JsonNode good : shop.path("goods")) {
        assertItemReference(zoneId, requiredText(good, "templateId", file), file, globalIds, zoneItemIds);
      }
    }
  }

  private static void validateLockedExitKeys(String zoneId, Path file, Set<String> globalIds,
      Map<String, Set<String>> zoneItemIds) throws IOException {
    if (!Files.exists(file)) return;
    for (JsonNode room : JSON.readTree(file.toFile())) {
      JsonNode exits = room.path("exits");
      if (!exits.isObject()) continue;
      for (JsonNode exit : exits) {
        if (exit.path("isLocked").asBoolean()) {
          assertItemReference(zoneId, requiredText(exit, "keyId", file), file, globalIds, zoneItemIds);
        }
      }
    }
  }

  private static void assertItemReference(String currentZone, String reference, Path source,
      Set<String> globalIds, Map<String, Set<String>> zoneItemIds) {
    String[] parts = reference.split(":", -1);
    assertTrue(parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank(),
        () -> source + " item reference must be global:<id> or <zone>:<id>: " + reference);
    if ("global".equals(parts[0])) {
      assertTrue(globalIds.contains(parts[1]), () -> source + " references unknown global item: " + reference);
      return;
    }
    assertTrue(zoneItemIds.containsKey(parts[0]), () -> source + " references unknown zone: " + reference);
    assertTrue(zoneItemIds.get(parts[0]).contains(parts[1]), () -> source + " references unknown zone item: " + reference);
  }

  private static String requiredText(JsonNode node, String field, Path source) {
    JsonNode value = node.get(field);
    assertNotNull(value, () -> source + " is missing " + field);
    assertTrue(value.isTextual() && !value.asText().isBlank(), () -> source + " has blank " + field);
    return value.asText();
  }
}
