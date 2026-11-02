package com.example.htmlmud.item;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.party.PartyItemSlot;
import lombok.extern.slf4j.Slf4j;

/**
 * 隨機裝備詞綴生成與加持服務 (Item Affix Service)
 * 遵循 Level 2 ITEM_SCHEMA 規格，為擊敗首領或地牢掉落裝備動態附加前綴與後綴。
 */
@Slf4j
@Service
public class ItemAffixService {

  private final List<ItemAffix> weaponPrefixes = List.of(
      new ItemAffix("affix_overclock_prefix", ItemAffix.AffixType.PREFIX, "超頻透鏡之", "physicalAttack", 5.0),
      new ItemAffix("affix_pojun_prefix", ItemAffix.AffixType.PREFIX, "破軍之", "physicalAttack", 8.0),
      new ItemAffix("affix_taisu_prefix", ItemAffix.AffixType.PREFIX, "太素之", "poiseBreakPower", 10.0),
      new ItemAffix("affix_crit_prefix", ItemAffix.AffixType.PREFIX, "敏銳之", "physicalAttack", 4.0),
      new ItemAffix("affix_shao_prefix", ItemAffix.AffixType.PREFIX, "凝煞之", "physicalAttack", 6.0)
  );

  private final List<ItemAffix> armorPrefixes = List.of(
      new ItemAffix("affix_xuanling_prefix", ItemAffix.AffixType.PREFIX, "玄靈之", "armor", 4.0),
      new ItemAffix("affix_jingang_prefix", ItemAffix.AffixType.PREFIX, "金剛之", "armor", 6.0),
      new ItemAffix("affix_resilient_prefix", ItemAffix.AffixType.PREFIX, "堅韌之", "armor", 5.0),
      new ItemAffix("affix_taisu_armor_prefix", ItemAffix.AffixType.PREFIX, "太素之", "poiseBreakPower", 8.0)
  );

  private final List<ItemAffix> weaponSuffixes = List.of(
      new ItemAffix("affix_jam_suffix", ItemAffix.AffixType.SUFFIX, "·干擾頻譜", "poiseBreakPower", 10.0),
      new ItemAffix("affix_pomo_suffix", ItemAffix.AffixType.SUFFIX, "·破魔", "physicalAttack", 6.0),
      new ItemAffix("affix_rage_suffix", ItemAffix.AffixType.SUFFIX, "·狂暴", "physicalAttack", 7.0),
      new ItemAffix("affix_breach_suffix", ItemAffix.AffixType.SUFFIX, "·破陣", "poiseBreakPower", 12.0)
  );

  private final List<ItemAffix> armorSuffixes = List.of(
      new ItemAffix("affix_heavy_armor_suffix", ItemAffix.AffixType.SUFFIX, "·重甲", "armor", 5.0),
      new ItemAffix("affix_fortress_suffix", ItemAffix.AffixType.SUFFIX, "·金湯", "armor", 6.0),
      new ItemAffix("affix_jam_armor_suffix", ItemAffix.AffixType.SUFFIX, "·干擾頻譜", "poiseBreakPower", 8.0)
  );

  public List<ItemAffix> getAvailablePrefixes() {
    List<ItemAffix> all = new ArrayList<>(weaponPrefixes);
    all.addAll(armorPrefixes);
    return all;
  }

  public List<ItemAffix> getAvailableSuffixes() {
    List<ItemAffix> all = new ArrayList<>(weaponSuffixes);
    all.addAll(armorSuffixes);
    return all;
  }

  /**
   * 為裝備槽位 (武器/防具/飾品/晶片) 生成並綁定隨機前綴與後綴
   */
  public void applyRandomAffixes(PartyItemSlot slot) {
    if (slot == null || !slot.isEquipment()) {
      return;
    }
    // 根據部位選取詞綴池
    List<ItemAffix> prefPool = slot.isArmor() || slot.isShield() ? armorPrefixes : weaponPrefixes;
    List<ItemAffix> suffPool = slot.isArmor() || slot.isShield() ? armorSuffixes : weaponSuffixes;

    // 至少賦予 1 個詞綴，高品質 (RARE, EPIC, LEGENDARY, MYTHIC) 必定賦予前後雙詞綴
    boolean isHighQuality = slot.getQuality() != null &&
        (slot.getQuality().equalsIgnoreCase("RARE") ||
         slot.getQuality().equalsIgnoreCase("EPIC") ||
         slot.getQuality().equalsIgnoreCase("LEGENDARY") ||
         slot.getQuality().equalsIgnoreCase("MYTHIC"));

    int roll = ThreadLocalRandom.current().nextInt(100);
    boolean givePrefix = isHighQuality || roll < 70;
    boolean giveSuffix = isHighQuality || (givePrefix && roll < 40) || (!givePrefix);

    if (givePrefix && !prefPool.isEmpty()) {
      ItemAffix p = prefPool.get(ThreadLocalRandom.current().nextInt(prefPool.size()));
      slot.addAffix(p);
    }
    if (giveSuffix && !suffPool.isEmpty()) {
      ItemAffix s = suffPool.get(ThreadLocalRandom.current().nextInt(suffPool.size()));
      slot.addAffix(s);
    }

    log.debug("Applied affixes to item {}: {}", slot.getItemId(), slot.getDisplayNameWithAffixes());
  }

  /**
   * 手動指定賦予詞綴
   */
  public void applyAffix(PartyItemSlot slot, ItemAffix affix) {
    if (slot != null && affix != null) {
      slot.addAffix(affix);
    }
  }
}

