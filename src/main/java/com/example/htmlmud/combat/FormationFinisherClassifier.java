package com.example.htmlmud.combat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.example.htmlmud.party.Party;
import com.example.htmlmud.party.PartyMember;

/**
 * 15 大動態小隊終結技分類器 (Formation Finisher Classifier)
 * 依據小隊存活隊員之四大歷史道統門派 (CULTIVATOR / ENGINEER / HIGH_TECH / CTHULHU)
 * 動態分類解算為《SYNERGY_AND_COMBOS.md》規範之 15 種終結絕技，達成 100% 陣容窮盡覆蓋。
 */
public final class FormationFinisherClassifier {

  // --- 1. 四大純粹極限型 (單一體系 >= 4 人) ---
  public static final FormationFinisherSpec PURE_CULTIVATOR = new FormationFinisherSpec(
      "finisher_pure_cultivator",
      "【萬劍歸宗·九天太乙誅仙劍陣】",
      "Plasma Super-Conductor Cascade: 漫天金色飛劍傾瀉，九大元素爆發，全隊 MP 回滿且技能零真元消耗。",
      "HOLY",
      0.5,
      20,
      20,
      10,
      "萬千飛劍撕裂虛空如暴雨傾瀉，太乙清聖天威洗滌全屏！"
  );

  public static final FormationFinisherSpec PURE_ENGINEER = new FormationFinisherSpec(
      "finisher_pure_engineer",
      "【大地沉淪·太古神山崩天闕】",
      "Planetary Core Gravity Singularity: 五座巨碑拔起引發黑洞坍縮，敵怪全體行動條鎖死且 Poise 韌性清零。",
      "EARTH",
      0.3,
      50,
      0,
      5,
      "太古重峰地脈定錨轟然拔起，地心重力坍縮將全場敵怪鎮壓粉碎！"
  );

  public static final FormationFinisherSpec PURE_HIGH_TECH = new FormationFinisherSpec(
      "finisher_pure_high_tech",
      "【天衍極限·千星神念天網劫】",
      "Mil-Spec Grid Overclock: 10000 FLOPS: 五大晶片超頻，真·時停 3 秒！無數激光切面打出 100% 暴擊真實傷害。",
      "LIGHTNING",
      0.6,
      30,
      0,
      5,
      "天衍算力過載轟鳴，主觀時鐘頻率定格，千星雷射切面縱橫滅世！"
  );

  public static final FormationFinisherSpec PURE_CTHULHU = new FormationFinisherSpec(
      "finisher_pure_cthulhu",
      "【森羅萬象·黑山羊母神血宴】",
      "Biological Prime: Macro-Cellular Eruption: 地面撕裂爆發千丈血肉觸手引爆全屏 DoT，傷害 100% 轉為治療。",
      "DARK",
      0.4,
      20,
      50,
      15,
      "地底深井卵鞘破土狂舞，無數漆黑血肉觸手化為暴食之宴吞噬敵怪！"
  );

  // --- 2. 萬象混元型 (4 大體系均衡各 1 人 + 主角萬象) ---
  public static final FormationFinisherSpec ALL_UNIVERSAL = new FormationFinisherSpec(
      "finisher_all_universal",
      "【大衍混元·諸天萬象開天闢地神劫】",
      "Protocol Genesis: Planetary Cold Reformat: 全維度無死角轟殺：100% 削韌破防、全元素擴散、唱條打斷、裂魂引爆且全隊回滿！",
      "CHAOS",
      1.0,
      60,
      50,
      0,
      "仙法、神機、天衍、古神四大天道交融共鳴，天地開闢混沌初分，降下終極滅度神劫！"
  );

  // --- 3. 六大雙曜交織型 ---
  public static final FormationFinisherSpec XIAN_MO = new FormationFinisherSpec(
      "finisher_xian_mo",
      "【太極血海·逆亂生死神魔印】",
      "Taiji Blood Abyss: 清聖太極劍氣破除護甲，九幽血煞海潮吞噬氣血，斬殺敵怪並為全隊殘血隊友大急救。",
      "CHAOS",
      0.5,
      30,
      40,
      10,
      "太極陰陽圖倒映九幽血海，生死逆亂神魔同誅，殘血隊友氣血逆流大回穩！"
  );

  public static final FormationFinisherSpec CYBER_ENGINEER = new FormationFinisherSpec(
      "finisher_cyber_engineer",
      "【天基軌道·萬鈞乾坤神機炮】",
      "Orbital Kinetic Strike: 工程師地脈定錨引力鎖死，巨企天基衛星發射超音速鎢棒直擊，全屏物理真實巨額粉碎。",
      "PHYSICAL",
      0.6,
      60,
      0,
      0,
      "地脈磁力定錨鎖死座標，外層空間軌道天基鎢棒超音速轟天墜落，全場粉碎！"
  );

  public static final FormationFinisherSpec XIAN_CYBER = new FormationFinisherSpec(
      "finisher_xian_cyber",
      "【九天星網·神念超頻萬劍矩陣】",
      "Cyber-Celestial Grid: 晶片高速演算敵怪弱點，飛劍經由電磁超導折射自動鎖定弱點，100% 暴擊破防。",
      "LIGHTNING",
      0.5,
      35,
      15,
      10,
      "天衍晶片光纖牽引九天飛劍，超導電磁折射以光速穿心封穴！"
  );

  public static final FormationFinisherSpec XIAN_ENGINEER = new FormationFinisherSpec(
      "finisher_xian_engineer",
      "【天工造化·九嶽神山崩天闕】",
      "Prime Genesis: 五行符文與深井液壓重構共振，地底拔起九座太古巨嶽，強制眩暈全場敵怪。",
      "EARTH",
      0.4,
      45,
      20,
      10,
      "天工機變引動五行神山，地火噴湧神嶽壓頂，敵怪罡氣崩散陷入重度昏厥！"
  );

  public static final FormationFinisherSpec CTHULHU_CYBER = new FormationFinisherSpec(
      "finisher_cthulhu_cyber",
      "【生化天羅·黑煞奈米噬魂木馬】",
      "Bio-Synthetic Trojan: 軍規病毒注入黑山羊母體細胞，敵怪全體陷入系統癱瘓，前搖 100% 打斷並持續腐蝕扣血。",
      "POISON",
      0.5,
      35,
      25,
      5,
      "外神母體生化代碼攜奈米病毒入侵敵方神經總線，全身器官反噬爆裂！"
  );

  public static final FormationFinisherSpec CTHULHU_ENGINEER = new FormationFinisherSpec(
      "finisher_cthulhu_engineer",
      "【深淵巨擘·太古血肉魔導重機】",
      "Abyssal Harvester: 重型採礦設備與活性魔血融合，召喚巨型血肉液壓泰坦一拳震碎戰場，全隊霸體護甲翻倍。",
      "BLUNT",
      0.5,
      40,
      30,
      5,
      "血肉巨臂同調液壓採礦重機，地心轟鳴一拳碎裂地脈，鋼筋鐵骨魔神降臨！"
  );

  // --- 4. 四大三才鼎立型 ---
  public static final FormationFinisherSpec TRIO_CULTIVATOR_CTHULHU_CYBER = new FormationFinisherSpec(
      "finisher_trio_cultivator_cthulhu_cyber",
      "【偷天換日·生化天道逆亂真訣】",
      "Tri-Matrix Override: 瞬間淨化全隊負面並 200% 反噬敵怪，飛劍附帶血肉撕裂與時序抽幀，全場敵怪混亂。",
      "CHAOS",
      0.6,
      35,
      30,
      10,
      "仙道、血肉與神念三才逆亂，反噬敵方命數，全屏撕裂重創！"
  );

  public static final FormationFinisherSpec TRIO_CULTIVATOR_CTHULHU_ENGINEER = new FormationFinisherSpec(
      "finisher_trio_cultivator_cthulhu_engineer",
      "【冥河重嶽·混沌太虛開山印】",
      "Macro-Tectonic Void: 重力聚怪、黑山羊血海蔓延、五行神山壓頂三段式毀滅打擊，全隊吸血回滿。",
      "CHAOS",
      0.6,
      45,
      35,
      10,
      "冥河血海翻湧，太古重峰引力壓頂，三道古老真威合壁崩天裂地！"
  );

  public static final FormationFinisherSpec TRIO_CULTIVATOR_CYBER_ENGINEER = new FormationFinisherSpec(
      "finisher_trio_cultivator_cyber_engineer",
      "【天衍算籌·九天神雷星際裂變陣】",
      "Trio Prime: 引力黑洞鎖怪、協議 EMP 斷網清空蓄力條、九天紫電全屏轟頂破防，全場韌性清零。",
      "LIGHTNING",
      0.6,
      50,
      15,
      10,
      "神機定鼎、天衍斷脈、九天神雷浩蕩降世，全場敵怪罡氣屏障瞬間蒸發！"
  );

  public static final FormationFinisherSpec TRIO_CTHULHU_CYBER_ENGINEER = new FormationFinisherSpec(
      "finisher_trio_cthulhu_cyber_engineer",
      "【萬機歸淵·黑山羊奇異點格式化】",
      "Techno-Organic Purge: 深井開採口全開，黑山羊幼崽攜等離子過載炸彈席捲戰場，無視防禦造成巨額真實混沌傷害。",
      "CHAOS",
      0.7,
      45,
      20,
      10,
      "深井開採口徹底崩潰，黑山羊畸變核攜電漿炸彈撕裂一切物理常數！"
  );

  private static final Map<String, FormationFinisherSpec> REGISTRY = new HashMap<>();

  static {
    REGISTRY.put(PURE_CULTIVATOR.id(), PURE_CULTIVATOR);
    REGISTRY.put(PURE_ENGINEER.id(), PURE_ENGINEER);
    REGISTRY.put(PURE_HIGH_TECH.id(), PURE_HIGH_TECH);
    REGISTRY.put(PURE_CTHULHU.id(), PURE_CTHULHU);
    REGISTRY.put(ALL_UNIVERSAL.id(), ALL_UNIVERSAL);
    REGISTRY.put(XIAN_MO.id(), XIAN_MO);
    REGISTRY.put(CYBER_ENGINEER.id(), CYBER_ENGINEER);
    REGISTRY.put(XIAN_CYBER.id(), XIAN_CYBER);
    REGISTRY.put(XIAN_ENGINEER.id(), XIAN_ENGINEER);
    REGISTRY.put(CTHULHU_CYBER.id(), CTHULHU_CYBER);
    REGISTRY.put(CTHULHU_ENGINEER.id(), CTHULHU_ENGINEER);
    REGISTRY.put(TRIO_CULTIVATOR_CTHULHU_CYBER.id(), TRIO_CULTIVATOR_CTHULHU_CYBER);
    REGISTRY.put(TRIO_CULTIVATOR_CTHULHU_ENGINEER.id(), TRIO_CULTIVATOR_CTHULHU_ENGINEER);
    REGISTRY.put(TRIO_CULTIVATOR_CYBER_ENGINEER.id(), TRIO_CULTIVATOR_CYBER_ENGINEER);
    REGISTRY.put(TRIO_CTHULHU_CYBER_ENGINEER.id(), TRIO_CTHULHU_CYBER_ENGINEER);
  }

  private FormationFinisherClassifier() {}

  public static Map<String, FormationFinisherSpec> getAllSpecs() {
    return Collections.unmodifiableMap(REGISTRY);
  }

  /**
   * 根據小隊當前存活隊員之門派道統組合，動態判定 15 大天外終結技規格
   */
  public static FormationFinisherSpec classify(Party party) {
    if (party == null) return ALL_UNIVERSAL;
    List<PartyMember> alive = party.getAliveMembers();
    if (alive.isEmpty()) return ALL_UNIVERSAL;

    Set<String> distinctSchools = new HashSet<>();
    int universalCount = 0;

    for (PartyMember m : alive) {
      String school = normalizeSchool(m.getOriginSchool());
      if ("UNIVERSAL".equals(school)) {
        universalCount++;
      } else {
        distinctSchools.add(school);
      }
    }

    // 1. 若全隊均為 UNIVERSAL 或無明確門派，預設純仙道或萬象
    if (distinctSchools.isEmpty()) {
      return ALL_UNIVERSAL;
    }

    // 2. 單一體系 (全體具名成員相同，UNIVERSAL 適配該體系)
    if (distinctSchools.size() == 1) {
      String pureSchool = distinctSchools.iterator().next();
      return switch (pureSchool) {
        case "ENGINEER" -> PURE_ENGINEER;
        case "HIGH_TECH" -> PURE_HIGH_TECH;
        case "CTHULHU" -> PURE_CTHULHU;
        default -> PURE_CULTIVATOR;
      };
    }

    // 3. 四大體系俱全 (4 門派全在，或 3 門派 + UNIVERSAL 萬象陣眼補足第 4 門派)
    if (distinctSchools.size() == 4 || (distinctSchools.size() == 3 && universalCount > 0 && alive.size() >= 4)) {
      if (distinctSchools.size() == 4) {
        return ALL_UNIVERSAL;
      }
    }

    // 4. 雙曜交織型 (2 種體系)
    if (distinctSchools.size() == 2) {
      boolean hasCultivator = distinctSchools.contains("CULTIVATOR");
      boolean hasEngineer = distinctSchools.contains("ENGINEER");
      boolean hasHighTech = distinctSchools.contains("HIGH_TECH");
      boolean hasCthulhu = distinctSchools.contains("CTHULHU");

      if (hasCultivator && hasCthulhu) return XIAN_MO;
      if (hasHighTech && hasEngineer) return CYBER_ENGINEER;
      if (hasCultivator && hasHighTech) return XIAN_CYBER;
      if (hasCultivator && hasEngineer) return XIAN_ENGINEER;
      if (hasCthulhu && hasHighTech) return CTHULHU_CYBER;
      if (hasCthulhu && hasEngineer) return CTHULHU_ENGINEER;
    }

    // 5. 三才鼎立型 (3 種體系)
    if (distinctSchools.size() == 3) {
      boolean hasCultivator = distinctSchools.contains("CULTIVATOR");
      boolean hasEngineer = distinctSchools.contains("ENGINEER");
      boolean hasHighTech = distinctSchools.contains("HIGH_TECH");
      boolean hasCthulhu = distinctSchools.contains("CTHULHU");

      if (hasCultivator && hasCthulhu && hasHighTech) return TRIO_CULTIVATOR_CTHULHU_CYBER;
      if (hasCultivator && hasCthulhu && hasEngineer) return TRIO_CULTIVATOR_CTHULHU_ENGINEER;
      if (hasCultivator && hasHighTech && hasEngineer) return TRIO_CULTIVATOR_CYBER_ENGINEER;
      if (hasCthulhu && hasHighTech && hasEngineer) return TRIO_CTHULHU_CYBER_ENGINEER;
    }

    // 預設兜底
    return ALL_UNIVERSAL;
  }

  private static String normalizeSchool(String school) {
    if (school == null || school.isBlank()) return "UNIVERSAL";
    String upper = school.trim().toUpperCase();
    return switch (upper) {
      case "CULTIVATOR", "ENGINEER", "HIGH_TECH", "CTHULHU" -> upper;
      default -> "UNIVERSAL";
    };
  }
}
