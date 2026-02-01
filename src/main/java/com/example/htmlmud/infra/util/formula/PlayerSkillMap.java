package com.example.htmlmud.infra.util.formula;

import java.util.HashMap;
import com.example.htmlmud.domain.actor.impl.Player;

/**
 * 這是一個「動態」的 Map 它平時是空的，但當你跟它要 "wudang_force" 時， 它會立刻去 Player 身上查等級，並回傳一個 SkillContext
 */
public class PlayerSkillMap extends HashMap<String, Object> {
  private final Player player;

  public PlayerSkillMap(Player player) {
    this.player = player;
  }

  @Override
  public Object get(Object key) {
    // 1. 如果 Map 裡原本就有這個 key (例如 "skill", "Math")，直接回傳
    if (super.containsKey(key)) {
      return super.get(key);
    }

    // 2. 如果沒有，我們假設這個 key 是一個 Skill ID (例如 "wudang_force")
    String skillId = (String) key;

    // 3. 去 Player 身上查等級 (如果沒學過，通常回傳 0)
    int level = player.getSkillLevel(skillId);

    // 4. 包裝成 Wrapper 回傳，這樣 SpEL 就能繼續呼叫 .level
    return new SkillContext(level);
  }

  // 騙 SpEL 說我們什麼 Key 都有，誘發它呼叫 get()
  @Override
  public boolean containsKey(Object key) {
    return true;
  }
}
