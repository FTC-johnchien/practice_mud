package com.example.htmlmud.domain.actor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.example.htmlmud.domain.actor.behavior.AggressiveBehavior;
import com.example.htmlmud.domain.actor.behavior.MerchantBehavior;
import com.example.htmlmud.domain.actor.behavior.MobBehavior;
import com.example.htmlmud.domain.actor.behavior.PassiveBehavior;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.GameObjectId;
import com.example.htmlmud.domain.model.json.LivingState;
import com.example.htmlmud.domain.model.map.MobTemplate;
import com.example.htmlmud.protocol.ActorMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MobActor extends LivingActor {

  @Getter
  private final MobTemplate template;

  private ScheduledFuture<?> aiTask; // 用來保存排程，以便怪物死亡時取消

  // 【核心】AI 行為模式
  private MobBehavior behavior;

  // Mob 特有：掉落表、仇恨列表、AI 狀態
  // protected final List<Long> dropItemIds;
  protected final Map<GameObjectId, Integer> aggroTable = new HashMap<>();

  public MobActor(MobTemplate template, LivingState state, GameServices gameServices) {
    String tmpId = UUID.randomUUID().toString().substring(0, 8);
    super("mob-" + tmpId, state, gameServices);
    this.template = template;
    // this.dropItemIds = drops;
    // log.info("template.currentRoomId: {}", this.template.currentRoomId());

    // 根據 Template 初始化預設行為
    if (template.shopId() != null) {
      this.behavior = new MerchantBehavior(template.shopId());
    } else if (template.isAggressive()) {
      this.behavior = new AggressiveBehavior();
    } else {
      this.behavior = new PassiveBehavior();
    }

    log.info("currentRoomId: {}", this.currentRoomId);

    this.start(); // 在 MobActor 初始化完成後啟動
  }

  @Override
  public void start() {
    super.start();
    // startHeartbeat();
  }

  @Override
  protected void handleMessage(ActorMessage msg) {
    // 處理 AI 邏輯 (例如：收到 Tick 訊息 -> 攻擊仇恨最高的人)

    // ... 處理通用訊息 (扣血等) ...

    // 將事件轉發給 Behavior 決策
    behavior.onMessage(this, msg);
  }

  public void startAi() {
    // 透過 ScheduledExecutor 安排每 3 秒執行一次 tick
    // 注意：實際執行時要切換到 Virtual Thread 以免阻塞 Scheduler
    services.scheduler().scheduleAtFixedRate(() -> {
      Thread.ofVirtual().start(() -> {
        try {
          this.tick();
        } catch (Exception e) {
          // log error
        }
      });
    }, 1, 3, TimeUnit.SECONDS);
  }

  // 當有玩家進入房間時
  public void onPlayerEnter(PlayerActor player) {
    behavior.onPlayerEnter(this, player);
  }

  // 當被玩家點擊/說話時 (互動)
  public void onInteract(PlayerActor player, String command) {
    behavior.onInteract(this, player, command);
  }

  // 當受到攻擊時
  @Override
  public void onAttacked(LivingActor attacker, int damage) {
    if (template.isInvincible()) {
      // 無敵處理
      if (attacker instanceof PlayerActor p) {
        p.sendText(this.template.name() + " 毫髮無傷！");
      }
      return;
    }

    super.onAttacked(attacker, damage); // 扣血

    // AI 反擊決策
    behavior.onDamaged(this, attacker);
  }

  // --- 讓 AI 動起來 (Heartbeat) ---
  // 可以在 start() 時啟動一個定期任務
  public void tick() {
    behavior.onTick(this);
  }

  @Override
  protected void onDeath(String killerId) {
    super.onDeath(killerId);
    stopHeartbeat();

    // 怪物死亡邏輯：
    // 1. 計算掉寶 -> 產生 ItemActor 丟到房間
    // 2. 給兇手經驗值 -> WorldManager.getActor(killerId).send(ExpMsg)
    // 3. 從房間移除自己 -> currentRoom.send(RemoveMsg)
    // 4. 停止自己的 Actor -> this.stop()

    // 丟出 async eventBus 事件

  }

  public void sayToRoom(String message) {

  }

  public void attack(PlayerActor player) {

  }

  private void startHeartbeat() {
    // 防止重複啟動
    if (aiTask != null && !aiTask.isCancelled())
      return;

    // 【關鍵模式】
    // 1. Scheduler (Platform Thread): 每 3 秒鐘醒來一次
    this.aiTask = services.scheduler().scheduleAtFixedRate(() -> {

      // 2. 啟動 Virtual Thread: 執行實際邏輯
      // 這樣做的好處是：就算 mob.tick() 卡住或計算很久，
      // 也不會影響到 Scheduler 準時叫醒下一隻怪。
      Thread.ofVirtual().start(() -> {
        try {
          // 檢查怪物是否還活著
          if (this.state.hp > 0) {
            this.tick();
          } else {
            stopHeartbeat();
          }
        } catch (Exception e) {
          log.error("Mob tick error: {}", this.id, e);
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
}
