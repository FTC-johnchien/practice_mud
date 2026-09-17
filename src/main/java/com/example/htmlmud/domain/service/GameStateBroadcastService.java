package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.dungeon.battle.DrpgBattleService;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto.TownCapabilityDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto.TownExitDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto.TownItemDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto.TownNpcDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;
import com.example.htmlmud.domain.model.enums.MobKind;
import com.example.htmlmud.domain.model.template.RoomTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.service.PartyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 集中管理與前端 WebSockets 廣播雙模遊戲狀態 (城鎮羅盤 vs 地牢雷達)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameStateBroadcastService {

  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final PartyService partyService;
  private final DrpgBattleService battleService;
  private final WorldManager worldManager;

  public void broadcastState(Player player) {
    if (player == null) return;
    Party party = partyService.getOrCreateParty(player.getName());

    if (player.isInDungeon()) {
      broadcastDungeonState(player, party);
    } else {
      broadcastTownState(player, party);
    }
  }

  public void broadcastDungeonState(Player player, Party party) {
    DungeonPosition pos = dungeonManager.getPlayerPosition(player.getName());
    String floorId = pos != null ? pos.getFloorId() : "mozhu_mines_b1f";
    DungeonFloor floor = dungeonManager.getFloor(floorId);
    String inspect = (floor != null && pos != null) ? dungeonNavigator.inspectForward(floor, pos) : "";
    var battleView = battleService.createBattleView(player.getName());
    player.sendJson(DrpgStateDto.of(floor, pos, inspect, party, battleView));
  }

  public void broadcastTownState(Player player, Party party) {
    Room room = player.getCurrentRoom();
    if (room == null && player.getCurrentRoomId() != null) {
      room = worldManager.getRoomActor(player.getCurrentRoomId());
    }
    if (room == null) return;

    RoomTemplate tmpl = room.getTemplate();
    String zoneId = tmpl.zoneId();
    String zoneName = "新手仙鄉";
    if (zoneId != null && zoneId.contains("mozhu")) zoneName = "墨竹山脈";
    else if (zoneId != null && zoneId.contains("newbie")) zoneName = "桃源新手村";
    else if (zoneId != null && zoneId.contains("snow")) zoneName = "雪亭鎮";
    else if (zoneId != null && zoneId.contains("silverleaf")) zoneName = "銀葉村";

    boolean safe = tmpl.flags() != null && tmpl.flags().contains("SAFE");
    if (tmpl.id().contains("inn")) safe = true;

    List<TownExitDto> exits = new ArrayList<>();
    if (tmpl.exits() != null) {
      for (var entry : tmpl.exits().entrySet()) {
        String dirKey = entry.getKey();
        Direction dir = Direction.parse(dirKey);
        String displayName = dir != null ? dir.getDisplayName() : dirKey;
        String targetRoomId = entry.getValue().targetRoomId();
        String targetName = targetRoomId;
        Room targetRoom = worldManager.getRoomActor(targetRoomId);
        if (targetRoom != null && targetRoom.getTemplate() != null) {
          targetName = targetRoom.getTemplate().name();
        }
        exits.add(new TownExitDto(dirKey, displayName, targetRoomId, targetName));
      }
    }

    List<TownNpcDto> npcs = new ArrayList<>();
    for (Mob mob : room.getMobs()) {
      if (mob == null || !mob.isValid()) continue;
      String id = mob.getTemplate().id();
      String alias = (mob.getAliases() != null && !mob.getAliases().isEmpty()) ? mob.getAliases().get(0) : id;
      String name = mob.getName();
      String title = mob.getTemplate().lookDescription();
      if (title == null || title.isBlank()) title = mob.getTemplate().description();
      String status = "狀態佳";

      List<TownCapabilityDto> caps = new ArrayList<>();
      if (mob.getTemplate().kind() == MobKind.FRIENDLY) {
        caps.add(new TownCapabilityDto("TALK", "交談", "ask " + alias, "💬"));
      }
      if (mob.getTemplate().shopId() != null) {
        caps.add(new TownCapabilityDto("SHOP", "貨棧買賣", "shop", "🛒"));
      }
      if (id.contains("innkeeper") || (mob.getAliases() != null && mob.getAliases().contains("innkeeper"))) {
        caps.add(new TownCapabilityDto("REST", "客棧安歇", "rest", "🛏️"));
      }
      if (id.contains("tie_niu") || id.contains("ling_shuang") || id.contains("iron") || id.contains("ling") || id.contains("companion")) {
        boolean inParty = (party != null && party.getMembers() != null && party.getMembers().stream().anyMatch(m ->
            (m.getId() != null && (m.getId().equals(id) || m.getId().contains(alias))) ||
            (m.getName() != null && (m.getName().contains(name) || name.contains(m.getName())))));
        if (inParty) {
          caps.add(new TownCapabilityDto("DISMISS", "請離隊友", "dismiss " + alias, "👋"));
        } else {
          caps.add(new TownCapabilityDto("RECRUIT", "招募入隊", "recruit " + alias, "🤝"));
        }
      }
      if (id.contains("elder")) {
        caps.add(new TownCapabilityDto("QUEST", "任務指引", "ask " + alias, "📜"));
      }
      if (mob.getTemplate().kind() == MobKind.AGGRESSIVE) {
        caps.add(new TownCapabilityDto("FIGHT", "拔劍迎擊", "kill " + alias, "⚔️"));
      }

      npcs.add(new TownNpcDto(id, alias, name, title, status, caps));
    }

    List<TownItemDto> items = new ArrayList<>();
    for (GameItem item : room.getItems()) {
      if (item == null) continue;
      items.add(new TownItemDto(item.getId(), item.getDisplayName(), "📦", 1));
    }

    DrpgStateDto dto = DrpgStateDto.ofTown(
        zoneId, zoneName, tmpl.id(), tmpl.name(), tmpl.description(), safe, exits, npcs, items, party
    );
    player.sendJson(dto);
  }
}
