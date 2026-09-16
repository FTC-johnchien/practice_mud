package com.example.htmlmud.domain.dungeon.dto;

import java.util.ArrayList;
import java.util.List;
import com.example.htmlmud.domain.dungeon.model.DungeonFloor;
import com.example.htmlmud.domain.dungeon.model.DungeonPosition;
import com.example.htmlmud.domain.dungeon.model.DungeonTile;
import com.example.htmlmud.domain.party.model.Party;
import com.example.htmlmud.domain.party.model.PartyMember;

public record DrpgStateDto(
    String type,
    DungeonViewDto dungeon,
    PartyViewDto party,
    BattleViewDto battle
) {

  public static DrpgStateDto of(DungeonFloor floor, DungeonPosition pos, String forwardInspection, Party party) {
    return of(floor, pos, forwardInspection, party, null);
  }

  public static DrpgStateDto of(DungeonFloor floor, DungeonPosition pos, String forwardInspection, Party party, BattleViewDto battle) {
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

    PartyViewDto partyView = null;
    if (party != null) {
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
        if (m.getResourceType() == com.example.htmlmud.domain.party.model.ResourceType.RAGE) {
          curRes = m.getCurrentRage();
          maxRes = m.getMaxRage();
        } else if (m.getResourceType() == com.example.htmlmud.domain.party.model.ResourceType.COMBO) {
          curRes = m.getCurrentCombo();
          maxRes = m.getMaxCombo();
        }

        List<PartySkillViewDto> skillDtos = new ArrayList<>();
        if (m.getSkills() != null) {
          for (var s : m.getSkills()) {
            boolean avail = true;
            if (m.isOnCooldown(s.getId())) avail = false;
            if (s.getCostType() == com.example.htmlmud.domain.party.model.ResourceType.MP && mp < s.getCostValue()) avail = false;
            if (s.getCostType() == com.example.htmlmud.domain.party.model.ResourceType.RAGE && m.getCurrentRage() < s.getCostValue()) avail = false;
            if (s.getCostType() == com.example.htmlmud.domain.party.model.ResourceType.COMBO && m.getCurrentCombo() < s.getCostValue()) avail = false;

            String costDesc = "無消耗";
            if (s.getCostType() != null && s.getCostValue() > 0) {
              costDesc = switch (s.getCostType()) {
                case RAGE -> s.getCostValue() + " 怒氣";
                case COMBO -> s.getCostValue() + " 連擊";
                case MP -> s.getCostValue() + " 真元";
              };
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
                avail
            ));
          }
        }

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
            PartyItemSlotViewDto.of(m.getEquippedArmor())
        ));
      }

      String formName = party.getEquippedFormation() != null ? party.getEquippedFormation().getName() : "無";
      String ultName = (party.getEquippedFormation() != null && party.getEquippedFormation().getUltimateSkill() != null)
          ? party.getEquippedFormation().getUltimateSkill().getName() : "";

      PartyInventoryViewDto invView = PartyInventoryViewDto.of(party.getInventory());

      partyView = new PartyViewDto(
          party.getPartyName(),
          formName,
          party.getFormationEnergy(),
          ultName,
          party.canCastUltimate(),
          memberViews,
          invView
      );
    }

    return new DrpgStateDto("DRPG_STATE", dungeonView, partyView, battle);
  }

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
      boolean armor
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
          s.isArmor()
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
      PartyItemSlotViewDto equippedArmor
  ) {}
}
