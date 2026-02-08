package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.application.service.LivingService;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.SkillCategory;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.GameCommand;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

// 泛型 T 讓我們可以在子類別擴充更多 Message 類型
@Getter
@Slf4j
public abstract sealed class Living extends VirtualActor<ActorMessage> permits Player, Mob {

  private LivingService livingService;

  protected String id;

  @Setter
  protected String name;

  @Setter
  protected List<String> aliases;

  // 所有生物都有狀態 (HP/MP) 存放玩家需要進資料庫的變化值(寫入/讀取)
  protected LivingState state;

  // 所有生物都在某個房間 (房間可能未載入)
  @Setter
  protected String currentRoomId;



  // 背包
  protected List<GameItem> inventory = new ArrayList<>();

  protected DamageSource baseDamageSource;

  // 記錄 GCD 結束的「系統時間 (毫秒)」
  protected long gcdEndTimestamp = 0;

  // 記錄當前是否正在詠唱/硬直 (Cast Time)
  protected boolean isCasting = false;

  protected boolean isDirty = false;
  // 一個標記，代表這個物件是否還在遊戲世界中
  protected boolean valid = true;



  // 動態戰鬥資源 (不需要存檔，戰鬥結束清除)
  // === 戰鬥狀態 ===
  public boolean isInCombat = false;
  // 當前鎖定的攻擊目標 (null 代表沒在打架)
  public String combatTargetId;
  // 下一次可以攻擊的時間點 (System.currentTimeMillis)
  public long nextAttackTime = 0;
  // 附加增益/減益
  // public Map<String, Object> dynamicProps = new HashMap<>();



  // 衍生屬性 (快取用，每次穿脫裝備後重新計算 通常不存 DB，由基礎屬性計算，但為了簡單先存這裡)
  public int minDamage = 0; // 最小傷害
  public int maxDamage = 0; // 最大傷害
  public int hitRate = 0; // 命中率
  public int defense = 0; // 防禦力
  public int attackSpeed = 2000; // 攻擊速度 (毫秒，例如 2000 代表 2秒打一次)
  public int weightCapacity = 0;



  public Living(String id, String name, LivingState state, LivingService livingService) {
    super(id);
    this.id = id;
    this.name = name;
    this.state = state;
    this.livingService = livingService;
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
      case ActorMessage.OnDamage(var amount, var attacker) -> {
        doOnDamage(amount, attacker);
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
        livingService.doEquip(this, item, future);
        recalculateStats();
      }
      case ActorMessage.Unequip(var slot, var future) -> {
        livingService.doUnequip(this, slot, future);
        recalculateStats();
      }
      case ActorMessage.OnMessage(var self, var actorMessage) -> {
        // command(traceId, cmd);
      }

      default -> log.warn("handleLivingMessage 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }



  // default 應依自己需求改寫 ------------------------------------------------------------------
  protected void doTick(long tickCount, long time) {

    // 狀態無效不處理心跳
    if (!isValid()) {
      return;
    }

    // === 1. 戰鬥心跳 (最優先，每秒執行) ===
    // 頻率：1秒 (因為 WorldPulse 就是 1秒)
    // log.info(this.name + " tickCount: {} inCombat: {}", tickCount, this.state.isInCombat());
    // if (isInCombat()) {
    // processAutoAttack(time); // 之前討論過的自動攻擊
    // }

    // === 2. 回復/狀態心跳 (Regen Tick) ===
    // 頻率：每 3 秒執行一次 ( tickCount % 3 == 0 )
    // 只有沒在戰鬥時才回血，或者戰鬥中回得比較慢
    if (tickCount % 30 == 0) {
      processRegen();
      // processBuffs(); // 檢查 Buff 是否過期
    }

    // === 3. AI 行為心跳 (AI Tick) ===
    // 頻率：每 5 秒執行一次
    // 只有怪物需要，玩家不需要
    if (this instanceof Mob mob && tickCount % 5 == 0) {
      // mob.processAI(); // 例如：隨機移動、喊話
    }
  }

  // 被攻擊觸發戰鬥狀態
  protected void doOnAttacked(Living attacker) {
    livingService.getCombatService().onAttacked(this, attacker);
  }

  // 受傷處理
  protected void doOnDamage(int amount, Living attacker) {
    livingService.getCombatService().onDamage(amount, this, attacker);
  }

  // 死亡處理
  protected void doDie(String killerId) {
    livingService.onDie(this, killerId);
  }

  // 治療處理
  protected void doHeal(int amount) {
    if (!isValid()) {
      // log.info("{} 已經死亡，無法治療", name);
      return;
    }

    if (isInCombat()) {
      // log.info("{} 正在戰鬥，無法治療", name);
      return;
    }

    // reply(this.state.getGender().getYou() + "回復了 " + amount + " 點 HP 目前 " + state.getHp() + " / "
    // + state.getMaxHp());
    this.state.setHp(Math.min(state.getHp() + amount, state.getMaxHp()));
  }



  // ---------------------------------------------------------------------------------------------



  // 實作 defualt 的公開方法給外部呼叫用 -----------------------------------------------------------
  public void tick(long tickCount, long time) {
    if (!this.isValid()) {
      return;
    }

    this.send(new ActorMessage.Tick(tickCount, time));
  }

  // 被攻擊處理
  public void onAttacked(Living attacker) {
    this.send(new ActorMessage.OnAttacked(attacker));
  }

  // 受傷處理
  public void onDamage(int amount, Living attacker) {
    this.send(new ActorMessage.OnDamage(amount, attacker));
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
      case Player player -> this.send(new ActorMessage.SendText(player.getSession(), msg));
      case Mob mob -> {
      }
    }
  }

  public void sendText(String msg) {
    reply(msg);
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
  // protected void processAutoAttack(long now) {
  // livingService.getCombatService().processAutoAttack(this, now);
  // }

  protected void processRegen() {
    if (state.getHp() < state.getMaxHp()) {
      int regenAmount = (int) (state.getMaxHp() * 0.05); // 回復 5%
      doHeal(regenAmount);
      sendStatUpdate();
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

    this.minDamage = minDamage;
    this.maxDamage = maxDamage;
    this.defense = def;
    log.info("{} stats updated: minDamage={}, maxDamage={}, Def={}", this.name, minDamage,
        maxDamage, def);
  }

  // 取得當前的攻擊方式
  public DamageSource getCurrentAttackSource() {
    // 1. 先檢查主手有沒有武器
    GameItem weapon = getMainHandWeapon(); // 假設您有實作裝備系統

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

  public GameItem getMainHandWeapon() {
    return state.equipment.get(EquipmentSlot.MAIN_HAND);
  }

  public GameItem getOffHandEquip() {
    return state.equipment.get(EquipmentSlot.OFF_HAND);
  }

  // 輔助方法：取得主手武器類型 (共用)
  public SkillCategory getMainHandType() {
    GameItem weapon = getMainHandWeapon();
    return (weapon != null) ? weapon.getWeaponSkillCategory() : SkillCategory.UNARMED;
  }

  // 輔助方法：取得自身等級 (共用)
  public int getLevel() {
    return state.getLevel();
  }

  public Map<String, SkillEntry> getLearnedSkills() {
    return this.state.getLearnedSkills();
  }

  public int getSkillLevel(String skillId) {
    if (!getLearnedSkills().containsKey(skillId)) {
      return 0;
    }
    return getLearnedSkills().get(skillId).getLevel();
  }

  public Map<SkillCategory, String> getEnabledSkills() {
    return this.state.getEnabledSkills();
  }

  public String getEnabledSkillId(SkillCategory category) {
    return getEnabledSkills().get(category);
  }

  public void setNextAttackTime(long time) {
    this.nextAttackTime = time;
  }

  public long getAttackSpeed() {
    GameItem weapon = getMainHandWeapon();
    if (weapon != null) {
      return weapon.getTemplate().equipmentProp().attackSpeed();
    }

    // 赤手空拳
    return 2000;
  }

  public int getAttacksPerRound() {
    return livingService.getAttacksPerRound(this);
  }



  // 建構子與輔助方法...
  public boolean isDead() {
    return state.getHp() <= 0;
  }

  // 判斷是否在戰鬥中
  public boolean isInCombat() {
    return isInCombat && combatTargetId != null;
  }



  public boolean isBusy() {
    return (isInCombat) ? isInCombat : false;
  }

  public void markInvalid() {
    this.valid = false;
  }

  public void markValid() {
    this.valid = true;
  }

  public boolean isValid() {
    return valid && !isDead(); // 活著且有效才算有效
  }

  public Room getCurrentRoom() {
    Room room = livingService.getWorldManagerProvider().getObject().getRoomActor(currentRoomId);
    // TODO 要丟到安全的房間
    if (room == null) {
      log.error("player:{} currentRoomId:{} 不存在", this.name, currentRoomId);
      throw new RuntimeException("你處於一片虛空之中...");
    }

    return room;
  }

  public Optional<Living> getCombatTarget() {
    if (combatTargetId == null) {
      return Optional.empty();
    }

    return getCurrentRoom().findLiving(combatTargetId);
  }

  public void removeFromRoom() {
    if (currentRoomId == null) {
      return;
    }

    // 安全地獲取房間，避免觸發自動創建房間的邏輯
    Room room =
        livingService.getWorldManagerProvider().getObject().getActiveRooms().get(currentRoomId);
    if (room != null) {
      performRemoveFromRoom(room);
    }

    currentRoomId = null;
  }

  public boolean equip(GameItem item) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new ActorMessage.Equip(item, future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
      return future.get();
    } catch (Exception e) {
      log.error("equip itemId:{} itemName:{} error", item.getId(), item.getDisplayName(), e);
    }

    return false;
  }

  public boolean unequip(EquipmentSlot slot) {
    CompletableFuture<Boolean> future = new CompletableFuture<>();
    this.send(new ActorMessage.Unequip(slot, future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
      return future.get();
    } catch (Exception e) {
      log.error("unequip slot:{} error", slot.getDisplayName(), e);
    }

    return false;
  }



  public void sendStatUpdate() {}

  public void processDeath() {
    livingService.processDeath(this);
  }

  /**
   * 由子類實作具體的移除邏輯
   */
  protected abstract void performRemoveFromRoom(Room room);

}
