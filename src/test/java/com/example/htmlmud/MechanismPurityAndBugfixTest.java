package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyInventory;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.save.service.SaveGameService;
import com.example.htmlmud.domain.service.TemplateCatalog;

@SpringBootTest
public class MechanismPurityAndBugfixTest {

  @Autowired
  private PartyService partyService;

  @Autowired
  private SaveGameService saveGameService;

  @Test
  @DisplayName("機制驗證：PartyInventory 容器建構子必須純淨，無任何寫死初始道具")
  void testPartyInventoryConstructorHasNoSideEffects() {
    PartyInventory pureInv = new PartyInventory(10, new TemplateCatalog());
    assertThat(pureInv.getSlots()).isEmpty();

    PartyInventory defaultInv = new PartyInventory();
    assertThat(defaultInv.getSlots()).isEmpty();
  }

  @Test
  @DisplayName("機制驗證：可堆疊物品超過 maxStack 時必須自動分槽，且不超出容量上限")
  void testPartyInventoryItemStackCapMechanism() {
    PartyInventory inv = new PartyInventory(5, new TemplateCatalog());

    // taiyin_pill 為可堆疊消耗品，maxStack 預設為 99
    // 加入 150 顆藥丸：第一格應滿 99 顆，第二格應有 51 顆
    boolean added = inv.addItem("taiyin_pill", 150);
    assertThat(added).isTrue();
    assertThat(inv.getSlots()).hasSize(2);

    PartyItemSlot slot1 = inv.getSlots().get(0);
    PartyItemSlot slot2 = inv.getSlots().get(1);

    assertThat(slot1.getCount()).isEqualTo(slot1.getMaxStack());
    assertThat(slot2.getCount()).isEqualTo(150 - slot1.getMaxStack());

    // 填滿剩餘容量至 5 格
    inv.addItem("taiyin_pill", 99);
    inv.addItem("taiyin_pill", 99);
    inv.addItem("taiyin_pill", 99);
    assertThat(inv.getSlots()).hasSize(5);

    // 當容量已達上限 (5)，再次加入新堆疊時應返回 false
    boolean overflowAdded = inv.addItem("steel_blade", 1);
    assertThat(overflowAdded).isFalse();
    assertThat(inv.getSlots()).hasSize(5);
  }

  @Test
  @DisplayName("機制驗證：夥伴別名與職業特徵技能完全由 JSON 資料驅動載入")
  void testDataDrivenCompanionLoading() {
    // 透過 JSON 中的別名查詢 (例如 "鐵牛" 定義於 default_companions.json 之 aliases)
    PartyMember tieNiu = partyService.createCompanion("鐵牛");
    assertThat(tieNiu).isNotNull();
    assertThat(tieNiu.getId()).isEqualTo("m-tie_niu");

    // 職業技能是動態掃描 tags:["CLASS"] 與 school:"WARRIOR" 而載入，絕非 Java switch hardcode
    boolean hasTaunt = tieNiu.getSkills().stream()
        .anyMatch(s -> s.getId().equals("class_warrior_taunt"));
    assertThat(hasTaunt).isTrue();

    // 驗證主角別名
    PartyMember leader = partyService.createCompanion("主角");
    assertThat(leader).isNotNull();
    assertThat(leader.getId()).isEqualTo("m-leader");
  }

  @Test
  @DisplayName("機制驗證：SaveGameService 槽位邊界校驗與原子化寫入防禦")
  void testSaveGameAtomicWriteAndSlotValidation() {
    // 1. 邊界校驗：非法槽位 (<0 或 >5) 必須拒絕
    assertThatThrownBy(() -> saveGameService.saveGame("tester", -1, "Illegal Slot"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("無效的存檔槽位");

    assertThatThrownBy(() -> saveGameService.saveGame("tester", 99, "Illegal Slot"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("無效的存檔槽位");

    // 2. 正常存檔：寫入 slot 1，驗證成功落盤且不殘留 .tmp 暫存檔
    var saved = saveGameService.saveGame("tester", 1, "機制驗證存檔");
    assertThat(saved).isNotNull();

    File targetFile = new File("saves", "slot_1.json");
    File tempFile = new File("saves", "slot_1.json.tmp");

    assertThat(targetFile.exists()).isTrue();
    assertThat(tempFile.exists()).isFalse();

    // 3. 讀檔驗證
    var loaded = saveGameService.loadGame("tester", 1);
    assertThat(loaded.getTitle()).isEqualTo("機制驗證存檔");
  }
}
