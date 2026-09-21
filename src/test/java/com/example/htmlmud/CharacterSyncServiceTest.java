package com.example.htmlmud;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.htmlmud.domain.actor.core.MessageOutput;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.PlayerService;
import com.example.htmlmud.domain.service.WorldManager;

@SpringBootTest
class CharacterSyncServiceTest {

  @Autowired
  private CharacterSyncService characterSyncService;

  @Autowired
  private PartyService partyService;

  @Autowired
  private WorldManager worldManager;

  @Autowired
  private PlayerService playerService;

  @Test
  @DisplayName("驗證 Player 到 PartyMember 隊長之屬性單一真相源同步")
  void testSyncFromPlayerToParty() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    Player player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "玄天子");
    player.getStats().setLevel(5);
    player.getStats().setHp(150);
    player.getStats().setMaxHp(150);
    player.getStats().setMp(80);
    player.getStats().setMaxMp(80);
    player.getStats().setStr(20);
    player.getStats().setCon(18);
    player.getStats().setDex(15);
    player.getStats().setExp(450);
    player.getStats().setFreeStatPoints(3);

    Party party = partyService.createSoloParty("玄天子");
    PartyMember leader = party.getLeader();

    characterSyncService.syncFromPlayerToParty(player, leader);

    assertThat(leader.getStats().getLevel()).isEqualTo(5);
    assertThat(leader.getStats().getHp()).isEqualTo(150);
    assertThat(leader.getStats().getMaxHp()).isEqualTo(150);
    assertThat(leader.getStats().getMp()).isEqualTo(80);
    assertThat(leader.getStats().getMaxMp()).isEqualTo(80);
    assertThat(leader.getStats().getStr()).isEqualTo(20);
    assertThat(leader.getStats().getCon()).isEqualTo(18);
    assertThat(leader.getStats().getDex()).isEqualTo(15);
    assertThat(leader.getStats().getExp()).isEqualTo(450);
    assertThat(leader.getStats().getFreeStatPoints()).isEqualTo(3);
  }

  @Test
  @DisplayName("驗證 PartyMember 戰鬥歷練後正確同步回 Player 實體")
  void testSyncFromPartyToPlayer() {
    MessageOutput mockOutput = new MessageOutput() {
      @Override public void sendJson(Object payload) {}
      @Override public void close() {}
      @Override public org.springframework.web.socket.WebSocketSession getSession() { return null; }
    };
    Player player = Player.createSinglePlayer(mockOutput, worldManager, playerService, "紫陽道人");
    Party party = partyService.createSoloParty("紫陽道人");
    PartyMember leader = party.getLeader();

    // 模擬在 DRPG 戰鬥中升級並消耗氣血真元
    leader.getStats().setLevel(3);
    leader.getStats().setExp(260);
    leader.getStats().setNextLevelExp(350);
    leader.getStats().setHp(95);
    leader.getStats().setMaxHp(140);
    leader.getStats().setMp(30);
    leader.getStats().setMaxMp(70);
    leader.getStats().setStr(14);
    leader.getStats().setFreeStatPoints(2);

    // 執行戰後回寫同步
    characterSyncService.syncFromPartyToPlayer(leader, player);

    // 驗證 Player 實體已獲取最新歷練數據
    LivingStats ps = player.getStats();
    assertThat(ps.getLevel()).isEqualTo(3);
    assertThat(ps.getExp()).isEqualTo(260);
    assertThat(ps.getNextLevelExp()).isEqualTo(350);
    assertThat(ps.getHp()).isEqualTo(95);
    assertThat(ps.getMaxHp()).isEqualTo(140);
    assertThat(ps.getMp()).isEqualTo(30);
    assertThat(ps.getMaxMp()).isEqualTo(70);
    assertThat(ps.getStr()).isEqualTo(14);
    assertThat(ps.getFreeStatPoints()).isEqualTo(2);
  }
}
