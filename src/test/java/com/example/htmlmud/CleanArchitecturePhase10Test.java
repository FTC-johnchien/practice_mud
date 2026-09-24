package com.example.htmlmud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.enums.ItemActionType;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.port.AuthenticationPort;
import com.example.htmlmud.domain.port.ClientSessionManagerPort;
import com.example.htmlmud.domain.port.CommandDispatcherPort;
import com.example.htmlmud.domain.port.WorldEntityFactoryPort;

@SpringBootTest
public class CleanArchitecturePhase10Test {

  @Autowired
  private WorldEntityFactoryPort worldEntityFactory;

  @Autowired
  private CommandDispatcherPort commandDispatcher;

  @Autowired
  private ClientSessionManagerPort clientSessionManager;

  @Autowired
  private AuthenticationPort authenticationPort;

  @Test
  @DisplayName("1. 領域邊界檢驗：Domain 層 100% 杜絕反向依賴 Application 與 WebSocket")
  void testDomainHasZeroApplicationOrWebSocketImports() throws IOException {
    Path domainDir = Paths.get("src", "main", "java", "com", "example", "htmlmud", "domain");
    assertTrue(Files.exists(domainDir), "domain 目錄必須存在！");

    try (Stream<Path> paths = Files.walk(domainDir)) {
      List<String> violations = paths
          .filter(p -> p.toString().endsWith(".java"))
          .flatMap(p -> {
            try {
              return Files.lines(p)
                  .filter(line -> line.trim().startsWith("import com.example.htmlmud.application.")
                               || line.trim().startsWith("import org.springframework.web.socket."))
                  .map(line -> p.getFileName() + ": " + line.trim());
            } catch (IOException e) {
              return Stream.empty();
            }
          })
          .toList();

      assertTrue(violations.isEmpty(),
          "Domain 層不得存在任何對 Application 或 WebSocket 的反向依賴！違規項目: " + violations);
    }
  }

  @Test
  @DisplayName("2. 輸出埠口反轉：四組 Output Ports 均正常被 Spring 注入並實現依賴反轉")
  void testOutputPortsInjectedAndFunctional() {
    assertNotNull(worldEntityFactory, "WorldEntityFactoryPort 必須被注入！");
    assertNotNull(commandDispatcher, "CommandDispatcherPort 必須被注入！");
    assertNotNull(clientSessionManager, "ClientSessionManagerPort 必須被注入！");
    assertNotNull(authenticationPort, "AuthenticationPort 必須被注入！");

    // 驗證工廠埠口運作 (測試已存在的安全房實體)
    assertNotNull(worldEntityFactory.createRoom("newbie_village:inn"), "埠口可正常生產領域房間實體！");
  }

  @Test
  @DisplayName("3. 4 方向模型收斂：Direction 正交位移、旋轉、符號與字串解析正確")
  void testDirectionFourWayMechanisms() {
    // 轉向檢定
    assertEquals(Direction.WEST, Direction.NORTH.turnLeft());
    assertEquals(Direction.EAST, Direction.NORTH.turnRight());
    assertEquals(Direction.SOUTH, Direction.NORTH.opposite());
    assertEquals(Direction.NORTH, Direction.SOUTH.getOpposite());

    // 座標位移檢定
    assertEquals(0, Direction.NORTH.getDx());
    assertEquals(-1, Direction.NORTH.getDy());
    assertEquals(1, Direction.EAST.getDx());
    assertEquals(0, Direction.EAST.getDy());

    // 符號與中文字
    assertEquals("▲", Direction.NORTH.getArrow());
    assertEquals("北方", Direction.NORTH.getDisplayName());
    assertEquals("北方", Direction.NORTH.getChineseName());

    // 方向指令縮寫解析 (MUD 標準短代碼: n=North, s=South, w=West, e=East)
    assertEquals(Direction.NORTH, Direction.parse("n"));
    assertEquals(Direction.SOUTH, Direction.parse("s"));
    assertEquals(Direction.WEST, Direction.parse("w"));
    assertEquals(Direction.EAST, Direction.parse("e"));
    assertEquals(Direction.NORTH, Direction.parse("north"));
  }

  @Test
  @DisplayName("4. 戰鬥資源收斂：CombatResourceType 與 LivingStats 數值安全扣除與邊界防禦")
  void testCombatResourceTypeMechanisms() {
    LivingStats stats = new LivingStats();
    stats.setHp(100);
    stats.setMp(50);
    stats.setStamina(80);

    assertEquals(100, CombatResourceType.HP.getCurrent(stats));
    assertEquals(50, CombatResourceType.MP.getCurrent(stats));
    assertEquals(80, CombatResourceType.SP.getCurrent(stats));

    // 扣除並驗證非負防禦
    CombatResourceType.HP.deduct(stats, 30);
    assertEquals(70, stats.getHp());

    CombatResourceType.HP.deduct(stats, 200);
    assertEquals(0, stats.getHp(), "氣血扣除不可為負數！");

    CombatResourceType.MP.deduct(stats, 60);
    assertEquals(0, stats.getMp(), "真元扣除不可為負數！");
  }

  @Test
  @DisplayName("5. 物品行為獨立：ItemActionType 獨立於 Buff 體系")
  void testItemActionTypeConsumableIsolation() {
    assertEquals(ItemActionType.HEAL_HP, ItemActionType.fromString("HEAL_HP"));
    assertEquals(ItemActionType.RESTORE_MP, ItemActionType.fromString("RESTORE_MP"));
    assertEquals(ItemActionType.RESTORE_SAN, ItemActionType.fromString("RESTORE_SAN"));
    assertEquals(ItemActionType.LEARN_SKILL, ItemActionType.fromString("LEARN_SKILL"));
    assertEquals("恢復氣血", ItemActionType.HEAL_HP.getDescription());
  }
}
