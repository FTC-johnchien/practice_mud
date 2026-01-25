package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// 泛型 T 讓我們可以在子類別擴充更多 Message 類型
@Slf4j
public abstract sealed class LivingActor extends VirtualActor<ActorMessage>
    permits PlayerActor, MobActor {

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

  // 背包
  @Getter
  protected List<GameItem> inventory = new ArrayList<>();

  protected DamageSource baseDamageSource;

  public LivingActor(String id, String name, LivingState state, GameServices services) {
    super(id);
    this.id = id;
    this.name = name;
    this.state = state;
    this.services = services;
  }

  @Override
  protected void handleMessage(ActorMessage msg) {

    // 使用 switch pattern matching
    switch (msg) {
      // 處理通用生物訊息
      case ActorMessage.LivingMessage livingMsg -> handleLivingMessage(livingMsg);

      // 其他我不懂的 (例如 PlayerMessage)，如果是抽象類別，可以選擇忽略或報錯
      // 但因為我們會被子類別 override，所以這裡通常是 "default fallback"
      default -> log.warn("LivingActor 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }

  protected void handleLivingMessage(ActorMessage.LivingMessage msg) {
    switch (msg) {
      case ActorMessage.Tick(var tickCount, var timestamp) -> {
        doTick(tickCount, timestamp);
      }
      case ActorMessage.OnAttacked(var attacker) -> {
        doOnAttacked(attacker);
      }
      case ActorMessage.OnDamage(var amount, var attackerId) -> {
        doOnDamage(amount, attackerId);
      }
      case ActorMessage.Die(var killerId) -> {
        doDie(killerId);
      }
      case ActorMessage.Heal(var amount) -> {
        doHeal(amount);
      }
      case ActorMessage.Say(var content) -> {
      }
      case ActorMessage.BuffEffect(var buff) -> {
      }
      case ActorMessage.Equip(var item, var future) -> {
        doEquip(item, future);
      }
      case ActorMessage.Unequip(var slot, var future) -> {
        doUnequip(slot, future);
      }
      case ActorMessage.onMessage(var self, var actorMessage) -> {
        // command(traceId, cmd);
      }

      default -> log.warn("handleLivingMessage 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }



  // default 應依自己需求改寫 ------------------------------------------------------------------
  protected void doTick(long tickCount, long time) {

    // 死亡停止心跳
    if (this.state.isDead()) {
      return;
    }

    // === 1. 戰鬥心跳 (最優先，每秒執行) ===
    // 頻率：1秒 (因為 WorldPulse 就是 1秒)
    // log.info(this.name + " tickCount: {} inCombat: {}", tickCount, this.state.isInCombat());
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

  // 被攻擊觸發戰鬥狀態
  protected void doOnAttacked(LivingActor attacker) {
    services.combatService().onAttacked(this, attacker.getId());
  }

  // 受傷處理
  protected void doOnDamage(int amount, String attackerId) {
    services.combatService().onDamage(amount, this, attackerId);
  }

  // 死亡處理
  protected void doDie(String killerId) {
    // 標記狀態 (Mark State)：設為 Dead，停止接受新的傷害或治療。
    // 交代後事 (Cleanup & Notify)：取消心跳、製造屍體、通知房間。
    services.combatService().onDie(this, killerId);
    // 自我毀滅 (Terminate)：確認訊息發出後，才停止 VT。
    stop(); // 停止 Actor
  }

  // 治療處理
  protected void doHeal(int amount) {
    if (state.isDead()) {
      // log.info("{} 已經死亡，無法治療", name);
      return;
    }
    if (state.isInCombat()) {
      // log.info("{} 正在戰鬥，無法治療", name);
      return;
    }
    reply(name + "回復了 " + amount + " 點 HP 目前 " + state.hp + " / " + state.maxHp);
    this.state.hp = Math.min(state.hp + amount, state.maxHp);
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
    if (tpl.type() != ItemType.WEAPON && tpl.type() != ItemType.SHIELD
        && tpl.type() != ItemType.ARMOR && tpl.type() != ItemType.ACCESSORY) {
      future.complete(item.getDisplayName() + " 不是裝備");
      return false;
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (state.equipment.containsKey(slot)) {
      // GameItem oldItem = state.equipment.get(slot);
      doUnequip(slot, new CompletableFuture<>()); // 先脫舊的
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
  protected boolean doUnequip(EquipmentSlot slot, CompletableFuture<String> future) {
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



  // ---------------------------------------------------------------------------------------------



  // 實作 defualt 的公開方法給外部呼叫用 -----------------------------------------------------------
  public void tick(long tickCount, long time) {
    this.send(new ActorMessage.Tick(tickCount, time));
  }

  // 被攻擊處理
  public void onAttacked(LivingActor attacker) {
    this.send(new ActorMessage.OnAttacked(attacker));
  }

  // 受傷處理
  public void onDamage(int amount, String attackerId) {
    this.send(new ActorMessage.OnDamage(amount, attackerId));
  }

  // 死亡處理
  public void die(String killerId) {
    this.send(new ActorMessage.Die(killerId));
  }

  public void command(String traceId, GameCommand cmd) {
    this.send(new ActorMessage.Command(traceId, cmd));
  }



  public void reply(String msg) {
    switch (this) {
      case PlayerActor player -> this.send(new ActorMessage.SendText(player.getSession(), msg));
      case MobActor mob -> {
      }
    }
  }

  // public CompletableFuture<String> equip(GameItem item) {
  // var future = new CompletableFuture<String>();
  // this.send(new ActorMessage.Equip(item, new CompletableFuture<>()));
  // return future;
  // }

  // public CompletableFuture<String> unequip(EquipmentSlot slot) {
  // var future = new CompletableFuture<String>();
  // this.send(new ActorMessage.Unequip(slot, future));
  // return future;
  // }

  // -----------------------------------------------------------------------------------



  // 攻擊邏輯(自動攻擊)
  protected void processAutoAttack(long now) {
    services.combatService().processAutoAttack(this, now);
  }

  protected void processRegen() {
    if (state.hp < state.maxHp) {
      int regenAmount = (int) (state.maxHp * 0.05); // 回復 5%
      doHeal(regenAmount);
    }
  }



  // --- 共用行為邏輯 ---

  /**
   * 【重要】重新計算總屬性 每次穿脫裝備、升級、Buff 改變時呼叫
   */
  public void recalculateStats() {
    int minDamage = 0; // 基礎攻擊力 (可從 Level 算)
    int maxDamage = 0; // 基礎防禦力
    int def = 0; // 基礎防禦力

    // 遍歷所有裝備
    for (GameItem item : state.equipment.values()) {
      ItemTemplate tpl = item.getTemplate();
      if (tpl != null) {
        minDamage += tpl.equipmentProp().minDamage();
        maxDamage += tpl.equipmentProp().maxDamage();
        def += tpl.equipmentProp().defense();

        // 處理額外屬性 (Bonus Stats)
        // if (tpl.bonusStats() != null) ...
      }
    }

    this.state.minDamage = minDamage;
    this.state.maxDamage = maxDamage;
    this.state.defense = def;
    log.info("{} stats updated: minDamage={}, maxDamage={}, Def={}", this.name, minDamage,
        maxDamage, def);
  }

  // 取得當前的攻擊方式
  public DamageSource getCurrentAttackSource() {
    // 1. 先檢查主手有沒有武器
    GameItem weapon = state.equipment.get(EquipmentSlot.MAIN_HAND); // 假設您有實作裝備系統

    if (weapon != null) {
      // 有武器：回傳武器資訊
      // 這裡假設 weapon 有對應欄位，或從 Template 查
      return weapon.getTemplate().equipmentProp().getDamageSource(weapon.getDisplayName());
    }
    // 取出 mob template 里的設定
    else if (baseDamageSource != null) {
      return baseDamageSource;
    }

    return DamageSource.DEFAULT_FIST;
  }
}
