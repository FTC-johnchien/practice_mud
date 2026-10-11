package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcome;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcomeApplier;
import com.example.htmlmud.domain.dungeon.battle.BattleParticipantSnapshot;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.PartyMember;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 Player (MUD 實體) 與 PartyMember (DRPG 隊長實體) 之間的數值同步與單一真相源維護。
 * 採用 Snapshot 與 Outcome 模型，杜絕併發共享參照與競態條件。
 */
@Slf4j
@Service
public class CharacterSyncService {

  private final SkillBridgeService skillBridgeService;
  private final BattleOutcomeApplier outcomeApplier;
  private final com.example.htmlmud.domain.repository.TemplateReader templateReader;
  private final org.springframework.beans.factory.ObjectProvider<LivingService> livingServiceProvider;

  @org.springframework.beans.factory.annotation.Autowired
  public CharacterSyncService(
      @org.springframework.beans.factory.annotation.Autowired(required = false) SkillBridgeService skillBridgeService,
      @org.springframework.beans.factory.annotation.Autowired(required = false) BattleOutcomeApplier outcomeApplier,
      @org.springframework.beans.factory.annotation.Autowired(required = false) com.example.htmlmud.domain.repository.TemplateReader templateReader,
      @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.beans.factory.ObjectProvider<LivingService> livingServiceProvider) {
    this.skillBridgeService = skillBridgeService != null ? skillBridgeService : new SkillBridgeService();
    this.outcomeApplier = outcomeApplier != null ? outcomeApplier : new BattleOutcomeApplier();
    this.templateReader = templateReader;
    this.livingServiceProvider = livingServiceProvider;
  }

  public CharacterSyncService() {
    this(new SkillBridgeService(), new BattleOutcomeApplier(), null, null);
  }

  public CharacterSyncService(SkillBridgeService skillBridgeService) {
    this(skillBridgeService, new BattleOutcomeApplier(), null, null);
  }

  public CharacterSyncService(SkillBridgeService skillBridgeService, BattleOutcomeApplier outcomeApplier) {
    this(skillBridgeService, outcomeApplier, null, null);
  }

  public SkillBridgeService getSkillBridgeService() {
    return skillBridgeService;
  }

  public BattleOutcomeApplier getOutcomeApplier() {
    return outcomeApplier;
  }

  /**
   * 裝備單一擁有者投影：將小隊隊長 (PartyMember) 的 7 大部位裝備單向投影至 Player
   * 維護不可變邊界與 Actor 線程隔離 (UNIFY-02)
   */
  public boolean syncEquipmentFromPartyToPlayer(PartyMember leader, Player player) {
    if (leader == null || player == null) return false;
    java.util.Map<com.example.htmlmud.domain.model.enums.EquipmentSlot, com.example.htmlmud.domain.party.model.PartyItemSlot> equipment = leader.getEquipment();

    if (player.isActorThread() || !player.isRunning()) {
      if (player.getStats() != null && player.getStats().getEquipment() != null) {
        player.getStats().getEquipment().clear();
        if (equipment != null) {
          for (var entry : equipment.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
              player.getStats().getEquipment().put(entry.getKey(), entry.getValue().toGameItem(templateReader));
            }
          }
        }
        LivingService ls = (livingServiceProvider != null) ? livingServiceProvider.getIfAvailable() : null;
        if (ls != null) {
          ls.recalculateStats(player);
        }
      }
      return true;
    }

    java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
    player.send(new com.example.htmlmud.protocol.ActorMessage.ProjectEquipment(equipment, future));
    try {
      return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("等待玩家 [{}] 透過 Mailbox 投影裝備超時或異常", player.getName(), e);
      return false;
    }
  }

  /**
   * 開戰前：從 Player 產生不可變戰鬥參與者快照
   */
  public BattleParticipantSnapshot createSnapshot(Player player) {
    return BattleParticipantSnapshot.fromPlayer(player);
  }

  /**
   * 開戰前：將快照安全套用至小隊隊長
   */
  public void applySnapshotToParty(BattleParticipantSnapshot snapshot, PartyMember leader) {
    if (snapshot == null || leader == null) return;
    snapshot.applyToPartyLeader(leader);
  }

  /**
   * 將 Player 的數值同步至小隊隊長 (進入副本 / 隊伍初始化)
   * 內部透過不可變快照進行解耦複製
   */
  public void syncFromPlayerToParty(Player player, PartyMember leader) {
    if (player == null || leader == null) return;
    BattleParticipantSnapshot snapshot = createSnapshot(player);
    applySnapshotToParty(snapshot, leader);

    // 技能與修為橋接同步
    if (getSkillBridgeService() != null) {
      getSkillBridgeService().syncSkills(player, leader);
    }

    // 裝備單一擁有者投影同步 (UNIFY-02)
    if (leader.getEquipment() != null && !leader.getEquipment().isEmpty()) {
      syncEquipmentFromPartyToPlayer(leader, player);
    }

    log.debug("Synced stats, skills and equipment between Player [{}] and PartyMember leader [{}]",
        player.getName(), leader.getName());
  }

  /**
   * 戰鬥結算：從小隊隊長當前狀態生成不可變 BattleOutcome
   */
  public BattleOutcome createOutcome(PartyMember leader, String battleId, boolean victory,
      long expGained, int freePointsGained, java.util.Map<String, Integer> statIncreases,
      java.util.List<String> droppedItemIds, java.util.List<String> announcements, String idempotencyKey) {
    LivingStats ls = (leader != null) ? leader.getStats() : null;
    int currentHp = (ls != null) ? ls.getHp() : 0;
    int currentMp = (ls != null) ? ls.getMp() : 0;
    int level = (ls != null) ? ls.getLevel() : 1;

    return BattleOutcome.builder()
        .battleId(battleId)
        .characterId((leader != null) ? leader.getId() : "")
        .victory(victory)
        .finalHp(currentHp)
        .finalMp(currentMp)
        .expGained(expGained)
        .newLevel(level)
        .freeStatPointsGained(freePointsGained)
        .statIncreases(statIncreases)
        .droppedItemIds(droppedItemIds)
        .announcements(announcements)
        .idempotencyKey(idempotencyKey)
        .build();
  }

  /**
   * 透過 Applier 將戰鬥結果單向套用至 Player
   * 若 Player Actor 處於運行中且非自身執行緒，自動改走 Actor Mailbox 循序派發 (SYNC-01)
   */
  public boolean applyOutcome(Player player, BattleOutcome outcome) {
    if (player == null || outcome == null) return false;
    // 若在該 Player 的虛擬執行緒中，或 Actor 尚未啟動 (單元測試/離線)，直接同步套用
    if (player.isActorThread() || !player.isRunning()) {
      return getOutcomeApplier().apply(player, outcome);
    }
    // 跨線程邊界：投遞至 Player Mailbox 保證 Actor 串行安全執行
    java.util.concurrent.CompletableFuture<Boolean> future = new java.util.concurrent.CompletableFuture<>();
    player.send(new com.example.htmlmud.protocol.ActorMessage.ApplyBattleOutcome(outcome, future));
    try {
      return future.get(5, java.util.concurrent.TimeUnit.SECONDS);
    } catch (Exception e) {
      log.error("等待玩家 [{}] 透過 Mailbox 套用戰鬥結果超時或異常", player.getName(), e);
      return false;
    }
  }

  /**
   * 將小隊隊長的最新數值同步回 Player (戰鬥勝利 / 經驗提升 / 屬性增長)
   * 透過不可變 BattleOutcome 單向發送結算，維護冪等性與 Actor 線程隔離
   */
  public void syncFromPartyToPlayer(PartyMember leader, Player player) {
    if (leader == null || player == null) return;
    LivingStats leaderStats = leader.getStats();
    LivingStats playerStats = player.getStats();
    if (leaderStats == null || playerStats == null) return;

    java.util.Map<String, Integer> statIncreases = new java.util.HashMap<>();
    if (leaderStats.getStr() > playerStats.getStr()) statIncreases.put("str", leaderStats.getStr() - playerStats.getStr());
    if (leaderStats.getCon() > playerStats.getCon()) statIncreases.put("con", leaderStats.getCon() - playerStats.getCon());
    if (leaderStats.getDex() > playerStats.getDex()) statIncreases.put("dex", leaderStats.getDex() - playerStats.getDex());
    if (leaderStats.getIntelligence() > playerStats.getIntelligence()) statIncreases.put("int", leaderStats.getIntelligence() - playerStats.getIntelligence());
    if (leaderStats.getWis() > playerStats.getWis()) statIncreases.put("wis", leaderStats.getWis() - playerStats.getWis());

    // 建立 Outcome 並套用
    BattleOutcome outcome = BattleOutcome.builder()
        .battleId("sync-" + System.nanoTime())
        .characterId(player.getId())
        .victory(true)
        .finalHp(leaderStats.getHp())
        .finalMp(leaderStats.getMp())
        .newLevel(leaderStats.getLevel())
        .leveledUp(leaderStats.getLevel() > playerStats.getLevel())
        .expGained(Math.max(0, leaderStats.getExp() - playerStats.getExp()))
        .freeStatPointsGained(Math.max(0, leaderStats.getFreeStatPoints() - playerStats.getFreeStatPoints()))
        .statIncreases(statIncreases)
        .build();

    applyOutcome(player, outcome);

    // 若處於非並發狀態 (同執行緒或未啟動 Actor)，安全補充覆蓋深層數值
    if (player.isActorThread() || !player.isRunning()) {
      copyStats(leaderStats, playerStats);
    }
    log.debug("Synced stats from PartyMember leader [{}] to Player [{}] via Outcome", leader.getName(), player.getName());
  }

  /**
   * 複製等級、經驗、氣血、法力及核心屬性數值
   */
  private void copyStats(LivingStats source, LivingStats target) {
    target.setLevel(source.getLevel());
    target.setExp(source.getExp());
    target.setNextLevelExp(source.getNextLevelExp());
    target.setMaxHp(source.getMaxHp());
    target.setHp(source.getHp());
    target.setMaxMp(source.getMaxMp());
    target.setMp(source.getMp());
    target.setStr(source.getStr());
    target.setCon(source.getCon());
    target.setDex(source.getDex());
    target.setIntelligence(source.getIntelligence());
    target.setWis(source.getWis());
    target.setFreeStatPoints(source.getFreeStatPoints());
  }
}
