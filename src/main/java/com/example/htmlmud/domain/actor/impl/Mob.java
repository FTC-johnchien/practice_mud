package com.example.htmlmud.domain.actor.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.example.htmlmud.application.service.MobService;
import com.example.htmlmud.domain.actor.behavior.AggressiveBehavior;
import com.example.htmlmud.domain.actor.behavior.MerchantBehavior;
import com.example.htmlmud.domain.actor.behavior.MobBehavior;
import com.example.htmlmud.domain.actor.behavior.PassiveBehavior;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public final class Mob extends Living {

  private final MobService service;

  // 仇恨列表 (Key: 攻擊者 ID字串, Value: 仇恨值)
  // 因為 ID 已改為 UUID String，這裡的 Key 也同步調整為 String
  private final Map<String, Integer> aggroTable = new HashMap<>();

  private final MobTemplate template;

  private MobBehavior behavior; // AI 行為 (策略模式)

  // mob的戶籍地 ID
  private String homeRoomId;


  /**
   * 建構子： 1. 接收 Template 與 Services 2. 自動生成 UUID 3. 自動從 Template 建立 LivingState
   */
  public Mob(MobTemplate template, LivingState state, MobService mobService) {
    String uuid = UUID.randomUUID().toString();
    String mobId = "m-" + uuid.substring(0, 8) + uuid.substring(9, 11);
    super(mobId, template.name(), state, mobService.getLivingServiceProvider().getObject());
    this.template = template;
    this.service = mobService;

    this.baseDamageSource = new DamageSource(template.attackNoun(), template.attackVerb(),
        template.minDamage(), template.maxDamage(), template.attackSpeed(), template.hitRate(), -1);

    // 處理裝備

    // 初始化行為
    initBehavior();

    log.info("Mob created: {} (ID: {})", template.name(), this.getId());

    // 【重要】這裡移除了 this.start()，請在外部 (MobFactory) 建立實體後呼叫 mob.start()
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
  }

  @Override
  public void stop() {
    super.stop(); // 停止 Actor
  }

  // --- 訊息處理 ---



  @Override
  protected void handleMessage(ActorMessage msg) {
    switch (msg) {
      // 1. 攔截怪物訊息
      case ActorMessage.MobMessage mobMsg -> handleMobMessage(mobMsg);

      // 2. 丟給父類別
      default -> super.handleMessage(msg);
    }

  }

  private void handleMobMessage(ActorMessage.MobMessage msg) {
    switch (msg) {
      case ActorMessage.OnPlayerEnter(var playerId) -> behavior.handle(this, msg);
      case ActorMessage.OnPlayerFlee(var playerId, var direction) -> behavior.handle(this, msg);
      case ActorMessage.OnInteract(var playerId, var command) -> behavior.handle(this, msg);
      case ActorMessage.AgroScan() -> behavior.handle(this, msg);
      case ActorMessage.RandomMove() -> behavior.handle(this, msg);
      case ActorMessage.Respawn() -> behavior.handle(this, msg);
    }
  }

  // public void tick() {
  // // 委派給 Behavior
  // // behavior.onTick(this);

  // long now = System.currentTimeMillis();

  // // 1. 戰鬥邏輯
  // // log.info("this.state.isInCombat: {}", this.state.isInCombat);
  // if (this.state.isInCombat) {
  // processCombatRound(now);
  // } else {
  // // 2. 非戰鬥邏輯 (巡邏、回復 HP)
  // // processRegen();
  // behavior.onTick(this);
  // }
  // }

  // -----------------------------------------------------------------------------------

  // --- 互動與事件 ---

  public void onPlayerEnter(String playerId) {
    this.send(new ActorMessage.OnPlayerEnter(playerId));
  }

  public void onInteract(Player player, String command) {
    behavior.onInteract(this, player, command);
  }

  // @Override
  // protected void doOnAttacked(LivingActor attacker) {

  // // 檢查是否正在戰鬥
  // if (!this.state.isInCombat) {
  // this.state.isInCombat = true;
  // }

  // // TODO 檢查仇恨表，選最高的當對手
  // this.state.combatTargetId = attacker.getId();

  // // TODO 檢查仇恨表，選最高的當對手
  // // if (this.state.combatTargetId == null) {
  // // this.state.combatTargetId = attacker.getId();
  // // }

  // // 無敵判定
  // if (template.isInvincible()) {
  // if (attacker instanceof PlayerActor p) {
  // p.reply(this.template.name() + " 毫髮無傷！");
  // }
  // return;
  // }

  // // 1. 扣血 (呼叫父類別)
  // log.info("hp: {} defense: {} attacker.damage: {}", this.state.hp, this.state.defense,
  // attacker.getState().damage);
  // // super.onAttacked(attacker);

  // if (this.state.hp <= 0) {
  // doDie(attacker.id);
  // }

  // // 2. 增加仇恨值 (使用 String ID)
  // addAggro(attacker.getId(), attacker.getState().damage);

  // // 3. 通知 AI
  // behavior.onDamaged(this, attacker);
  // }

  // @Override
  // protected void doDie(String killerId) {
  // log.info("{} died. Killer: {}", this.template.name(), killerId);

  // super.doDie(killerId);
  // stop(); // 停止 Actor
  // this.state.isInCombat = false;
  // this.state.combatTargetId = null;



  // 觸發掉寶、給予經驗值等邏輯...

  // 製造屍體
  // GameItem corpse = services.worldFactory().createCorpse(this);

  // 通知房間：移除我，加入屍體
  // 這裡需要發送一個複合訊息，或者兩個訊息
  // 為了簡單，我們先發送 "加入物品" 再發送 "移除 Actor"

  // 注意：這裡假設 RoomMessage 有 AddItem
  // room.send(new RoomMessage.AddItem(corpse));
  // room.send(new RoomMessage.MobLeave(this.id)); // 或 RemoveActor

  // 但更建議用一個專門的 Death 訊息讓 Room 處理所有事
  // room.send(new RoomMessage.MobDeath(this, corpse, killer));
  // }

  // 仇恨值管理
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

  public void attack(Living target) {
    // 實作：發送 AttackMessage 給 target
  }

  private void processCombatRound(long now) {
    // 檢查攻擊冷卻
    log.info("this.state.nextAttackTime: {}", nextAttackTime);
    if (now < nextAttackTime) {
      return;
    }

    // 取得當前最高仇恨目標
    log.info("getHighestAggroTarget: {}", getHighestAggroTarget());
    String targetId = getHighestAggroTarget();
    if (targetId == null) {
      isInCombat = false; // 沒目標，脫離戰鬥
      return;
    }

    // processAutoAttack(now);

    // 從房間取得目標 Actor (這裡需要 WorldManager 協助，或 RoomActor 傳遞)
    // 假設這段邏輯在 RoomActor 處理會更好，但若在 Mob 處理：
    // LivingActor target = services.worldManager().getRoomActor(currentRoomId).findActor(targetId);

    // 為了簡單，假設我們能取到 target
    // int dmg = combatService.calculateDamage(this, target);
    // target.onAttacked(this, dmg);

    // 重設冷卻時間
    nextAttackTime = now + attackSpeed;
  }

}
