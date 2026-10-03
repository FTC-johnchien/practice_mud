package com.example.htmlmud.domain.dungeon.battle;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.party.model.PartyMember;

/**
 * 戰鬥參與者不可變快照 (Immutable Battle Participant Snapshot)
 * 解決開戰前直接共享 Player 引用或 LivingStats 所帶來的併發競爭與雙向資料污染。
 */
public record BattleParticipantSnapshot(
    String characterId,
    String name,
    int level,
    int hp,
    int maxHp,
    int mp,
    int maxMp,
    int str,
    int con,
    int dex,
    int intelligence,
    int wis,
    int exp,
    long nextLevelExp,
    int freeStatPoints,
    Map<String, Integer> skillProficiencies
) {

  public static BattleParticipantSnapshot fromPlayer(Player player) {
    if (player == null || player.getStats() == null) {
      return new BattleParticipantSnapshot(
          "", "", 1, 100, 100, 50, 50, 10, 10, 10, 10, 10, 0, 180L, 0, Map.of()
      );
    }
    LivingStats s = player.getStats();
    Map<String, Integer> skills = new HashMap<>();
    if (player.getLearnedSkills() != null) {
      player.getLearnedSkills().forEach((k, v) -> {
        if (v != null) {
          skills.put(k, v.getLevel());
        }
      });
    }

    return new BattleParticipantSnapshot(
        player.getId() != null ? player.getId() : "",
        player.getName() != null ? player.getName() : "",
        s.getLevel(),
        s.getHp(),
        s.getMaxHp(),
        s.getMp(),
        s.getMaxMp(),
        s.getStr(),
        s.getCon(),
        s.getDex(),
        s.getIntelligence(),
        s.getWis(),
        s.getExp(),
        s.getNextLevelExp(),
        s.getFreeStatPoints(),
        Collections.unmodifiableMap(skills)
    );
  }

  public void applyToPartyLeader(PartyMember leader) {
    if (leader == null) return;
    if (leader.getStats() == null) {
      leader.setStats(new LivingStats());
    }
    LivingStats s = leader.getStats();
    s.setLevel(level);
    s.setHp(hp);
    s.setMaxHp(maxHp);
    s.setMp(mp);
    s.setMaxMp(maxMp);
    s.setStr(str);
    s.setCon(con);
    s.setDex(dex);
    s.setIntelligence(intelligence);
    s.setWis(wis);
    s.setExp(exp);
    s.setNextLevelExp(nextLevelExp);
    s.setFreeStatPoints(freeStatPoints);
  }
}
