package com.example.htmlmud.domain.service;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.enums.Gender;

@Component
public class MessageUtil {

  /**
   * 格式化訊息: 自己
   *
   * @param template 原始訊息 (只有 $N 標記)
   * @param executor (做動作的人)
   */
  // public void send(String template, Living executor) {
  // send(template, executor, null, executor);
  // }

  public static String format(String template, Living executor) {
    return format(template, executor, null, executor);
  }

  /**
   * 格式化訊息: 自己、房間的他人
   *
   * @param template 原始訊息 (只有 $N 標記)
   * @param self (做動作的人)
   * @param receiver 接收訊息的人 (誰在看這行字？)
   */
  public static void send(String template, Living executor, Player receiver) {
    send(template, executor, null, receiver);
  }

  public static String format(String template, Living executor, Player receiver) {
    return format(template, executor, null, receiver);
  }

  /**
   * 格式化訊息: 自己、對象、房間的他人
   *
   * @param template 原始訊息 (包含 $N, $n 等標記)
   * @param self (做動作的人)
   * @param target (對象)
   * @param receiver 接收訊息的人 (誰在看這行字？)
   */
  public static void send(String template, Living executor, Living target, Player receiver) {
    String msg = format(template, executor, target, receiver);
    if (msg == null) {
      return;
    }

    receiver.reply(msg);
  }

  public static String format(String template, Living executor, Living target, Living receiver) {
    if (template == null) {
      return "";
    }
    if (receiver == null || !(receiver instanceof Player)) {
      return template;
    }
    if (executor == null) {
      return template;
    }

    // 判斷接收者是不是主角本人 (執行者)
    boolean isExecutor = executor.getId().equals(receiver.getId());
    String executorNoun = executor.getName();
    if (isExecutor) {
      Gender g = (executor.getStats() != null) ? executor.getStats().getGender() : null;
      executorNoun = (g != null && g.getYou() != null) ? g.getYou() : "你";
    }

    // 判斷接收者是不是受詞本人 (目標)
    String targetNoun = "";
    if (target != null) {
      boolean isTarget = target.getId().equals(receiver.getId());
      targetNoun = target.getName();
      if (isTarget) {
        Gender g = (target.getStats() != null) ? target.getStats().getGender() : null;
        targetNoun = (g != null && g.getYou() != null) ? g.getYou() : "你";
      }
    }

    // 進行替換：主語 $N 與受詞 $n
    return template.replace("$N", executorNoun).replace("$n", targetNoun);
  }
}
