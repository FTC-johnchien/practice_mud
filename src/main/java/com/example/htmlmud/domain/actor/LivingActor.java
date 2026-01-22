package com.example.htmlmud.domain.actor;

import java.util.ArrayList;
import java.util.List;
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
      }
      case ActorMessage.Die(var killerId) -> {
        die(killerId);
      }
      case ActorMessage.SendText(var content) -> {
      }
    }
  }

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

  protected void processRegen() {
    if (state.hp < state.maxHp) {
      int regenAmount = (int) (state.maxHp * 0.05); // 回復 5%
      heal(regenAmount);
    }
  }

  // --- 共用行為邏輯 ---



  // 1. 受傷處理
  public void takeDamage(int amount, String attackerId) {
    if (state.isDead)
      return;

    this.state.hp -= amount;
    log.info("{} took {} damage from {}. HP: {}/{}", id, amount, attackerId, state.hp, state.maxHp);

    // 發送訊息給房間 (讓其他人看到噴血)
    // if (currentRoom != null) {
    // currentRoom.broadcast(...);
    // }

    if (state.hp <= 0) {
      die(attackerId);
    }
  }

  // 2. 死亡處理
  private void die(String killerId) {
    this.state.hp = 0;
    this.state.isDead = true;
    log.info("{} has been slain by {}!", id, killerId);

    onDeath(killerId); // 樣板方法 (Template Method)，給子類別實作掉寶或重生地
  }

  // 3. 治療處理
  public void heal(int amount) {
    if (state.isDead)
      return;
    this.state.hp = Math.min(state.hp + amount, state.maxHp);
  }

  public void onAttacked(LivingActor attacker, int damage) {
    // 檢查是否正在戰鬥
    if (!this.state.isInCombat) {
      this.state.isInCombat = true;
      this.state.combatTargetId = attacker.getId();
    }

    // TODO 檢查仇恨表，選最高的當對手
    else if (this.state.combatTargetId == null) {
      this.state.combatTargetId = attacker.getId();
    }
  }

  // 定義抽象方法，子類別必須實作
  protected void onDeath(String killerId) {

  }

  // 進入房間時呼叫此方法
  public void markEnterRoom() {
    this.lastEnterRoomTime = System.nanoTime();
  }

  /**
   * 穿上裝備 (核心邏輯)
   *
   * @param item 要穿的物品
   * @return 成功回傳 true
   */
  public boolean equip(GameItem item) {
    // 1. 取得 ItemTemplate (需要依賴 Service 或是 Item 本身帶有 slot 資訊)
    // 假設 GameItem 已經從 Template 複製了 slot 資訊，或者這裡去查 Template
    ItemTemplate tpl = item.getTemplate();
    if (tpl.type() != ItemType.EQUIPMENT) {
      return false; // 這不是裝備
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (state.equipment.containsKey(slot)) {
      GameItem oldItem = state.equipment.get(slot);
      unequip(slot); // 先脫舊的
      // 如果脫不下來（例如背包滿了），則穿戴失敗
      if (state.equipment.containsKey(slot)) {
        return false;
      }
    }

    // 3. 從背包移除該物品
    // 注意：這裡假設 inventory 是 Mutable List
    if (!inventory.remove(item)) {
      return false; // 物品不在背包裡
    }

    // 4. 放入裝備欄
    state.equipment.put(slot, item);

    // 5. 重新計算數值
    recalculateStats();

    return true;
  }

  /**
   * 脫下裝備
   */
  public boolean unequip(EquipmentSlot slot) {
    GameItem item = state.equipment.get(slot);
    if (item == null)
      return false;

    // 1. 放入背包
    inventory.add(item);

    // 2. 從裝備欄移除
    state.equipment.remove(slot);

    // 3. 重新計算數值
    recalculateStats();

    return true;
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
    // log.info("{} stats updated: Atk={}, Def={}", this.id, atk, def);
  }

  // 通用的心跳邏輯
  public void tick() {
    long now = System.currentTimeMillis();

    // 【核心】：如果有戰鬥目標，就嘗試攻擊
    // log.info("isInCombat:{}", state.isInCombat);
    if (state.isInCombat()) {
      processAutoAttack(now);
    }
  }

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

  public void stopCombat() {
    state.isInCombat = false;
    state.combatTargetId = null;
    // this.re("戰鬥結束了。");
  }
}
