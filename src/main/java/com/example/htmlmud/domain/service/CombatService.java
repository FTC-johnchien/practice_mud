package com.example.htmlmud.domain.service;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.service.BodyPartSelector;
import com.example.htmlmud.domain.actor.impl.Living;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.actor.impl.Room;
import com.example.htmlmud.domain.model.config.MoveAction;
import com.example.htmlmud.domain.model.entity.LivingStats;
import com.example.htmlmud.domain.model.entity.SkillEntry;
import com.example.htmlmud.domain.model.skill.dto.ActiveSkillResult;
import com.example.htmlmud.domain.model.template.SkillTemplate;
import com.example.htmlmud.domain.model.vo.DamageSource;
import com.example.htmlmud.domain.port.DomainMetricsPort;
import com.example.htmlmud.domain.util.RandomUtil;
import com.example.htmlmud.protocol.util.ColorText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CombatService {
  private final DomainMetricsPort gameMetrics;
  // 【戰鬥名單】
  // 使用 ConcurrentHashMap.newKeySet() 建立一個執行緒安全的 Set
  // 只有在名單裡的 Actor，系統才會計算它的攻擊 CD
  private final Set<Living> combatants = ConcurrentHashMap.newKeySet();

  private final SkillService skillService;
  private final XpService xpService;
  private final XpProgressionService xpProgressionService;
  private final com.example.htmlmud.config.GameConfig gameConfig;
  private final com.example.htmlmud.domain.dungeon.battle.DefenseResolver defenseResolver;
  private final org.springframework.beans.factory.ObjectProvider<com.example.htmlmud.domain.party.service.PartyService> partyServiceProvider;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.dungeon.battle.BuffSettlementService buffSettlementService;

  public com.example.htmlmud.domain.dungeon.battle.BuffSettlementService getBuffSettlementService() {
    if (buffSettlementService == null) {
      buffSettlementService = new com.example.htmlmud.domain.dungeon.battle.BuffSettlementService();
    }
    return buffSettlementService;
  }



  /**
   * 【戰鬥系統心跳】 由 WorldPulse 呼叫
   */
  public void tick(long currentTick, long now) {
    // 只遍歷「正在戰鬥」的生物，效率極高
    // log.info("tick combatants: {}" + combatants.size());
    for (Living actor : combatants) {

      // 檢查是否正在戰鬥
      if (!actor.isValid() || !actor.isInCombat()) {
        // log.info("actor.isInCombat:false name:{}", actor.getName());
        endCombat(actor);
        continue;
      }

      // 檢查攻擊冷卻時間 (CD)
      if (now < actor.getNextAttackTime()) {
        // log.info("actor.nextAttackTime:{} now:{}", actor.nextAttackTime, now);
        continue;
      }

      Living target = null;
      // 如果是 Mob 每次攻擊要先找仇恨最高的目標(如果有的話)
      if (actor instanceof Mob mob) {
        target = mob.getHighestAggroTarget().orElse(null);
      } else {
        target = actor.getCombatTarget().orElse(null);
      }

      // 檢查戰鬥目標是有效
      // Living target = actor.getCombatTarget().orElse(null);
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
  public void startCombat(Living self, String targetId) {
    if (self == null) return;
    Room room = self.getCurrentRoom();
    if (room != null && room.hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.SAFE_ZONE)) {
      if (self instanceof Player p) {
        p.reply("此處乃安全祥和之地，嚴禁動武！");
      }
      return;
    }
    self.enterCombat(targetId);

    // 【加入名單】
    combatants.add(self);
    // log.info(self.getName() + " 進入戰鬥名單！");
  }

  /**
   * 【離開戰鬥】 一方死亡、逃跑、或目標消失時呼叫
   */
  public void endCombat(Living self) {
    if (self == null) {
      return;
    }

    self.exitCombat();

    // 【移出名單】
    combatants.remove(self);
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  /**
   * 執行基礎原始傷害計算 (武器物理/法術基值 - 目標護甲防禦，加浮動)
   */
  private int calculateRawBaseDamage(Living attacker, Living defender) {
    DamageSource weapon = attacker.getCurrentAttackSource();
    if (attacker instanceof Player player && partyServiceProvider != null) {
      var ps = partyServiceProvider.getIfAvailable();
      if (ps != null) {
        var party = ps.getOrCreateParty(player.getName());
        if (party != null && !party.getMembers().isEmpty()) {
          var leader = party.getMembers().get(0);
          var equippedWeapon = leader.getEquippedWeapon();
          String wName = (equippedWeapon != null) ? equippedWeapon.getName() : "徒手";
          weapon = new DamageSource(wName, "攻擊", leader.getEffectiveMinDamage(), leader.getEffectiveMaxDamage(), 2000, 0, -1);
        }
      }
    }

    int damage = random(weapon.minDamage(), weapon.maxDamage());
    int rawDmg = damage - defender.defense;
    if (rawDmg <= 0) {
      rawDmg = 1; // 至少造成 1 點傷害
    }

    // 加入浮動 (0.9 ~ 1.1)
    double variance = 0.9 + (ThreadLocalRandom.current().nextDouble() * 0.2);
    int finalDmg = (int) (rawDmg * variance);

    // 怪物階級 (MobRank) 傷害倍率加成
    if (attacker instanceof Mob mob && mob.getTemplate() != null && mob.getTemplate().rank() != null) {
      finalDmg = (int) Math.round(finalDmg * mob.getTemplate().rank().getDamageMultiplier());
    }

    return Math.max(1, finalDmg);
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
          if (target == null || !target.isValid()) {
            break;
          }

          ActiveSkillResult skill = skillService.getAutoAttackSkill(self);
          // 由技能里隨機抽出一招
          MoveAction action = RandomUtil.pickWeighted(skill.getTemplate().getMoves());

          // 範圍攻擊
          if (skill.getTemplate().getTags().contains("AOE")) {
            // 優化：直接在取得列表後過濾掉自己，不使用會導致報錯的 remove()
            List<Living> targets = self.getCurrentRoom().getLivings().stream()
                .filter(l -> !l.getId().equals(self.getId())).toList();

            for (Living living : targets) {
              performAttack(self, living, skill, action);
              gameMetrics.incrementSystemTask(); // 記錄每一次對單體的攻擊行動
            }

          }

          // 單體攻擊
          else {
            performAttack(self, target, skill, action);
            gameMetrics.incrementSystemTask(); // 記錄一次單體攻擊行動
          }


          // 如果還有下一次攻擊且目標未死，則等待間隔
          if (i < attacks - 1 && target.isValid()) {

            // 種族的多次攻擊非由 tick 觸發，間隔由 GameConfig 驅動
            long minInterval = (gameConfig != null && gameConfig.getCombat() != null)
                ? gameConfig.getCombat().getMultiAttackMinIntervalMs()
                : 450;
            long maxInterval = (gameConfig != null && gameConfig.getCombat() != null)
                ? gameConfig.getCombat().getMultiAttackMaxIntervalMs()
                : 551;
            long nextAttackTime = ThreadLocalRandom.current().nextLong(minInterval, maxInterval);
            Thread.sleep(nextAttackTime);
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Throwable t) {
        log.error("CombatRound 執行異常 self:{} target:{}", self.getId(), (target != null ? target.getId() : "null"), t);
      }
    });
  }


  private void performAttack(Living self, Living target, ActiveSkillResult skill,
      MoveAction action) {
    String sWeapon = "";
    String tWeapon = "";
    if (self instanceof Player player && partyServiceProvider != null) {
      var ps = partyServiceProvider.getIfAvailable();
      if (ps != null) {
        var party = ps.getOrCreateParty(player.getName());
        if (party != null && !party.getMembers().isEmpty()) {
          var leader = party.getMembers().get(0);
          if (leader.getEquippedWeapon() != null) {
            sWeapon = leader.getEquippedWeapon().getName();
          }
        }
      }
    }
    if (sWeapon.isEmpty() && self.getMainHandWeapon() != null) {
      sWeapon = self.getMainHandWeapon().getDisplayName();
    }
    if (target.getMainHandWeapon() != null) {
      tWeapon = target.getMainHandWeapon().getDisplayName();
    }
    String part = BodyPartSelector.getRandomBodyPart();
    String msg = action.msg().cast();
    List<Player> audiences = self.getCurrentRoom().getPlayers();

    // 0. 檢查禁魔區域 (NO_MAGIC)
    if (self.getCurrentRoom() != null && self.getCurrentRoom().hasFlag(com.example.htmlmud.domain.model.enums.RoomFlag.NO_MAGIC)) {
      if (skill != null && skill.template() != null) {
        boolean isMagic = skill.template().getType() == com.example.htmlmud.domain.model.enums.SkillType.MAGIC
            || (skill.template().getTags() != null && (skill.template().getTags().contains("SPELL") || skill.template().getTags().contains("MAGIC")));
        if (isMagic) {
          if (self instanceof Player p) {
            p.reply("此處為禁魔之地，靈氣枯竭，法術無法施展！");
          }
          return;
        }
      }
    }

    // 1. 基礎物理/法術原始傷害
    int rawBaseDmg = calculateRawBaseDamage(self, target);

    // 2. 套用 skill 倍率 (基礎傷害 + 等級 * 升級加級)
    double skillDmg = skill.template().getMechanics().damage()
        + (skill.getLevel() * skill.template().getScaling().damagePerLevel());
    int rawDmg = Math.max(1, (int) ((rawBaseDmg + skillDmg) * action.damageMod()));

    // 2.1 傷害類型抗性結算 (DamageType & LivingStats.resistances)
    var dmgType = (skill.template().getMechanics() != null && skill.template().getMechanics().damageType() != null)
        ? skill.template().getMechanics().damageType()
        : com.example.htmlmud.domain.model.enums.DamageType.PHYSICAL;
    double resistance = (target.getStats() != null) ? target.getStats().getResistance(dmgType) : 0.0;
    if (resistance != 0.0) {
      rawDmg = Math.max(1, (int) Math.round(rawDmg * Math.max(0.1, 1.0 - resistance)));
    }

    // 3. 一元一次擲骰圓桌判定 (One-Roll Combat Table: [Miss] -> [Dodge] -> [Parry] -> [Block] -> [Crit] -> [Normal Hit])
    com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseResolution resolution =
        defenseResolver.resolveLivingAttack(self, target, rawDmg);

    if (resolution.outcome() == com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome.MISS) {
      msg += "\r\n" + action.msg().miss();
      for (Player receiver : audiences) {
        MessageUtil.send(CombineString(msg, sWeapon, tWeapon, part), self, target, receiver);
      }
      return;
    }

    if (resolution.outcome() == com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome.DODGED) {
      msg += "\r\n" + resolution.combatLog();
      for (Player receiver : audiences) {
        MessageUtil.send(CombineString(msg, sWeapon, tWeapon, part), self, target, receiver);
      }
      return;
    }

    int finalDmg = resolution.finalDamage();

    // 將結算後傷害送給 target
    target.onDamage(finalDmg, self.getId());

    // 破招反擊 (Riposte) 傷害結算：防守方成功招架後反刺攻擊方
    if (resolution.riposteTriggered() && resolution.riposteDamage() > 0) {
      self.onDamage(resolution.riposteDamage(), target.getId());
      if (self.isDead()) {
        self.onDeath(target.getId());
      }
    }

    // 4. 技能附加之 Buff / Debuff 狀態施加
    if (skill != null && skill.template() != null && skill.template().getBuff() != null) {
      var buffCfg = skill.template().getBuff();
      if (buffCfg.type() == com.example.htmlmud.domain.model.enums.BuffType.DEBUFF) {
        var debuff = getBuffSettlementService().createActiveBuffFromConfig(buffCfg, target, self.getId(), skill.template().getId());
        if (debuff != null) {
          getBuffSettlementService().applyBuff(target, debuff);
        }
      } else if (buffCfg.type() == com.example.htmlmud.domain.model.enums.BuffType.BUFF) {
        var buff = getBuffSettlementService().createActiveBuffFromConfig(buffCfg, self, self.getId(), skill.template().getId());
        if (buff != null) {
          getBuffSettlementService().applyBuff(self, buff);
        }
      }
    }

    // 5. 攻擊後經驗與熟練度增長
    if (self instanceof Player attacker) {
      afterAttack(attacker, skill);
    }

    if (resolution.outcome() == com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome.CRIT) {
      msg += "\r\n" + resolution.combatLog();
    } else if (resolution.outcome() == com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome.PARRIED
        || resolution.outcome() == com.example.htmlmud.domain.dungeon.battle.DefenseResolver.DefenseOutcome.BLOCKED) {
      msg += "\r\n" + resolution.combatLog();
    } else {
      msg += "\r\n" + action.msg().hit();
      if (resolution.poiseBroken()) {
        msg += "\r\n\u001B[1;35m⚠️【架勢破防】" + target.getName() + " 精力枯竭架勢崩潰，破綻大開！受到額外 20% 傷害！\u001B[0m";
      }
    }

    msg = CombineString(msg, sWeapon, tWeapon, part);
    msg = msg.replace("$d", ColorText.damage(finalDmg));

    // 發送純淨無時間戳戰鬥訊息
    for (Player receiver : audiences) {
      MessageUtil.send(msg, self, target, receiver);
    }
  }



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------



  // ---------------------------------------------------------------------------------------------


  /**
   * 計算並設定下一次可攻擊的時間點。 引入約 ±20% 的隨機浮動，增加戰鬥節奏的自然感。
   */
  private void nextAttackTime(Living self) {
    long speed = self.getAttackSpeed();
    double variance = 0.8 + (ThreadLocalRandom.current().nextDouble() * 0.4);
    long delay = (long) (speed * variance);
    self.setNextAttackTime(System.currentTimeMillis() + delay);
  }

  private long calculateNextLevelXp(int level) {
    return (xpProgressionService != null)
        ? xpProgressionService.calculateNextLevelExp(level)
        : Math.max(180L, (long) Math.floor(60.0 * Math.pow(level, 1.6) + 120.0 * level));
  }

  // 取出隨機數值
  private int random(int min, int max) {
    return ThreadLocalRandom.current().nextInt(min, max + 1);
  }

  private String CombineString(String msg, String W, String w, String l) {
    return msg.replace("$l", l).replace("$W", W).replace("$w", w);
  }

  private void processSkillExperience(Player player, ActiveSkillResult result) {
    if (player == null || result == null) return;
    SkillEntry entry = result.entry();
    SkillTemplate template = result.template();
    if (entry == null || template == null) return;

    if (entry.getLevel() >= template.getMechanics().maxLevel()) {
      return;
    }

    // 增加經驗值 (智力越高練越快)
    long gain = 10 + (player.getStats().intelligence / 2);
    var res = (xpProgressionService != null)
        ? xpProgressionService.awardSkillExp(entry, template, gain)
        : (xpService != null ? xpService.awardSkillExp(entry, template, gain) : null);

    if (res != null && res.leveledUp()) {
      player.reply("你的 \u001B[33m" + template.getName() + "\u001B[0m 進步了！(等級 "
          + res.newLevel() + ")");
    }
  }

  private void afterAttack(Player attacker, ActiveSkillResult skillResult) {
    processSkillExperience(attacker, skillResult);
  }

}
