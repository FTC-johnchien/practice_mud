package com.example.htmlmud.domain.context;

import com.example.htmlmud.domain.actor.PlayerActor;
import com.fasterxml.jackson.databind.ObjectMapper;

public class MudContext {
  // 1. 當前操作的玩家 Actor (最重要的)
  public static final ScopedValue<PlayerActor> CURRENT_PLAYER = ScopedValue.newInstance();

  // 2. 當前的 Trace ID (除錯用)
  public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

  public static final ScopedValue<ObjectMapper> OBJECT_MAPPER = ScopedValue.newInstance();

  // 輔助方法：取得當前玩家，如果沒設定(例如系統背景作業)則拋出異常
  public static PlayerActor currentPlayer() {
    if (!CURRENT_PLAYER.isBound()) {
      throw new IllegalStateException("當前不在玩家操作的 Context 中！");
    }
    return CURRENT_PLAYER.get();
  }

  // 輔助方法：取得當前 TraceId，沒綁定就回傳 "SYSTEM"
  public static String traceId() {
    return TRACE_ID.isBound() ? TRACE_ID.get() : "UNKNOWN";
  }

}
