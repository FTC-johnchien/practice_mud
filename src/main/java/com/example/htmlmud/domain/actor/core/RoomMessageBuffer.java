package com.example.htmlmud.domain.actor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.protocol.MudMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RoomMessageBuffer {
  // 存放一段時間內的碎片訊息
  private final List<MessageFragment> fragments = Collections.synchronizedList(new ArrayList<>());
  private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
  private final AtomicBoolean isScheduled = new AtomicBoolean(false);
  private final Room room; // 引用所屬的 Room Actor

  private static final long BATCH_WINDOW_MS = 100; // 100ms 的收集窗口

  public RoomMessageBuffer(Room room) {
    this.room = room;
  }

  /**
   * 由外部 Actor (如被攻擊的 Mob) 呼叫，將事件丟入 Buffer
   */
  public void push(String type, Object payload) {
    fragments.add(new MessageFragment(type, payload, System.currentTimeMillis()));

    // 如果還沒排程 Flush 任務，則啟動一個
    if (isScheduled.compareAndSet(false, true)) {
      scheduler.schedule(this::flush, BATCH_WINDOW_MS, TimeUnit.MILLISECONDS);
    }
  }

  /**
   * 合併並發送訊息
   */
  private void flush() {
    try {
      if (fragments.isEmpty())
        return;

      List<MessageFragment> snapshot;
      synchronized (fragments) {
        snapshot = new ArrayList<>(fragments);
        fragments.clear();
      }
      isScheduled.set(false);

      // 執行合併邏輯
      Map<String, Object> combinedPayload = mergeMessages(snapshot);

      // 透過 Room 廣播給所有玩家
      room.broadcastJson(
          MudMessage.builder().type("BATCHED_UPDATE").payload(combinedPayload).build());

    } catch (Exception e) {
      log.error("Room Message Flush 失敗", e);
    }
  }

  private Map<String, Object> mergeMessages(List<MessageFragment> snapshot) {
    Map<String, Object> merged = new HashMap<>();
    List<Object> combatEvents = new ArrayList<>();
    List<Object> statusEvents = new ArrayList<>();

    for (MessageFragment frag : snapshot) {
      switch (frag.getType()) {
        case "COMBAT":
          combatEvents.add(frag.getPayload());
          break;
        case "STATUS":
          statusEvents.add(frag.getPayload());
          break;
        // 其他類型可依序擴充
      }
    }

    if (!combatEvents.isEmpty())
      merged.put("combat", combatEvents);
    if (!statusEvents.isEmpty())
      merged.put("status", statusEvents);

    return merged;
  }
}
