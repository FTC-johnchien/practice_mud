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

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private SkillBridgeService skillBridgeService;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private BattleOutcomeApplier outcomeApplier;

  public SkillBridgeService getSkillBridgeService() {
    if (skillBridgeService == null) {
      skillBridgeService = new SkillBridgeService();
    }
    return skillBridgeService;
  }

  public void setSkillBridgeService(SkillBridgeService skillBridgeService) {
    this.skillBridgeService = skillBridgeService;
  }

  public BattleOutcomeApplier getOutcomeApplier() {
    if (outcomeApplier == null) {
      outcomeApplier = new BattleOutcomeApplier();
    }
    return outcomeApplier;
  }

  public void setOutcomeApplier(BattleOutcomeApplier outcomeApplier) {
    this.outcomeApplier = outcomeApplier;
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

    log.debug("Synced stats and skills from Player [{}] to PartyMember leader [{}] via snapshot",
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
   */
  public boolean applyOutcome(Player player, BattleOutcome outcome) {
    return getOutcomeApplier().apply(player, outcome);
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
        .build();

    applyOutcome(player, outcome);

    // 確保其餘自訂屬性完全同步 (深度複製，不共用引用)
    copyStats(leaderStats, playerStats);
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
