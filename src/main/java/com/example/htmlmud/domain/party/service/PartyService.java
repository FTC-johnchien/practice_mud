package com.example.htmlmud.domain.party.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.EffectiveCombatStats;
import com.example.htmlmud.domain.party.model.FormationSkill;
import com.example.htmlmud.domain.party.model.FormationSlot;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.ResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PartyService {

  private final Map<String, FormationTemplate> formationRegistry = new HashMap<>();
  private final Map<String, Party> partyCache = new java.util.concurrent.ConcurrentHashMap<>();

  public Party getOrCreateParty(String playerName) {
    return partyCache.computeIfAbsent(playerName, this::createInitialParty);
  }

  public void setParty(String playerName, Party party) {
    if (playerName != null && party != null) {
      partyCache.put(playerName, party);
    }
  }

  public Party resetParty(String playerName, String protagonistName) {
    String pName = (protagonistName != null && !protagonistName.isBlank()) ? protagonistName : playerName;
    Party party = createSoloParty(pName);
    partyCache.put(playerName, party);
    return party;
  }

  @PostConstruct
  public void initDefaultFormations() {
    // 1. 正道正宗：《四象辟邪陣》
    FormationTemplate fourSymbols = FormationTemplate.builder()
        .id("formation_four_symbols")
        .name("四象辟邪陣")
        .description("正統道家護法大陣，依青龍、白虎、朱雀、玄武四象而列，鎮守心神，破除污濁煞氣。")
        .passiveAura("【辟邪靈言】全隊受擊傷害減少 10%，道心動搖機率下降 25%。")
        .ultimateSkill(FormationSkill.builder()
            .id("skill_four_symbols_flame")
            .name("四象純陽真火")
            .description("引動天地四象純陽真火，焚滅全體妖邪，並為全隊驅散一層污濁狀態！")
            .energyCost(100)
            .sanCost(0) // 正道陣法不侵蝕 SAN
            .effectType("AOE_DAMAGE")
            .basePower(120)
            .build())
        .slots(List.of(
            FormationSlot.builder().slotIndex(0).slotName("天樞【陣眼主位】").requiredRow(RowPosition.FRONT).attackMultiplier(1.15).defenseMultiplier(1.10).sanResistanceBonus(10).specialBonusDesc("全招式威力+15%").build(),
            FormationSlot.builder().slotIndex(1).slotName("天璇【玄武巨盾】").requiredRow(RowPosition.FRONT).attackMultiplier(0.90).defenseMultiplier(1.35).sanResistanceBonus(15).specialBonusDesc("免傷+35%，受擊率大幅提升").build(),
            FormationSlot.builder().slotIndex(2).slotName("天璣【白虎先鋒】").requiredRow(RowPosition.FRONT).attackMultiplier(1.25).defenseMultiplier(1.05).sanResistanceBonus(0).specialBonusDesc("近戰暴擊率提升").build(),
            FormationSlot.builder().slotIndex(3).slotName("天權【朱雀靈符】").requiredRow(RowPosition.BACK).attackMultiplier(1.20).defenseMultiplier(0.95).sanResistanceBonus(5).specialBonusDesc("道術傷害+20%").build(),
            FormationSlot.builder().slotIndex(4).slotName("玉衡【青龍生息】").requiredRow(RowPosition.BACK).attackMultiplier(0.85).defenseMultiplier(1.00).sanResistanceBonus(20).specialBonusDesc("治療效果+30%").build(),
            FormationSlot.builder().slotIndex(5).slotName("開陽【游龍輔御】").requiredRow(RowPosition.BACK).attackMultiplier(1.05).defenseMultiplier(1.10).sanResistanceBonus(10).specialBonusDesc("靈巧速度+15%").build()
        ))
        .build();

    // 2. 禁忌古神邪陣：《玄陰噬魂陣》
    FormationTemplate xuanYin = FormationTemplate.builder()
        .id("formation_xuan_yin")
        .name("玄陰噬魂陣")
        .description("參照域外不可名狀星屑所創的凶煞魔陣，以瘋狂與道心為代價，換取撕裂虛空的毀滅威能。")
        .passiveAura("【深淵共鳴】全隊道術傷害提高 25%，但每場戰鬥開始全員損失 3 點道心 SAN。")
        .ultimateSkill(FormationSkill.builder()
            .id("skill_abyss_eclipse")
            .name("不可名狀星蝕")
            .description("撕開星空裂隙，召喚無形盲目癡愚之凝視，對敵方全體造成毀滅性暗蝕傷害！")
            .energyCost(100)
            .sanCost(8) // 禁忌陣法：扣除全隊 8 點 SAN
            .effectType("AOE_DAMAGE")
            .basePower(250)
            .build())
        .slots(List.of(
            FormationSlot.builder().slotIndex(0).slotName("歸墟【古神祭眼】").requiredRow(RowPosition.BACK).attackMultiplier(1.40).defenseMultiplier(0.80).sanResistanceBonus(-10).specialBonusDesc("道術傷害+40%，SAN衰減加劇").build(),
            FormationSlot.builder().slotIndex(1).slotName("冥煞【死肉魔傀】").requiredRow(RowPosition.FRONT).attackMultiplier(1.10).defenseMultiplier(1.30).sanResistanceBonus(0).specialBonusDesc("承受主要打擊").build(),
            FormationSlot.builder().slotIndex(2).slotName("血刃【弒生狂劍】").requiredRow(RowPosition.FRONT).attackMultiplier(1.35).defenseMultiplier(0.90).sanResistanceBonus(-5).specialBonusDesc("物理傷害+35%").build(),
            FormationSlot.builder().slotIndex(3).slotName("幽蠱【百足噬靈】").requiredRow(RowPosition.FRONT).attackMultiplier(1.15).defenseMultiplier(1.10).sanResistanceBonus(0).specialBonusDesc("附加劇毒與詛咒").build(),
            FormationSlot.builder().slotIndex(4).slotName("濁魂【引渡巫使】").requiredRow(RowPosition.BACK).attackMultiplier(1.20).defenseMultiplier(0.90).sanResistanceBonus(0).specialBonusDesc("吸取敵方生命反哺隊伍").build(),
            FormationSlot.builder().slotIndex(5).slotName("幻夢【黃衣信徒】").requiredRow(RowPosition.BACK).attackMultiplier(1.30).defenseMultiplier(0.85).sanResistanceBonus(-15).specialBonusDesc("高機率使敵方陷入混亂").build()
        ))
        .build();

    formationRegistry.put(fourSymbols.getId(), fourSymbols);
    formationRegistry.put(xuanYin.getId(), xuanYin);
    log.info("Initialized {} default formations: {}", formationRegistry.size(), formationRegistry.keySet());
  }

  public FormationTemplate getFormation(String id) {
    return formationRegistry.get(id);
  }

  public Party createInitialParty(String leaderName) {
    return createFullParty(leaderName);
  }

  public Party createSoloParty(String leaderName) {
    Party party = Party.builder()
        .id("party-" + leaderName)
        .partyName(leaderName + "的問道旅團")
        .equippedFormation(formationRegistry.get("formation_four_symbols"))
        .formationEnergy(50)
        .build();

    // 1. 主角 (天劍修士)
    LivingStats leaderStats = new LivingStats();
    leaderStats.setHp(120);
    leaderStats.setMaxHp(120);
    leaderStats.setMp(60);
    leaderStats.setMaxMp(60);
    leaderStats.setStr(12);
    leaderStats.setDex(10);
    leaderStats.setCon(10);
    leaderStats.setIntelligence(10);

    party.addMember(PartyMember.builder()
        .id("m-leader")
        .name(leaderName)
        .roleTitle("太陰傳真・天劍弟子")
        .row(RowPosition.FRONT)
        .stats(leaderStats)
        .baseMinDamage(15)
        .baseMaxDamage(25)
        .baseDefense(5)
        .currentSan(100)
        .maxSan(100)
        .resourceType(ResourceType.COMBO)
        .skills(List.of(
            PartyMemberSkill.builder().id("sword_pierce").name("破空劍氣").icon("🗡️").description("凝聚太陰劍氣，貫穿單一敵人造成 180% 物理傷害").costType(ResourceType.COMBO).costValue(2).cooldownMs(4000).damageMultiplier(1.8).build(),
            PartyMemberSkill.builder().id("sword_storm").name("太陰萬劍訣").icon("⚔️").description("引動漫天劍雨，對敵方全體造成 140% AOE 劍氣傷害").costType(ResourceType.COMBO).costValue(4).cooldownMs(8000).damageMultiplier(1.4).aoe(true).build()
        ))
        .build());

    return party;
  }

  public Party createFullParty(String leaderName) {
    Party party = createSoloParty(leaderName);
    recruitCompanion(party, "iron");
    recruitCompanion(party, "yan");
    recruitCompanion(party, "ling");
    recruitCompanion(party, "mo");
    recruitCompanion(party, "zi");
    return party;
  }

  public PartyMember createCompanion(String key) {
    if (key == null) return null;
    String k = key.toLowerCase();
    if (k.contains("iron") || k.contains("tie_niu") || k.contains("鐵牛")) {
      LivingStats tankStats = new LivingStats();
      tankStats.setHp(180);
      tankStats.setMaxHp(180);
      tankStats.setMp(20);
      tankStats.setMaxMp(20);
      tankStats.setStr(15);
      tankStats.setCon(16);
      tankStats.setDex(6);

      return PartyMember.builder()
          .id("m-iron")
          .name("鐵牛")
          .roleTitle("搬山力士・玄甲體修")
          .row(RowPosition.FRONT)
          .stats(tankStats)
          .baseMinDamage(10)
          .baseMaxDamage(18)
          .baseDefense(12)
          .currentSan(90)
          .maxSan(100)
          .resourceType(ResourceType.RAGE)
          .skills(List.of(
              PartyMemberSkill.builder().id("tank_taunt").name("金剛怒目").icon("🛡️").description("怒吼嘲諷全體敵人強行攻擊自己 5 秒，並提升 30% 防禦").costType(ResourceType.RAGE).costValue(30).cooldownMs(10000).taunt(true).build(),
              PartyMemberSkill.builder().id("tank_smash").name("裂地崩山").icon("🔨").description("揮舞玄重鎚重砸前排，造成 200% 鈍擊傷害並震懾暈眩 2 秒").costType(ResourceType.RAGE).costValue(60).cooldownMs(12000).damageMultiplier(2.0).stun(true).stunDurationSeconds(2).build()
          ))
          .build();
    } else if (k.contains("ling") || k.contains("ling_shuang") || k.contains("凌霜")) {
      LivingStats healerStats = new LivingStats();
      healerStats.setHp(80);
      healerStats.setMaxHp(80);
      healerStats.setMp(100);
      healerStats.setMaxMp(100);
      healerStats.setStr(6);
      healerStats.setCon(8);
      healerStats.setDex(8);
      healerStats.setIntelligence(14);

      return PartyMember.builder()
          .id("m-ling")
          .name("凌霜")
          .roleTitle("懸壺青囊・素問靈醫")
          .row(RowPosition.BACK)
          .stats(healerStats)
          .baseMinDamage(5)
          .baseMaxDamage(10)
          .baseDefense(3)
          .currentSan(95)
          .maxSan(100)
          .resourceType(ResourceType.MP)
          .skills(List.of(
              PartyMemberSkill.builder().id("heal_single").name("九轉回春").icon("🌿").description("運轉素問真元，為我方血量最低成員回復 40 點氣血").costType(ResourceType.MP).costValue(30).cooldownMs(5000).heal(true).healAmount(40).build(),
              PartyMemberSkill.builder().id("heal_all_purify").name("辟邪清心咒").icon("✨").description("誦唸辟邪心咒，回復全隊 25 點氣血並平復道心 (+10 SAN)").costType(ResourceType.MP).costValue(40).cooldownMs(12000).heal(true).aoe(true).healAmount(25).sanRestore(10).build()
          ))
          .build();
    } else if (k.contains("yan") || k.contains("yan_qing") || k.contains("燕青")) {
      LivingStats rogueStats = new LivingStats();
      rogueStats.setHp(110);
      rogueStats.setMaxHp(110);
      rogueStats.setMp(50);
      rogueStats.setMaxMp(50);
      rogueStats.setStr(11);
      rogueStats.setCon(9);
      rogueStats.setDex(16);

      return PartyMember.builder()
          .id("m-yan")
          .name("燕青")
          .roleTitle("驚鴻瞬影・穿林快劍")
          .row(RowPosition.FRONT)
          .stats(rogueStats)
          .baseMinDamage(18)
          .baseMaxDamage(28)
          .baseDefense(6)
          .currentSan(92)
          .maxSan(100)
          .resourceType(ResourceType.COMBO)
          .skills(List.of(
              PartyMemberSkill.builder().id("rogue_shadow_strike").name("穿心瞬影").icon("⚡").description("化作殘影直刺敵方弱點，造成 220% 暴擊傷害").costType(ResourceType.COMBO).costValue(2).cooldownMs(3000).damageMultiplier(2.2).build(),
              PartyMemberSkill.builder().id("rogue_seven_star").name("七曜絕殺").icon("🌟").description("消耗 5 層連擊點，瞬間打出 5 連穿刺總計 450% 毀滅性爆發！").costType(ResourceType.COMBO).costValue(5).cooldownMs(10000).damageMultiplier(4.5).build()
          ))
          .build();
    } else if (k.contains("mo") || k.contains("mo_yan") || k.contains("墨衍")) {
      LivingStats taoistStats = new LivingStats();
      taoistStats.setHp(85);
      taoistStats.setMaxHp(85);
      taoistStats.setMp(120);
      taoistStats.setMaxMp(120);
      taoistStats.setStr(6);
      taoistStats.setCon(7);
      taoistStats.setDex(9);
      taoistStats.setIntelligence(15);

      return PartyMember.builder()
          .id("m-mo")
          .name("墨衍")
          .roleTitle("太陰御符・天機策士")
          .row(RowPosition.BACK)
          .stats(taoistStats)
          .baseMinDamage(8)
          .baseMaxDamage(16)
          .baseDefense(4)
          .currentSan(88)
          .maxSan(100)
          .resourceType(ResourceType.MP)
          .skills(List.of(
              PartyMemberSkill.builder().id("taoist_seal").name("太陰定身符").icon("📜").description("祭出黃陵定身符，定身敵方單體 4 秒無法行動").costType(ResourceType.MP).costValue(35).cooldownMs(8000).stun(true).stunDurationSeconds(4).build(),
              PartyMemberSkill.builder().id("taoist_thunder").name("五雷天罡符").icon("🌩️").description("引太陰玄雷轟擊敵方全體，造成 160% 道術雷傷害").costType(ResourceType.MP).costValue(50).cooldownMs(10000).damageMultiplier(1.6).aoe(true).build()
          ))
          .build();
    } else if (k.contains("zi") || k.contains("zhi_ruo") || k.contains("芷若")) {
      LivingStats starStats = new LivingStats();
      starStats.setHp(75);
      starStats.setMaxHp(75);
      starStats.setMp(140);
      starStats.setMaxMp(140);
      starStats.setStr(5);
      starStats.setCon(6);
      starStats.setDex(10);
      starStats.setIntelligence(17);

      return PartyMember.builder()
          .id("m-zi")
          .name("芷若")
          .roleTitle("九幽星宿・洞虛術士")
          .row(RowPosition.BACK)
          .stats(starStats)
          .baseMinDamage(12)
          .baseMaxDamage(22)
          .baseDefense(3)
          .currentSan(82)
          .maxSan(100)
          .resourceType(ResourceType.MP)
          .skills(List.of(
              PartyMemberSkill.builder().id("star_warp").name("星移斗轉").icon("🌌").description("撕裂敵方單體防禦，造成 180% 虛空暗蝕傷害").costType(ResourceType.MP).costValue(30).cooldownMs(6000).damageMultiplier(1.8).build(),
              PartyMemberSkill.builder().id("star_meteor").name("九幽星隕").icon("☄️").description("引動九幽星辰墜落，對敵方全體造成 220% 巨額星煞傷害！").costType(ResourceType.MP).costValue(60).cooldownMs(15000).damageMultiplier(2.2).aoe(true).build()
          ))
          .build();
    }
    return null;
  }

  public boolean recruitCompanion(Party party, String companionKey) {
    if (party == null || companionKey == null) return false;
    if (party.size() >= Party.MAX_PARTY_SIZE) return false;

    PartyMember companion = createCompanion(companionKey);
    if (companion == null) return false;

    // 檢查是否已在隊伍中
    boolean alreadyInParty = party.getMembers().stream()
        .anyMatch(m -> m.getId().equals(companion.getId()) || m.getName().equals(companion.getName()));
    if (alreadyInParty) return false;

    party.addMember(companion);
    return true;
  }

  public boolean dismissCompanion(Party party, String memberIdOrName) {
    if (party == null || memberIdOrName == null) return false;
    PartyMember target = party.getMembers().stream()
        .filter(m -> m.getId().equalsIgnoreCase(memberIdOrName) || m.getName().equalsIgnoreCase(memberIdOrName))
        .findFirst().orElse(null);

    if (target == null) return false;
    if ("m-leader".equals(target.getId()) || party.getMembers().indexOf(target) == 0) {
      return false; // 主角隊長不可請離
    }

    party.removeMember(target.getId());
    return true;
  }

  public String formatPartyStatus(Party party) {
    if (party == null) return "您尚未組成任何小隊。";

    StringBuilder sb = new StringBuilder();
    sb.append("======================================================================\n");
    sb.append("【").append(party.getPartyName()).append("】 編制狀態 (人數: ")
        .append(party.size()).append("/").append(Party.MAX_PARTY_SIZE).append(")\n");
    if (party.getEquippedFormation() != null) {
      sb.append("當前啟動陣法: 【").append(party.getEquippedFormation().getName()).append("】\n");
      sb.append("陣法靈威槽: [").append(party.getFormationEnergy()).append("/").append(party.getMaxFormationEnergy()).append("] ");
      int bars = party.getFormationEnergy() / 10;
      sb.append("■".repeat(bars)).append("□".repeat(10 - bars));
      if (party.canCastUltimate()) {
        sb.append("  ★【陣法奧義就緒！】");
      }
      sb.append("\n");
      sb.append("全隊光環: ").append(party.getEquippedFormation().getPassiveAura()).append("\n");
    }
    sb.append("----------------------------------------------------------------------\n");

    for (int i = 0; i < party.size(); i++) {
      PartyMember m = party.getMember(i);
      FormationSlot slot = party.getEquippedFormation() != null ? party.getEquippedFormation().getSlot(i) : null;
      EffectiveCombatStats eff = party.calculateEffectiveStats(i);

      sb.append(String.format("[%d號位] %-8s | %-12s | 站位: %-2s\n",
          (i + 1), m.getName(), m.getRoleTitle(), m.getRow().getChineseName()));

      sb.append(String.format("       生命: %3d/%3d  真元: %3d/%3d  道心(SAN): %3d/%3d %s\n",
          m.getStats().getHp(), m.getStats().getMaxHp(),
          m.getStats().getMp(), m.getStats().getMaxMp(),
          m.getCurrentSan(), m.getMaxSan(), m.getSanityStatus()));

      Map<EquipmentSlot, PartyItemSlot> eq = m.getEquipment();
      sb.append(String.format("       裝備: [主手: %s] [副手: %s] [頭部: %s] [身軀: %s] [靴履: %s] [飾品1: %s] [飾品2: %s]\n",
          formatEquipSlotName(eq, EquipmentSlot.MAIN_HAND),
          formatEquipSlotName(eq, EquipmentSlot.OFF_HAND),
          formatEquipSlotName(eq, EquipmentSlot.HEAD),
          formatEquipSlotName(eq, EquipmentSlot.BODY),
          formatEquipSlotName(eq, EquipmentSlot.FEET),
          formatEquipSlotName(eq, EquipmentSlot.ACCESSORY_1),
          formatEquipSlotName(eq, EquipmentSlot.ACCESSORY_2)));

      if (slot != null && eff != null) {
        sb.append(String.format("       陣位加成: %-12s | 攻:%2d~%2d(x%.2f) 防:%2d(x%.2f) | %s\n",
            slot.getSlotName(),
            eff.minDamage(), eff.maxDamage(), slot.getAttackMultiplier(),
            eff.defense(), slot.getDefenseMultiplier(),
            slot.getSpecialBonusDesc() != null ? slot.getSpecialBonusDesc() : "無"));
      }
      sb.append("\n");
    }
    sb.append("======================================================================\n");
    return sb.toString();
  }

  private String formatEquipSlotName(Map<EquipmentSlot, PartyItemSlot> eq, EquipmentSlot slot) {
    if (eq == null || !eq.containsKey(slot) || eq.get(slot) == null) {
      return "空";
    }
    PartyItemSlot item = eq.get(slot);
    return item.getName();
  }

  public String formatFormationDetails(FormationTemplate formation) {
    if (formation == null) return "查無此陣法資訊。";

    StringBuilder sb = new StringBuilder();
    sb.append("=== 道門陣法：【").append(formation.getName()).append("】 ===\n");
    sb.append("說明: ").append(formation.getDescription()).append("\n");
    sb.append("全域被動: ").append(formation.getPassiveAura()).append("\n");
    if (formation.getUltimateSkill() != null) {
      FormationSkill ult = formation.getUltimateSkill();
      sb.append("陣法大招: 【").append(ult.getName()).append("】\n");
      sb.append("   消耗: ").append(ult.getEnergyCost()).append(" 靈威");
      if (ult.getSanCost() > 0) {
        sb.append("，全隊道心(SAN)侵蝕 -").append(ult.getSanCost()).append(" 點 (克蘇魯代價！)");
      }
      sb.append("\n   威能: 基礎威力 ").append(ult.getBasePower()).append("，").append(ult.getDescription()).append("\n");
    }
    sb.append("--- 陣位孔位加成 ---\n");
    for (FormationSlot s : formation.getSlots()) {
      sb.append(String.format("  [%d號位] %-12s (站位需求:%-2s) 攻:x%.2f 防:x%.2f 抗SAN:%+d | %s\n",
          s.getSlotIndex() + 1, s.getSlotName(), s.getRequiredRow().getChineseName(),
          s.getAttackMultiplier(), s.getDefenseMultiplier(), s.getSanResistanceBonus(),
          s.getSpecialBonusDesc() != null ? s.getSpecialBonusDesc() : ""));
    }
    return sb.toString();
  }
}
