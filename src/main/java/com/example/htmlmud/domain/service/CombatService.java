package com.example.htmlmud.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.application.command.parser.BodyPartSelector;
import com.example.htmlmud.application.factory.WorldFactory;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.GameItem;
import com.example.htmlmud.domain.model.LivingState;
import com.example.htmlmud.domain.model.MoveAction;
import com.example.htmlmud.domain.model.map.SkillTemplate;
import com.example.htmlmud.domain.model.skill.dto.ActiveSkillResult;
import com.example.htmlmud.domain.model.vo.DamageSource;
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

  // 【戰鬥名單】
  // 使用 ConcurrentHashMap.newKeySet() 建立一個執行緒安全的 Set
  // 只有在名單裡的 Actor，系統才會計算它的攻擊 CD
  private final Set<Living> combatants = ConcurrentHashMap.newKeySet();

  private final MessageUtil messageUtil;
  private final SkillService skillService;
  private final WorldFactory worldFactory;
  private final XpService xpService;



  /**
   * 【註冊入口】 當發生攻擊行為時 (Player kill Mob 或 Mob aggro Player) 呼叫此方法
   */
  public void startCombat(Living self, Living target) {

    if (self.combatTarget == null) {
      self.combatTarget = target;
    }
    if (target.combatTarget == null) {
      target.combatTarget = self;
    }

    self.isInCombat = true;
    target.isInCombat = true;

    long now = System.currentTimeMillis();

    // 【節奏控制】
    // 攻擊者：立即獲得攻擊機會 (或是很短的延遲)
    self.nextAttackTime = now;

    // 防禦者：需要反應時間 (攻速的一半 + 亂數)
    // 這樣可以避免雙方同時出刀的頓挫感
    long reactionDelay = (target.getAttackSpeed() / 2) + RandomUtil.jitter(200);
    self.setNextAttackTime(now + reactionDelay);

    // 【加入名單】
    combatants.add(self);
    combatants.add(target);

    log.info(self.getName() + " 與 " + target.getName() + " 進入戰鬥名單！");
  }

  /**
   * 【離開戰鬥】 一方死亡、逃跑、或目標消失時呼叫
   */
  public void endCombat(Living self) {
    self.isInCombat = false;
    self.combatTarget = null;
    self.nextAttackTime = 0;

    // 【移出名單】
    combatants.remove(self);
  }

  /**
   * 【核心心跳】 由 ServerEngine 每 100ms 呼叫一次
   */
  public void tick() {
    // 在這個 tick 以 now 的時間為準
    long now = System.currentTimeMillis();

    // 只遍歷「正在戰鬥」的生物，效率極高
    for (Living actor : combatants) {

      // 檢查是否正在戰鬥
      if (!actor.isInCombat) {
        endCombat(actor);
        return;
      }

      // 1. 檢查對手是否還活著/存在
      Living target = actor.combatTarget;
      if (target == null || target.isDead()
          || !target.getCurrentRoom().equals(actor.getCurrentRoom())) {
        endCombat(actor); // 對手不見了，脫離戰鬥
        continue;
      }

      // 2. 檢查攻擊冷卻時間 (CD)
      if (now >= actor.nextAttackTime) {
        performAttackRound(actor, target, now);
      }
    }
  }

  /**
   * 執行一次攻擊判定
   *
   * @return 造成的傷害值 (0 代表未命中或被格擋)
   */
  public int calculateDamage(Living attacker, Living defender) {
    LivingState attState = attacker.getState();
    LivingState defState = defender.getState();

    // 1. 命中判定 (範例：靈巧越高，命中越高)
    // 假設基礎命中 80% + (攻方靈巧 - 守方靈巧)%
    double hitChance = 0.8 + ((attState.dex - defState.dex) * 0.01);
    if (ThreadLocalRandom.current().nextDouble() > hitChance) {
      Room room = attacker.getCurrentRoom();
      room.broadcast("log:calculateDamage " + attacker.getName() + " miss");
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

  /**
   * 計算獲得經驗值 (範例)
   */
  public int calculateExp(Living mob, Living player) {
    return mob.getState().getLevel() * 10;
  }

  public void onAttacked(Living self, Living attacker) {
    self.isInCombat = true;
    if (self.combatTarget == null) {
      self.combatTarget = attacker;
    }

    // 攻擊準備時間
    long reactionTime = (self.getAttackSpeed() / 2) + RandomUtil.range(0, 500);
    self.setNextAttackTime(System.currentTimeMillis() + reactionTime);

    // 加入戰鬥名單
    combatants.add(self);
  }

  public void onDamage(int amount, Living self, Living attacker) {

    // 檢查是否還存在，死亡可能會消失
    if (self == null) {
      log.info("onDamage 對象已消失");
      return;
    }

    // 檢查是否還活著
    if (self.isDead()) {
      log.info("{} 已經死亡，無法受傷", self.getName());
      return;
    }

    // 扣除 HP
    self.getState().setHp(self.getState().getHp() - amount);

    // for test----------------------------------------------------------------------------------
    Room room = self.getCurrentRoom();
    room.broadcast("log:" + self.getName() + " 目前 HP: " + self.getState().getHp() + "/"
        + self.getState().getMaxHp());
    // for test----------------------------------------------------------------------------------

    // 檢查是否死亡，通知 self 死亡事件
    if (self.isDead()) {
      log.info("{} 死亡ing", self.getName());
      self.die(attacker);
    }
  }

  public void onDie(Living self, Living killer) {
    Room room = self.getCurrentRoom();

    String messageTemplate = "$N殺死了$n";
    List<Living> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());
    for (Living receiver : audiences) {
      messageUtil.send(messageTemplate, killer, self, receiver);
    }
    // room.broadcast(killerId + " 殺死了 " + self.getName());

    // 停止戰鬥狀態
    self.getState().setHp(0);
    self.isInCombat = false;
    self.combatTarget = null;

    // 製造屍體
    GameItem corpse = worldFactory.createCorpse(self);

    if (self.getCurrentRoom() == null) {
      log.error("{} onDie currentRoomId is null", self.getName());
      return;
    }

    // TODO 應交由房間處理 roomMessage livingDead 房間廣播死亡訊息
    // RoomActor room = worldManagerProvider.getObject().getRoomActor(self.getCurrentRoomId());
    // if (room != null) {
    // CompletableFuture<LivingActor> future = new CompletableFuture<>();
    // room.findActor(killerId, future);
    // LivingActor killer = future.orTimeout(1, java.util.concurrent.TimeUnit.SECONDS).join();
    // if (killer == null) {
    // log.error("killerId LivingActor not found: {}", killerId);
    // return;
    // }
    // room.broadcastToOthers(killerId, self.getName() + " 被 " + killer.getName() + " 殺死了！");
    // } else {
    // log.error("currentRoomId RoomActor not found: {}", self.getCurrentRoomId());
    // }
  }

  public void afterAttack(Player attacker, ActiveSkillResult skillResult) {
    SkillEntry userSkill = skillResult.entry();
    SkillTemplate template = skillResult.template();

    // 1. 計算獲得熟練度
    // 智力越高練越快，或者根據怪物強度
    int xpGain = 1 + (attacker.getState().intelligence / 10);

    // 2. 增加熟練度
    userSkill.addXp(xpGain);

    // 3. 檢查升級
    if (userSkill.getXp() >= calculateNextLevelXp(userSkill.getLevel())) {
      userSkill.levelUp();
      attacker.reply("你的 " + template.getName() + " 進步了！(等級 " + userSkill.getLevel() + ")");
    }
  }

  private long calculateNextLevelXp(int level) {
    return 10;
  }

  // 取出隨機數值
  private int random(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  private void processSkillExperience(Player player, ActiveSkillResult result) {
    SkillEntry entry = result.entry();
    SkillTemplate template = result.template();

    // 如果是最高等級，就不加經驗了
    if (entry.getLevel() >= template.getMechanics().maxLevel()) {
      return;
    }

    // A. 增加經驗值 (公式：智力越高練越快)
    long gain = 10 + (player.getState().intelligence / 2);
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

  public void startRound(Player player) {
    ActiveSkillResult activeSkill = skillService.getAutoAttackSkill(player);
    SkillTemplate template = activeSkill.getTemplate();

    // 1. 計算本回合回復的 Charge
    String regenFormula = template.getMechanics().getFormula("chargeRegen");
    if (regenFormula != null) {
      int regenAmount = FormulaEvaluator.evaluateInt(regenFormula, player, template);
      player.getState().modifyCombatResource("charge", regenAmount);

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

  public void performAttackRound(Living self, Living target, long now) {
    // 1. 決定攻擊次數 (預設 1，龍可能是 2 或 3)
    for (int i = 0; i < self.getAttacksPerRound(); i++) {
      // 2. 每次攻擊都重新抽一次技能
      // 第一下可能是 claw，第二下可能是 tail_swipe
      ActiveSkillResult skill = skillService.getAutoAttackSkill(self);

      // 3. 執行傷害
      performAttack(self, target, skill, now);

      // 如果對方死了就中斷
      if (target.isDead()) {
        break;
      }
    }
  }

  public void performAttack(Living self, Living target, ActiveSkillResult skill, long now) {

    // 2. 從 SkillManager 取得當前的一招 (包含描述與倍率)
    // log.info("{}", skill.getTemplate().getName());
    // List<CombatAction> actions = activeSkill.getTemplate().getActions();
    // CombatAction action = activeSkill.getTemplate().getActions()
    // CombatAction action = actions.get(ThreadLocalRandom.current().nextInt(actions.size()));

    // 3. 計算基礎傷害 取出 attacker 的 DamageSource
    // DamageSource weapon = self.getCurrentAttackSource();

    // 設定下一次攻擊時間 (攻速 2秒)
    // self.nextAttackTime = now + getNextAttackDelay(self.getAttackSpeed());



    int rawDmg = calculateDamage(self, target);

    // 套用 skill 倍率 (基礎傷害 + 等級 * 升級加級)
    double skillDmg = skill.template().getMechanics().damage()
        + (skill.getLevel() * skill.template().getScaling().damagePerLevel());
    log.info("skillDmg:{}", skillDmg);
    rawDmg += skillDmg;
    log.info("rawDmg:{}", rawDmg);

    // 選出那一招
    MoveAction action = RandomUtil.pickWeighted(skill.getTemplate().getMoves());

    int dmgAmout = (int) (rawDmg * action.damageMod());

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
    Room room = self.getCurrentRoom();
    List<Living> audiences = new ArrayList<>();
    audiences.addAll(room.getPlayers());

    // 招架 parry
    if (dmgAmout <= 0) {
      msg += "\r\n" + action.msg().miss();

      for (Living receiver : audiences) {
        messageUtil.send(CombineString(msg, sWeapon, tWeapon, part), self, target, receiver);
      }
      return;
    }

    // 將傷害送給 target
    target.onDamage(dmgAmout, self);

    // 3. 【設定下一次攻擊時間】 (關鍵：加入亂數擾動)
    long baseSpeed = self.getAttackSpeed(); // 例如 2000ms
    long jitter = ThreadLocalRandom.current().nextLong(-200, 200); // ±200ms
    self.nextAttackTime = (now + baseSpeed + jitter);

    for (Living receiver : audiences) {
      msg += "\r\n" + action.msg().hit();
      msg = CombineString(msg, sWeapon, tWeapon, part);
      msg = msg.replace("$d", ColorText.damage(dmgAmout));
      messageUtil.send(msg, self, target, receiver);
    }
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
}
