package com.example.htmlmud.domain.exception;

import java.util.Collections;
import java.util.List;

/**
 * 遊戲 JSON 資料拓撲校驗例外
 * 當啟動期或校驗期發現懸空出口、不存在之物品/技能/怪物參照等斷鏈時拋出
 */
public class DataValidationException extends RuntimeException {

  private final List<String> validationErrors;

  public DataValidationException(String message) {
    super(message);
    this.validationErrors = List.of(message);
  }

  public DataValidationException(String message, List<String> validationErrors) {
    super(message + (validationErrors != null && !validationErrors.isEmpty()
        ? "\n" + String.join("\n", validationErrors)
        : ""));
    this.validationErrors = validationErrors != null ? List.copyOf(validationErrors) : List.of();
  }

  public List<String> getValidationErrors() {
    return Collections.unmodifiableList(validationErrors);
  }
}
