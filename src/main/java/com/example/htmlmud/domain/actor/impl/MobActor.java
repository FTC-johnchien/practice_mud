package com.example.htmlmud.domain.actor.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.domain.actor.LivingActor;
import com.example.htmlmud.domain.actor.behavior.AggressiveBehavior;
import com.example.htmlmud.domain.actor.behavior.MerchantBehavior;
import com.example.htmlmud.domain.actor.behavior.MobBehavior;
import com.example.htmlmud.domain.actor.behavior.PassiveBehavior;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.map.Equipment;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MobActor extends LivingActor {

  // 仇恨列表 (Key: 攻擊者 ID字串, Value: 仇恨值)
  // 因為 ID 已改為 UUID String，這裡的 Key 也同步調整為 String
  protected final Map<String, Integer> aggroTable = new HashMap<>();

  @Getter
  private final MobTemplate template;

  private ScheduledFuture<?> aiTask; // Heartbeat 排程
  private MobBehavior behavior; // AI 行為 (策略模式)

  // 裝備欄位
  private Map<EquipmentSlot, GameItem> equipments;

  /**
   * 建構子： 1. 接收 Template 與 Services 2. 自動生成 UUID 3. 自動從 Template 建立 LivingState
   */
  public MobActor(MobTemplate template, GameServices services) {
    // 【修正 1】直接使用 UUID 作為 ID，不再依賴 GameObjectId.mob()
    // 【修正 2】呼叫 helper method 建立初始 State，確保血量與 Template 一致
    String mobId = "mob-" + UUID.randomUUID().toString().substring(0, 8);
    super(mobId, template.name(), createInitialState(template), services);
    this.template = template;

    // 處理裝備

    // 初始化行為
    initBehavior();

    log.debug("Mob created: {} (ID: {})", template.name(), this.getId());

    // 【重要】這裡移除了 this.start()，請在外部 (MobFactory) 建立實體後呼叫 mob.start()
  }

  // 輔助方法：根據 Template 產生初始的 LivingState
  private static LivingState createInitialState(MobTemplate tpl) {
    LivingState state = new LivingState();

    // 基本屬性設定
    state.level = tpl.level();
    state.hp = tpl.maxHp();
    state.maxHp = tpl.maxHp();
    state.mp = tpl.maxMp();
    state.maxMp = tpl.maxMp();
    state.stamina = tpl.maxStamina();
    state.maxStamina = tpl.maxStamina();
    // state.san = tpl.maxSan();
    // state.maxSan = tpl.maxSan();

    // 戰鬥屬性設定
    state.damage = tpl.baseDamage();
    state.defense = tpl.baseDefense();
    state.attackSpeed = tpl.attackSpeed();

    return state;
  }

  // 處理初期裝備
  private void initEquipment(MobTemplate tpl) {
    if (tpl.equipment() != null) {
      for (String itemId : tpl.equipment().values()) {

        // equip(itemId);
      }
    }
  }

  private void initBehavior() {
    if (template.shopId() != null) {
      this.behavior = new MerchantBehavior(template.shopId());
    } else if (template.isAggressive()) {
      this.behavior = new AggressiveBehavior();
    } else {
      this.behavior = new PassiveBehavior();
    }
  }

  // --- 生命週期控制 ---

  @Override
  public void start() {
    super.start(); // 啟動 Actor 訊息佇列處理
    // startHeartbeat(); // 啟動 AI 心跳
  }

  @Override
  public void stop() {
    stopHeartbeat(); // 停止 AI
    super.stop(); // 停止 Actor
  }

  // --- 訊息處理 ---

  @Override
  protected void handleMessage(ActorMessage msg) {
    // 1. AI 決策優先 (例如：受到攻擊決定是否逃跑)
    behavior.onMessage(this, msg);

    // 2. 如果父類別有通用邏輯 (如 Buff 結算)，可視需求呼叫
    super.handleMessage(msg);
  }

  // --- AI 心跳機制 (Heartbeat) ---

  private void startHeartbeat() {
    if (aiTask != null && !aiTask.isCancelled())
      return;

    // Scheduler (Platform Thread) 負責定時觸發
    this.aiTask = services.scheduler().scheduleAtFixedRate(() -> {
      // Virtual Thread 負責執行邏輯
      Thread.ofVirtual().name("mob-tick-" + this.getId()).start(() -> {
        try {
          if (this.state.hp > 0) {
            this.tick();
          } else {
            stopHeartbeat();
          }
        } catch (Exception e) {
          log.error("Mob tick error: {}", this.getId(), e);
        }
      });
    }, 1, 3, TimeUnit.SECONDS); // 延遲1秒開始，每3秒一次
  }

  private void stopHeartbeat() {
    if (aiTask != null) {
      aiTask.cancel(false);
      aiTask = null;
    }
  }

  public void tick() {
    // 委派給 Behavior
    // behavior.onTick(this);

    long now = System.currentTimeMillis();

    // 1. 戰鬥邏輯
    // log.info("this.state.isInCombat: {}", this.state.isInCombat);
    if (this.state.isInCombat) {
      processCombatRound(now);
    } else {
      // 2. 非戰鬥邏輯 (巡邏、回復 HP)
      // processRegen();
      behavior.onTick(this);
    }
  }

  // --- 互動與事件 ---

  public void onPlayerEnter(PlayerActor player) {
    behavior.onPlayerEnter(this, player);
  }

  public void onInteract(PlayerActor player, String command) {
    behavior.onInteract(this, player, command);
  }

  @Override
  protected void onAttacked(LivingActor attacker) {

    // 檢查是否正在戰鬥
    if (!this.state.isInCombat) {
      this.state.isInCombat = true;
    }

    // TODO 檢查仇恨表，選最高的當對手
    this.state.combatTargetId = attacker.getId();

    // TODO 檢查仇恨表，選最高的當對手
    // if (this.state.combatTargetId == null) {
    // this.state.combatTargetId = attacker.getId();
    // }

    // 無敵判定
    if (template.isInvincible()) {
      if (attacker instanceof PlayerActor p) {
        p.reply(this.template.name() + " 毫髮無傷！");
      }
      return;
    }

    // 1. 扣血 (呼叫父類別)
    log.info("damage: {}", attacker.getState().damage);
    log.info("this.state.hp: {}", this.state.hp);
    log.info("this.state.defense: {}", this.state.defense);
    super.onAttacked(attacker);

    // 2. 增加仇恨值 (使用 String ID)
    addAggro(attacker.getId(), attacker.getState().damage);

    // 3. 通知 AI
    behavior.onDamaged(this, attacker);
  }

  @Override
  protected void onDeath(String killerId) {
    // super.onDie(killerId);
    stopHeartbeat();

    log.info("{} died. Killer: {}", this.template.name(), killerId);
    // 觸發掉寶、給予經驗值等邏輯...
  }

  // 仇恨值管理 (Key 改為 String)
  public void addAggro(String sourceId, int value) {
    aggroTable.merge(sourceId, value, Integer::sum);
  }

  // 取得當前仇恨最高目標 ID
  public String getHighestAggroTarget() {
    return aggroTable.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey)
        .orElse(null);
  }

  // 行為層需要的輔助方法 (Facade)
  public void sayToRoom(String content) {
    // 實作：取得當前 RoomActor 並廣播
    // services.worldManager().getRoom(currentRoomId).broadcast(...)
  }

  public void attack(LivingActor target) {
    // 實作：發送 AttackMessage 給 target
  }

  private void processCombatRound(long now) {
    // 檢查攻擊冷卻
    log.info("this.state.nextAttackTime: {}", this.state.nextAttackTime);
    if (now < this.state.nextAttackTime) {
      return;
    }

    // 取得當前最高仇恨目標
    log.info("getHighestAggroTarget: {}", getHighestAggroTarget());
    String targetId = getHighestAggroTarget();
    if (targetId == null) {
      this.state.isInCombat = false; // 沒目標，脫離戰鬥
      return;
    }

    processAutoAttack(now);

    // 從房間取得目標 Actor (這裡需要 WorldManager 協助，或 RoomActor 傳遞)
    // 假設這段邏輯在 RoomActor 處理會更好，但若在 Mob 處理：
    // LivingActor target = services.worldManager().getRoomActor(currentRoomId).findActor(targetId);

    // 為了簡單，假設我們能取到 target
    // int dmg = combatService.calculateDamage(this, target);
    // target.onAttacked(this, dmg);

    // 重設冷卻時間
    this.state.nextAttackTime = now + this.state.attackSpeed;
  }

  // 取得當前的攻擊方式
  public DamageSource getCurrentAttackSource() {
    // 1. 先檢查主手有沒有武器 (針對 Guard)
    GameItem weapon = state.equipment.get(EquipmentSlot.MAIN_HAND); // 假設您有實作裝備系統

    if (weapon != null) {
      // 有武器：回傳武器資訊
      // 這裡假設 weapon 有對應欄位，或從 Template 查
      return new DamageSource(weapon.getTemplate().name(),
          weapon.getTemplate().equipmentProp().attackVer(),
          weapon.getTemplate().equipmentProp().damage(),
          weapon.getTemplate().equipmentProp().attackSpeed());
    }

    // 2. 沒武器：回傳天生武器 (針對 Rat 或 空手的人)
    return new DamageSource(template.attackNoun(), template.attackVerb(), template.baseDamage(),
        template.attackSpeed());
  }
}
