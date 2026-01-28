package com.example.htmlmud.infra.util;

import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.LivingActor;

@Component
public class MessageUtil {

  /**
   * 格式化訊息: 自己
   *
   * @param template 原始訊息 (只有 $N 標記)
   * @param executor (做動作的人)
   */
  public void send(String template, LivingActor executor) {
    send(template, executor, null, executor);
  }

  /**
   * 格式化訊息: 自己、房間的他人
   *
   * @param template 原始訊息 (只有 $N 標記)
   * @param self (做動作的人)
   * @param receiver 接收訊息的人 (誰在看這行字？)
   */
  public void send(String template, LivingActor executor, LivingActor receiver) {
    send(template, executor, null, receiver);
  }

  /**
   * 格式化訊息: 自己、對象、房間的他人
   *
   * @param template 原始訊息 (包含 $N, $n 等標記)
   * @param self (做動作的人)
   * @param target (對象)
   * @param receiver 接收訊息的人 (誰在看這行字？)
   */
  public void send(String template, LivingActor executor, LivingActor target,
      LivingActor receiver) {

    // 1. 判斷接收者是不是主角本人
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
        targetNoun = target.getState().gender.getYou();
      }
    }

    // 你是executor: 你舉起 wepon，用盡全力揮向野狼！
    isExecutor = executor.getId().equals(receiver.getId());

    executorNoun = executor.getName();
    if (isExecutor) {
      executorNoun = executor.getState().gender.getYou();
    }

    // 2. 進行替換
    // 注意：這裡使用簡單的 replace，效能足夠。若要更嚴謹可用 Regex
    receiver.reply(template.replace("$N", executorNoun).replace("$n", targetNoun));
  }
}
