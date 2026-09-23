package com.example.htmlmud.domain.dungeon.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;
import com.example.htmlmud.domain.party.model.CombatResourceType;
import com.example.htmlmud.domain.repository.TemplateReader;
import com.example.htmlmud.domain.service.TemplateCatalog;

public record DrpgStateDto(
    String type,
    String mode, // "TOWN" or "DUNGEON"
    TownViewDto town,
    DungeonViewDto dungeon,
    PartyViewDto party,
    BattleViewDto battle
) {

  public DrpgStateDto(String type, DungeonViewDto dungeon, PartyViewDto party, BattleViewDto battle) {
    this(type, "DUNGEON", null, dungeon, party, battle);
  }

  public static DrpgStateDto of(DungeonFloor floor, DungeonPosition pos, String forwardInspection, Party party) {
    return of(floor, pos, forwardInspection, party, null, new TemplateCatalog());
  }

  public static DrpgStateDto of(DungeonFloor floor, DungeonPosition pos, String forwardInspection, Party party, BattleViewDto battle) {
    return of(floor, pos, forwardInspection, party, battle, new TemplateCatalog());
  }

  public static DrpgStateDto of(DungeonFloor floor, DungeonPosition pos, String forwardInspection,
      Party party, BattleViewDto battle, TemplateReader templateReader) {
    DungeonViewDto dungeonView = null;
    if (floor != null && pos != null) {
      int w = floor.getWidth();
      int h = floor.getHeight();
      String[][] tileSymbols = new String[h][w];
      boolean[][] visited = new boolean[h][w];

      boolean[][] openedChests = new boolean[h][w];

      for (int y = 0; y < h; y++) {
        for (int x = 0; x < w; x++) {
          DungeonTile tile = floor.getTile(x, y);
          boolean isOpened = pos.isChestOpened(x, y);
          openedChests[y][x] = isOpened;
          if (tile != null && tile.getType() == DungeonTile.TileType.TREASURE && isOpened) {
            tileSymbols[y][x] = "◇";
          } else {
            tileSymbols[y][x] = tile != null ? tile.getType().getSymbol() : "#";
          }
          visited[y][x] = pos.isVisited(x, y);
        }
      }

      dungeonView = new DungeonViewDto(
          floor.getId(),
          floor.getName(),
          pos.getX(),
          pos.getY(),
          pos.getFacing().name(),
          pos.getFacing().getArrow(),
          w,
          h,
          tileSymbols,
          visited,
          forwardInspection != null ? forwardInspection : "",
          pos.getDangerLevel(),
          pos.getDangerStatus(),
          openedChests
      );
    }

    PartyViewDto partyView = toPartyViewDto(party, templateReader);
    return new DrpgStateDto("DRPG_STATE", "DUNGEON", null, dungeonView, partyView, battle);
  }

  public static DrpgStateDto ofTown(
      String zoneId,
      String zoneName,
      String roomId,
      String roomName,
      String description,
      boolean safeZone,
      List<TownExitDto> exits,
      List<TownNpcDto> npcs,
      List<TownItemDto> items,
      Party party
  ) {
    return ofTown(zoneId, zoneName, roomId, roomName, description, safeZone, exits, npcs, items, party, null,
        new TemplateCatalog());
  }

  public static DrpgStateDto ofTown(
      String zoneId,
      String zoneName,
      String roomId,
      String roomName,
      String description,
      boolean safeZone,
      List<TownExitDto> exits,
      List<TownNpcDto> npcs,
      List<TownItemDto> items,
      Party party,
      BattleViewDto battle
  ) {
    return ofTown(zoneId, zoneName, roomId, roomName, description, safeZone, exits, npcs, items, party, battle,
        new TemplateCatalog());
  }

  public static DrpgStateDto ofTown(
      String zoneId,
      String zoneName,
      String roomId,
      String roomName,
      String description,
      boolean safeZone,
      List<TownExitDto> exits,
      List<TownNpcDto> npcs,
      List<TownItemDto> items,
      Party party,
      BattleViewDto battle,
      TemplateReader templateReader
  ) {
    TownViewDto townView = new TownViewDto(zoneId, zoneName, roomId, roomName, description, safeZone, exits, npcs, items);
    PartyViewDto partyView = toPartyViewDto(party, templateReader);
    return new DrpgStateDto("DRPG_STATE", "TOWN", townView, null, partyView, battle);
  }

  public static PartyViewDto toPartyViewDto(Party party) {
    return toPartyViewDto(party, new TemplateCatalog());
  }

  public static PartyViewDto toPartyViewDto(Party party, TemplateReader templateReader) {
    if (party == null) return null;
    List<PartyMemberViewDto> memberViews = new ArrayList<>();
    for (PartyMember m : party.getMembers()) {
      int hp = m.getStats() != null ? m.getStats().getHp() : 100;
      int maxHp = m.getStats() != null ? m.getStats().getMaxHp() : 100;
      int mp = m.getStats() != null ? m.getStats().getMp() : 50;
      int maxMp = m.getStats() != null ? m.getStats().getMaxMp() : 50;
      int san = m.getCurrentSan();
      int maxSan = m.getMaxSan();

      String level = "NORMAL";
      if (san <= 20) {
        level = "MADNESS";
      } else if (san <= 50) {
        level = "DANGER";
      } else if (san <= 75) {
        level = "WARNING";
      }

      String resType = m.getResourceType() != null ? m.getResourceType().name() : "MP";
      int curRes = mp;
      int maxRes = maxMp;
      if (m.getResourceType() == CombatResourceType.RAGE) {
        curRes = m.getCurrentRage();
        maxRes = m.getMaxRage();
      } else if (m.getResourceType() == CombatResourceType.COMBO) {
        curRes = m.getCurrentCombo();
        maxRes = m.getMaxCombo();
      }

      List<PartySkillViewDto> skillDtos = new ArrayList<>();
      if (m.getSkills() != null) {
        for (var s : m.getSkills()) {
          boolean avail = true;
          if (m.isOnCooldown(s.getId())) avail = false;
          if (s.getCostType() == CombatResourceType.MP && mp < s.getCostValue()) avail = false;
          if (s.getCostType() == CombatResourceType.SP && m.getCurrentSp() < s.getCostValue()) avail = false;
          if (s.getCostType() == CombatResourceType.RAGE && m.getCurrentRage() < s.getCostValue()) avail = false;
          if (s.getCostType() == CombatResourceType.COMBO && m.getCurrentCombo() < s.getCostValue()) avail = false;
          if (s.getCostType() == CombatResourceType.HP && hp <= s.getCostValue()) avail = false;
          if (!m.isSkillUsable(s)) avail = false;

          String costDesc = "無消耗";
          if (s.getCostType() != null && s.getCostValue() > 0) {
            costDesc = switch (s.getCostType()) {
              case SP -> s.getCostValue() + " 戰氣";
              case HP -> s.getCostValue() + " 氣血";
              case RAGE -> s.getCostValue() + " 怒氣";
              case COMBO -> s.getCostValue() + " 連擊";
              case MP -> s.getCostValue() + " 真元";
              case ENERGY -> s.getCostValue() + " 精力";
              case FORCE -> s.getCostValue() + " 內力";
            };
          }
          if (!m.isSkillUsable(s) && s.getAllowedWeapons() != null && !s.getAllowedWeapons().isEmpty()) {
            costDesc += " (需" + String.join("/", s.getAllowedWeapons()) + ")";
          }

          skillDtos.add(new PartySkillViewDto(
              s.getId(),
              s.getName(),
              s.getIcon(),
              s.getDescription(),
              s.getCostType() != null ? s.getCostType().name() : "MP",
              s.getCostValue(),
              costDesc,
              s.getCooldownMs(),
              m.getRemainingCooldownMs(s.getId()),
              avail,
              s.getCategory() != null ? s.getCategory() : "CLASS",
              s.isSynergy()
          ));
        }
      }

      Map<String, PartyItemSlotViewDto> equipMap = new HashMap<>();
      if (m.getEquipment() != null) {
        m.getEquipment().forEach((slot, item) -> {
          if (slot != null && item != null) {
            equipMap.put(slot.name(), PartyItemSlotViewDto.of(item));
          }
        });
      }

      var basicSkill = m.getEnabledBasicSkill();
      String basicSkillId = m.getEffectiveBasicSkillId();
      String basicSkillName = (basicSkill != null) ? basicSkill.getName() : "基礎武學";

      com.example.htmlmud.domain.model.enums.SkillCategory currentCat =
          PartyMember.toSkillCategory(m.getMainHandWeaponType());
      List<PartyStanceSkillDto> availableStances = new ArrayList<>();

      // 1. 預設基礎套路 (如 basic_sword, basic_fist 等)
      String defaultBasicId = m.getBasicSkillId();
      var defSkillOpt = templateReader.findSkill(defaultBasicId);
      String defName = defSkillOpt.map(com.example.htmlmud.domain.model.template.SkillTemplate::getName).orElse(defaultBasicId);
      String defDesc = defSkillOpt.map(com.example.htmlmud.domain.model.template.SkillTemplate::getDescription).orElse("");
      availableStances.add(new PartyStanceSkillDto(defaultBasicId, defName, currentCat.name(), defDesc, defaultBasicId.equals(basicSkillId)));

      // 2. 角色已學的該武器分類套路 (若有)
      if (m.getLearnedStances() != null) {
        for (String stId : m.getLearnedStances()) {
          if (stId.equals(defaultBasicId)) continue;
          var skOpt = templateReader.findSkill(stId);
          if (skOpt.isPresent()) {
            var sk = skOpt.get();
            if (PartyMember.resolveSkillCategory(sk) == currentCat) {
              availableStances.add(new PartyStanceSkillDto(stId, sk.getName(), currentCat.name(), sk.getDescription(), stId.equals(basicSkillId)));
            }
          }
        }
      }

      List<TacticsRuleViewDto> tacticsDtos = (m.getTactics() != null)
          ? m.getTactics().stream().map(r -> TacticsRuleViewDto.of(r, m, templateReader)).toList()
          : List.of();

      int memberLevel = (m.getStats() != null) ? m.getStats().getLevel() : 1;
      int memberExp = (m.getStats() != null) ? m.getStats().getExp() : 0;
      long nextExp = (m.getStats() != null) ? m.getStats().getNextLevelExp() : 180;
      int freePoints = (m.getStats() != null) ? m.getStats().getFreeStatPoints() : 0;
      int str = (m.getStats() != null) ? m.getStats().getStr() : 5;
      int con = (m.getStats() != null) ? m.getStats().getCon() : 5;
      int dex = (m.getStats() != null) ? m.getStats().getDex() : 5;
      int intStat = (m.getStats() != null) ? m.getStats().getIntelligence() : 5;
      int wis = (m.getStats() != null) ? m.getStats().getWis() : 5;

      memberViews.add(new PartyMemberViewDto(
          m.getId(),
          m.getName(),
          m.getRoleTitle(),
          m.getRow() != null ? m.getRow().name() : "FRONT",
          hp,
          maxHp,
          mp,
          maxMp,
          san,
          maxSan,
          m.getSanityStatus(),
          level,
          resType,
          curRes,
          maxRes,
          m.isAlive(),
          skillDtos,
          m.getMadnessState() != null ? m.getMadnessState().name() : "SANE",
          m.getAberrationCounter(),
          PartyItemSlotViewDto.of(m.getEquippedWeapon()),
          PartyItemSlotViewDto.of(m.getEquippedArmor()),
          equipMap,
          basicSkillId,
          basicSkillName,
          availableStances,
          m.getClassId(),
          m.getEffectiveClassName(),
          m.getClassTemplate().map(com.example.htmlmud.domain.model.template.ClassTemplate::description).orElse(""),
          tacticsDtos,
          memberLevel,
          memberExp,
          nextExp,
          freePoints,
          str,
          con,
          dex,
          intStat,
          wis
      ));
    }

    String formName = party.getEquippedFormation() != null ? party.getEquippedFormation().getName() : "無";
    String ultName = (party.getEquippedFormation() != null && party.getEquippedFormation().getUltimateSkill() != null)
        ? party.getEquippedFormation().getUltimateSkill().getName() : "";

    PartyInventoryViewDto invView = PartyInventoryViewDto.of(party.getInventory());

    return new PartyViewDto(
        party.getPartyName(),
        formName,
        party.getFormationEnergy(),
        ultName,
        party.canCastUltimate(),
        memberViews,
        invView
    );
  }

  public record TownExitDto(
      String direction,
      String displayName,
      String targetRoomId,
      String targetRoomName
  ) {}

  public record TownCapabilityDto(
      String type,
      String label,
      String command,
      String icon
  ) {}

  public record TownNpcDto(
      String id,
      String alias,
      String name,
      String title,
      String status,
      List<TownCapabilityDto> capabilities,
      String rank,
      boolean isUnique
  ) {
    public TownNpcDto(String id, String alias, String name, String title, String status, List<TownCapabilityDto> capabilities) {
      this(id, alias, name, title, status, capabilities, "NORMAL", false);
    }
  }

  public record TownItemDto(
      String id,
      String name,
      String icon,
      int count
  ) {}

  public record TownViewDto(
      String zoneId,
      String zoneName,
      String roomId,
      String roomName,
      String description,
      boolean safeZone,
      List<TownExitDto> exits,
      List<TownNpcDto> npcs,
      List<TownItemDto> items
  ) {}

  public record DungeonViewDto(
      String floorId,
      String floorName,
      int x,
      int y,
      String direction,
      String directionArrow,
      int width,
      int height,
      String[][] tiles,
      boolean[][] visited,
      String forwardInspection,
      int dangerLevel,
      String dangerStatus,
      boolean[][] openedChests
  ) {}

  public record PartyItemSlotViewDto(
      String slotId,
      String itemId,
      String name,
      String icon,
      String itemType,
      String subType,
      String equipSlot,
      int count,
      String description,
      String quality,
      String effectType,
      int effectValue,
      String grantedSkillName,
      int bonusMinDamage,
      int bonusMaxDamage,
      int bonusDefense,
      int bonusHp,
      int bonusSan,
      boolean consumable,
      boolean weapon,
      boolean armor,
      boolean equipment
  ) {
    public static PartyItemSlotViewDto of(com.example.htmlmud.domain.party.model.PartyItemSlot s) {
      if (s == null) return null;
      return new PartyItemSlotViewDto(
          s.getSlotId(),
          s.getItemId(),
          s.getName(),
          s.getIcon(),
          s.getItemType() != null ? s.getItemType().name() : "MISC",
          s.getSubType(),
          s.getEquipSlot() != null ? s.getEquipSlot().name() : null,
          s.getCount(),
          s.getDescription(),
          s.getQuality(),
          s.getEffectType(),
          s.getEffectValue(),
          s.getGrantedSkillName(),
          s.getBonusMinDamage(),
          s.getBonusMaxDamage(),
          s.getBonusDefense(),
          s.getBonusHp(),
          s.getBonusSan(),
          s.isConsumable(),
          s.isWeapon(),
          s.isArmor(),
          s.isEquipment()
      );
    }
  }

  public record PartyInventoryViewDto(
      int capacity,
      int usedCount,
      List<PartyItemSlotViewDto> slots
  ) {
    public static PartyInventoryViewDto of(com.example.htmlmud.domain.party.model.PartyInventory inv) {
      if (inv == null) return new PartyInventoryViewDto(30, 0, List.of());
      List<PartyItemSlotViewDto> slotDtos = inv.getSlots().stream()
          .map(PartyItemSlotViewDto::of)
          .toList();
      return new PartyInventoryViewDto(inv.getCapacity(), slotDtos.size(), slotDtos);
    }
  }

  public record PartyViewDto(
      String name,
      String formationName,
      int formationEnergy,
      String ultimateSkillName,
      boolean canCastUltimate,
      List<PartyMemberViewDto> members,
      PartyInventoryViewDto inventory
  ) {}

  public record PartyMemberViewDto(
      String id,
      String name,
      String roleTitle,
      String row,
      int hp,
      int maxHp,
      int mp,
      int maxMp,
      int san,
      int maxSan,
      String sanState,
      String sanLevel,
      String resourceType,
      int currentResource,
      int maxResource,
      boolean alive,
      List<PartySkillViewDto> skills,
      String madnessState,
      int aberrationCounter,
      PartyItemSlotViewDto equippedWeapon,
      PartyItemSlotViewDto equippedArmor,
      Map<String, PartyItemSlotViewDto> equipment,
      String basicSkillId,
      String basicSkillName,
      List<PartyStanceSkillDto> availableStances,
      String classId,
      String className,
      String classDescription,
      List<TacticsRuleViewDto> tactics,
      int level,
      int exp,
      long nextLevelExp,
      int freeStatPoints,
      int str,
      int con,
      int dex,
      int intStat,
      int wis
  ) {}

  public record TacticsRuleViewDto(
      int priority,
      String condition,
      String conditionLabel,
      int conditionValue,
      String target,
      String targetLabel,
      String skillId,
      String skillName,
      boolean enabled,
      String description
  ) {
    public static TacticsRuleViewDto of(com.example.htmlmud.domain.party.model.TacticsRule r, com.example.htmlmud.domain.party.model.PartyMember m) {
      return of(r, m, new TemplateCatalog());
    }

    public static TacticsRuleViewDto of(com.example.htmlmud.domain.party.model.TacticsRule r,
        com.example.htmlmud.domain.party.model.PartyMember m, TemplateReader templateReader) {
      if (r == null) return null;
      String skillName = r.getSkillId();
      var skOpt = templateReader.findSkill(r.getSkillId());
      if (skOpt.isPresent()) {
        skillName = skOpt.get().getName();
      } else if (m.getSkills() != null) {
        skillName = m.getSkills().stream()
            .filter(s -> s.getId().equalsIgnoreCase(r.getSkillId()))
            .map(com.example.htmlmud.domain.party.model.PartyMemberSkill::getName)
            .findFirst()
            .orElse(r.getSkillId());
      }
      return new TacticsRuleViewDto(
          r.getPriority(),
          r.getCondition() != null ? r.getCondition().name() : "",
          r.getCondition() != null ? r.getCondition().getLabel() : "",
          r.getConditionValue(),
          r.getTarget() != null ? r.getTarget().name() : "",
          r.getTarget() != null ? r.getTarget().getLabel() : "",
          r.getSkillId(),
          skillName,
          r.isEnabled(),
          r.formatDescription(skillName)
      );
    }
  }

  public record PartyStanceSkillDto(
      String skillId,
      String skillName,
      String category,
      String description,
      boolean isCurrentEnabled
  ) {}
}
