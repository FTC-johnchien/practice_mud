package com.example.htmlmud.infra.persistence.service;

/**
 * 異步批次持久化提交結果 (ASYNC-03: Atomic Lifecycle & Explicit Backpressure)
 * 供呼叫端檢測存檔是否成功入列，或因背壓/關機被明確拒絕
 */
public enum SubmitResult {
  /** 成功接受並入列 */
  ACCEPTED,
  /** 服務正在關機或已關機，拒絕新資料 */
  REJECTED_SHUTTING_DOWN,
  /** 佇列已滿，觸發有界背壓拒絕 */
  REJECTED_QUEUE_FULL,
  /** 空資料無效拒絕 */
  REJECTED_NULL;

  public boolean isAccepted() {
    return this == ACCEPTED;
  }

  public boolean isRejected() {
    return this != ACCEPTED;
  }
}
