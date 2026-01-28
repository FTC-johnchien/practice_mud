package com.example.htmlmud.application.perform.impl;

import org.springframework.stereotype.Component;
import com.example.htmlmud.application.perform.Perform;
import com.example.htmlmud.domain.actor.impl.LivingActor;
import com.example.htmlmud.domain.exception.MudException;

@Component
public class TaichiHitPerform implements Perform {
  @Override
  public String getId() {
    return "taichi_hit";
  }

  @Override
  public void execute(LivingActor user, LivingActor target) {
    // 1. 檢查內力
    if (user.getMp() < 50)
      throw new MudException("你內力不夠！");
    user.consumeMp(50);

    // 2. 執行特殊效果 (暈眩)
    target.addBuff(new StunBuff(3)); // 暈 3 秒

    // 3. 造成傷害
    int dmg = 500;
    target.receiveDamage(dmg);

    // 4. 顯示訊息
    user.getRoom().broadcast(user.getName() + " 運起太極真氣，一掌擊中 " + target.getName() + " 的丹田！");
  }
}
