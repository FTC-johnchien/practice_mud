package com.example.htmlmud.application.command.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import com.example.htmlmud.application.command.CommandAlias;
import com.example.htmlmud.application.command.PlayerCommand;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.context.MudContext;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.dungeon.model.DungeonTile.TileType;
import com.example.htmlmud.domain.dungeon.model.StepResult;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.template.ItemTemplate;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@CommandAlias({"dg", "step", "map"})
public class DungeonCommand implements PlayerCommand {

  private static final List<String> TOMB_MOBS = List.of(
      "taiyin_tomb:corpse_doll",
      "taiyin_tomb:blood_python",
      "taiyin_tomb:bone_bat",
      "taiyin_tomb:tomb_guard"
  );

  private static final List<String> TOMB_ITEMS = List.of(
      "taiyin_tomb:taiyin_pill",
      "taiyin_tomb:purify_talisman",
      "taiyin_tomb:bronze_sword",
      "taiyin_tomb:yin_robe",
      "taiyin_tomb:ancient_relic",
      "taiyin_tomb:tomb_key"
  );

  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final PartyService partyService;
  private final com.example.htmlmud.domain.dungeon.battle.DrpgBattleService battleService;

  @Override
  public String getKey() {
    return "dungeon";
  }

  @Override
  public void execute(String args) {
    Player self = MudContext.currentPlayer();
    String floorId = "taiyin_tomb_b1f";
    DungeonFloor floor = dungeonManager.getFloor(floorId);
    if (floor == null) {
      self.reply("【系統】地牢尚未開啟。");
      return;
    }
    DungeonPosition pos = dungeonManager.getOrCreatePosition(self.getName(), floorId);

    if (battleService.isInBattle(self.getName())) {
      self.reply("\u001B[1;31m⚠️ 戰鬥交鋒中，無法隨意移步！請點擊【⚔️ 迎戰】進攻或【🏃 遁地撤退】！\u001B[0m");
      broadcastDrpgState(self, floor, pos);
      return;
    }

    String input = args != null ? args.trim().toLowerCase() : "";

    // 處理無參數、map 或 ascii 指令
    if (input.isEmpty() || input.equals("map")) {
      self.reply("【靈識感應】已掃描太陰地宮一層 (B1F) 靈識雷達 [X: " + pos.getX() + ", Y: " + pos.getY() + "]，朝向: " + pos.getFacing().getSymbol() + " " + pos.getFacing().getChineseName() + "。\n"
          + dungeonNavigator.inspectForward(floor, pos));
      broadcastDrpgState(self, floor, pos);
      return;
    }

    if (input.equals("ascii")) {
      self.reply(dungeonNavigator.renderAsciiMap(floor, pos) + "\n" + dungeonNavigator.inspectForward(floor, pos));
      broadcastDrpgState(self, floor, pos);
      return;
    }

    // 處理移動與轉向指令
    switch (input) {
      case "w", "forward", "f", "step w" -> {
        StepResult res = dungeonNavigator.moveForward(floor, pos);
        self.reply(res.message() + (res.success() ? "\n" + dungeonNavigator.inspectForward(floor, pos) : ""));
        handlePostMovement(self, floor, pos, res);
        broadcastDrpgState(self, floor, pos);
      }
      case "s", "back", "b", "step s" -> {
        StepResult res = dungeonNavigator.moveBackward(floor, pos);
        self.reply(res.message() + (res.success() ? "\n" + dungeonNavigator.inspectForward(floor, pos) : ""));
        handlePostMovement(self, floor, pos, res);
        broadcastDrpgState(self, floor, pos);
      }
      case "a", "left", "tl", "turn left" -> {
        StepResult res = dungeonNavigator.turnLeft(pos);
        self.reply(res.message() + "\n" + dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "d", "right", "tr", "turn right" -> {
        StepResult res = dungeonNavigator.turnRight(pos);
        self.reply(res.message() + "\n" + dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "look", "inspect" -> {
        self.reply(dungeonNavigator.inspectForward(floor, pos));
        broadcastDrpgState(self, floor, pos);
      }
      case "dummy", "test", "testmob" -> {
        battleService.startTrainingBattle(self, pos);
      }
      default -> {
        self.reply("【太陰地宮指令】\n"
            + "  dungeon / map         - 展開靈識感應地圖與前方視野\n"
            + "  dungeon ascii         - 印出終端文字版 ASCII 感應地圖\n"
            + "  step w / forward      - 向前邁步 (探索/遇敵檢定)\n"
            + "  step s / back         - 向後退步\n"
            + "  step a / left         - 向左轉 90 度\n"
            + "  step d / right        - 向右轉 90 度\n"
            + "  dungeon look          - 凝神探查正前方地塊\n"
            + "  dungeon dummy         - 召喚太陰玄鐵試道傀儡測試");
      }
    }
  }

  private void handlePostMovement(Player self, DungeonFloor floor, DungeonPosition pos, StepResult res) {
    if (!res.success()) {
      return;
    }

    // 1. 警戒度累積與暗雷檢定
    int delta = ThreadLocalRandom.current().nextInt(8, 17);
    int danger = pos.addDanger(delta);
    if (danger >= 100) {
      triggerRandomEncounter(self, pos);
      pos.resetDanger();
      return;
    }

    // 2. 特殊地塊踩點事件
    handleTileEvent(self, floor, pos);
  }

  private void triggerRandomEncounter(Player self, DungeonPosition pos) {
    int count = ThreadLocalRandom.current().nextInt(1, 4);
    List<String> mobIds = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      mobIds.add(TOMB_MOBS.get(ThreadLocalRandom.current().nextInt(TOMB_MOBS.size())));
    }
    battleService.startBattle(self, pos, mobIds);
  }

  private void handleTileEvent(Player self, DungeonFloor floor, DungeonPosition pos) {
    DungeonTile tile = floor.getTile(pos.getX(), pos.getY());
    if (tile == null) return;

    switch (tile.getType()) {
      case TREASURE -> {
        if (!pos.isChestOpened(pos.getX(), pos.getY())) {
          pos.markChestOpened(pos.getX(), pos.getY());
          String itemId = TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size()));
          var opt = TemplateRepository.findItem(itemId);
          String itemName = opt.map(ItemTemplate::name).orElse("古仙秘寶");
          String itemDesc = opt.map(ItemTemplate::description).orElse("");
          self.reply("\n\u001B[1;33m🎁【開啟古仙棺槨】沉重的石蓋緩緩滑開，青幽靈光流轉！你獲得了法寶：【"
              + itemName + "】！\n" + itemDesc + "\u001B[0m\n");
        } else {
          self.reply("【古仙棺槨】這座石槨已被開啟，裡面空無一物。");
        }
      }
      case TRAP -> {
        Party party = partyService.getOrCreateParty(self.getName());
        for (PartyMember m : party.getMembers()) {
          m.takeDamage(15);
          m.consumeSan(5);
        }
        self.reply("\n\u001B[1;35m⚠️【觸發深淵黏液陷阱】地面突然塌陷，不可名狀的腐蝕黏液自地底噴湧而出！全隊受到 15 點腐蝕傷害，道心動搖 (-5 SAN)！\u001B[0m\n");
      }
      case STAIRS_DOWN -> {
        self.reply("\n\u001B[1;36m🏛️【通往下層古階】前方是一條深不見底的玄黑石階，直通太陰古塚二層 (B2F)... (前方幽冥死氣濃烈，暫未開放)\u001B[0m\n");
      }
      case DOOR -> {
        self.reply("\n\u001B[1;32m🚪【穿過玄冥石門】厚重石門兩側刻滿了辟邪雲紋，穿過此門步入深處。\u001B[0m\n");
      }
      default -> {}
    }
  }

  public void broadcastDrpgState(Player player, DungeonFloor floor, DungeonPosition pos) {
    if (player == null || floor == null || pos == null) {
      return;
    }
    Party party = partyService.getOrCreateParty(player.getName());
    String inspect = dungeonNavigator.inspectForward(floor, pos);
    var battleView = battleService.createBattleView(player.getName());
    DrpgStateDto dto = DrpgStateDto.of(floor, pos, inspect, party, battleView);
    player.sendJson(dto);
  }

  @Override
  public String getDescription() {
    return "探索 DRPG 網格地牢 (支援 map, step w/s/a/d)";
  }
}
