package com.example.htmlmud.infra.util;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.infra.util.formula.PlayerSkillMap;
import com.example.htmlmud.infra.util.formula.SkillContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FormulaEvaluator {
  private static final ExpressionParser parser = new SpelExpressionParser();

  /**
   * 計算公式
   *
   * @param formula JSON 字串 (e.g., "Math.max(0, (skill.level / 10) - 2)")
   * @param player 玩家物件 (資料來源)
   * @param currentSkill 當前正在使用的技能 (提供 'skill' 上下文)
   */
  public static int evaluateInt(String formula, Player player, SkillTemplate currentSkill) {
    if (formula == null || formula.isEmpty()) {
      return 0;
    }

    try {
      // 1. 建立我們的魔法 Map
      PlayerSkillMap rootMap = new PlayerSkillMap(player);

      // 2. 處理 'skill' (this)
      // 將「當前這招技能」的等級放進去
      // 這樣公式裡的 skill.level 就能讀到這招的等級
      int currentSkillLv = player.getSkillLevel(currentSkill.getId());
      rootMap.put("skill", new SkillContext(currentSkillLv));

      // 3. 處理 'Math'
      // SpEL 預設要用 T(java.lang.Math).max(...) 很醜
      // 我們直接把 Math 類別塞進去，讓公式可以直接寫 Math.max(...)
      rootMap.put("Math", Math.class);

      // 4. 設定 Context
      // 將 rootMap 設為 RootObject，這樣公式裡的變數會直接對應到 Map 的 key
      StandardEvaluationContext context = new StandardEvaluationContext(rootMap);

      // 5. 解析並計算
      // SpEL 會自動轉型，我們預期結果是 Number
      Double result = parser.parseExpression(formula).getValue(context, Double.class);

      return result != null ? result.intValue() : 0;

    } catch (Exception e) {
      // 建議用 Logger 記錄錯誤，這裡先印出
      System.err.println("公式計算失敗 [" + formula + "]: " + e.getMessage());
      return 0; // 發生錯誤時的回退值
    }
  }

}
