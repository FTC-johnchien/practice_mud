package com.example.htmlmud.domain.actor;

import com.example.htmlmud.domain.actor.core.VirtualActor;
import com.example.htmlmud.domain.context.GameServices;
import com.example.htmlmud.domain.model.GameObjectId;
import com.example.htmlmud.domain.model.json.LivingState;
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
  protected String name;

  @Getter
  @Setter
  protected String displayName;

  // 所有生物都有狀態 (HP/MP)
  @Getter
  protected LivingState state;

  // 所有生物都在某個房間 (可能是 null)
  @Getter
  @Setter
  protected int currentRoomId;

  public LivingActor(String id, LivingState state, GameServices services) {
    super(id); // Actor Name: "PLAYER:1"
    this.id = id;
    this.state = state;
    this.services = services;
  }

  // 供子類別 (PlayerActor) 呼叫，用來切換數據
  protected void swapIdentity(String newId, String name, String displayName, LivingState state) {
    this.id = newId;
    this.name = name;
    this.displayName = displayName;
    this.state = state;
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
  protected void die(String killerId) {
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

  protected void onAttacked(LivingActor attacker, int damage) {

  }

  // 定義抽象方法，子類別必須實作
  protected void onDeath(String killerId) {

  }
}
