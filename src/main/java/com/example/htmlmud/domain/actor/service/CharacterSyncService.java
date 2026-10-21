package com.example.htmlmud.domain.actor.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcome;
import com.example.htmlmud.domain.dungeon.battle.BattleOutcomeApplier;
import com.example.htmlmud.domain.dungeon.battle.BattleParticipantSnapshot;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.repository.TemplateCatalog;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 Player (MUD 實體) 與 PartyMember (DRPG 隊長實體) 之間的數值同步與單一真相源維護。
 * 採用 Snapshot 與 Outcome 模型，杜絕併發共享參照與競態條件。
 */
@Slf4j
@Service
public class CharacterSyncService {

  private final BattleOutcomeApplier outcomeApplier;
  private final TemplateReader templateReader;
  private final org.springframework.beans.factory.ObjectProvider<LivingService> livingServiceProvider;

  @org.springframework.beans.factory.annotation.Autowired
  public CharacterSyncService(
      @org.springframework.beans.factory.annotation.Autowired(required = false) BattleOutcomeApplier outcomeApplier,
      @org.springframework.beans.factory.annotation.Autowired(required = false) TemplateReader templateReader,
      @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.beans.factory.ObjectProvider<LivingService> livingServiceProvider) {
    this.outcomeApplier = outcomeApplier != null ? outcomeApplier : new BattleOutcomeApplier();
    this.templateReader = templateReader;
    this.livingServiceProvider = livingServiceProvider;
  }

  public CharacterSyncService() {
    this(new BattleOutcomeApplier(), null, null);
  }

  public CharacterSyncService(BattleOutcomeApplier outcomeApplier) {
    this(outcomeApplier, null, null);
  }

  public BattleOutcomeApplier getOutcomeApplier() {
    return outcomeApplier;
  }

  /**
   * 裝備單一擁有者投影：將小隊隊長 (PartyMember) 的 7 大部位裝備實時同步至 Player
   * 徹底除役 CompletableFuture 與 5 秒超時阻塞，保證單機架構下 0ms 零延遲即時生效 (UNIFY-02)
   */
  public boolean syncEquipmentFromPartyToPlayer(PartyMember leader, Player player) {
    if (leader == null || player == null) return false;
    java.util.Map<com.example.htmlmud.domain.model.enums.EquipmentSlot, com.example.htmlmud.domain.party.model.PartyItemSlot> equipment = leader.getEquipment();

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

    // 技能大一統同步 (SP-06 / SP-07)：直接將 Player 的 UnifiedSkill 同步至 PartyMember 隊長
    if (player.getLearnedSkills() != null && !player.getLearnedSkills().isEmpty()) {
      var reader = this.templateReader != null ? this.templateReader : new TemplateCatalog();
      for (var entry : player.getLearnedSkills().values()) {
        if (entry != null && entry.getSkillId() != null) {
          boolean updated = false;
          if (leader.getSkills() != null) {
            for (var s : leader.getSkills()) {
              if (s.matchesIdOrAlias(entry.getSkillId())) {
                s.setLevel(Math.max(s.getLevel(), entry.getLevel()));
                s.setXp(entry.getXp());
                if (entry.getMaxXp() > 0) s.setMaxXp(entry.getMaxXp());
                updated = true;
                break;
              }
            }
          }
          if (!updated) {
            leader.addSkill(entry.toUnifiedSkill(reader));
          }
        }
      }
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
   * 在單機單體大一統架構下，直接同步鏡像實體數值，徹底廢除偽裝 BattleOutcome 與 5s 超時阻塞 (UNIFY-03)
   */
  public void syncFromPartyToPlayer(PartyMember leader, Player player) {
    if (leader == null || player == null) return;
    LivingStats leaderStats = leader.getStats();
    LivingStats playerStats = player.getStats();
    if (leaderStats == null || playerStats == null) return;

    // 1. 直接深度覆蓋同步數值
    copyStats(leaderStats, playerStats);

    // 2. 重新計算主角衍生戰鬥數值
    LivingService ls = (livingServiceProvider != null) ? livingServiceProvider.getIfAvailable() : null;
    if (ls != null) {
      ls.recalculateStats(player);
    }

    log.debug("Directly mirrored stats from PartyMember leader [{}] to Player [{}]", leader.getName(), player.getName());
  }

  /**
   * 複製等級、經驗、氣血、法力、理智、體力及核心五維屬性數值
   */
  private void copyStats(LivingStats source, LivingStats target) {
    target.setLevel(source.getLevel());
    target.setExp(source.getExp());
    target.setNextLevelExp(source.getNextLevelExp());
    target.setCombatExp(source.getCombatExp());
    target.setPotential(source.getPotential());
    target.setMaxHp(source.getMaxHp());
    target.setHp(source.getHp());
    target.setMaxMp(source.getMaxMp());
    target.setMp(source.getMp());
    target.setMaxStamina(source.getMaxStamina());
    target.setStamina(source.getStamina());
    target.setMaxSan(source.getMaxSan());
    target.setSan(source.getSan());
    target.setStr(source.getStr());
    target.setCon(source.getCon());
    target.setDex(source.getDex());
    target.setIntelligence(source.getIntelligence());
    target.setWis(source.getWis());
    target.setFreeStatPoints(source.getFreeStatPoints());
    target.setCoin(source.getCoin());
  }
}
