package com.example.htmlmud.infra.util;

import com.example.htmlmud.domain.actor.impl.LivingActor;

public class MessageFormatter {

  /**
   * 格式化訊息
   *
   * @param template 原始訊息 (包含 $N, $e 等標記)
   * @param subject 主詞 (做動作的人)
   * @param receiver 接收訊息的人 (誰在看這行字？)
   */
  public static String format(String template, LivingActor source, LivingActor target,
      LivingActor receiver) {

    // 1. 判斷接收者是不是主角本人
    boolean isSource = source.getId().equals(receiver.getId());
    boolean isTarget = target.getId().equals(receiver.getId());

    // template: $N舉起wepon，用盡全力揮向$n！

    // John舉起wepon，用盡全力揮向野狼！
    String sourceStr = source.getName();
    String targetStr = target.getName();

    // 你舉起wepon，用盡全力揮向野狼！
    if (isSource) {
      sourceStr = source.getState().sex.getYou();
    }
    // 野狼舉起 wepon，用盡全力揮向你！
    else if (isTarget) {
      targetStr = target.getState().sex.getYou();
    }

    // 2. 進行替換
    // 注意：這裡使用簡單的 replace，效能足夠。若要更嚴謹可用 Regex
    return template.replace("$N", sourceStr).replace("$n", targetStr);
  }

  // 進階：如果有兩個對象 (A 打 B)
  // 標記可以是 $N (A的名字), $n (B的名字)
  public static String formatCombat(String template, LivingActor source, LivingActor target,
      LivingActor receiver) {
    // 處理 source 的代名詞
    String result = format(template, source, target, receiver);

    // 處理 target 的代名詞 (這裡需要另一組標記，例如 $T_N, $T_e)
    // 這邊邏輯會稍微複雜一點，建議先實作單人版本
    return result;
  }
}
