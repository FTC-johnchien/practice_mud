package com.example.htmlmud.infra.util;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Player;

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
    if (receiver == null || !(receiver instanceof Player)) {
      return template;
    }
    if (executor == null || executor.getStats().getGender() == null) {
      return template;
    }


    // 判斷接收者是不是主角本人
    boolean isExecutor = false;
    boolean isTarget = false;
    String executorNoun = null;
    String targetNoun = "";

    // template: $N舉起 wepon，用盡全力揮向$n！
    // 房間其他人: John舉起 wepon，用盡全力揮向野狼！

    // 你是target: 野狼舉起 wepon，用盡全力揮向你！
    if (target != null) {
      isTarget = target.getId().equals(receiver.getId());

      targetNoun = target.getName();
      if (isTarget) {
        targetNoun = target.getStats().getGender().getYou();
      }
    }

    // 你是executor: 你舉起 wepon，用盡全力揮向野狼！
    isExecutor = executor.getId().equals(receiver.getId());

    executorNoun = executor.getName();
    if (isExecutor) {
      executorNoun = executor.getStats().getGender().getYou();
    }

    // 進行替換
    // 注意：這裡使用簡單的 replace，效能足夠。若要更嚴謹可用 Regex
    return template.replace("$N", executorNoun).replace("$n", targetNoun);
  }

}
