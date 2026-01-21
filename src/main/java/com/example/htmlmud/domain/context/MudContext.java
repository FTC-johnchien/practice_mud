package com.example.htmlmud.domain.context;

import com.example.htmlmud.domain.actor.impl.PlayerActor;

public class MudContext {

  // 1. 當前操作的玩家 Actor (最重要的)
  public static final ScopedValue<PlayerActor> CURRENT_PLAYER = ScopedValue.newInstance();

  // 2. 當前的 Trace ID (除錯用)
  public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

  // 輔助方法：取得當前玩家，如果沒設定(例如系統背景作業)則拋出異常
  public static PlayerActor currentPlayer() {
    return CURRENT_PLAYER
        .orElseThrow(() -> new IllegalStateException("錯誤：嘗試在非 Request Scope 獲取 CurrentPlayer"));
  }

  // 輔助方法：取得當前 TraceId，沒綁定就回傳 "SYSTEM"
  public static String traceId() {
    return TRACE_ID.orElse("UNKNOWN");
  }

}
