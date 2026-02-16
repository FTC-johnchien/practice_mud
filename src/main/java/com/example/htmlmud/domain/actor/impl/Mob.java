package com.example.htmlmud.domain.actor.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.domain.actor.behavior.AggressiveBehavior;
import com.example.htmlmud.domain.actor.behavior.MerchantBehavior;
import com.example.htmlmud.domain.actor.behavior.MobBehavior;
import com.example.htmlmud.domain.actor.behavior.PassiveBehavior;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.domain.service.MobService;
import com.example.htmlmud.protocol.ActorMessage;
import com.example.htmlmud.protocol.ActorMessage.MobMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
public final class Mob extends Living {

  private final MobService service;

  // 仇恨列表 (Key: 攻擊者 ID字串, Value: 仇恨值)
  // 因為 ID 已改為 UUID String，這裡的 Key 也同步調整為 String
  private final Map<String, Integer> aggroTable = new ConcurrentHashMap<>();

  private final MobTemplate template;

  private MobBehavior behavior; // AI 行為 (策略模式)

  // mob的戶籍地 ID
  private String homeRoomId;


  /**
   * 建構子： 1. 接收 Template 與 Services 2. 自動生成 UUID 3. 自動從 Template 建立 LivingState
   */
  public Mob(MobTemplate template, LivingStats state, MobService mobService) {
    String uuid = UUID.randomUUID().toString();
    String mobId = "m-" + uuid.substring(0, 8) + uuid.substring(9, 11);
    super(mobId, template.name(), state, mobService.getLivingServiceProvider().getObject());
    this.template = template;
    this.service = mobService;
    this.name = template.name();
    this.aliases = template.aliases();

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
      case ActorMessage.GetHighestAggroTarget(var future) -> {
        future.complete(service.getHighestAggroTarget(this));
      }
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



  @Override
  protected void performRemoveFromRoom(Room room) {
    room.removeMob(id);
  }

  @Override
  protected void performDeath() {

    // 將自己移出房間
    super.removeFromRoom();

    // 自我毀滅 (Terminate)：確認訊息發出後，才停止 VT。
    this.stop(); // 等待 gc 回收
  }

  @Override
  protected void handleOnAttacked(String attackerId) {
    super.handleOnAttacked(attackerId);
    aggroTable.merge(attackerId, 1, Integer::sum);
  }

  @Override
  protected void handleOnDamage(int amount, String attackerId) {
    super.handleOnDamage(amount, attackerId);

    // log.info("增加仇恨 name:{} {}", attacker.getName(), amount);
    if (isValid()) {
      aggroTable.merge(attackerId, amount, Integer::sum);
    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // 公開給外部呼叫的方法 --------------------------------------------------------------------------



  // 取得當前仇恨最高目標 ID
  public Optional<Living> getHighestAggroTarget() {
    CompletableFuture<Optional<Living>> future = new CompletableFuture<>();
    this.send(new ActorMessage.GetHighestAggroTarget(future));
    try {
      return future.orTimeout(1, TimeUnit.SECONDS).join();
    } catch (Exception e) {
      log.error("Mob 取得最高仇恨目標失敗 mobId:{}", id, e);
    }

    return Optional.empty();
  }

  // --- 互動與事件 ---
  public void onPlayerEnter(String playerId) {
    this.send(new ActorMessage.OnPlayerEnter(playerId));
  }

  public void onInteract(Player player, String command) {
    this.send(new ActorMessage.OnInteract(player.getId(), command));
  }

  // 行為層需要的輔助方法 (Facade)
  public void sayToRoom(String content) {
    // 實作：取得當前 RoomActor 並廣播
    // services.worldManager().getRoom(currentRoomId).broadcast(...)
  }

  public void attack(Living target) {
    // 實作：發送 AttackMessage 給 target
  }

  public String lookAtTarget() {
    return "你看我幹嘛 (TODO)";
  }
}
