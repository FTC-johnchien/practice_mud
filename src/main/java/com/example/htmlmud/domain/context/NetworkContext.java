package com.example.htmlmud.domain.context;

public class NetworkContext {
    // 用來追蹤每個網路封包的 Trace ID (除錯神器)
    public static final ScopedValue<String> TRACE_ID = ScopedValue.newInstance();

    // 如果你確定邏輯是在同一個 Thread 跑 (例如 Handler 內)，可以放 SessionId
    public static final ScopedValue<String> SESSION_ID = ScopedValue.newInstance();
}
