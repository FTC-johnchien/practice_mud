package com.example.htmlmud.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.service.DungeonManager;
import com.example.htmlmud.domain.exception.DataValidationException;
import com.example.htmlmud.domain.model.config.LootEntry;
import com.example.htmlmud.domain.model.enums.EquipmentSlot;
import com.example.htmlmud.domain.model.enums.SkillCategory;
import com.example.htmlmud.template.model.CompanionTemplate;
import com.example.htmlmud.template.model.ItemTemplate;
import com.example.htmlmud.template.model.MobTemplate;
import com.example.htmlmud.template.model.RoomExit;
import com.example.htmlmud.template.model.RoomTemplate;
import com.example.htmlmud.template.model.ShopTemplate;
import com.example.htmlmud.template.model.SpawnRule;
import com.example.htmlmud.domain.party.model.FormationTemplate;
import com.example.htmlmud.domain.model.enums.RoleCategory;
import com.example.htmlmud.template.TemplateReader;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 集中式遊戲 JSON 啟動期拓撲校驗器 (VAL-01)
 * 於 Spring 容器就緒後執行全面 Fail-Fast 校驗，
 * 檢核全域物品、怪物掉落物、房間出口、商店商品、地牢座標與陣法依賴一致性。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class DataIntegrityValidator implements ApplicationRunner {

  private final TemplateReader templateReader;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private DungeonManager dungeonManager;

  @org.springframework.beans.factory.annotation.Autowired(required = false)
  private com.example.htmlmud.domain.party.service.PartyService partyService;

  @Value("${game.validation.fail-fast:true}")
  private boolean failFast = true;

  public void setFailFast(boolean failFast) {
    this.failFast = failFast;
  }

  public boolean isFailFast() {
    return failFast;
  }

  public void setDungeonManager(DungeonManager dungeonManager) {
    this.dungeonManager = dungeonManager;
  }

  public void setPartyService(com.example.htmlmud.domain.party.service.PartyService partyService) {
    this.partyService = partyService;
  }

  @Override
  public void run(ApplicationArguments args) {
    validate();
  }

  public ValidationReport validate() {
    log.info("=== 🔍 開始執行遊戲 JSON 啟動期拓撲與資料完整性校驗 (DataIntegrityValidator) ===");
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    int itemsChecked = validateItems(errors, warnings);
    int mobsChecked = validateMobs(errors, warnings);
    int roomsChecked = validateRooms(errors, warnings);
    int shopsChecked = validateShops(errors, warnings);
    int companionsChecked = validateCompanions(errors, warnings);
    int formationsChecked = validateFormations(errors, warnings);
    int dungeonsChecked = validateDungeons(errors, warnings);
    int skillsChecked = validateSkills(errors, warnings);
    int suppliesChecked = validateStartingSupplies(errors, warnings);

    ValidationReport report = new ValidationReport(
        errors, warnings, itemsChecked, mobsChecked, roomsChecked, shopsChecked,
        companionsChecked, formationsChecked, dungeonsChecked, skillsChecked, suppliesChecked);

    if (report.isValid()) {
      log.info("=== ✅ 遊戲資料拓撲校驗全部通過！(物品: {}, 怪物: {}, 房間: {}, 商店: {}, 夥伴: {}, 陣法: {}, 地牢: {}, 技能: {}, 開局物資: {}) ===",
          itemsChecked, mobsChecked, roomsChecked, shopsChecked, companionsChecked, formationsChecked, dungeonsChecked, skillsChecked, suppliesChecked);
      if (report.hasWarnings()) {
        log.warn("⚠️ 存在 {} 項次要警示：\n{}", report.warningCount(), String.join("\n", report.getWarnings()));
      }
    } else {
      String summary = String.format("❌ 遊戲資料拓撲校驗失敗！發現 %d 項嚴重錯誤與 %d 項警示：\n%s",
          report.errorCount(), report.warningCount(), String.join("\n", report.getErrors()));
      log.error(summary);
      if (failFast) {
        throw new DataValidationException(summary, report.getErrors());
      }
    }

    return report;
  }

  private int validateItems(List<String> errors, List<String> warnings) {
    Map<String, ItemTemplate> items = templateReader.getAllItems();
    for (ItemTemplate item : items.values()) {
      if (item.id() == null || item.id().isBlank()) {
        errors.add("發現 ID 為空的物品模板！");
      }
      if (item.name() == null || item.name().isBlank()) {
        warnings.add("物品 [" + item.id() + "] 名稱為空！");
      }
      if (item.equipmentProp() != null && item.equipmentProp().slot() == null) {
        warnings.add("裝備物品 [" + item.id() + "] 未指定裝備部位 (slot)！");
      }
    }
    return items.size();
  }

  private int validateMobs(List<String> errors, List<String> warnings) {
    Map<String, MobTemplate> mobs = templateReader.getAllMobs();
    for (MobTemplate mob : mobs.values()) {
      if (mob.id() == null || mob.id().isBlank()) {
        errors.add("發現 ID 為空的怪物模板！");
        continue;
      }
      // 1. 掉落表校驗
      if (mob.loot() != null) {
        for (LootEntry loot : mob.loot()) {
          if (loot.itemId() != null && !loot.itemId().isBlank()) {
            if (templateReader.findItem(loot.itemId()).isEmpty()) {
              errors.add("怪物 [" + mob.id() + "] 掉落物 [" + loot.itemId() + "] 不存在於物品庫！");
            }
          }
        }
      }
      // 2. 初始裝備校驗
      if (mob.equipment() != null) {
        for (Map.Entry<String, String> eq : mob.equipment().entrySet()) {
          String eqItemId = eq.getValue();
          if (eqItemId != null && !eqItemId.isBlank()) {
            if (templateReader.findItem(eqItemId).isEmpty()) {
              errors.add("怪物 [" + mob.id() + "] 裝備 [" + eq.getKey() + ": " + eqItemId + "] 不存在於物品庫！");
            }
          }
        }
      }
      // 3. 種族定義校驗
      if (mob.race() != null && !mob.race().isBlank()) {
        if (templateReader.findRace(mob.race()).isEmpty()) {
          warnings.add("怪物 [" + mob.id() + "] 指定之種族 [" + mob.race() + "] 未在種族庫註冊！");
        }
      }
      // 4. 啟用技能校驗
      if (mob.enabledSkills() != null) {
        for (Map.Entry<SkillCategory, String> sk : mob.enabledSkills().entrySet()) {
          if (sk.getValue() != null && !sk.getValue().isBlank()) {
            if (templateReader.findSkill(sk.getValue()).isEmpty()) {
              errors.add("怪物 [" + mob.id() + "] 技能 [" + sk.getKey() + ": " + sk.getValue() + "] 不存在於技能庫！");
            }
          }
        }
      }
    }
    return mobs.size();
  }

  private int validateRooms(List<String> errors, List<String> warnings) {
    Map<String, RoomTemplate> rooms = templateReader.getAllRooms();
    for (RoomTemplate room : rooms.values()) {
      if (room.id() == null || room.id().isBlank()) {
        errors.add("發現 ID 為空的房間模板！");
        continue;
      }
      // 1. 出口目標房間校驗
      if (room.exits() != null) {
        for (Map.Entry<String, RoomExit> entry : room.exits().entrySet()) {
          RoomExit exit = entry.getValue();
          if (exit == null) continue;
          if (exit.actionCommand() != null && !exit.actionCommand().isBlank()) {
            continue; // 特殊指令出口跳過
          }
          String targetId = exit.targetRoomId();
          if (targetId == null || targetId.isBlank()) {
            errors.add("房間 [" + room.id() + "] 出口 [" + entry.getKey() + "] 的 targetRoomId 為空！");
            continue;
          }
          String resolvedId = targetId.contains(":") ? targetId
              : (room.zoneId() != null ? room.zoneId() + ":" + targetId : targetId);
          if (templateReader.findRoom(resolvedId).isEmpty() && templateReader.findRoom(targetId).isEmpty()) {
            errors.add("房間 [" + room.id() + "] 出口 [" + entry.getKey() + "] 指向不存在之房間 [" + targetId + "] (解析為 " + resolvedId + ")");
          }
        }
      }
      // 2. 怪物/物品刷新規則校驗
      if (room.spawnRules() != null) {
        for (SpawnRule spawn : room.spawnRules()) {
          if (spawn.id() != null && !spawn.id().isBlank()) {
            if ("MOB".equalsIgnoreCase(spawn.type())) {
              if (templateReader.findMob(spawn.id()).isEmpty()) {
                errors.add("房間 [" + room.id() + "] 刷新規則指向不存在之怪物 [" + spawn.id() + "]！");
              }
            } else if ("ITEM".equalsIgnoreCase(spawn.type())) {
              if (templateReader.findItem(spawn.id()).isEmpty()) {
                errors.add("房間 [" + room.id() + "] 刷新規則指向不存在之物品 [" + spawn.id() + "]！");
              }
            }
          }
        }
      }
    }
    return rooms.size();
  }

  private int validateShops(List<String> errors, List<String> warnings) {
    Map<String, ShopTemplate> shops = templateReader.getAllShops();
    for (ShopTemplate shop : shops.values()) {
      if (shop.id() == null || shop.id().isBlank()) {
        errors.add("發現 ID 為空的商店模板！");
        continue;
      }
      if (shop.roomId() != null && !shop.roomId().isBlank()) {
        if (templateReader.findRoom(shop.roomId()).isEmpty()) {
          errors.add("商店 [" + shop.id() + "] 指定之所在房間 [" + shop.roomId() + "] 不存在！");
        }
      }
      if (shop.goods() != null) {
        for (ShopTemplate.ShopItemTemplate item : shop.goods()) {
          String targetId = item.templateId() != null ? item.templateId() : item.id();
          if (targetId != null && !targetId.isBlank()) {
            if (templateReader.findItem(targetId).isEmpty()) {
              errors.add("商店 [" + shop.id() + "] 商品 [" + targetId + "] 不存在於物品庫！");
            }
          }
        }
      }
    }
    return shops.size();
  }

  private int validateCompanions(List<String> errors, List<String> warnings) {
    var uniqueCompanions = new java.util.HashSet<>(templateReader.getAllCompanions().values());
    for (CompanionTemplate comp : uniqueCompanions) {
      if (comp.id() == null || comp.id().isBlank()) {
        errors.add("發現 ID 為空的夥伴模板！");
        continue;
      }
      if (comp.classId() != null && !comp.classId().isBlank()) {
        boolean validClass = templateReader.findClass(comp.classId()).isPresent()
            || RoleCategory.parseRequirement(comp.classId()) != null;
        if (!validClass) {
          errors.add("夥伴 [" + comp.id() + "] 指定之職業/職能 [" + comp.classId() + "] 無效或未註冊！");
        }
      }
      if (comp.homeRoomId() != null && !comp.homeRoomId().isBlank()) {
        if (templateReader.findRoom(comp.homeRoomId()).isEmpty()) {
          warnings.add("夥伴 [" + comp.id() + "] 預設歸屬房間 [" + comp.homeRoomId() + "] 不存在！");
        }
      }
      if (comp.initialEquipment() != null) {
        for (Map.Entry<EquipmentSlot, String> eq : comp.initialEquipment().entrySet()) {
          if (eq.getValue() != null && !eq.getValue().isBlank()) {
            if (templateReader.findItem(eq.getValue()).isEmpty()) {
              errors.add("夥伴 [" + comp.id() + "] 初始裝備 [" + eq.getKey() + ": " + eq.getValue() + "] 不存在於物品庫！");
            }
          }
        }
      }
      if (comp.skills() != null) {
        for (String skId : comp.skills()) {
          if (skId != null && !skId.isBlank()) {
            if (templateReader.findPartySkill(skId).isEmpty() && templateReader.findSkill(skId).isEmpty()) {
              errors.add("夥伴 [" + comp.id() + "] 招式 [" + skId + "] 不存在於技能庫！");
            }
          }
        }
      }
    }
    return uniqueCompanions.size();
  }

  private int validateFormations(List<String> errors, List<String> warnings) {
    Map<String, FormationTemplate> formations = templateReader.getAllFormations();
    for (FormationTemplate form : formations.values()) {
      if (form.getId() == null || form.getId().isBlank()) {
        errors.add("發現 ID 為空的陣法模板！");
        continue;
      }
      if (form.getRequiredClasses() != null) {
        for (String req : form.getRequiredClasses()) {
          if (req != null && !req.isBlank()) {
            boolean valid = templateReader.findClass(req).isPresent()
                || RoleCategory.parseRequirement(req) != null;
            if (!valid) {
              errors.add("陣法 [" + form.getId() + "] 要求之職業/職能分類 [" + req + "] 無效或不存在！");
            }
          }
        }
      }
    }
    return formations.size();
  }

  private int validateDungeons(List<String> errors, List<String> warnings) {
    if (dungeonManager == null) return 0;
    List<String> floorIds = dungeonManager.getAllFloorIds();
    for (String floorId : floorIds) {
      DungeonFloor floor = dungeonManager.getFloor(floorId);
      if (floor == null) continue;
      if (floor.getStartCoord() == null) {
        errors.add("地牢樓層 [" + floorId + "] 未設定起點坐標 (startCoord)！");
      } else {
        if (!floor.isInBounds(floor.getStartCoord())) {
          errors.add("地牢樓層 [" + floorId + "] 起點坐標 (" + floor.getStartCoord().x() + ", "
              + floor.getStartCoord().y() + ") 超出邊界！");
        } else if (!floor.getTile(floor.getStartCoord()).isPassable()) {
          errors.add("地牢樓層 [" + floorId + "] 起點坐標 (" + floor.getStartCoord().x() + ", "
              + floor.getStartCoord().y() + ") 位於不可通行之障礙牆體！");
        }
      }
      if (floor.getExitRoomId() != null && !floor.getExitRoomId().isBlank()) {
        if (templateReader.findRoom(floor.getExitRoomId()).isEmpty()) {
          errors.add("地牢樓層 [" + floorId + "] 撤退出口房間 [" + floor.getExitRoomId() + "] 不存在於房間庫！");
        }
      }
      if (floor.getMobPool() != null) {
        for (String mobId : floor.getMobPool()) {
          if (mobId != null && !mobId.isBlank()) {
            if (templateReader.findMob(mobId).isEmpty()) {
              errors.add("地牢樓層 [" + floorId + "] 怪物池怪物 [" + mobId + "] 不存在於怪物庫！");
            }
          }
        }
      }

      // 深度盤查樓層所有地塊之掉落物、首領與特殊事件怪物 (GUARD-01)
      for (int y = 0; y < floor.getHeight(); y++) {
        for (int x = 0; x < floor.getWidth(); x++) {
          com.example.htmlmud.domain.dungeon.model.DungeonTile tile = floor.getTile(x, y);
          if (tile == null) continue;
          if (tile.getDrops() != null) {
            for (String dropId : tile.getDrops()) {
              if (dropId != null && !dropId.isBlank()) {
                if (templateReader.findItem(dropId).isEmpty()) {
                  errors.add("地牢樓層 [" + floorId + "] 坐標 (" + x + ", " + y + ") 掉落物 [" + dropId + "] 不存在於物品庫！");
                }
              }
            }
          }
          if (tile.getType() == com.example.htmlmud.domain.dungeon.model.DungeonTile.TileType.BOSS) {
            String bossId = tile.getEventId();
            if (bossId == null || bossId.isBlank()) {
              errors.add("地牢樓層 [" + floorId + "] 首領地塊 (" + x + ", " + y + ") 未配置 eventId (首領怪物 ID)！");
            } else if (templateReader.findMob(bossId).isEmpty()) {
              errors.add("地牢樓層 [" + floorId + "] 首領地塊 (" + x + ", " + y + ") 指定首領怪物 [" + bossId + "] 不存在於怪物庫！");
            }
          } else if (tile.getEventId() != null && !tile.getEventId().isBlank()) {
            if (tile.getEventId().contains("boss_") || tile.getEventId().contains(":mob_") || tile.getEventId().contains("corrupted_")) {
              if (templateReader.findMob(tile.getEventId()).isEmpty()) {
                errors.add("地牢樓層 [" + floorId + "] 事件地塊 (" + x + ", " + y + ") 指定怪物 [" + tile.getEventId() + "] 不存在於怪物庫！");
              }
            }
          }
        }
      }
    }
    return floorIds.size();
  }

  private int validateSkills(List<String> errors, List<String> warnings) {
    Map<String, com.example.htmlmud.template.model.SkillTemplate> skills = templateReader.getAllSkills();
    int count = skills.size();
    for (com.example.htmlmud.template.model.SkillTemplate skill : skills.values()) {
      if (skill.getId() == null || skill.getId().isBlank()) {
        errors.add("發現 ID 為空的技能模板！");
        continue;
      }
      com.example.htmlmud.domain.model.definition.SkillDefinition def = skill.toDefinition();
      if (def != null && def.hasBridgeRules()) {
        for (com.example.htmlmud.domain.model.definition.SkillBridgeRule rule : def.bridgeRules()) {
          String target = rule.targetDrpgSkillId();
          if (target != null && !target.isBlank()) {
            if (templateReader.findPartySkill(target).isEmpty() && templateReader.findSkill(target).isEmpty()) {
              errors.add("技能 [" + skill.getId() + "] 橋接目標 DRPG 技能 [" + target + "] 不存在於技能庫！");
            }
          }
        }
      }
      List<com.example.htmlmud.domain.model.definition.SkillBridgeRule> defaultBridges =
          com.example.htmlmud.domain.model.definition.SkillDefinition.resolveDefaultBridges(skill.getId());
      if (defaultBridges != null) {
        for (com.example.htmlmud.domain.model.definition.SkillBridgeRule rule : defaultBridges) {
          String target = rule.targetDrpgSkillId();
          if (target != null && !target.isBlank()) {
            if (templateReader.findPartySkill(target).isEmpty() && templateReader.findSkill(target).isEmpty()) {
              errors.add("技能 [" + skill.getId() + "] 預設橋接目標 DRPG 技能 [" + target + "] 不存在於技能庫！");
            }
          }
        }
      }
    }

    Map<String, com.example.htmlmud.domain.party.model.PartyMemberSkill> partySkills = templateReader.getAllPartySkills();
    for (var psk : partySkills.values()) {
      if (psk.getId() == null || psk.getId().isBlank()) {
        errors.add("發現 ID 為空的小隊技能模板！");
        continue;
      }
      if (psk.getOverclockTier() < 0 || psk.getOverclockTier() > 3) {
        errors.add("小隊技能 [" + psk.getId() + "] 超頻階級 overclockTier (" + psk.getOverclockTier() + ") 超出合法範圍 [0, 3]！");
      }
    }
    return count + partySkills.size();
  }

  private int validateStartingSupplies(List<String> errors, List<String> warnings) {
    if (partyService == null) return 0;
    Map<String, Integer> supplies = partyService.getStartingSupplies();
    if (supplies != null) {
      for (String itemId : supplies.keySet()) {
        if (itemId != null && !itemId.isBlank()) {
          if (templateReader.findItem(itemId).isEmpty()) {
            errors.add("開局初始物資 [" + itemId + "] 不存在於物品庫！");
          }
        }
      }
      return supplies.size();
    }
    return 0;
  }

  @Getter
  @AllArgsConstructor
  public static class ValidationReport {
    private final List<String> errors;
    private final List<String> warnings;
    private final int itemsChecked;
    private final int mobsChecked;
    private final int roomsChecked;
    private final int shopsChecked;
    private final int companionsChecked;
    private final int formationsChecked;
    private final int dungeonsChecked;
    private final int skillsChecked;
    private final int suppliesChecked;

    public ValidationReport(
        List<String> errors, List<String> warnings, int itemsChecked, int mobsChecked,
        int roomsChecked, int shopsChecked, int companionsChecked, int formationsChecked,
        int dungeonsChecked) {
      this(errors, warnings, itemsChecked, mobsChecked, roomsChecked, shopsChecked,
          companionsChecked, formationsChecked, dungeonsChecked, 0, 0);
    }

    public boolean isValid() {
      return errors.isEmpty();
    }

    public boolean hasWarnings() {
      return !warnings.isEmpty();
    }

    public int errorCount() {
      return errors.size();
    }

    public int warningCount() {
      return warnings.size();
    }

    public List<String> getErrors() {
      return Collections.unmodifiableList(errors);
    }

    public List<String> getWarnings() {
      return Collections.unmodifiableList(warnings);
    }
  }
}
