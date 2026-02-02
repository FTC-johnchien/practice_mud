package com.example.htmlmud.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
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



  public void onDie(Living self, Living killer) {
    // 標記狀態 (Mark State)：設為 Dead，停止接受新的傷害或治療。
    self.getState().setHp(0);
    self.isInCombat = false;
    self.combatTargetId = null;

    // 交代後事 (Cleanup & Notify)：取消心跳、製造屍體、通知房間。

    // 自我毀滅 (Terminate)：確認訊息發出後，才停止 VT。


    // 區分玩家跟mob
    switch (self) {
      case Player player -> {
        // TODO 玩家死亡步驟
      }
      case Mob mob -> {
        mob.stop(); // 停止 Actor
      }
    }

    Room room = self.getCurrentRoom();

    String messageTemplate = "$N殺死了$n";
    List<Living> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());
    for (Living receiver : audiences) {
      messageUtil.send(messageTemplate, killer, self, receiver);
    }
    // room.broadcast(killerId + " 殺死了 " + self.getName());


    // 製造屍體
    GameItem corpse = worldFactory.createCorpse(self);

    if (self.getCurrentRoom() == null) {
      log.error("{} onDie currentRoomId is null", self.getName());
      return;
    }

    // TODO 應交由房間處理 roomMessage livingDead 房間廣播死亡訊息
    // RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    // if (room != null) {
    // CompletableFuture<LivingActor> future = new CompletableFuture<>();
    // room.findActor(killerId, future);
    // LivingActor killer = future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
    // if (killer == null) {
    // log.error("killerId LivingActor not found: {}", killerId);
    // return;
    // }
    // room.broadcastToOthers(killerId, self.getName() + " 被 " + killer.getName() + " 殺死了！");
    // } else {
    // log.error("currentRoomId RoomActor not found: {}", self.getCurrentRoomId());
    // }
  }

  public boolean equip(Living self, GameItem item) {
    // 1. 取得 ItemTemplate (需要依賴 Service 或是 Item 本身帶有 slot 資訊)
    // 假設 GameItem 已經從 Template 複製了 slot 資訊，或者這裡去查 Template
    ItemTemplate tpl = item.getTemplate();
    if (tpl.type() != ItemType.WEAPON && tpl.type() != ItemType.SHIELD
        && tpl.type() != ItemType.ARMOR && tpl.type() != ItemType.ACCESSORY) {
      log.info(item.getDisplayName() + " 不是裝備");
      self.reply(item.getDisplayName() + " 不是裝備");
      return false;
    }

    EquipmentSlot slot = tpl.equipmentProp().slot();

    // 2. 檢查該部位是否已經有裝備？如果有，先脫下來 (Swap)
    if (self.getState().equipment.containsKey(slot)) {
      // GameItem oldItem = state.equipment.get(slot);
      unequip(self, slot); // 先脫舊的
      if (self.getState().equipment.containsKey(slot)) {
        log.info("無法脫下 " + slot.getDisplayName() + "，或背包已滿");
        self.reply("無法脫下 " + slot.getDisplayName() + "，或背包已滿");
        return false;
      }
    }

    // 3. 從背包移除該物品
    // 注意：這裡假設 inventory 是 Mutable List
    if (!self.getInventory().remove(item)) {
      log.info(item.getDisplayName() + "不在背包裡");
      self.reply(item.getDisplayName() + "不在背包裡");
      return false; // 物品不在背包裡
    }

    // 4. 放入裝備欄
    self.getState().equipment.put(slot, item);

    log.info("你裝上 " + item.getDisplayName());
    self.reply("你裝上 " + item.getDisplayName());
    return true;
  }

  public boolean unequip(Living self, EquipmentSlot slot) {
    log.info("unequip slot:{}", slot);

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

}
