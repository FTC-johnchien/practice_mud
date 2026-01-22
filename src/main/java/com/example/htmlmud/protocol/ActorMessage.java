package com.example.htmlmud.protocol;

public sealed interface ActorMessage
    permits ActorMessage.Tick, ActorMessage.Command, ActorMessage.Die, ActorMessage.SendText {

  /**
   * 心跳訊息
   *
   * @param tickCount 全域累計的 Tick 次數 (用來取餘數判斷頻率)
   * @param timestamp 當前時間戳
   */
  record Tick(long tickCount, long timestamp) implements ActorMessage {
  }

  record Command(String traceId, GameCommand command) implements ActorMessage {
  }

  record Die(String killerId) implements ActorMessage {
  }

  record SendText(String content) implements ActorMessage {
  }

}
