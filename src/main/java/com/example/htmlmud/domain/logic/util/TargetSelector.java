package com.example.htmlmud.domain.logic.util;

import com.example.htmlmud.domain.actor.MobActor;
import com.example.htmlmud.domain.model.map.MobTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TargetSelector {

  // 用來解析 "name index" (例如: goblin 2)
  private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*)\\s+(\\d+)$");

  // 用來解析 "index.name" (例如: 2.goblin)
  private static final Pattern DOT_NOTATION = Pattern.compile("^(\\d+)\\.(.*)$");

  /**
   * 從列表中找到符合描述的目標
   * 
   * @param candidates 房間裡的所有怪物 (或物品)
   * @param input 玩家輸入的字串 (例如 "elite soldier 3")
   * @return 找到的 Mob，若無則回傳 null
   */
  public MobActor selectMob(List<MobActor> candidates, String input) {
    if (input == null || input.isBlank())
      return null;

    // 1. 解析輸入，分離出 "名稱關鍵字" 和 "第幾個"
    ParsedTarget parsed = parseInput(input);
    String keyword = parsed.name.toLowerCase();
    int targetIndex = parsed.index; // 1-based index

    int matchCount = 0;

    // 2. 遍歷候選名單
    for (MobActor mob : candidates) {
      // 3. 檢查是否匹配
      if (isMatch(mob, keyword)) {
        matchCount++;
        // 4. 如果匹配次數等於玩家指定的索引，就是它了
        if (matchCount == targetIndex) {
          return mob;
        }
      }
    }

    return null; // 找不到
  }

  private boolean isMatch(MobActor mob, String keyword) {
    MobTemplate tpl = mob.getTemplate();

    // 規則 A: 直接比對 Name (忽略大小寫)
    if (tpl.name().toLowerCase().contains(keyword))
      return true;

    // 規則 B: 比對 Aliases
    // 只要 Alias 列表裡有任何一個字串 "包含" 玩家輸入的關鍵字
    // 例如 alias=["red goblin king"], input="red goblin" -> true
    // 例如 alias=["goblin"], input="red" -> false
    if (tpl.aliases() != null) {
      for (String alias : tpl.aliases()) {
        if (alias.toLowerCase().contains(keyword)) {
          return true;
        }
      }
    }
    return false;
  }

  // --- 解析邏輯 ---

  private record ParsedTarget(String name, int index) {
  }

  private ParsedTarget parseInput(String input) {
    input = input.trim();

    // 檢查 "2.goblin" 格式
    Matcher dotMatcher = DOT_NOTATION.matcher(input);
    if (dotMatcher.find()) {
      int idx = Integer.parseInt(dotMatcher.group(1));
      String name = dotMatcher.group(2);
      return new ParsedTarget(name, idx);
    }

    // 檢查 "goblin 2" 格式
    Matcher trailMatcher = TRAILING_NUMBER.matcher(input);
    if (trailMatcher.find()) {
      String name = trailMatcher.group(1);
      int idx = Integer.parseInt(trailMatcher.group(2));
      return new ParsedTarget(name, idx);
    }

    // 預設: 找第 1 個
    return new ParsedTarget(input, 1);
  }
}
