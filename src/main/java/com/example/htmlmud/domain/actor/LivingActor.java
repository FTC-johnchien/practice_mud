package com.example.htmlmud.domain.actor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.example.htmlmud.application.service.WorldManager;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.actor.impl.MobActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// 泛型 T 讓我們可以在子類別擴充更多 Message 類型
@Slf4j
public abstract class LivingActor extends VirtualActor<ActorMessage> {

  @Getter
  protected GameServices services;

  @Getter
  protected String id;

  @Getter
  @Setter
  protected String name;

  // 所有生物都有狀態 (HP/MP)
  @Getter
  protected LivingState state;

  // 所有生物都在某個房間 (可能是 null)
  @Getter
  @Setter
  protected String currentRoomId;

  // 用來記錄進入房間的時間戳記 (奈秒精度以防同時進入)
  @Getter
  private long lastEnterRoomTime;

  // 背包
  @Getter
  protected List<GameItem> inventory = new ArrayList<>();


  public LivingActor(String id, String name, LivingState state, GameServices services) {
    super(id);
    this.id = id;
    this.name = name;
    this.state = state;
    this.services = services;
  }

  @Override
  protected void handleMessage(ActorMessage msg) {
    switch (msg) {
      case ActorMessage.Tick(var tickCount, var timestamp) -> {
        onTick(tickCount, timestamp);
      }
      case ActorMessage.Command(var traceId, var cmd) -> {
        // services.commandDispatcher().dispatch(this, cmd);
        onCommand(traceId, cmd);
      }
      case ActorMessage.Die(var killerId) -> {
        onDeath(killerId);
      }
      case ActorMessage.SendText(var session, var content) -> {
        onSendText(session, content);
      }
      case ActorMessage.Attacked(var attacker) -> {
        onAttacked(attacker);
      }
      case ActorMessage.Equip(var item, var future) -> {
        doEquip(item, future);
      }
      case ActorMessage.Unequip(var slot, var future) -> {
        unequip(slot, future);
      }
    }
  }


  // default 應依自己需求改寫 -----------------------------------------------------------
  protected void onTick(long tickCount, long time) {
    // === 1. 戰鬥心跳 (最優先，每秒執行) ===
    // 頻率：1秒 (因為 WorldPulse 就是 1秒)
    if (this.state.isInCombat()) {
      processAutoAttack(time); // 之前討論過的自動攻擊
    }

    // === 2. 回復/狀態心跳 (Regen Tick) ===
    // 頻率：每 3 秒執行一次 ( tickCount % 3 == 0 )
    // 只有沒在戰鬥時才回血，或者戰鬥中回得比較慢
    if (tickCount % 3 == 0) {
      processRegen();
      // processBuffs(); // 檢查 Buff 是否過期
    }

    // === 3. AI 行為心跳 (AI Tick) ===
    // 頻率：每 5 秒執行一次
    // 只有怪物需要，玩家不需要
    if (this instanceof MobActor mob && tickCount % 5 == 0) {
      // mob.processAI(); // 例如：隨機移動、喊話
    }
  }

  protected void onCommand(String traceId, GameCommand cmd) {}

  protected void onDeath(String killerId) {
    this.state.hp = 0;
    this.state.isDead = true;
    log.info("{} has been slain by {}!", id, killerId);
  }

  protected void onSendText(WebSocketSession session, String msg) {}

  protected void onAttacked(LivingActor attacker) {
    // 檢查是否正在戰鬥
    if (!this.state.isInCombat) {
      this.state.isInCombat = true;
      this.state.combatTargetId = attacker.getId();
    }
  }

  /**
   * 穿上裝備 (核心邏輯)
   *
   * @param item 要穿的物品
   * @return 成功回傳 true
   */
  protected boolean doEquip(GameItem item, CompletableFuture<String> future) {
    // 1. 取得 ItemTemplate (需要依賴 Service 或是 Item 本身帶有 slot 資訊)
    // 假設 GameItem 已經從 Template 複製了 slot 資訊，或者這裡去查 Template
    ItemTemplate tpl = item.getTemplate();
    if (tpl.type() != ItemType.EQUIPMENT) {
      future.complete(item.getDisplayName() + " 不是裝備");
      return false;
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (state.equipment.containsKey(slot)) {
      // GameItem oldItem = state.equipment.get(slot);
      unequip(slot, new CompletableFuture<>()); // 先脫舊的
      if (state.equipment.containsKey(slot)) {
        future.complete("無法脫下 " + slot.getDisplayName() + "，或背包已滿");
        return false;
      }
    }

    // 3. 從背包移除該物品
    // 注意：這裡假設 inventory 是 Mutable List
    if (!inventory.remove(item)) {
      future.complete(item.getDisplayName() + "不在背包裡");
      return false; // 物品不在背包裡
    }

    // 4. 放入裝備欄
    state.equipment.put(slot, item);

    // 5. 重新計算數值
    recalculateStats();

    future.complete("你裝上 " + item.getDisplayName());
    return true;
  }

  /**
   * 脫下裝備
   */
  public boolean unequip(EquipmentSlot slot, CompletableFuture<String> future) {
    GameItem item = state.equipment.get(slot);
    if (item == null) {
      future.complete("你 " + slot.getDisplayName() + " 上沒有裝備任何東西");
      return false;
    }

    // 1. 放入背包
    inventory.add(item);

    // 2. 從裝備欄移除
    state.equipment.remove(slot);

    // 3. 重新計算數值
    recalculateStats();

    future.complete("你將 " + slot.getDisplayName() + " 放入背包");
    return true;
  }
  // -----------------------------------------------------------------------------------


  // 實作 defualt 的公開方法給外部呼叫用 --------------------------------------------------
  public void tick() {
    this.send(new ActorMessage.Tick(System.currentTimeMillis(), System.nanoTime()));
  }

  public void command(GameCommand cmd) {
    this.send(new ActorMessage.Command("internal", cmd));
  }

  public void death(String killerId) {
    this.send(new ActorMessage.Die(killerId));
  }

  // 被攻擊處理
  public void attacked(LivingActor attacker) {
    this.send(new ActorMessage.Attacked(attacker));
  }

  public CompletableFuture<String> equip(GameItem item) {
    var future = new CompletableFuture<String>();
    this.send(new ActorMessage.Equip(item, new CompletableFuture<>()));
    return future;
  }

  public CompletableFuture<String> unequip(EquipmentSlot slot) {
    var future = new CompletableFuture<String>();
    this.send(new ActorMessage.Unequip(slot, future));
    return future;
  }

  // -----------------------------------------------------------------------------------


  protected void processAutoAttack(long now) {
    // 1. 檢查攻速冷卻 (例如 2000ms 一刀)
    if (now < state.nextAttackTime) {
      return; // 還在冷卻中
    }

    LivingActor target =
        services.combatService().processAutoAttack(this, currentRoomId, state.combatTargetId);
    if (target == null || target.getState().hp <= 0) {
      stopCombat(); // 目標消失或死亡，停止戰鬥
      return;
    }

    // 6. 設定下一次攻擊時間 (攻速 2秒)
    state.nextAttackTime = now + 2000;
  }

  protected void processRegen() {
    if (state.hp < state.maxHp) {
      int regenAmount = (int) (state.maxHp * 0.05); // 回復 5%
      heal(regenAmount);
    }
  }

  // 治療處理
  protected void heal(int amount) {
    if (state.isDead)
      return;
    this.state.hp = Math.min(state.hp + amount, state.maxHp);
  }



  // 1. 受傷處理
  private void takeDamage(int amount, String attackerId) {
    if (state.isDead)
      return;

    this.state.hp -= amount;
    log.info("{} took {} damage from {}. HP: {}/{}", id, amount, attackerId, state.hp, state.maxHp);

    // 發送訊息給房間 (讓其他人看到噴血)
    // if (currentRoom != null) {
    // currentRoom.broadcast(...);
    // }

    if (state.hp <= 0) {
      death(attackerId);
    }
  }



  // --- 共用行為邏輯 ---
  // 進入房間時，房間會呼叫此方法來紀錄進入時間戳記
  public void markEnterRoom() {
    this.lastEnterRoomTime = System.nanoTime();
  }


  /**
   * 【重要】重新計算總屬性 每次穿脫裝備、升級、Buff 改變時呼叫
   */
  public void recalculateStats() {
    int atk = 0; // 基礎攻擊力 (可從 Level 算)
    int def = 0; // 基礎防禦力

    // 遍歷所有裝備
    for (GameItem item : state.equipment.values()) {
      ItemTemplate tpl = item.getTemplate();
      if (tpl != null) {
        atk += tpl.equipmentProp().damage();
        def += tpl.equipmentProp().defense();

        // 處理額外屬性 (Bonus Stats)
        // if (tpl.bonusStats() != null) ...
      }
    }

    this.state.damage = atk;
    this.state.defense = def;
    log.info("{} stats updated: Atk={}, Def={}", this.name, atk, def);
  }



  public void stopCombat() {
    state.isInCombat = false;
    state.combatTargetId = null;
    // this.re("戰鬥結束了。");
  }
}
