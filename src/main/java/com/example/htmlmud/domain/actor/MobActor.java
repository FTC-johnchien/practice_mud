package com.example.htmlmud.domain.actor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.model.GameObjectId;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.protocol.ActorMessage;

public class MobActor extends LivingActor {

  // Mob 特有：掉落表、仇恨列表、AI 狀態
  private final List<Long> dropItemIds;
  private final Map<GameObjectId, Integer> aggroTable = new HashMap<>();

  public MobActor(String instanceId, LivingState state, List<Long> drops) {
    super(instanceId, state);
    this.dropItemIds = drops;
  }

  @Override
  protected void handleMessage(ActorMessage msg) {
    // 處理 AI 邏輯 (例如：收到 Tick 訊息 -> 攻擊仇恨最高的人)
  }

  @Override
  protected void onDeath(GameObjectId killerId) {
    // 怪物死亡邏輯：
    // 1. 計算掉寶 -> 產生 ItemActor 丟到房間
    // 2. 給兇手經驗值 -> WorldManager.getActor(killerId).send(ExpMsg)
    // 3. 從房間移除自己 -> currentRoom.send(RemoveMsg)
    // 4. 停止自己的 Actor -> this.stop()
  }
}
