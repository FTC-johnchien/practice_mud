package com.example.htmlmud.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.model.enums.Direction;

@Component
public class TargetSelector {

  // 用來解析 "name index" (例如: goblin 2)
  private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*)\\s+(\\d+)$");

  // 用來解析 "index.name" (例如: 2.goblin)
  private static final Pattern DOT_NOTATION = Pattern.compile("^(\\d+)\\.(.*)$");

  public static Object findTarget(Player self, Room room, String input) {
    if (input == null || input.isBlank()) {
      return room;
    }

    ParsedTarget parsed = parseInput(input);
    String keyword = parsed.name;
    int index = parsed.index;

    // 1. 優先找方向 (這不需要序號)
    Direction dir = Direction.parse(keyword);
    if (dir != null) {
      return dir;
    }

    // 2. 找自己
    if (keyword.equals("me") || self.getAliases().contains(keyword)) {
      return self;
    }

    // 3. 找房間內的生物 (包含 NPC 與其他玩家)
    // 這裡我們把所有生物集合起來，過濾出符合關鍵字的，再取第 N 個
    Optional<Living> foundLiving = room.getLivings().stream()
        .filter(l -> l.isValid() && l.getAliases().contains(keyword)).skip(index - 1).findFirst();
    if (foundLiving.isPresent()) {
      return foundLiving.get();
    }

    // 4. 找房間內的物品
    Optional<GameItem> foundRoomItem = room.getItems().stream()
        .filter(i -> i.getAliases().contains(keyword)).skip(index - 1).findFirst();
    if (foundRoomItem.isPresent()) {
      return foundRoomItem.get();

    }

    // 5. 找身上的物品 (背囊)
    Optional<GameItem> foundInvItem = self.getInventory().stream()
        .filter(i -> i.getAliases().contains(keyword)).skip(index - 1).findFirst();
    if (foundInvItem.isPresent()) {
      return foundInvItem.get();
    }

    return null; // 通通找不到
  }

  /**
   * 從列表中找到符合描述的目標
   *
   * @param candidates 房間裡的所有物品
   * @param input 玩家輸入的字串 (例如 "ring")
   * @return 找到的物品，若無則回傳 null
   */
  public GameItem selectItem(List<GameItem> candidates, String input) {
    return select(candidates, input, this::isMatchItem);
  }

  /**
   * 從列表中找到符合描述的目標
   *
   * @param candidates 房間裡的所有怪物 (或物品)
   * @param input 玩家輸入的字串 (例如 "elite soldier 3" 或 "2.goblin")
   * @return 找到的 Mob，若無則回傳 null
   */
  public Mob selectMob(List<Mob> candidates, String input) {
    return select(candidates, input, this::isMatchMob);
  }

  /**
   * 通用的選擇邏輯，減少重複程式碼
   */
  private <T> T select(List<T> candidates, String input, BiPredicate<T, String> matcher) {
    if (input == null || input.isBlank())
      return null;

    ParsedTarget parsed = parseInput(input);
    String keyword = parsed.name.toLowerCase();
    int targetIndex = parsed.index;
    int matchCount = 0;

    for (T candidate : candidates) {
      if (matcher.test(candidate, keyword)) {
        matchCount++;
        if (matchCount == targetIndex) {
          return candidate;
        }
      }
    }
    return null;
  }

  private boolean isMatchItem(GameItem item, String keyword) {
    return isMatch(item.getName(), item.getAliases(), keyword);
  }

  private boolean isMatchMob(Mob mob, String keyword) {
    return isMatch(mob.getTemplate().name(), mob.getTemplate().aliases(), keyword);
  }

  private boolean isMatch(String name, List<String> aliases, String keyword) {
    if (name.toLowerCase().contains(keyword))
      return true;

    if (aliases != null) {
      for (String alias : aliases) {
        if (alias.toLowerCase().contains(keyword))
          return true;
      }
    }
    return false;
  }

  // --- 解析邏輯 ---

  private record ParsedTarget(String name, int index) {
  }

  private static ParsedTarget parseInput(String input) {
    input = input.trim();

    // 檢查 "goblin 2" 格式
    Matcher trailMatcher = TRAILING_NUMBER.matcher(input);
    if (trailMatcher.find()) {
      String name = trailMatcher.group(1);
      int idx = Integer.parseInt(trailMatcher.group(2));
      return new ParsedTarget(name, idx);
    }

    // 檢查 "2.goblin" 格式
    Matcher dotMatcher = DOT_NOTATION.matcher(input);
    if (dotMatcher.find()) {
      int idx = Integer.parseInt(dotMatcher.group(1));
      String name = dotMatcher.group(2);
      return new ParsedTarget(name, idx);
    }

    // 預設: 找第 1 個
    return new ParsedTarget(input, 1);
  }
}
