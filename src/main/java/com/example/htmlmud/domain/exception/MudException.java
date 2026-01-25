package com.example.htmlmud.domain.exception;

public class MudException extends RuntimeException {
  // 這個訊息是要直接顯示給玩家看的
  public MudException(String message) {
    super(message);
  }
}
