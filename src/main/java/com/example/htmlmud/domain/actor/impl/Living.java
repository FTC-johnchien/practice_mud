package com.example.htmlmud.domain.actor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.LivingPosture;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.domain.service.LivingService;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.MudMessage;
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
  protected LivingStats stats;

  // 姿勢 (站立, 戰鬥中, 死亡)
  private LivingPosture posture = LivingPosture.STANDING;

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



  public Living(String id, String name, LivingStats stats, LivingService livingService) {
    super(id);
    this.id = id;
    this.name = name;
    this.stats = stats;
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

  private void handleLivingMessage(ActorMessage.LivingMessage msg) {
    switch (msg) {
      case ActorMessage.Tick(var tickCount, var timestamp) -> {
        handleTick(tickCount, timestamp);
      }
      case ActorMessage.OnAttacked(var attackerId) -> {
        handleOnAttacked(attackerId);
      }
      case ActorMessage.OnDamage(var amount, var attackerId) -> {
        handleOnDamage(amount, attackerId);
      }
      case ActorMessage.onDeath(var killerId) -> {
        handleOnDeath(killerId);
      }
      case ActorMessage.onHeal(var amount) -> {
        handleHeal(amount);
      }
      case ActorMessage.Say(var content) -> {
      }
      case ActorMessage.BuffEffect(var buff) -> {
      }
      case ActorMessage.Equip(var item, var future) -> {
        future.complete(handleEquip(item));
      }
      case ActorMessage.Unequip(var slot, var future) -> {
        future.complete(handleUnequip(slot));
      }
      case ActorMessage.OnMessage(var self, var actorMessage) -> {
        // command(traceId, cmd);
      }
      case ActorMessage.LookAtMe(var future) -> {
        future.complete(performLookAtMe());
      }

      default -> log.warn("handleLivingMessage 收到無法處理的訊息: {} {}", this.id, msg);
    }
  }



  // 這是為了讓子類別可依自己需求進行改寫 ----------------------------------------------------------------------------
  protected void handleTick(long tickCount, long time) {
    livingService.tick(this, tickCount, time);
  }

  // 被攻擊觸發戰鬥狀態
  protected void handleOnAttacked(String attackerId) {
    livingService.onAttacked(this, attackerId);
  }

  // 受傷處理
  protected void handleOnDamage(int amount, String attackerId) {
    livingService.onDamage(this, amount, attackerId);
  }

  // 死亡處理
  protected void handleOnDeath(String killerId) {
    livingService.onDeath(this, killerId);
    performDeath();
  }

  // 治療處理
  protected void handleHeal(int amount) {
    livingService.heal(this, amount);
  }

  protected boolean handleEquip(GameItem item) {
    return livingService.equip(this, item);
  }

  protected boolean handleUnequip(EquipmentSlot slot) {
    return livingService.unequip(this, slot);
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  /**
   * 由子類實作具體的移除邏輯
   */
  protected abstract void performRemoveFromRoom(Room room);

  protected abstract void performDeath();

  protected abstract MudMessage<?> performLookAtMe();


  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------



  public void tick(long tickCount, long time) {
    if (!this.isValid()) {
      return;
    }

    this.send(new ActorMessage.Tick(tickCount, time));
  }

  // 被攻擊處理
  public void onAttacked(String attackerId) {
    this.send(new ActorMessage.OnAttacked(attackerId));
  }

  // 受傷處理
  public void onDamage(int amount, String attackerId) {
    this.send(new ActorMessage.OnDamage(amount, attackerId));
  }

  // 死亡處理
  public void onDeath(String killerId) {
    this.send(new ActorMessage.onDeath(killerId));
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

  public MudMessage<?> lookAtMe() {
    CompletableFuture<MudMessage<?>> future = new CompletableFuture<>();
    this.send(new ActorMessage.LookAtMe(future));
    try {
      future.orTimeout(1, TimeUnit.SECONDS).join();
      return future.get();
    } catch (Exception e) {
      log.error("lookAtMe error", e);
      throw new MudException("lookAtMe error livingId:" + this.id);
    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // --- 共用行為邏輯 ---



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
    return stats.equipment.get(EquipmentSlot.MAIN_HAND);
  }

  public GameItem getOffHandEquip() {
    return stats.equipment.get(EquipmentSlot.OFF_HAND);
  }

  // 輔助方法：取得主手武器類型 (共用)
  public SkillCategory getMainHandType() {
    GameItem weapon = getMainHandWeapon();
    return (weapon != null) ? weapon.getWeaponSkillCategory() : SkillCategory.UNARMED;
  }

  // 輔助方法：取得自身等級 (共用)
  public int getLevel() {
    return stats.getLevel();
  }

  public Map<String, SkillEntry> getLearnedSkills() {
    return this.stats.getLearnedSkills();
  }

  public int getSkillLevel(String skillId) {
    if (!getLearnedSkills().containsKey(skillId)) {
      return 0;
    }
    return getLearnedSkills().get(skillId).getLevel();
  }

  public Map<SkillCategory, String> getEnabledSkills() {
    return this.stats.getEnabledSkills();
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
    return stats.getHp() <= 0;
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

  // 判斷範例
  public boolean canMove() {
    return posture != LivingPosture.DEAD && posture != LivingPosture.SLEEPING;
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



}
