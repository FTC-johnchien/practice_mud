package com.example.htmlmud.application.command.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"meditate", "dazuo", "sleep"})
public class RestCommand implements PlayerCommand {

  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;
  private final com.example.htmlmud.domain.service.GameStateBroadcastService broadcastService;

  @Override
  public String getKey() {
    return "rest";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    if (battleService.isInBattle(self.getName())) {
      self.reply("戰鬥交鋒正烈，命在旦夕，無法安然調息！");
      return;
    }

    Party party = partyService.getOrCreateParty(self.getName());

    // 1. 若處於客棧 (Hub Safe Zone)，全體氣血、真元、道心理智全數回滿
    boolean isInn = self.getCurrentRoomId() != null && self.getCurrentRoomId().contains("inn");
    if (isInn) {
      for (PartyMember member : party.getMembers()) {
        member.heal(member.getStats() != null ? member.getStats().getMaxHp() : 200);
        member.restoreSan(100);
        if (member.getStats() != null) {
          member.getStats().setMp(member.getStats().getMaxMp());
        }
      }
      self.reply("🛌【客棧安歇】小隊在客棧暖榻上飽食安睡，飲下一碗溫熱的百草參茶……\n"
          + "全體成員氣血、真元與道心理智全數回滿！神完氣足！");
      broadcastService.broadcastState(self);
      return;
    }

    // 2. 地牢環境檢驗 (安全點檢定與夜襲伏擊判定)
    DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), null);
    DungeonFloor floor = (pos != null && pos.getFloorId() != null) ? dungeonManager.getFloor(pos.getFloorId()) : null;
    boolean isSafePoint = false;

    if (pos != null && floor != null) {
      com.example.htmlmud.domain.dungeon.model.DungeonTile tile = floor.getTile(pos.getX(), pos.getY());
      if (tile != null && (tile.getType() == com.example.htmlmud.domain.dungeon.model.DungeonTile.TileType.EVENT
          || tile.getType() == com.example.htmlmud.domain.dungeon.model.DungeonTile.TileType.STAIRS_UP)) {
        isSafePoint = true;
      }

      // 若非安全節點，進行伏擊判定
      if (!isSafePoint) {
        int currentDanger = pos.getDangerLevel();
        // 伏擊機率：基礎 25% + (當前靈壓 * 0.5%)
        int ambushChance = 25 + (int) (currentDanger * 0.5);
        if (java.util.concurrent.ThreadLocalRandom.current().nextInt(100) < ambushChance) {
          pos.addDanger(10);
          self.reply("\u001B[1;31m⚠️【陰煞暴動・夜襲伏擊】四周陰風驟起，骨磷血影撕裂虛空！妖邪趁小隊盤膝調息發動突襲！\u001B[0m");
          if (floor.getMobPool() != null && !floor.getMobPool().isEmpty()) {
            battleService.startBattle(self, pos, floor.getMobPool());
            return;
          }
        }
      }
    }

    // 3. 調息恢復
    for (PartyMember member : party.getMembers()) {
      member.heal(30);
      member.restoreSan(10);
      if (member.getStats() != null) {
        member.getStats().setMp(Math.min(member.getStats().getMaxMp(), member.getStats().getMp() + 20));
      }
    }

    int danger = 0;
    if (pos != null && floor != null) {
      if (!isSafePoint) {
        pos.addDanger(15);
        danger = pos.getDangerLevel();
        self.reply("【凝神調息】小隊席地盤膝調息，運轉大周天心法……\n"
            + "全體成員氣血回復 30 點、真元回復 20 點，心神平復 10 點！\n"
            + "（於陰煞深處調息，四周陰靈騷動，警戒靈壓上升至 " + danger + "%）");
      } else {
        danger = pos.getDangerLevel();
        self.reply("【古碑寧神】小隊於墨家非攻殘碑/安全生路旁盤膝調息……\n"
            + "古仙道紋流轉，全體成員氣血回復 30 點、真元回復 20 點，心神平復 10 點！\n"
            + "（受玄妙禁制庇護，陰煞辟易，未引動任何警戒靈壓）");
      }
    } else {
      self.reply("【凝神調息】小隊席地盤膝調息，運轉大周天心法……\n"
          + "全體成員氣血回復 30 點、真元回復 20 點，心神平復 10 點！");
    }

    // 推送最新狀態給前端
    broadcastService.broadcastState(self);
  }

  @Override
  public String getDescription() {
    return "凝神調息 (回復小隊氣血、真元與理智，但增加地牢警戒靈壓)";
  }
}
