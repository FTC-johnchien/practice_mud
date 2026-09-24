package com.example.htmlmud.domain.party.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.template.SkillTemplate;
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
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.domain.model.template.CompanionTemplate;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyInventory;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PartyService {

  private final TemplateReader templateReader;

  @org.springframework.context.annotation.Lazy
  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  private final Map<String, FormationTemplate> formationRegistry = new HashMap<>();

  private final Map<String, Party> partyCache = new java.util.concurrent.ConcurrentHashMap<>();

  public PartyService(TemplateReader templateReader) {
    this.templateReader = templateReader;
  }

  public PartyService() {
    this(new TemplateCatalog());
  }

  public Party getOrCreateParty(com.example.htmlmud.domain.model.vo.CharacterId characterId) {
    if (characterId == null) return getOrCreateParty("default");
    return getOrCreateParty(characterId.value());
  }

  public Party getOrCreateParty(com.example.htmlmud.domain.actor.impl.Player player) {
    if (player == null) return getOrCreateParty("default");
    Party party = partyCache.get(player.getId());
    if (party == null && player.getName() != null) {
      party = partyCache.get(player.getName());
    }
    if (party != null) {
      return party;
    }
    return getOrCreateParty(player.getCharacterId());
  }

  public Party getOrCreateParty(String playerName) {
    if (playerName == null || playerName.isBlank()) return createSoloParty("道友");
    return partyCache.computeIfAbsent(playerName, this::createInitialParty);
  }

  public void setParty(String playerName, Party party) {
    if (playerName != null && party != null) {
      partyCache.put(playerName, party);
      if (party.getLeader() != null && party.getLeader().getName() != null
          && !party.getLeader().getName().equals(playerName)) {
        partyCache.put(party.getLeader().getName(), party);
      }
    }
  }

  public Party resetParty(String playerName, String protagonistName) {
    String pName = (protagonistName != null && !protagonistName.isBlank()) ? protagonistName : playerName;
    Party party = createSoloParty(pName);
    partyCache.put(playerName, party);
    if (!playerName.equals(pName)) {
      partyCache.put(pName, party);
    }
    return party;
  }

  @PostConstruct
  public void initDefaultFormations() {
    Map<String, FormationTemplate> repoFormations = templateReader.getAllFormations();
    if (repoFormations != null && !repoFormations.isEmpty()) {
      formationRegistry.putAll(repoFormations);
    } else {
      log.warn("No formations found in template reader!");
    }
  }
  public FormationTemplate getFormation(String id) {
    FormationTemplate ft = formationRegistry.get(id);
    if (ft == null) {
      ft = templateReader.findFormation(id).orElse(null);
    }

    return ft;
  }

  public Party createInitialParty(String leaderName) {
    return createFullParty(leaderName);
  }

  public Party createSoloParty(String leaderName) {
    FormationTemplate form = getFormation("formation_four_symbols");
    PartyInventory inv = new PartyInventory(PartyInventory.DEFAULT_CAPACITY, templateReader);
    // 開局初始配置應急物資
    inv.addItem("taiyin_pill", 3);
    inv.addItem("purify_talisman", 2);
    inv.addItem("steel_blade", 1);
    inv.addItem("standard_spear", 1);

    Party party = Party.builder()
        .id("party-" + leaderName)
        .partyName(leaderName + "的問道旅團")
        .equippedFormation(form)
        .formationEnergy(50)
        .inventory(inv)
        .build();

    PartyMember leader = createCompanion("leader");
    if (leader != null) {
      leader.setId("m-leader");
      if (leaderName != null && !leaderName.isBlank()) {
        leader.setName(leaderName);
      }
      party.addMember(leader);
    }

    return party;
  }

  public Party createFullParty(String leaderName) {
    Party party = createSoloParty(leaderName);
    // 黃金 5 人陣容：1 主角 + 4 夥伴
    recruitCompanion(party, "tie_niu");
    recruitCompanion(party, "yan_qing");
    recruitCompanion(party, "ling_shuang");
    recruitCompanion(party, "mo_yan");
    return party;
  }

  public PartyMember createCompanion(String key) {
    if (key == null) return null;
    String k = key.toLowerCase();

    // 1. 優先透過 template reader 查詢 (支持標準 ID 與 aliases)
    var tplOpt = templateReader.findCompanion(key);
    if (tplOpt.isEmpty()) {
      tplOpt = templateReader.findCompanion(k);
    }

    // 2. 若傳入包含前綴或中文別名 (資料驅動：遍歷所有伴侶模板之 name 與 aliases 比對，絕不硬編碼)
    if (tplOpt.isEmpty()) {
      Map<String, CompanionTemplate> allCompanions = templateReader.getAllCompanions();
      if (allCompanions != null) {
        for (CompanionTemplate c : allCompanions.values()) {
          if (c.name() != null && (k.contains(c.name().toLowerCase()) || c.name().toLowerCase().contains(k))) {
            tplOpt = Optional.of(c);
            break;
          }
          if (c.aliases() != null) {
            for (String alias : c.aliases()) {
              if (alias != null && (k.contains(alias.toLowerCase()) || alias.toLowerCase().contains(k))) {
                tplOpt = Optional.of(c);
                break;
              }
            }
          }
          if (tplOpt.isPresent()) break;
        }
      }
    }

    if (tplOpt.isPresent()) {
      CompanionTemplate tpl = tplOpt.get();
      LivingStats stats = new LivingStats();
      stats.setHp(tpl.maxHp());
      stats.setMaxHp(tpl.maxHp());
      stats.setMp(tpl.maxMp());
      stats.setMaxMp(tpl.maxMp());
      stats.setStr(tpl.str());
      stats.setCon(tpl.con());
      stats.setDex(tpl.dex());
      stats.setIntelligence(tpl.intelligence());

      List<PartyMemberSkill> memberSkills = new java.util.ArrayList<>();
      for (String sId : tpl.skills()) {
        templateReader.findPartySkill(sId).ifPresent(s -> {
          if (memberSkills.stream().noneMatch(existing -> existing.getId().equalsIgnoreCase(s.getId())
              || (existing.getName() != null && s.getName() != null && existing.getName().equalsIgnoreCase(s.getName())))) {
            memberSkills.add(s);
          }
        });
      }

      // 自動掛載職業特徵技能 (完全資料驅動：查詢所有 tags 包含 CLASS 且 school 符合該職業的技能，不限武器別)
      if (tpl.classId() != null) {
        String cId = tpl.classId().toUpperCase();
        Map<String, SkillTemplate> allSkills = templateReader.getAllSkills();
        if (allSkills != null) {
          for (SkillTemplate st : allSkills.values()) {
            if (st.getSchool() != null && st.getSchool().equalsIgnoreCase(cId)
                && st.getTags() != null && st.getTags().contains("CLASS")) {
              templateReader.findPartySkill(st.getId()).ifPresent(s -> {
                if (memberSkills.stream().noneMatch(existing -> existing.getId().equalsIgnoreCase(s.getId())
                    || (existing.getName() != null && s.getName() != null && existing.getName().equalsIgnoreCase(s.getName())))) {
                  memberSkills.add(s);
                }
              });
            }
          }
        }
      }

      PartyMember member = PartyMember.builder()
          .id("m-" + tpl.id())
          .name(tpl.name())
          .roleTitle(tpl.roleTitle())
          .classId(tpl.classId())
          .row(tpl.defaultRow())
          .stats(stats)
          .baseMinDamage(tpl.baseMinDamage())
          .baseMaxDamage(tpl.baseMaxDamage())
          .baseDefense(tpl.baseDefense())
          .currentSan(tpl.maxSan())
          .maxSan(tpl.maxSan())
          .resourceType(tpl.resourceType())
          .skills(memberSkills)
          .build();
          member.setTemplateReader(templateReader);

      // 裝配初始裝備
      if (tpl.initialEquipment() != null) {
        for (var entry : tpl.initialEquipment().entrySet()) {
          var itemOpt = templateReader.findItem(entry.getValue());
          if (itemOpt.isPresent()) {
            ItemTemplate it = itemOpt.get();
            int minD = (it.equipmentProp() != null) ? it.equipmentProp().minDamage() : 0;
            int maxD = (it.equipmentProp() != null) ? it.equipmentProp().maxDamage() : 0;
            int def = (it.equipmentProp() != null) ? it.equipmentProp().defense() : 0;
            int bHp = (it.bonusStats() != null) ? it.bonusStats().getOrDefault("MAX_HP", 0) : 0;
            int bSan = (it.bonusStats() != null) ? it.bonusStats().getOrDefault("MAX_SAN", 0) : 0;
            PartyItemSlot slot = PartyItemSlot.builder()
                .slotId("init-" + it.id())
                .itemId(it.id())
                .name(it.name())
                .itemType(it.type())
                .subType(it.subType())
                .equipSlot(entry.getKey())
                .bonusMinDamage(minD)
                .bonusMaxDamage(maxD)
                .bonusDefense(def)
                .bonusHp(bHp)
                .bonusSan(bSan)
                .build();
            member.equip(entry.getKey(), slot);
          }
        }
      }
      // 初始化主修武學套路 (Learned Stances) - 100% Data-Driven
      if (tpl.learnedStances() != null && !tpl.learnedStances().isEmpty()) {
        for (String stanceId : tpl.learnedStances()) {
          member.learnStance(stanceId);
        }
      } else {
        member.learnStance("basic_fist");
      }
      // 自動裝配初始被動心法 (完全資料驅動：優先裝配含 BASIC 標籤之基礎心法，其餘心法填補未裝配槽位)
      List<String> initStances = tpl.learnedStances() != null ? tpl.learnedStances() : List.copyOf(member.getLearnedStances());
      for (String stanceId : initStances) {
        templateReader.findSkill(stanceId).ifPresent(sk -> {
          if (sk.getTags() != null && sk.getTags().contains("BASIC")) {
            var cat = PartyMember.resolveSkillCategory(sk);
            if (cat == SkillCategory.DODGE || cat == SkillCategory.PARRY || cat == SkillCategory.FORCE) {
              if (!member.getEnabledSkills().containsKey(cat)) {
                member.enableSkill(cat, stanceId);
              }
            }
          }
        });
      }
      for (String stanceId : initStances) {
        templateReader.findSkill(stanceId).ifPresent(sk -> {
          var cat = PartyMember.resolveSkillCategory(sk);
          if (cat == SkillCategory.DODGE || cat == SkillCategory.PARRY || cat == SkillCategory.FORCE) {
            if (!member.getEnabledSkills().containsKey(cat)) {
              member.enableSkill(cat, stanceId);
            }
          }
        });
      }
      if (!member.getEnabledSkills().containsKey(SkillCategory.DODGE)) {
        member.enableSkill(SkillCategory.DODGE, "basic_dodge");
      }
      if (!member.getEnabledSkills().containsKey(SkillCategory.PARRY)) {
        member.enableSkill(SkillCategory.PARRY, "basic_parry");
      }
      if (!member.getEnabledSkills().containsKey(SkillCategory.FORCE)) {
        member.enableSkill(SkillCategory.FORCE, "basic_breathing");
      }

      return member;
    }

    log.warn("Companion template not found for key: {}", key);
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
    String cleanTarget = memberIdOrName.trim().toLowerCase();
    PartyMember target = party.getMembers().stream()
        .filter(m -> m.getId().equalsIgnoreCase(memberIdOrName)
            || m.getName().equalsIgnoreCase(memberIdOrName)
            || m.getId().replace("m-", "").equalsIgnoreCase(cleanTarget)
            || ((cleanTarget.contains("iron") || cleanTarget.contains("tie_niu") || cleanTarget.contains("鐵牛")) && (m.getId().contains("tie_niu") || m.getName().contains("鐵牛")))
            || ((cleanTarget.contains("ling") || cleanTarget.contains("ling_shuang") || cleanTarget.contains("凌霜")) && (m.getId().contains("ling_shuang") || m.getName().contains("凌霜")))
            || ((cleanTarget.contains("yan") || cleanTarget.contains("yan_qing") || cleanTarget.contains("燕青")) && (m.getId().contains("yan_qing") || m.getName().contains("燕青")))
            || ((cleanTarget.contains("mo") || cleanTarget.contains("mo_yan") || cleanTarget.contains("墨衍")) && (m.getId().contains("mo_yan") || m.getName().contains("墨衍"))))
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

  public boolean recruitCompanionForPlayer(com.example.htmlmud.domain.actor.impl.Player self, String target) {
    if (self == null) return false;
    Party party = getOrCreateParty(self);
    if (party.size() >= Party.MAX_PARTY_SIZE) {
      self.reply("【旅團滿編】問道旅團已達上限 " + Party.MAX_PARTY_SIZE + " 人，無法再結納更多隊友！");
      return false;
    }

    boolean success = recruitCompanion(party, target);
    if (success) {
      PartyMember added = party.getMembers().get(party.getMembers().size() - 1);
      self.reply("\u001B[1;32m🤝【結識同道】「" + added.getName() + "」爽朗抱拳應允，正式加入【" + party.getPartyName() + "】！\u001B[0m\n"
          + "【" + added.getName() + "】(" + added.getRoleTitle() + ") 當前站位: " + (added.getRow() == RowPosition.FRONT ? "前衛" : "後衛"));
    } else {
      boolean alreadyIn = party.getMembers().stream().anyMatch(m -> m.getName().equalsIgnoreCase(target) || m.getId().equalsIgnoreCase(target));
      if (alreadyIn) {
        self.reply("【同道同行】「" + target + "」已身處隊伍之中，與你生死與共。");
      } else {
        self.reply("此地並未見到能結納為同道的「" + target + "」。(可招募夥伴：鐵牛 tie_niu、凌霜 ling_shuang)");
      }
    }
    if (broadcastService != null) {
      broadcastService.broadcastState(self);
    }
    return success;
  }

  public boolean dismissCompanionForPlayer(com.example.htmlmud.domain.actor.impl.Player self, String target) {
    if (self == null) return false;
    Party party = getOrCreateParty(self);
    boolean success = dismissCompanion(party, target);
    if (success) {
      self.reply("\u001B[1;33m👋【道別】已將隊員「" + target + "」請離隊伍，其已抱拳告辭返回客棧安歇。\u001B[0m");
    } else {
      self.reply("無法請離「" + target + "」（隊長不可離隊，或隊伍中查無此人）。");
    }
    if (broadcastService != null) {
      broadcastService.broadcastState(self);
    }
    return success;
  }
}
