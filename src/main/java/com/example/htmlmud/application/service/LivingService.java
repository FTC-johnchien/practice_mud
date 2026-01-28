package com.example.htmlmud.application.service;

import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.model.EquipmentSlot;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.ItemType;
import com.example.htmlmud.domain.model.map.ItemTemplate;
import com.example.htmlmud.domain.service.CombatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class LivingService {

  @Getter
  protected final ObjectMapper objectMapper;

  @Getter
  protected final CombatService combatService;

  public boolean equip(LivingActor self, GameItem item) {
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

  public boolean unequip(LivingActor self, EquipmentSlot slot) {
    log.info("unequip slot:{}", slot);

    return true;
  }

  public boolean use() {
    return false;
  }

}
