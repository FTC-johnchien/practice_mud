package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.PartyMember;
import lombok.extern.slf4j.Slf4j;

/**
 * 負責 Player (MUD 實體) 與 PartyMember (DRPG 隊長實體) 之間的數值同步與單一真相源維護。
 */
@Slf4j
@Service
public class CharacterSyncService {

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private SkillBridgeService skillBridgeService;

  public SkillBridgeService getSkillBridgeService() {
    if (skillBridgeService == null) {
      skillBridgeService = new SkillBridgeService();
    }
    return skillBridgeService;
  }

  public void setSkillBridgeService(SkillBridgeService skillBridgeService) {
    this.skillBridgeService = skillBridgeService;
  }

  /**
   * 將 Player 的數值同步至小隊隊長 (進入副本 / 隊伍初始化)
   */
  public void syncFromPlayerToParty(Player player, PartyMember leader) {
    if (player == null || leader == null) return;
    LivingStats playerStats = player.getStats();
    if (playerStats == null) return;

    if (leader.getStats() == null) {
      leader.setStats(playerStats.deepCopy());
    } else {
      copyStats(playerStats, leader.getStats());
    }

    // 技能與修為橋接同步
    if (getSkillBridgeService() != null) {
      getSkillBridgeService().syncSkills(player, leader);
    }

    log.debug("Synced stats and skills from Player [{}] to PartyMember leader [{}]", player.getName(), leader.getName());
  }

  /**
   * 將小隊隊長的最新數值同步回 Player (戰鬥勝利 / 經驗提升 / 屬性增長)
   */
  public void syncFromPartyToPlayer(PartyMember leader, Player player) {
    if (leader == null || player == null) return;
    LivingStats leaderStats = leader.getStats();
    LivingStats playerStats = player.getStats();
    if (leaderStats == null || playerStats == null) return;

    copyStats(leaderStats, playerStats);
    log.debug("Synced stats from PartyMember leader [{}] to Player [{}]", leader.getName(), player.getName());
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
