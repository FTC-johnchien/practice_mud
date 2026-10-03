package com.example.htmlmud.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.port.WorldEntityFactoryPort;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.protocol.MudMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Service
@RequiredArgsConstructor
public class LivingService {

  private final ObjectMapper objectMapper;

  private final CombatService combatService;

  private final SkillService skillService;

  private final TemplateReader templateReader;

  private final WorldEntityFactoryPort worldFactory;

  private final com.example.htmlmud.config.GameConfig gameConfig;

  private final ObjectProvider<WorldManager> worldManagerProvider;

  private final ObjectProvider<GameStateBroadcastService> broadcastServiceProvider;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private XpProgressionService xpProgressionService;

  public XpProgressionService getXpProgressionService() {
    if (xpProgressionService == null) {
      xpProgressionService = new XpProgressionService();
    }
    return xpProgressionService;
  }

  public void setXpProgressionService(XpProgressionService xpProgressionService) {
    this.xpProgressionService = xpProgressionService;
  }

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.dungeon.battle.BuffSettlementService buffSettlementService;

  public com.example.htmlmud.domain.dungeon.battle.BuffSettlementService getBuffSettlementService() {
    if (buffSettlementService == null) {
      buffSettlementService = new com.example.htmlmud.domain.dungeon.battle.BuffSettlementService();
    }
    return buffSettlementService;
  }

  public void setBuffSettlementService(com.example.htmlmud.domain.dungeon.battle.BuffSettlementService buffSettlementService) {
    this.buffSettlementService = buffSettlementService;
  }



  public void tick(Living self, long tickCount, long time) {

    // 狀態無效不處理心跳
    if (!self.isValid()) {
      return;
    }

    // === 回復/狀態心跳 (Regen Tick) ===
    int regenModulo = (gameConfig != null && gameConfig.getRegen() != null)
        ? gameConfig.getRegen().getTickModulo()
        : 150;
    if (!self.isInCombat() && tickCount % regenModulo == 0) {
      processRegen(self);
    }

    // === Buff / Debuff 週期跳算 (1 Tick = 500ms，在 100ms 基準下每 5 ticks 觸發一次) ===
    if (tickCount % 5 == 0) {
      processBuffs(self);
    }

    // === AI 行為心跳 (AI Tick) ===
    // 頻率：每 5 秒執行一次
    // 只有怪物需要，玩家不需要
    if (self instanceof Mob mob && tickCount % 5 == 0) {
      // mob.processAI(); // 例如：隨機移動、喊話
    }
  }

  public void processBuffs(Living self) {
    if (self == null || !self.isValid() || self.isDead()) {
      return;
    }
    List<String> logs = getBuffSettlementService().processTicks(self);
    if (logs != null && !logs.isEmpty()) {
      boolean sent = false;
      if (self.getCurrentRoomId() != null) {
        try {
          Room room = self.getCurrentRoom();
          if (room != null && !room.getPlayers().isEmpty()) {
            for (Player p : room.getPlayers()) {
              for (String l : logs) {
                p.sendText(l);
              }
            }
            sent = true;
          }
        } catch (Exception ignored) {
        }
      }
      if (!sent && self instanceof Player p) {
        for (String l : logs) {
          p.sendText(l);
        }
      }

      // 如果 Buff / DoT 致死
      if (self.isDead()) {
        log.info("{} 因狀態效果死亡", self.getName());
        onDeath(self, null);
      }
    }
  }

  public void onAttacked(Living self, String attackerId) {

    // 攻擊準備時間
    reactionTime(self);

    combatService.startCombat(self, attackerId);
  }

  public void onDamage(Living self, int amount, String attackerId) {

    // 檢查是否還活著
    if (!self.isValid()) {
      // log.info("{} 已經死亡，無法受傷", self.getName());
      return;
    }

    // 透過 Buffable 護盾扣除與扣除 HP
    self.takeDamage(amount);

    // for test----------------------------------------------------------------------------------

    // room.broadcast("log:" + self.getName() + " 目前 HP: " + self.getStats().getHp() + "/"
    // + self.getStats().getMaxHp());
    // for test----------------------------------------------------------------------------------

    // 檢查是否死亡
    if (self.isDead()) {
      log.info("{} 被殺死了-", self.getName());

      // 終止戰鬥並移出戰鬥名單
      combatService.endCombat(self);

      // Room room = self.getCurrentRoom();

      // 先判斷 self 是 player 還是 mob 然後將其移出房間，避免後續的攻擊還持續的打到已死亡的對象
      // if (self instanceof Player player) {
      // room.removePlayer(player.getId());
      // } else if (self instanceof Mob mob) {
      // room.removeMob(mob.getId());
      // }

      // 發送 self 死亡事件
      self.onDeath(attackerId);
    } else {
      if (!self.isInCombat()) {

        // 準備反應時間
        reactionTime(self);

        // 加入戰鬥
        combatService.startCombat(self, attackerId);
      }
    }
  }

  public void onDeath(Living self, String killerId) {

    // 標記狀態 (Mark State)：設為 Dead，停止接受新的傷害或治療。
    self.getStats().setHp(0);
    self.exitCombat();
    self.setPosture(com.example.htmlmud.domain.model.enums.LivingPosture.DEAD);

    // 交代後事 (Cleanup & Notify)：取消心跳、製造屍體、通知房間。
    // 設定為無效狀態 (不處理心跳 tick)
    self.markInvalid();

    Room room = self.getCurrentRoom();
    if (room == null) {
      return;
    }

    // 廣播死亡訊息
    String messageTemplate = "$n殺死了$N";
    // 先找房間里的 living
    Living killer = room.findLiving(killerId).orElse(null);
    String killerName = null;
    if (killer == null) {
      killer = worldManagerProvider.getObject().findLivingActor(killerId).orElse(null);
    }
    if (killer != null) {
      killerName = killer.getName();
    }

    // killer(mob) 也可能在這輪攻擊中死亡(例如30個 monk 用獅子吼互打)，但處理速度比較快造成 null
    if (killer == null) {
      killer = self;
      messageTemplate = "$N被殺死了";
    }

    // 若為怪物，立即自房間生靈清單移除，並依掉落表決定消散或掉落儲物袋
    if (self instanceof com.example.htmlmud.domain.actor.impl.Mob mob) {
      room.removeMob(mob.getId());

      List<GameItem> drops = worldFactory.generateMobDrops(mob);
      if (drops.isEmpty()) {
        // 無掉落物：直接化作青煙消散，地面不留任何物品
        messageTemplate = "\u001B[1;30m💀 $N 倒地身亡，化作一縷青煙消散於天地之間...\u001B[0m";
      } else {
        GameItem lootPouch = worldFactory.createLootPouch(mob, drops);
        // 普通怪的儲物袋（非首領寶箱）：檢查地面是否已有【散落的儲物袋】，若有則自動合併 (增加房間同步鎖消除併發競態)
        boolean isNormalPouch = "【散落的儲物袋】".equals(lootPouch.getName());
        synchronized (room) {
          Optional<GameItem> existingPouch = isNormalPouch ? room.getItems().stream()
              .filter(it -> it != null && it.getType() == ItemType.CONTAINER && "【散落的儲物袋】".equals(it.getName()))
              .findFirst() : Optional.empty();

          if (existingPouch.isPresent()) {
            GameItem targetPouch = existingPouch.get();
            synchronized (targetPouch) {
              targetPouch.getContents().addAll(drops);
            }
            messageTemplate = "\u001B[1;32m💥 $N 被擊敗倒地，戰利品歸攏入地面的 " + targetPouch.getName() + "！\u001B[0m";
          } else {
            room.dropItem(lootPouch);
            messageTemplate = "\u001B[1;32m💥 $N 被擊敗倒地，戰利品散落在地，化為 " + lootPouch.getName() + "！\u001B[0m";
          }
        }
      }

      // 若擊殺者為玩家，結算修為經驗值並發放給玩家 (MUD-02)
      if (killer instanceof Player player && player.isValid()) {
        int mobLevel = (mob.getStats() != null) ? mob.getStats().getLevel() : 1;
        int playerLevel = (player.getStats() != null) ? player.getStats().getLevel() : 1;
        int expReward = getXpProgressionService().calculateMobExpReward(mobLevel, playerLevel);
        player.gainExp(expReward);
        player.reply("\u001B[1;32m你擊敗了 " + mob.getName() + "，獲得了 " + expReward + " 點修為經驗！\u001B[0m");
      }
    } else {
      // 玩家死亡時保留屍體轉移裝備遺物
      GameItem corpse = worldFactory.createCorpse(self, killerName);
      room.dropItem(corpse);
    }

    List<Player> audiences = room.getPlayers();
    GameStateBroadcastService bs = broadcastServiceProvider.getIfAvailable();
    for (Player receiver : audiences) {
      MessageUtil.send(messageTemplate, self, killer, receiver);
      if (bs != null) {
        bs.broadcastState(receiver);
      }
    }
  }

  public void heal(Living self, int amount) {
    if (!self.isValid()) {
      // log.info("{} 已經死亡，無法治療", name);
      return;
    }

    // reply(this.stats.getGender().getYou() + "回復了 " + amount + " 點 HP 目前 " + stats.getHp() + " / "
    // + stats.getMaxHp());
    self.getStats().setHp(Math.min(self.getStats().getHp() + amount, self.getStats().getMaxHp()));
  }



  public boolean equip(Living self, GameItem item) {

    // 1. 取得 ItemTemplate (需要依賴 Service 或是 Item 本身帶有 slot 資訊)
    // 假設 GameItem 已經從 Template 複製了 slot 資訊，或者這裡去查 Template
    ItemTemplate tpl = item.getTemplate();
    if (tpl.type() != ItemType.WEAPON && tpl.type() != ItemType.SHIELD
        && tpl.type() != ItemType.ARMOR && tpl.type() != ItemType.ACCESSORY) {

      if (self instanceof Player player) {
        player.reply(item.getDisplayName() + " 不是裝備");
      }

      return false;
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (self.getStats().equipment.containsKey(slot)) {
      // GameItem oldItem = state.equipment.get(slot);
      unequip(self, slot); // 先脫舊的
      if (self.getStats().equipment.containsKey(slot)) {
        if (self instanceof Player player) {
          player.reply("無法脫下 " + slot.getDisplayName() + "，或背包已滿");
        }

        return false;
      }
    }

    // 3. 從背包移除該物品
    // 注意：這裡假設 inventory 是 Mutable List
    if (!self.getInventory().remove(item)) {

      if (self instanceof Player player) {
        player.reply(item.getDisplayName() + "不在背包裡");
      }

      return false;
    }

    // 4. 放入裝備欄
    self.getStats().equipment.put(slot, item);

    // 重新計算數值
    recalculateStats(self);

    if (self instanceof Player player) {
      player.reply("你裝上 " + item.getDisplayName());
    }

    return true;
  }

  /**
   * 脫下裝備
   */
  public boolean unequip(Living self, EquipmentSlot slot) {
    log.info("unequip slot:{}", slot);

    GameItem item = self.getStats().equipment.get(slot);

    // 該 slot 沒有裝備
    if (item == null) {
      if (self instanceof Player player) {
        player.reply("該 slot 沒有裝備");
      }

      return true;
    }

    // 1. 放入背包
    self.getInventory().add(item);

    // 2. 從裝備欄移除
    self.getStats().equipment.remove(slot);

    if (self instanceof Player player) {
      player.reply("你將 " + slot.getDisplayName() + " 的 " + item.getDisplayName() + " 放入背包");
    }

    // 重新計算數值
    recalculateStats(self);

    return true;
  }

  public boolean use(Living self, GameItem item) {
    if (self == null || item == null || !self.isValid() || self.isDead()) {
      return false;
    }

    ItemTemplate tpl = item.getTemplate();
    var def = item.getDefinition();
    if ((tpl != null && tpl.type() != ItemType.CONSUMABLE) || (def != null && !def.isConsumable())) {
      if (self instanceof Player p) {
        p.reply("這件物品無法直接服用使用。");
      }
      return false;
    }

    // 處理消耗品效果
    if (def != null) {
      String effType = def.effectType();
      int effVal = def.effectValue();
      if ("HEAL_HP".equalsIgnoreCase(effType)) {
        int healAmt = effVal > 0 ? effVal : 50;
        heal(self, healAmt);
        if (self instanceof Player p) {
          p.reply("\u001B[1;32m你服用了【" + item.getDisplayName() + "】，體內靈氣奔湧，氣血回復了 " + healAmt + " 點！\u001B[0m");
        }
      } else if ("HEAL_MP".equalsIgnoreCase(effType)) {
        int healAmt = effVal > 0 ? effVal : 30;
        if (self.getStats() != null) {
          self.getStats().setMp(Math.min(self.getStats().getMaxMp(), self.getStats().getMp() + healAmt));
        }
        if (self instanceof Player p) {
          p.reply("\u001B[1;34m你服用了【" + item.getDisplayName() + "】，體內法力充盈，回復了 " + healAmt + " 點法力！\u001B[0m");
        }
      } else if ("RESTORE_SAN".equalsIgnoreCase(effType)) {
        if (self.getStats() != null) {
          self.getStats().setSan(Math.min(self.getStats().getMaxSan(), self.getStats().getSan() + effVal));
        }
        if (self instanceof Player p) {
          p.reply("\u001B[1;36m你服用了【" + item.getDisplayName() + "】，神識清明，道心穩定。\u001B[0m");
        }
      } else {
        if (effVal > 0) {
          heal(self, effVal);
        }
      }
    }

    // 扣除物品數量或自背包移除
    if (item.getAmount() > 1) {
      item.setAmount(item.getAmount() - 1);
    } else {
      self.getInventory().remove(item);
    }

    return true;
  }

  public boolean use(Living self, String itemId) {
    if (self == null || itemId == null) return false;
    GameItem item = self.getInventory().stream()
        .filter(it -> it != null && (itemId.equalsIgnoreCase(it.getId()) || (it.getTemplate() != null && itemId.equalsIgnoreCase(it.getTemplate().id()))))
        .findFirst().orElse(null);
    if (item == null) {
      if (self instanceof Player p) {
        p.reply("你身上沒有這件物品。");
      }
      return false;
    }
    return use(self, item);
  }

  public int getAttacksPerRound(Living self) {
    Optional<RaceTemplate> opt = templateReader.findRace(self.getStats().getRace());
    if (opt.isPresent()) {
      RaceTemplate race = opt.get();
      if (race.combat() != null && race.combat().naturalAttacks() != null) {
        return race.combat().attacksPerRound();
      }
    }

    return 1;
  }



  public void processRegen(Living self) {
    if (self == null || self.getStats() == null || self.isDead()) {
      return;
    }

    double hpRatio = (gameConfig != null && gameConfig.getRegen() != null)
        ? gameConfig.getRegen().getHpPercent()
        : 0.05;
    double mpRatio = (gameConfig != null && gameConfig.getRegen() != null)
        ? gameConfig.getRegen().getMpPercent()
        : 0.01;

    // RoomFlag.HIGH_REGEN: 聚靈/高恢復環境加速回復 (加倍)
    Room room = self.getCurrentRoom();
    if (room != null && room.hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.HIGH_REGEN)) {
      hpRatio *= 2.0;
      mpRatio *= 2.0;
    }

    // 姿勢加成：打坐休息 (RESTING) 或安睡 (SLEEPING) 加速回復 50%
    if (self.getPosture() == com.example.htmlmud.domain.model.enums.LivingPosture.RESTING
        || self.getPosture() == com.example.htmlmud.domain.model.enums.LivingPosture.SLEEPING) {
      hpRatio *= 1.5;
      mpRatio *= 1.5;
    }

    // hp 回復
    if (self.getStats().getHp() < self.getStats().getMaxHp()) {
      int regenAmount = Math.max(1, (int) Math.round(self.getStats().getMaxHp() * hpRatio));
      heal(self, regenAmount);
    }

    // mp 回復
    if (self.getStats().getMp() < self.getStats().getMaxMp()) {
      int regenMpAmount = Math.max(1, (int) Math.round(self.getStats().getMaxMp() * mpRatio));
      self.getStats().setMp(Math.min(self.getStats().getMaxMp(), self.getStats().getMp() + regenMpAmount));
    }

    // stamina 回復
    if (self.getStats().getStamina() < self.getStats().getMaxStamina()) {
      int regenStamina = Math.max(5, (int) Math.round(self.getStats().getMaxStamina() * 0.10));
      self.getStats().setStamina(Math.min(self.getStats().getMaxStamina(), self.getStats().getStamina() + regenStamina));
    }
  }

  // 準備反應的時間
  private void reactionTime(Living self) {
    long speed = self.getAttackSpeed();
    // 計算 0.5 * speed +/- 0.1 * speed，即範圍 [0.4 * speed, 0.6 * speed]
    long reactionTime = ThreadLocalRandom.current().nextLong(speed * 4 / 10, (speed * 6 / 10) + 1);
    self.setNextAttackTime(System.currentTimeMillis() + reactionTime);
  }



  /**
   * 【重要】重新計算總屬性 每次穿脫裝備、升級、Buff 改變時呼叫
   */
  private void recalculateStats(Living self) {
    int minDamage = 0; // 基礎攻擊力 (可從 Level 算)
    int maxDamage = 0; // 基礎防禦力
    int def = 0; // 基礎防禦力

    // 遍歷所有裝備
    for (GameItem item : self.getStats().equipment.values()) {
      ItemTemplate tpl = item.getTemplate();
      if (tpl != null) {
        minDamage += tpl.equipmentProp().minDamage();
        maxDamage += tpl.equipmentProp().maxDamage();
        def += tpl.equipmentProp().defense();

        // 處理額外屬性 (Bonus Stats)
        // if (tpl.bonusStats() != null) ...
      }
    }

    self.minDamage = minDamage;
    self.maxDamage = maxDamage;
    self.defense = def;
    log.info("{} stats updated: minDamage={}, maxDamage={}, Def={}", self.getName(), minDamage,
        maxDamage, def);
  }

}
