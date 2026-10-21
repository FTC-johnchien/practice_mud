package com.example.htmlmud.domain.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 系統架構與防回歸自動化掃描護欄 (GUARD-01)
 * 監控代碼庫以防新增靜默 Fallback 反模式、未授權硬編碼或領域層反向依賴。
 */
class ArchitectureGuardrailsTest {

  private static final Path SRC_MAIN_JAVA = Paths.get("src", "main", "java");

  @Test
  @DisplayName("GUARD-01: 監控 new TemplateCatalog() 靜默 fallback 數量不得增長 (Ratchet 棘輪基準)")
  void testTemplateCatalogFallbackRatchet() throws IOException {
    if (!Files.exists(SRC_MAIN_JAVA)) {
      return; // 確保在非標準根目錄執行時安全略過
    }

    List<String> violations = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(SRC_MAIN_JAVA)) {
      stream.filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java"))
          .forEach(file -> {
            try {
              List<String> lines = Files.readAllLines(file);
              for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("new TemplateCatalog()")) {
                  violations.add(file.getFileName() + ":" + (i + 1) + " -> " + line.trim());
                }
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }

    // 歷史基線已自 35 -> 33 處收斂至 26 處，後續 Phase 1/2 清理時逐步縮小此閾值直至歸零
    assertThat(violations.size())
        .as("禁止新增 new TemplateCatalog() 靜默 fallback，發現新增呼叫：\n" + String.join("\n", violations))
        .isLessThanOrEqualTo(26);
  }

  @Test
  @DisplayName("GUARD-01: 監控 GameConfig.getInstance() 靜態存取不得增長")
  void testGameConfigGetInstanceRatchet() throws IOException {
    if (!Files.exists(SRC_MAIN_JAVA)) {
      return;
    }

    List<String> violations = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(SRC_MAIN_JAVA)) {
      stream.filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java"))
          .filter(p -> !p.toString().endsWith("GameConfig.java")) // 排除自身定義
          .forEach(file -> {
            try {
              List<String> lines = Files.readAllLines(file);
              for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.contains("GameConfig.getInstance()")) {
                  violations.add(file.getFileName() + ":" + (i + 1) + " -> " + line.trim());
                }
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }

    // 歷史基線已自 12 處收緊至 3 處 (僅餘 SaveGameService, PartyService, DungeonCommand 建構子安全防禦)，禁止新增
    assertThat(violations.size())
        .as("禁止新增 GameConfig.getInstance() 靜態呼叫，應使用 Spring 建構子依賴注入：\n" + String.join("\n", violations))
        .isLessThanOrEqualTo(3);
  }

  @Test
  @DisplayName("GUARD-01: 領域層 (domain) 絕對禁止依賴表現層/網路層 Controller 或 Servlet")
  void testDomainDoesNotDependOnWebOrControllers() throws IOException {
    Path domainPath = SRC_MAIN_JAVA.resolve("com").resolve("example").resolve("htmlmud").resolve("domain");
    if (!Files.exists(domainPath)) {
      return;
    }

    List<String> illegalImports = new ArrayList<>();
    try (Stream<Path> stream = Files.walk(domainPath)) {
      stream.filter(Files::isRegularFile)
          .filter(p -> p.toString().endsWith(".java"))
          .forEach(file -> {
            try {
              List<String> lines = Files.readAllLines(file);
              for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("import") && (line.contains(".controller.") || line.contains("jakarta.servlet"))) {
                  illegalImports.add(file.getFileName() + ":" + (i + 1) + " -> " + line);
                }
              }
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
    }

    assertThat(illegalImports)
        .as("領域層 Clean Architecture 邊界違規：發現領域層引入 Web/Servlet：\n" + String.join("\n", illegalImports))
        .isEmpty();
  }

  @Test
  @DisplayName("GUARD-02: 核心戰鬥迴圈 (DrpgCombatLoop) 絕對禁止硬編碼特例中文名稱字串比對 (Data-Driven Guardrail)")
  void testNoHardcodedSkillOrEntityNamesInCombatLoop() throws IOException {
    Path combatLoopPath = SRC_MAIN_JAVA.resolve("com").resolve("example").resolve("htmlmud")
        .resolve("domain").resolve("dungeon").resolve("battle").resolve("DrpgCombatLoop.java");
    if (!Files.exists(combatLoopPath)) {
      return;
    }

    List<String> forbiddenSnippets = List.of(
        ".contains(\"解毒\")",
        ".contains(\"清心\")",
        ".contains(\"鐵牛\")",
        ".contains(\"天劍\")"
    );

    List<String> violations = new ArrayList<>();
    List<String> lines = Files.readAllLines(combatLoopPath);
    for (int i = 0; i < lines.size(); i++) {
      String line = lines.get(i);
      for (String snippet : forbiddenSnippets) {
        if (line.contains(snippet)) {
          violations.add("DrpgCombatLoop.java:" + (i + 1) + " -> " + line.trim());
        }
      }
    }

    assertThat(violations)
        .as("🚨 違反專案第一紅線：DrpgCombatLoop 出現硬編碼中文特例比對，必須改用 tags (如 CLEANSE) 或 Model 資料驅動！\n" + String.join("\n", violations))
        .isEmpty();
  }
}
