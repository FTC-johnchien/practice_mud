package com.example.htmlmud.infra.gui;

import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;
import com.example.htmlmud.application.dto.GameRequest;
import com.example.htmlmud.application.service.GameCommandService;
import com.example.htmlmud.config.GuiBridge;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;
import com.example.htmlmud.protocol.JavaFXOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import netscape.javascript.JSObject;

@Slf4j
public class MudGuiLauncher extends Application {

  private static ConfigurableApplicationContext springContext;
  private static WebEngine webEngine;
  private static MudGuiLauncher instance;

  // 關鍵：定義一個類別層級的變數，確保橋接物件不會被 GC 回收
  private final JavaBridge bridge = new JavaBridge();

  @Override
  public void start(Stage stage) {
    instance = this;
    WebView webView = new WebView();
    webEngine = webView.getEngine();

    // 監聽 JS Console 訊息 (除錯用)
    webEngine.setOnAlert(event -> log.info("JS Alert: {}", event.getData()));

    // 1. 載入你的 index2.html (放在 src/main/resources/static 下)
    String url = getClass().getResource("/static/singleplayer.html").toExternalForm();
    webEngine.load(url);

    // 2. 當網頁載入完成，注入 Java 對象
    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
      if (newState == Worker.State.SUCCEEDED) {
        JSObject window = (JSObject) webEngine.executeScript("window");
        // 這裡將 Java 的後端邏輯注入，前端 JS 可以直接呼叫 javaConnector.send(...)
        window.setMember("javaConnector", bridge);
        log.info("JavaFX 與 HTML 橋接完成");

        // 關鍵：確保網頁載入完成後，才啟動玩家邏輯並推送初始狀態
        ObjectMapper objectMapper = springContext.getBean(ObjectMapper.class);
        WorldManager worldManager = springContext.getBean(WorldManager.class);
        PlayerService playerService = springContext.getBean(PlayerService.class);
        GuiBridge guiBridge = springContext.getBean(GuiBridge.class);

        Player self =
            Player.createGuest(new JavaFXOutput(objectMapper), worldManager, playerService);

        // 1. 必須將玩家實體存入 GuiBridge，否則 JavaBridge (JS 呼叫端) 會找不到玩家
        guiBridge.setPlayer(self);

        self.start();
        log.info("單機模式核心啟動完成");

        // 使用 Platform.runLater 確保第一則訊息是在 UI 準備好後才推送到位
        // Platform.runLater(() -> {
        // self.reply("[1;32m系統：單機模式核心啟動成功！[0m");
        // self.reply("歡迎進入，" + self.getName());
        // playerService.handleSendStatUpdate(self);
        // });
      }
    });

    stage.setTitle("Java MUD JDK 25 - 單機版");
    stage.setScene(new Scene(webView, 360, 800));
    stage.show();
  }

  /**
   * 封裝：執行 JavaScript 腳本
   */
  public static void executeJavaScript(String script) {
    // 必須確保在 JavaFX UI 執行緒執行
    if (Platform.isFxApplicationThread()) {
      webEngine.executeScript(script);
    } else {
      Platform.runLater(() -> webEngine.executeScript(script));
    }
  }

  // 提供給 Java 後端呼叫的方法：把訊息推送到 HTML 上
  public static void pushToBrowser(String htmlContent) {
    Platform.runLater(() -> {
      if (instance != null && instance.webEngine != null) {
        // 呼叫 index2.html 裡的 appendHtml 函數
        instance.webEngine.executeScript("appendHtml('" + htmlContent.replace("'", "\\'") + "')");
      }
    });
  }

  public static void pushToLog(String html) {
    if (html == null)
      return;

    try {
      // 重要：不要在 Java 這裡把 \n 換成 <br>，
      // 因為 ansi_up 會負責處理文字換行。
      // 我們直接將整串原始文字（含 ANSI 碼）轉成 JSON 字串。
      ObjectMapper objectMapper = springContext.getBean(ObjectMapper.class);
      // String jsonPayload = objectMapper.writeValueAsString(html);
      // 這裡我們封裝成跟 WebSocket 類似的 JSON 格式
      String jsonPayload = objectMapper.writeValueAsString(Map.of("type", "TEXT", "content", html));

      // jsonPayload 現在長這樣: "\u001b[97m=== 村莊入口 ===\u001b[0m..."
      // 它已經自帶雙引號且處理好所有跳脫，直接丟進 JS 函數。
      executeJavaScript("handleServerMessage(" + jsonPayload + ")");

    } catch (Exception e) {
      log.error("推播至 Log 失敗", e);
    }
  }

  // 在啟動 Spring 的地方呼叫此方法
  public static void setSpringContext(ConfigurableApplicationContext context) {
    springContext = context;
  }

  /**
   * 內部的橋接類別：處理前端傳來的指令
   */
  public class JavaBridge {
    public void send(String command) {
      log.info("GUI 接收到指令: {}", command);

      // 透過 Spring Context 取得 Bean 並執行
      GuiBridge guiBridge = springContext.getBean(GuiBridge.class);
      Player player = guiBridge.getPlayer();

      if (player != null) {
        GameCommandService commandService = springContext.getBean(GameCommandService.class);
        commandService.execute(new GameRequest(player, command, "GUI"));
      } else {
        log.warn("尚未綁定玩家，無法執行指令");
      }
    }
  }

  public static void main(String[] args) {
    launch(args);
  }
}
