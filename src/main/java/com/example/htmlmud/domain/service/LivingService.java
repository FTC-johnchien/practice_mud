package com.example.htmlmud.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.impl.LookCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.ItemType;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.RaceTemplate;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.MessageUtil;
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

  private final WorldFactory worldFactory;

  private final ObjectProvider<WorldManager> worldManagerProvider;

  private final LookCommand lookCommand;



  public void tick(Living self, long tickCount, long time) {

    // 狀態無效不處理心跳
    if (!self.isValid()) {
      return;
    }

    // === 回復/狀態心跳 (Regen Tick) ===
    // 頻率：每 15 秒執行一次 (mud時間 0.1小時)
    if (!self.isInCombat() && tickCount % 150 == 0) {
      processRegen(self);
      // processBuffs(); // 檢查 Buff 是否過期
    }

    // === AI 行為心跳 (AI Tick) ===
    // 頻率：每 5 秒執行一次
    // 只有怪物需要，玩家不需要
    if (self instanceof Mob mob && tickCount % 5 == 0) {
      // mob.processAI(); // 例如：隨機移動、喊話
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

    // 扣除 HP
    self.getStats().setHp(self.getStats().getHp() - amount);

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
      if (!self.isInCombat) {

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
    self.isInCombat = false;
    self.combatTargetId = null;

    // 交代後事 (Cleanup & Notify)：取消心跳、製造屍體、通知房間。
    // 設定為無效狀態 (不處理心跳 tick)
    self.markInvalid();

    Room room = self.getCurrentRoom();

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

    // 製造屍體丟到房間
    GameItem corpse = worldFactory.createCorpse(self, killerName);
    room.dropItem(corpse);


    List<Player> audiences = room.getPlayers();
    for (Player receiver : audiences) {
      MessageUtil.send(messageTemplate, self, killer, receiver);
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

    // // 1. 放入背包
    // self.inventory.add(item);

    // // 2. 從裝備欄移除
    // state.equipment.remove(slot);

    // "你將 " + slot.getDisplayName() + " 放入背包";
    // return true;

    // 重新計算數值
    recalculateStats(self);

    return true;
  }

  public boolean use() {
    return false;
  }

  public int getAttacksPerRound(Living self) {
    Optional<RaceTemplate> opt = TemplateRepository.findRace(self.getStats().getRace());
    if (opt.isPresent()) {
      RaceTemplate race = opt.get();
      if (race.combat() != null && race.combat().naturalAttacks() != null) {
        return race.combat().attacksPerRound();
      }
    }

    return 1;
  }



  private void processRegen(Living self) {

    // hp 回復 5%
    if (self.getStats().getHp() < self.getStats().getMaxHp()) {
      int regenAmount = (int) (self.getStats().getMaxHp() * 0.05); // 回復 5%
      heal(self, regenAmount);
    }

    // mp 回復 1%

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
