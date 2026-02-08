package com.example.htmlmud.application.service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.impl.LookCommand;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.model.map.RaceTemplate;
import com.example.htmlmud.domain.service.CombatService;
import com.example.htmlmud.domain.service.SkillService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import com.example.htmlmud.infra.util.MessageUtil;
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

  private final TemplateRepository templateRepo;

  private final SkillService skillService;

  private final WorldFactory worldFactory;

  private final MessageUtil messageUtil;

  private final ObjectProvider<WorldManager> worldManagerProvider;

  private final LookCommand lookCommand;



  public void onDie(Living self, String killerId) {

    // 標記狀態 (Mark State)：設為 Dead，停止接受新的傷害或治療。
    self.getState().setHp(0);
    self.isInCombat = false;
    self.combatTargetId = null;

    // 交代後事 (Cleanup & Notify)：取消心跳、製造屍體、通知房間。
    // 取消心跳
    self.markInvalid();

    Room room = self.getCurrentRoom();
    // 將自己移出房間
    room.removeLiving(self.getId());

    // 製造屍體
    GameItem corpse = worldFactory.createCorpse(self);
    room.dropItem(corpse);

    // 廣播死亡訊息
    String messageTemplate = "$n殺死了$N";
    Living killer = worldManagerProvider.getObject().findLivingActor(killerId).orElse(null);
    if (killer == null) {
      log.error("killerId LivingActor not found: {}", killerId);

      // 不應該發生，但備用 XD
      killer = self;
      messageTemplate = "$N被殺死了";
    }

    List<Player> audiences = room.getPlayers();
    for (Player receiver : audiences) {
      messageUtil.send(messageTemplate, self, killer, receiver);
    }

    // 玩家死亡的後續流程
    if (self instanceof Player player) {

      // 讓 player 實作自行的過程
      self.processDeath();

    }

  }

  public void doEquip(Living self, GameItem item, CompletableFuture<Boolean> future) {

    // 1. 取得 ItemTemplate (需要依賴 Service 或是 Item 本身帶有 slot 資訊)
    // 假設 GameItem 已經從 Template 複製了 slot 資訊，或者這裡去查 Template
    ItemTemplate tpl = item.getTemplate();
    if (tpl.type() != ItemType.WEAPON && tpl.type() != ItemType.SHIELD
        && tpl.type() != ItemType.ARMOR && tpl.type() != ItemType.ACCESSORY) {

      if (self instanceof Player player) {
        player.reply(item.getDisplayName() + " 不是裝備");
      }

      future.complete(false);
      return;
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (self.getState().equipment.containsKey(slot)) {
      // GameItem oldItem = state.equipment.get(slot);
      unequip(self, slot); // 先脫舊的
      if (self.getState().equipment.containsKey(slot)) {
        if (self instanceof Player player) {
          player.reply("無法脫下 " + slot.getDisplayName() + "，或背包已滿");
        }

        future.complete(false);
        return;
      }
    }

    // 3. 從背包移除該物品
    // 注意：這裡假設 inventory 是 Mutable List
    if (!self.getInventory().remove(item)) {

      if (self instanceof Player player) {
        player.reply(item.getDisplayName() + "不在背包裡");
      }

      future.complete(false);
      return;
    }

    // 4. 放入裝備欄
    self.getState().equipment.put(slot, item);

    self.reply("你裝上 " + item.getDisplayName());
    future.complete(true);
  }

  /**
   * 脫下裝備
   */
  public void doUnequip(Living self, EquipmentSlot slot, CompletableFuture<Boolean> future) {
    GameItem item = self.getState().equipment.get(slot);
    if (item == null) {
      future.complete(false);
      self.reply("該 slot 沒有裝備");
      return;
    }

    // // 1. 放入背包
    // inventory.add(item);

    // // 2. 從裝備欄移除
    // state.equipment.remove(slot);

    // future.complete("你將 " + slot.getDisplayName() + " 放入背包");
    // return true;
    future.complete(true);
  }

  public boolean unequip(Living self, EquipmentSlot slot) {
    log.info("unequip slot:{}", slot);

    GameItem item = self.getState().equipment.get(slot);
    if (item == null) {
      // 該 slot 沒有裝備
      return true;
    }

    // // 1. 放入背包
    // self.inventory.add(item);

    // // 2. 從裝備欄移除
    // state.equipment.remove(slot);

    // future.complete("你將 " + slot.getDisplayName() + " 放入背包");
    // return true;

    return true;
  }

  public boolean use() {
    return false;
  }

  public int getAttacksPerRound(Living self) {
    Optional<RaceTemplate> opt = templateRepo.findRace(self.getState().getRace());
    if (opt.isPresent()) {
      RaceTemplate race = opt.get();
      if (race.combat() != null && race.combat().naturalAttacks() != null) {
        return race.combat().attacksPerRound();
      }
    }

    return 1;
  }


  public void processDeath(Living self) {
    // 自我毀滅 (Terminate)：確認訊息發出後，才停止 VT。
    if (self instanceof Mob mob) {
      mob.stop(); // 等待 gc 回收
    }
  }
}
