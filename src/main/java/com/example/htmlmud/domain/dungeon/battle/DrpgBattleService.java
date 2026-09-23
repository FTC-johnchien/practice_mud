package com.example.htmlmud.domain.dungeon.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.dungeon.dto.BattleEnemyViewDto;
import com.example.htmlmud.domain.dungeon.dto.BattleViewDto;
import com.example.htmlmud.domain.dungeon.dto.DrpgStateDto;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.dungeon.service.DungeonNavigator;
import com.example.htmlmud.domain.model.template.MobTemplate;
import com.example.htmlmud.domain.model.vo.CharacterId;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyItemSlot;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.PartyMemberSkill;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.party.model.RowPosition;
import com.example.htmlmud.domain.party.service.PartyService;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.CharacterSyncService;
import com.example.htmlmud.domain.service.SkillBridgeService;
import com.example.htmlmud.domain.service.TemplateCatalog;
import com.example.htmlmud.domain.service.XpProgressionService;
import lombok.extern.slf4j.Slf4j;

/**
 * DRPG 戰鬥系統門面協調服務 (Facade Coordinator)。
 * 負責管理活躍戰鬥會話、接收外部與指令操作，並協同 CombatLoop、TacticsService、RewardService 與 SkillBridge。
 */
@Slf4j
@Service
public class DrpgBattleService {

  private final PartyService partyService;
  private final DungeonManager dungeonManager;
  private final DungeonNavigator dungeonNavigator;
  private final TemplateReader templateReader;

  private final DrpgCombatLoop combatLoop;
  private final DrpgEnemyTacticsService tacticsService;
  private final DrpgRewardService rewardService;
  private final SkillBridgeService skillBridgeService;
  private final ComboResolver comboResolver;

  private final Map<String, BattleContext> activeBattles = new ConcurrentHashMap<>();
  private Consumer<Player> stateBroadcaster;

  private static final List<String> TOMB_ITEMS = List.of(
      "taiyin_pill", "purify_talisman", "bronze_sword", "yin_robe", "ancient_relic", "tomb_key"
  );

  @Autowired
  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator, TemplateReader templateReader,
      DrpgCombatLoop combatLoop, DrpgEnemyTacticsService tacticsService,
      DrpgRewardService rewardService, SkillBridgeService skillBridgeService,
      ComboResolver comboResolver) {
    this.partyService = partyService;
    this.dungeonManager = dungeonManager;
    this.dungeonNavigator = dungeonNavigator != null ? dungeonNavigator : new DungeonNavigator();
    this.templateReader = templateReader != null ? templateReader : new TemplateCatalog();
    this.tacticsService = tacticsService != null ? tacticsService : new DrpgEnemyTacticsService(this.templateReader);
    this.rewardService = rewardService != null ? rewardService : new DrpgRewardService(this.templateReader, this.dungeonManager);
    this.combatLoop = combatLoop != null ? combatLoop : new DrpgCombatLoop(this.tacticsService, this.rewardService);
    this.skillBridgeService = skillBridgeService != null ? skillBridgeService : new SkillBridgeService(this.templateReader);
    this.comboResolver = comboResolver != null ? comboResolver : new ComboResolver();
  }

  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator, TemplateReader templateReader,
      DrpgCombatLoop combatLoop, DrpgEnemyTacticsService tacticsService,
      DrpgRewardService rewardService, SkillBridgeService skillBridgeService) {
    this(partyService, dungeonManager, dungeonNavigator, templateReader, combatLoop, tacticsService, rewardService, skillBridgeService, null);
  }

  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator, TemplateReader templateReader) {
    this(partyService, dungeonManager, dungeonNavigator, templateReader, null, null, null, null, null);
  }

  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager) {
    this(partyService, dungeonManager, new DungeonNavigator(), new TemplateCatalog());
  }

  public DrpgBattleService(PartyService partyService, DungeonManager dungeonManager,
      DungeonNavigator dungeonNavigator) {
    this(partyService, dungeonManager, dungeonNavigator, new TemplateCatalog());
  }

  public void setStateBroadcaster(Consumer<Player> stateBroadcaster) {
    this.stateBroadcaster = stateBroadcaster;
  }

  public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
    if (this.rewardService != null) {
      this.rewardService.setEventPublisher(eventPublisher);
    }
  }

  public XpProgressionService getXpProgressionService() {
    return this.rewardService != null ? this.rewardService.getXpProgressionService() : new XpProgressionService();
  }

  public void setXpProgressionService(XpProgressionService xpProgressionService) {
    if (this.rewardService != null) {
      this.rewardService.setXpProgressionService(xpProgressionService);
    }
  }

  public CharacterSyncService getCharacterSyncService() {
    return this.rewardService != null ? this.rewardService.getCharacterSyncService() : new CharacterSyncService();
  }

  public void setCharacterSyncService(CharacterSyncService characterSyncService) {
    if (this.rewardService != null) {
      this.rewardService.setCharacterSyncService(characterSyncService);
    }
  }

  public SkillBridgeService getSkillBridgeService() {
    return this.skillBridgeService;
  }

  public DrpgCombatLoop getCombatLoop() {
    return this.combatLoop;
  }

  public DrpgEnemyTacticsService getTacticsService() {
    return this.tacticsService;
  }

  public DrpgRewardService getRewardService() {
    return this.rewardService;
  }

  public boolean isInBattle(String playerId) {
    BattleContext ctx = activeBattles.get(playerId);
    return ctx != null && !ctx.isOver();
  }

  public boolean isInBattle(CharacterId characterId) {
    return characterId != null && isInBattle(characterId.value());
  }

  public boolean isInBattle(Player player) {
    if (player == null) return false;
    return isInBattle(player.getId()) || (player.getName() != null && isInBattle(player.getName()));
  }

  public BattleContext getBattle(String playerId) {
    return activeBattles.get(playerId);
  }

  public BattleContext getBattle(CharacterId characterId) {
    return characterId != null ? getBattle(characterId.value()) : null;
  }

  public BattleContext getBattle(Player player) {
    if (player == null) return null;
    BattleContext ctx = activeBattles.get(player.getId());
    if (ctx == null && player.getName() != null) {
      ctx = activeBattles.get(player.getName());
    }
    return ctx;
  }

  public BattleContext getActiveBattle(String playerName) {
    return activeBattles.get(playerName);
  }

  public void startTrainingBattle(Player player, DungeonPosition pos) {
    if (isInBattle(player)) {
      player.reply("【試道場】當前已在戰鬥之中！");
      return;
    }
    Party party = partyService.getOrCreateParty(player);

    List<BattleEnemy> enemies = new ArrayList<>();
    // 前排玄鐵試道傀儡（攻極低 1~2，血厚 4000，防禦適中 6）
    BattleEnemy dummyFront = BattleEnemy.builder()
        .id("mob-dummy-1")
        .templateId("taiyin_tomb:training_dummy")
        .name("玄鐵試道傀儡・乾")
        .hp(4000)
        .maxHp(4000)
        .minDamage(1)
        .maxDamage(2)
        .defense(6)
        .dex(8)
        .row(RowPosition.FRONT)
        .attackIntervalMs(2500)
        .nextAttackTime(System.currentTimeMillis() + 1500)
        .alive(true)
        .xp(100)
        .dropItemId("ancient_relic")
        .build();
    enemies.add(dummyFront);

    // 後排避塵試法陣樁（血厚 3000，方便測試後排/AOE群攻技能）
    BattleEnemy dummyBack = BattleEnemy.builder()
        .id("mob-dummy-2")
        .templateId("taiyin_tomb:training_dummy_back")
        .name("避塵試法陣樁・坤")
        .hp(3000)
        .maxHp(3000)
        .minDamage(1)
        .maxDamage(1)
        .defense(4)
        .dex(5)
        .row(RowPosition.BACK)
        .attackIntervalMs(3000)
        .nextAttackTime(System.currentTimeMillis() + 2000)
        .alive(true)
        .xp(100)
        .dropItemId("purify_talisman")
        .build();
    enemies.add(dummyBack);

    BattleContext ctx = BattleContext.builder()
        .battleId("training-" + player.getName() + "-" + System.currentTimeMillis())
        .playerId(player.getName())
        .party(party)
        .enemies(enemies)
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    long now = System.currentTimeMillis();
    for (PartyMember m : party.getMembers()) {
      m.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(600, 1600));
    }

    broadcastLog(player, ctx, "\n\u001B[1;36m🎯【試道演武場開啟】太陰玄鐵試道傀儡已就位（血厚攻低）！請盡情測試隊員絕學與陣法奧義！\u001B[0m");
    activeBattles.put(player.getName(), ctx);
    pushDrpgState(player, pos, ctx);

    combatLoop.startBattleLoop(player, ctx, pos, activeBattles, p -> pushDrpgState(p, pos, ctx));
  }

  public void chargeCombatResources(Player player, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    // 陣法靈威補滿 100
    party.setFormationEnergy(100);
    // 各隊員戰意與真元補滿
    for (PartyMember m : party.getMembers()) {
      if (m.isAlive()) {
        m.setCurrentRage(m.getMaxRage());
        m.setCurrentCombo(m.getMaxCombo());
        if (m.getStats() != null) {
          m.getStats().setMp(m.getStats().getMaxMp());
        }
      }
    }
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx != null) {
      broadcastLog(player, ctx, "\u001B[1;33m⚡【靈威充能完畢】陣法靈威達 100 點（大招就緒）！全員怒氣/連擊點/真元全滿！\u001B[0m");
      pushDrpgState(player, pos, ctx);
    } else {
      player.reply("⚡【充能完畢】陣法靈威已充至 100 點！全員戰鬥能量充盈！");
      pushDrpgState(player, pos, null);
    }
  }

  public void startBattle(Player player, DungeonPosition pos, List<String> mobTemplateIds) {
    Party party = partyService.getOrCreateParty(player.getName());

    List<BattleEnemy> enemies = new ArrayList<>();
    int idx = 0;
    for (String templateId : mobTemplateIds) {
      idx++;
      var opt = templateReader.findMob(templateId);
      String name = opt.map(MobTemplate::name).orElse("太陰妖邪") + " " + (char) ('A' + idx - 1);
      int maxHp = opt.map(MobTemplate::maxHp).filter(h -> h > 0).orElse(220 + (idx * 60));
      int minDmg = opt.map(MobTemplate::minDamage).filter(d -> d > 0).orElse(10 + idx * 2);
      int maxDmg = opt.map(MobTemplate::maxDamage).filter(d -> d > 0).orElse(18 + idx * 3);
      int def = opt.map(MobTemplate::defense).filter(d -> d > 0).orElse(4 + idx);
      int dex = opt.map(MobTemplate::dex).filter(d -> d > 0).orElse(10);
      long attackInterval = opt.map(MobTemplate::attackSpeed).filter(s -> s > 0).orElse(2200);

      String dropId = opt.map(MobTemplate::loot)
          .filter(l -> l != null && !l.isEmpty())
          .map(l -> l.get(ThreadLocalRandom.current().nextInt(l.size())).itemId())
          .orElse(null);
      if (dropId == null) {
        if (templateId.contains("mozhu")) {
          dropId = "mozhu_mines:corrupted_spirit_stone";
        } else {
          dropId = TOMB_ITEMS.get(ThreadLocalRandom.current().nextInt(TOMB_ITEMS.size()));
        }
      }

      RowPosition row = (idx <= 2) ? RowPosition.FRONT : RowPosition.BACK;
      BattleEnemy enemy;
      if (opt.isPresent()) {
        enemy = BattleEnemy.fromTemplate("mob-" + idx, opt.get(), row, dropId, templateReader);
      } else {
        enemy = BattleEnemy.builder()
            .id("mob-" + idx)
            .templateId(templateId)
            .name(name)
            .hp(maxHp)
            .maxHp(maxHp)
            .minDamage(minDmg)
            .maxDamage(maxDmg)
            .defense(def)
            .dex(dex)
            .row(row)
            .attackIntervalMs(attackInterval)
            .nextAttackTime(System.currentTimeMillis() + ThreadLocalRandom.current().nextLong(1200, 2200))
            .alive(true)
            .xp(35 + idx * 10)
            .dropItemId(dropId)
            .build();
      }
      enemies.add(enemy);
    }

    BattleContext ctx = BattleContext.builder()
        .battleId("battle-" + player.getName() + "-" + System.currentTimeMillis())
        .playerId(player.getName())
        .party(party)
        .enemies(enemies)
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    // 隊員首次攻擊隨機錯開，製造錯落交鋒節奏，並重置初始仇恨
    long now = System.currentTimeMillis();
    for (PartyMember m : party.getMembers()) {
      m.resetThreat();
      m.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(600, 1600));
    }

    broadcastLog(player, ctx, "\n\u001B[1;31m⚔️【遭遇妖邪】墓道深處殺氣逼近，妖邪敵群列陣襲來！請迎戰！\u001B[0m");
    activeBattles.put(player.getName(), ctx);

    pushDrpgState(player, pos, ctx);

    combatLoop.startBattleLoop(player, ctx, pos, activeBattles, p -> pushDrpgState(p, pos, ctx));
  }

  public void startBossBattle(Player player, DungeonPosition pos, String bossTemplateId) {
    if (isInBattle(player.getName())) {
      player.reply("【首領之戰】當前已在戰鬥交鋒之中！");
      return;
    }
    Party party = partyService.getOrCreateParty(player.getName());
    var opt = templateReader.findMob(bossTemplateId);
    String name = opt.map(MobTemplate::name).orElse("煞氣領主");
    int hp = opt.map(MobTemplate::maxHp).filter(h -> h > 0).orElse(450);
    int minDmg = opt.map(MobTemplate::minDamage).filter(d -> d > 0).orElse(20);
    int maxDmg = opt.map(MobTemplate::maxDamage).filter(d -> d > 0).orElse(35);
    int def = opt.map(MobTemplate::defense).filter(d -> d > 0).orElse(10);
    long atkSpeed = opt.map(MobTemplate::attackSpeed).filter(s -> s > 0).orElse(2000);

    List<BattleEnemy> enemies = new ArrayList<>();
    BattleEnemy boss = BattleEnemy.builder()
        .id("boss-1")
        .templateId(bossTemplateId)
        .name("👑 " + name)
        .hp(hp)
        .maxHp(hp)
        .minDamage(minDmg)
        .maxDamage(maxDmg)
        .defense(def)
        .dex(12)
        .row(RowPosition.FRONT)
        .attackIntervalMs(atkSpeed)
        .nextAttackTime(System.currentTimeMillis() + 1500)
        .alive(true)
        .xp(500)
        .dropItemId("mozhu_mines:black_obsidian_scythe")
        .build();
    enemies.add(boss);

    BattleContext ctx = BattleContext.builder()
        .battleId("boss-" + player.getName() + "-" + System.currentTimeMillis())
        .playerId(player.getName())
        .party(party)
        .enemies(enemies)
        .state(BattleState.FIGHTING)
        .selectedTargetIndex(0)
        .build();

    long now = System.currentTimeMillis();
    for (PartyMember m : party.getMembers()) {
      m.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(500, 1500));
    }

    broadcastLog(player, ctx, "\n\u001B[1;31m🔥🔥🔥【煞氣首領降臨】血肉白骨祭壇劇烈震顫，" + name + " 發出癲狂嘶吼，壓迫感撲面而來！請拔劍迎戰！🔥🔥🔥\u001B[0m");
    activeBattles.put(player.getName(), ctx);
    pushDrpgState(player, pos, ctx);

    combatLoop.startBattleLoop(player, ctx, pos, activeBattles, p -> pushDrpgState(p, pos, ctx));
  }

  public void fight(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("【戰鬥】四周暫無妖邪現身，請移步探索。");
      return;
    }
    BattleEnemy target = ctx.getTargetEnemy();
    if (target != null && target.isAlive()) {
      broadcastLog(player, ctx, "\u001B[1;31m⚔️【迎戰】全隊拔劍出鞘，陣型收束，集中火力直攻【" + target.getName() + "】！\u001B[0m");
      long now = System.currentTimeMillis();
      for (PartyMember member : ctx.getParty().getMembers()) {
        if (member.isAlive() && member.getNextAttackTime() > now + 300) {
          member.setNextAttackTime(now + ThreadLocalRandom.current().nextLong(50, 250));
        }
      }
    }
    pushDrpgState(player, pos, ctx);
  }

  public void castSkill(Player player, int memberIdx, String skillId, int targetIdx, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前並未處於戰鬥之中！");
      return;
    }

    if (memberIdx < 0 || memberIdx >= ctx.getParty().getMembers().size()) {
      player.reply("無效的隊員編號！");
      return;
    }

    PartyMember member = ctx.getParty().getMembers().get(memberIdx);
    if (!member.isAlive()) {
      player.reply(member.getName() + " 已經倒下，無法施展技能！");
      return;
    }

    PartyMemberSkill skill = member.getSkills().stream()
        .filter(s -> s.getId().equalsIgnoreCase(skillId))
        .findFirst().orElse(null);

    if (skill == null) {
      player.reply(member.getName() + " 並未掌握該項技能！");
      return;
    }

    if (!member.isSkillUsable(skill)) {
      player.reply("【武器不符】「" + skill.getName() + "」需要裝備 " + String.join("/", skill.getAllowedWeapons()) + "，當前手持武器無法施展！");
      return;
    }

    if (member.isOnCooldown(skill.getId())) {
      long remainSec = (member.getRemainingCooldownMs(skill.getId()) / 1000) + 1;
      player.reply("招式冷卻中，尚需 " + remainSec + " 秒！");
      return;
    }

    // 資源扣除檢驗
    if (!tacticsService.consumeSkillResource(member, skill)) {
      if (skill.getCostType() == CombatResourceType.MP) {
        player.reply("真元不足！需要 " + skill.getCostValue() + " MP！");
      } else if (skill.getCostType() == CombatResourceType.RAGE) {
        player.reply("怒氣未滿！需要 " + skill.getCostValue() + " 點怒氣！");
      } else if (skill.getCostType() == CombatResourceType.COMBO) {
        player.reply("連擊點不足！需要 " + skill.getCostValue() + " 層連擊！");
      } else if (skill.getCostType() == CombatResourceType.SP) {
        player.reply("戰氣不足！需要 " + skill.getCostValue() + " 點戰氣！");
      } else {
        player.reply("資源不足，無法施展招式！");
      }
      return;
    }

    combatLoop.applySkillEffects(player, ctx, member, skill, targetIdx, "");
    pushDrpgState(player, pos, ctx);
  }

  public void castPartyUltimate(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前未在戰鬥中！");
      return;
    }
    Party party = ctx.getParty();
    if (!party.canCastUltimate()) {
      player.reply("陣法靈威不足 100，無法施展陣法奧義！");
      return;
    }

    party.setFormationEnergy(0);
    String formId = party.getEquippedFormation() != null ? party.getEquippedFormation().getId() : "";

    if ("formation_xuan_yin".equals(formId)) {
      for (PartyMember m : party.getMembers()) {
        m.consumeSan(8);
      }
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.takeDamage(240);
        }
      }
      broadcastLog(player, ctx, "\u001B[1;35m⚡⚡【陣法奧義・不可名狀星蝕】虛空被撕裂成深邃盲目之眼！狂亂囈語灌入心神 (-8 SAN)！全體敵方遭受 240 點暗蝕滅頂之災！\u001B[0m");
    } else {
      for (PartyMember m : party.getMembers()) {
        if (m.isAlive()) {
          m.heal(50);
          m.restoreSan(15);
        }
      }
      for (BattleEnemy e : ctx.getEnemies()) {
        if (e.isAlive()) {
          e.takeDamage(120);
        }
      }
      broadcastLog(player, ctx, "\u001B[1;33m⚡⚡【陣法奧義・四象辟邪聖光】青龍白虎朱雀玄武四聖法相齊現！聖光普照全員氣血大盛 (+50 HP, +15 SAN)，敵方遭受 120 點灼魂傷害！\u001B[0m");
    }

    if (ctx.isAllEnemiesDead()) {
      ctx.setState(BattleState.VICTORY);
    }
    pushDrpgState(player, pos, ctx);
  }

  public void castComboSkill(Player player, String comboSkillId, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前未在戰鬥中！");
      return;
    }
    PartyMemberSkill comboSkill = templateReader.findPartySkill(comboSkillId).orElse(null);
    if (comboSkill == null) {
      player.reply("找不到指定的合擊絕技：【" + comboSkillId + "】！");
      return;
    }

    boolean success = comboResolver.executeCombo(player, ctx, comboSkill, (p, msg) -> broadcastLog(p, ctx, msg));
    if (!success) {
      player.reply("【小隊合擊】當前條件不滿足（需特定職業全員存活、無失控且具備足夠戰氣/法力/陣法靈威）！");
      return;
    }

    pushDrpgState(player, pos, ctx);
  }

  public void selectTarget(Player player, int targetIdx, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx != null && targetIdx >= 0 && targetIdx < ctx.getEnemies().size()) {
      ctx.setSelectedTargetIndex(targetIdx);
      BattleEnemy e = ctx.getEnemies().get(targetIdx);
      broadcastLog(player, ctx, "🎯 全隊鎖定集火目標：【" + e.getName() + "】！");
      pushDrpgState(player, pos, ctx);
    }
  }

  public void flee(Player player, DungeonPosition pos) {
    BattleContext ctx = activeBattles.get(player.getName());
    if (ctx == null || ctx.isOver()) {
      player.reply("當前並未在戰鬥中！");
      return;
    }

    if (ThreadLocalRandom.current().nextInt(100) < 75) {
      for (PartyMember m : ctx.getParty().getMembers()) {
        m.consumeSan(5);
      }
      ctx.setState(BattleState.FLED);
      broadcastLog(player, ctx, "\u001B[1;33m🏃【遁地金光】小隊捏碎遁符，借陰風遁出百丈！代價道心微顫 (-5 SAN)！\u001B[0m");
      rewardService.postBattleCleanup(player, ctx);
      activeBattles.remove(player.getName());
      pushDrpgState(player, pos, null);
      player.reply("你成功擺脫了妖邪的追擊！");
    } else {
      broadcastLog(player, ctx, "\u001B[1;31m⚠️ 逃跑失敗！妖邪陰氣封鎖了退路！\u001B[0m");
      pushDrpgState(player, pos, ctx);
    }
  }

  public PartyMember selectPartyTarget(BattleContext ctx) {
    return tacticsService.selectPartyTarget(ctx);
  }

  public boolean tryTriggerCompanionTactics(Player player, BattleContext ctx, PartyMember member, DungeonPosition pos) {
    return combatLoop.tryTriggerCompanionTactics(player, ctx, member, pos);
  }

  public void resolveDefeat(Player player, BattleContext ctx, DungeonPosition pos) {
    rewardService.resolveDefeat(player, ctx, pos, activeBattles, p -> pushDrpgState(p, pos, null));
  }

  public void resolveVictory(Player player, BattleContext ctx, DungeonPosition pos) {
    rewardService.resolveVictory(player, ctx, pos, activeBattles, p -> pushDrpgState(p, pos, null));
  }

  public void sealTarget(Player player, int targetIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    // 1. 戰鬥中可封印敵方古神畸變體
    if (ctx != null && !ctx.isOver() && targetIdx >= 0 && targetIdx < ctx.getEnemies().size()) {
      BattleEnemy e = ctx.getEnemies().get(targetIdx);
      if (e.isAlive() && e.getId().startsWith("aberration-")) {
        e.applyStun(99999999L);
        broadcastLog(player, ctx, "\u001B[1;34m🔒【太上鎮魔大篆】天師金符凌空鎮壓！【" + e.getName() + "】全身畸變肉瘤被死死封禁，失去所有反抗與行動能力！\u001B[0m");
        pushDrpgState(player, pos, ctx);
        return;
      }
    }

    // 2. 封印走火入魔的我方隊友
    if (targetIdx >= 0 && targetIdx < party.getMembers().size()) {
      PartyMember m = party.getMembers().get(targetIdx);
      if (m.getName().equals(player.getName())) {
        player.reply("【鎮魔】身為隊長，道心受浩然天罡守護，無需對自己施展封印術！");
        return;
      }
      if (m.getMadnessState() == PartyMember.MadnessState.CHAOS) {
        m.setMadnessState(PartyMember.MadnessState.SEALED);
        broadcastLog(player, ctx, "\u001B[1;34m🔒【太上鎮魔符】施展封印秘術！" + m.getName() + " 眉心貼上鎖神靈符，異變計數器凍結在 " + m.getAberrationCounter() + "%！暫時失去所有行動能力，直到戰鬥結束方可解封！\u001B[0m");
        pushDrpgState(player, pos, ctx);
        return;
      } else if (m.getMadnessState() == PartyMember.MadnessState.SEALED) {
        player.reply(m.getName() + " 當前已處於封印鎮魔狀態！");
        return;
      } else {
        player.reply(m.getName() + " 並未走火入魔，無須施展鎮魔封印。");
        return;
      }
    }

    player.reply("無效的封印目標編號！可封印走火入魔的隊友 (0~5)，或戰場上的深淵畸變體！");
  }

  public void useItem(Player player, String slotIdOrIndex, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String realSlotId = resolveSlotId(party, slotIdOrIndex);
    if (realSlotId == null) {
      player.reply("行囊中找不到該物品！");
      return;
    }

    String result = party.useItemOnMember(realSlotId, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  public void equipItem(Player player, String slotIdOrIndex, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String realSlotId = resolveSlotId(party, slotIdOrIndex);
    if (realSlotId == null) {
      player.reply("行囊中找不到該物品！");
      return;
    }

    String result = party.equipItemOnMember(realSlotId, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  public void unequipItem(Player player, String slotType, int memberIdx, DungeonPosition pos) {
    Party party = partyService.getOrCreateParty(player.getName());
    BattleContext ctx = activeBattles.get(player.getName());

    String result = party.unequipItemFromMember(slotType, memberIdx);
    broadcastLog(player, ctx, result);
    pushDrpgState(player, pos, ctx);
  }

  private String resolveSlotId(Party party, String slotIdOrIndex) {
    if (slotIdOrIndex == null || party.getInventory() == null) return null;
    if (party.getInventory().getItem(slotIdOrIndex) != null) {
      return slotIdOrIndex;
    }
    try {
      int idx = Integer.parseInt(slotIdOrIndex);
      if (idx >= 1 && idx <= party.getInventory().getSlots().size()) {
        return party.getInventory().getSlots().get(idx - 1).getSlotId();
      } else if (idx >= 0 && idx < party.getInventory().getSlots().size()) {
        return party.getInventory().getSlots().get(idx).getSlotId();
      }
    } catch (NumberFormatException ignored) {}

    for (PartyItemSlot s : party.getInventory().getSlots()) {
      if (s.getItemId().equalsIgnoreCase(slotIdOrIndex) || s.getName().contains(slotIdOrIndex)) {
        return s.getSlotId();
      }
    }
    return null;
  }

  private void broadcastLog(Player player, BattleContext ctx, String log) {
    if (ctx != null) {
      ctx.addLog(log);
    }
    if (player != null && player.isValid()) {
      player.reply(log);
    }
  }

  public BattleViewDto createBattleView(String playerName) {
    BattleContext battleCtx = activeBattles.get(playerName);
    if (battleCtx == null || battleCtx.isOver()) {
      return null;
    }
    List<BattleEnemyViewDto> enemyDtos = new ArrayList<>();
    for (int i = 0; i < battleCtx.getEnemies().size(); i++) {
      BattleEnemy e = battleCtx.getEnemies().get(i);
      enemyDtos.add(new BattleEnemyViewDto(
          i,
          e.getId(),
          e.getName(),
          e.getHp(),
          e.getMaxHp(),
          e.getRow().name(),
          e.isAlive(),
          i == battleCtx.getSelectedTargetIndex(),
          e.isStunned()
      ));
    }
    return new BattleViewDto(
        true,
        battleCtx.getState().name(),
        enemyDtos,
        battleCtx.getSelectedTargetIndex(),
        battleCtx.getRecentLogs()
    );
  }

  public void pushDrpgState(Player player, DungeonPosition pos, BattleContext battleCtx) {
    if (player == null) return;
    if (stateBroadcaster != null) {
      stateBroadcaster.accept(player);
      return;
    }
    if (pos == null) {
      pos = dungeonManager.getPlayerPosition(player.getName());
    }
    String floorId = pos != null ? pos.getFloorId() : "mozhu_mines_b1f";
    DungeonFloor floor = dungeonManager != null ? dungeonManager.getFloor(floorId) : null;
    Party party = partyService.getOrCreateParty(player.getName());
    String inspect = (floor != null && pos != null && dungeonNavigator != null) ? dungeonNavigator.inspectForward(floor, pos) : "";

    BattleViewDto battleView = createBattleView(player.getName());
    DrpgStateDto dto = DrpgStateDto.of(floor, pos, inspect, party, battleView, templateReader);
    player.sendJson(dto);
  }
}