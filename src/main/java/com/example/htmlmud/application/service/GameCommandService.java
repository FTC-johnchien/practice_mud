package com.example.htmlmud.application.service;

import java.util.UUID;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.CommandDispatcher;
import com.example.htmlmud.application.dto.GameRequest;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.protocol.GameCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameCommandService {

  private final ObjectMapper objectMapper;

  private final CommandDispatcher commandDispatcher;

  /**
   * 所有遊戲指令的唯一入口
   */
  public void execute(GameRequest request) {
    if (request.commandText() == null || request.commandText().isBlank()) {
      return;
    }

    String traceId = UUID.randomUUID().toString().substring(0, 8);

    // 1. 在這裡綁定 ScopedValue (JDK 25 特性)
    // 這樣在接下來的指令解析與同步校驗中，都能直接拿到玩家
    ScopedValue.where(MudContext.CURRENT_PLAYER, request.player()).run(() -> {
      try {
        // log.debug("玩家 [{}] 透過 [{}] 發送指令: {}", request.player().getId(), request.source(),
        // request.commandText());

        // 2. 轉交給 Dispatcher 進行語法解析與分發
        // 注意：這裡 dispatcher 不需要再傳 player，因為 ScopedValue 已經帶進去了
        // commandDispatcher.dispatch(request.commandText());


        // C. 解析指令 (JSON -> Record)
        GameCommand cmd = objectMapper.readValue(request.commandText(), GameCommand.class);

        // D. 裝入信封並投遞
        // 這裡不綁定 ScopedValue，因為要跨執行緒傳遞
        request.player().command(traceId, cmd);


      } catch (Exception e) {
        log.error("[{}] 執行指令時發生未預期錯誤: {}", traceId, e.getMessage());
        request.player().reply("系統發生錯誤，請稍後再試。");
      }
    });
  }
}
