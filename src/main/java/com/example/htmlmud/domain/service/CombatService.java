package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.BodyPartSelector;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.exception.MudException;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.skill.dto.ActiveSkillResult;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.infra.monitor.GameMetrics;
import com.example.htmlmud.infra.persistence.entity.SkillEntry;
import com.example.htmlmud.infra.util.ColorText;
import com.example.htmlmud.infra.util.FormulaEvaluator;
import com.example.htmlmud.infra.util.MessageUtil;
import com.example.htmlmud.infra.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombatService {
  private final GameMetrics gameMetrics;
  // 【戰鬥名單】
  // 使用 ConcurrentHashMap.newKeySet() 建立一個執行緒安全的 Set
  // 只有在名單裡的 Actor，系統才會計算它的攻擊 CD
  private final Set<Living> combatants = ConcurrentHashMap.newKeySet();

  private final MessageUtil messageUtil;
  private final SkillService skillService;
  private final XpService xpService;



  /**
   * 【戰鬥系統心跳】 由 WorldPulse 呼叫
   */
  public void tick(long currentTick, long now) {
    // 只遍歷「正在戰鬥」的生物，效率極高
    // log.info("tick combatants: {}" + combatants.size());
    for (Living actor : combatants) {

      // 檢查是否正在戰鬥
      if (!actor.isInCombat) {
        // log.info("actor.isInCombat:false name:{}", actor.getName());
        endCombat(actor);
        continue;
      }

      // 檢查攻擊冷卻時間 (CD)
      if (now < actor.nextAttackTime) {
        // log.info("actor.nextAttackTime:{} now:{}", actor.nextAttackTime, now);
        continue;
      }

      // 如果是 Mob 每次攻擊要先找仇恨最高的目標(如果有的話)
      if (actor instanceof Mob mob) {
        if (!checkMobaggroTable(mob)) {
          log.info("mob aggroTable 為空 name:{}", actor.getName());
          endCombat(actor); // 對手不見了，脫離戰鬥
          continue;
        }
      }

      // 檢查戰鬥目標是有效
      Living target = actor.getCombatTarget().orElse(null);
      if (target == null || !target.isValid()
          || !target.getCurrentRoom().equals(actor.getCurrentRoom())) {
        // log.info("actor.combatTarget 無效 actor.name:{} isValid:{} isDead:{}", actor.getName(),
        endCombat(actor); // 對手不見了，脫離戰鬥
        continue;
      }


      // 必須在先設定下一次攻擊時間（冷卻鎖），防止 WorldPulse 的下一個 Tick (100ms後) 重複觸發新的回合
      nextAttackTime(actor);

      // 執行 passive 攻擊
      performAttackRound(actor, target, now);
    }
  }

  /**
   * 【註冊入口】 當發生攻擊行為時 (Player kill Mob 或 Mob aggro Player) 呼叫此方法
   */
  public void startCombat(Living self, Living target) {

    if (self.combatTargetId == null) {
      self.combatTargetId = target.getId();
    }

    self.isInCombat = true;

    // 【節奏控制】
    // 攻擊者：立即獲得攻擊機會 (或是很短的延遲)
    self.nextAttackTime = System.currentTimeMillis();

    // 【加入名單】
    combatants.add(self);

    target.onAttacked(self);
    // log.info(self.getName() + " 進入戰鬥名單！");
  }

  /**
   * 【離開戰鬥】 一方死亡、逃跑、或目標消失時呼叫
   */
  public void endCombat(Living self) {
    if (self == null) {
      return;
    }

    self.isInCombat = false;
    self.combatTargetId = null;
    // self.nextAttackTime = 0;

    // 【移出名單】
    combatants.remove(self);
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  /**
   * 執行一次攻擊判定
   *
   * @return 造成的傷害值 (0 代表未命中或被格擋)
   */
  private int calculateDamage(Living attacker, Living defender) {
    LivingStats attState = attacker.getStats();
    LivingStats defState = defender.getStats();

    // 1. 命中判定 (範例：靈巧越高，命中越高)
    // 假設基礎命中 80% + (攻方靈巧 - 守方靈巧)%
    double hitChance = 0.8 + ((attState.dex - defState.dex) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      // Room room = attacker.getCurrentRoom();
      // room.broadcast(attacker.getId(), "log:calculateDamage $N miss");
      // log.info("calculateDamage miss");
      return -1; // -1 代表 Miss
    }

    DamageSource weapon = attacker.getCurrentAttackSource();

    // 2. 傷害公式 (範例：攻擊力 - 防禦力，浮動 10%)
    int damage = random(weapon.minDamage(), weapon.maxDamage());
    // log.info("attState.damage:{} defState.defense:{}", damage, defState.defense);
    int rawDmg = damage - defender.defense;
    if (rawDmg <= 0)
      rawDmg = 1; // 至少造成 1 點傷害

    // 加入浮動 (0.9 ~ 1.1)
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    int finalDmg = (int) (rawDmg * variance);
    if (finalDmg <= 0) {
      finalDmg = 0; // 至少造成 1 點傷害
    }
    // log.info("finalDmg:{}", finalDmg);
    return finalDmg;
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  private void performAttackRound(Living self, Living target, long now) {
    // log.info("performAttackRound now:{}", now);

    // 使用虛擬執行緒處理每一次的攻擊，這樣可以使用 Thread.sleep 而不阻塞主引擎
    Thread.ofVirtual().name("CombatRound-" + self.getId()).start(() -> {
      try {
        // 取得攻擊次數：取「種族/生物基礎次數」與「技能額外次數」的最大值 (假設技能模板有此欄位)
        int attacks = self.getAttacksPerRound();

        for (int i = 0; i < attacks; i++) {
          // 每次攻擊前檢查雙方是否還具備戰鬥條件 (可能在 sleep 期間有人死了或離開了)
          if (!self.isValid() || target == null || !target.isValid() || !self.isInCombat()) {
            break;
          }

          ActiveSkillResult skill = skillService.getAutoAttackSkill(self);

          // 範圍攻擊
          if (skill.getTemplate().getTags().contains("AOE")) {
            // 取出 self 房間里的所有 living, 排除自己
            List<Living> targets = self.getCurrentRoom().getLivings();
            targets.remove(self);
            // log.info("範圍攻擊了 {} 人", targets.size());

            for (Living living : targets) {
              performAttack(self, living, skill, System.currentTimeMillis());
              gameMetrics.incrementSystemTask(); // 記錄每一次對單體的攻擊行動
            }

          }

          // 單體攻擊
          else {
            performAttack(self, target, skill, System.currentTimeMillis());
            gameMetrics.incrementSystemTask(); // 記錄一次單體攻擊行動
          }


          // 如果還有下一次攻擊且目標未死，則等待間隔
          if (i < attacks - 1 && target.isValid()) {

            // 種族的多次攻擊非由 tick 觸發，間隔 0.45 秒 - 0.55 秒
            long nextAttackTime = ThreadLocalRandom.current().nextLong(450, 551);
            Thread.sleep(nextAttackTime);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    });
  }


  private void performAttack(Living self, Living target, ActiveSkillResult skill, long now) {

    // 由技能里隨機抽出一招
    MoveAction action = RandomUtil.pickWeighted(skill.getTemplate().getMoves());



    // TODO
    // 施展招式



    // 招式 miss 判定



    // target 閃避判定



    // target 招架判定



    // 攻擊擊中
    // TODO 計算傷害公式 (敵我雙方差距，技能等級，招式倍率，武器的攻擊力，防具的防禦力，職業加成，抗性倍率)
    // ---------------------------------------------------------------------------------------------
    int rawDmg = calculateDamage(self, target);

    // 套用 skill 倍率 (基礎傷害 + 等級 * 升級加級)
    double skillDmg = skill.template().getMechanics().damage()
        + (skill.getLevel() * skill.template().getScaling().damagePerLevel());
    // log.info("skillDmg:{}", skillDmg);
    rawDmg += skillDmg;
    // log.info("rawDmg:{}", rawDmg);

    int dmgAmout = (int) (rawDmg * action.damageMod());
    // ---------------------------------------------------------------------------------------------



    // 5. 格式化戰鬥訊息
    String sWeapon = "";
    String tWeapon = "";
    if (self.getMainHandWeapon() != null) {
      sWeapon = self.getMainHandWeapon().getDisplayName();
    }
    if (target.getMainHandWeapon() != null) {
      tWeapon = target.getMainHandWeapon().getDisplayName();
    }
    String part = BodyPartSelector.getRandomBodyPart();
    String msg = action.msg().cast();

    List<Player> audiences = self.getCurrentRoom().getPlayers();

    // 招架 parry
    if (dmgAmout <= 0) {
      msg += "\r\n" + action.msg().miss();
      for (Player receiver : audiences) {
        messageUtil.send(CombineString(msg, sWeapon, tWeapon, part), self, target, receiver);
      }
      return;
    }

    // 將傷害送給 target
    target.onDamage(dmgAmout, self);

    msg += "\r\n" + action.msg().hit();
    msg = CombineString(msg, sWeapon, tWeapon, part);
    msg = msg.replace("$d", ColorText.damage(dmgAmout));

    // 產生 [秒.毫秒] 的時間戳記前綴
    long nowMs = System.currentTimeMillis();
    String timestamp = String.format("[%02d.%03d] ", (nowMs / 1000) % 60, nowMs % 1000);
    for (Living receiver : audiences) {
      messageUtil.send(timestamp + msg, self, target, receiver);
    }

    if (target instanceof Player player) {
      player.sendStatUpdate();
    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  public void onAttacked(Living self, Living attacker) {
    self.isInCombat = true;
    if (self.getCombatTarget() == null) {
      self.combatTargetId = attacker.getId();
    }

    // 若是 Mob 就增加仇恨值
    if (self instanceof Mob mob) {
      // log.info("初始放入仇恨表 name:{}", attacker.getName());
      mob.addAggro(attacker.getId(), 1);
    }

    // 攻擊準備時間
    reactionTime(self);

    // 加入戰鬥名單
    if (!combatants.contains(self)) {
      combatants.add(self);
      // log.info(self.getName() + " 進入戰鬥名單！");
    }
  }

  public void onDamage(int amount, Living self, Living attacker) {

    // 檢查是否還存在，死亡可能會消失
    if (self == null) {
      log.info("onDamage 對象已消失");
      return;
    }

    // 檢查是否還活著
    if (!self.isValid()) {
      // log.info("{} 已經死亡，無法受傷", self.getName());
      return;
    }

    // 扣除 HP
    self.getStats().hp -= amount;
    // self.getStats().setHp(self.getStats().getHp() - amount);

    // for test----------------------------------------------------------------------------------
    Room room = self.getCurrentRoom();
    // room.broadcast("log:" + self.getName() + " 目前 HP: " + self.getStats().getHp() + "/"
    // + self.getStats().getMaxHp());
    // for test----------------------------------------------------------------------------------

    // 檢查是否死亡
    if (self.isDead()) {
      log.info("{} 被打死了", self.getName());

      // 終止戰鬥並移出戰鬥名單
      endCombat(self);

      // 先判斷 self 是 player 還是 mob 然後將其移出房間，避免後續的攻擊還持續的打到已死亡的對象
      if (self instanceof Player player) {
        room.removePlayer(player.getId());

        // 更新前端玩家狀態
        player.getStats().setHp(0);
        player.sendStatUpdate();
      } else if (self instanceof Mob mob) {
        room.removeMob(mob.getId());
      }

      // 發送 self 死亡事件
      self.die(attacker.getId());
    } else {
      if (!self.isInCombat) {
        self.isInCombat = true;
        if (self.combatTargetId == null) {
          self.combatTargetId = attacker.getId();
        }

        // 準備反應時間
        reactionTime(self);

        // 加入戰鬥名單
        if (!combatants.contains(self)) {
          combatants.add(self);
          // log.info(self.getName() + " 進入戰鬥名單！");
        }
      }

      // mob 就增加仇恨值
      if (self instanceof Mob mob) {
        // log.info("增加仇恨 name:{} {}", attacker.getName(), amount);
        mob.addAggro(attacker.getId(), amount);
      }
    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  private boolean checkMobaggroTable(Mob mob) {

    // 取出當前最高仇恨目標
    String targetId = mob.getHighestAggroTarget();

    // log.info("checkMobaggroTable targetId:{}", targetId);
    // 仇恨表為空，脫離戰鬥
    if (targetId == null) {
      return false;
    }

    // 檢查新戰鬥目標是否有效
    mob.combatTargetId = targetId;
    Living target = mob.getCombatTarget().orElse(null);
    if (target == null || !target.isValid()
        || !target.getCurrentRoom().equals(mob.getCurrentRoom())) {
      // log.info("checkMobaggroTable target 無效 targetId:{}", targetId);
      // 無效的目標就移出仇恨表
      mob.removeAggro(mob.getCombatTargetId());
      mob.combatTargetId = null;
      // 呼叫自己直到仇恨表為空
      checkMobaggroTable(mob);
    }

    // 新戰鬥目標有效，繼續戰鬥
    return true;
  }

  // 準備反應的時間
  private void reactionTime(Living self) {
    long speed = self.getAttackSpeed();
    // 計算 0.5 * speed +/- 0.1 * speed，即範圍 [0.4 * speed, 0.6 * speed]
    long reactionTime = ThreadLocalRandom.current().nextLong(speed * 4 / 10, (speed * 6 / 10) + 1);
    self.setNextAttackTime(System.currentTimeMillis() + reactionTime);
  }

  private void nextAttackTime(Living self) {
    long speed = self.getAttackSpeed();
    // 計算 speed +/- 0.2 * speed，即範圍 [0.8 * speed, 1.2 * speed]
    long nextAttackTime =
        ThreadLocalRandom.current().nextLong(speed * 8 / 10, (speed * 12 / 10) + 1);
    self.setNextAttackTime(System.currentTimeMillis() + nextAttackTime);
  }

  private long calculateNextLevelXp(int level) {
    return 10;
  }

  // 取出隨機數值
  private int random(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  private String CombineString(String msg, String W, String w, String l) {
    return msg.replace("$l", l).replace("$W", W).replace("$w", w);
  }



  // 取得下一次攻擊的間隔時間
  private long getNextAttackDelay(long baseSpeed) {
    // 引入 10% ~ 20% 的隨機浮動
    // 如果武器速度是 2000ms，實際間隔會在 1800ms ~ 2200ms 之間飄移
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    return (long) (baseSpeed * variance);
  }

  private void doCombatBroadcast(List<Player> players, Living source, Living target,
      String messageTemplate) {
    if (source == null) {
      throw new MudException("source is null");
    }
    if (target == null) {
      throw new MudException("target is null");
    }

    // 1. 取得房間內所有人 (包含做動作的人自己)
    List<Living> audiences = new ArrayList<>();
    audiences.addAll(players);
    // audiences.addAll(self.getMobs()); // 怪物通常不需要收訊息，除非有 AI 反應

    // 2. 對每個人發送「客製化」的訊息
    for (Living receiver : audiences) {
      messageUtil.send(messageTemplate, source, target, receiver);
    }
  }



  private void startRound(Player player) {
    ActiveSkillResult activeSkill = skillService.getAutoAttackSkill(player);
    SkillTemplate template = activeSkill.getTemplate();

    // 1. 計算本回合回復的 Charge
    String regenFormula = template.getMechanics().getFormula("chargeRegen");
    if (regenFormula != null) {
      int regenAmount = FormulaEvaluator.evaluateInt(regenFormula, player, template);
      player.getStats().modifyCombatResource("charge", regenAmount);

      if (regenAmount > 0) {
        player.reply("你的太極心法運轉，回復了 " + regenAmount + " 點氣勁。");
      }
    }

    // 2. 決定連擊次數上限
    String comboFormula = template.getMechanics().getFormula("maxComboCount");
    int maxCombo = 0;
    if (comboFormula != null) {
      maxCombo = FormulaEvaluator.evaluateInt(comboFormula, player, template);
    }

    // 3. 執行連擊判定...
    // 如果 maxCombo 是 0，就不執行連擊迴圈
    // 如果 maxCombo 是 3，且目前 Charge 足夠，就最多打 3 下
  }


  private void processSkillExperience(Player player, ActiveSkillResult result) {
    SkillEntry entry = result.entry();
    SkillTemplate template = result.template();

    // 如果是最高等級，就不加經驗了
    if (entry.getLevel() >= template.getMechanics().maxLevel()) {
      return;
    }

    // A. 增加經驗值 (公式：智力越高練越快)
    long gain = 10 + (player.getStats().intelligence / 2);
    entry.addXp(gain);

    // B. 檢查是否升級 (呼叫我們第一部分寫的公式)
    long needed = xpService.getRequiredXp(entry, template);
    log.info("Level:{} needed to next Level:{}", entry.getLevel(), needed);

    if (entry.getXp() >= needed) {
      entry.levelUp();
      entry.setXp(entry.getXp() - needed); // 扣除升級所需，溢出的保留

      player.reply("你的 \u001B[33m" + template.getName() + "\u001B[0m 進步了！" + "(等級 "
          + entry.getLevel() + ")");
    }
  }

  /**
   * 計算獲得經驗值 (範例)
   */
  private int calculateExp(Living mob, Living player) {
    return mob.getStats().getLevel() * 10;
  }

  private void afterAttack(Player attacker, ActiveSkillResult skillResult) {
    SkillEntry userSkill = skillResult.entry();
    SkillTemplate template = skillResult.template();

    // 1. 計算獲得熟練度
    // 智力越高練越快，或者根據怪物強度
    int xpGain = 1 + (attacker.getStats().intelligence / 10);

    // 2. 增加熟練度
    userSkill.addXp(xpGain);

    // 3. 檢查升級
    if (userSkill.getXp() >= calculateNextLevelXp(userSkill.getLevel())) {
      userSkill.levelUp();
      attacker.reply("你的 " + template.getName() + " 進步了！(等級 " + userSkill.getLevel() + ")");
    }
  }

}
