package com.example.htmlmud.protocol;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.service.HealthStatusUtil;
import com.example.htmlmud.template.TemplateReader;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class MessageFactory {

  /**
   * 1. 觀察房間 (Look Room)
   */
  public static MudMessage<?> roomDescription(String name, String desc, List<String> exits,
      List<String> items, List<String> mobs) {
    Map<String, Object> data =
        Map.of("name", name, "description", desc, "exits", exits, "items", items, "mobs", mobs);
    return MudMessage.builder().type("ROOM_DESC").payload(data)
        .rawText(String.format("[%s]\n%s", name, desc)).build();
  }

  /**
   * 觀察Player (Look Player)
   */
  public static MudMessage<?> playerDetail(Player player) {
    log.info("playerDetail playerId:{}", player.getId());
    String noun = player.getStats().getGender().getHe();

    String lookDesc = player.getLookDescription();
    if (lookDesc == null)
      lookDesc = "這是一位冒險者。";
    lookDesc = lookDesc.replace("$N", noun);

    double pct = (double) player.getStats().getHp() / player.getStats().getMaxHp();
    String healthStatus =
        HealthStatusUtil.getHealthStatus(player.getStats().getHp(), player.getStats().getMaxHp())
            .replace("$N", noun);

    // 取出裝備的真實名稱 (優先從小隊隊長真實裝備讀取)
    List<String> itemNames;
    var ps = (player.getService() != null) ? player.getService().getPartyService() : null;
    var party = (ps != null) ? ps.getOrCreateParty(player.getName()) : null;
    if (party != null && party.getLeader() != null && party.getLeader().getEquipment() != null) {
      itemNames = party.getLeader().getEquipment().values().stream()
          .filter(slot -> slot != null && !slot.isEmpty())
          .map(com.example.htmlmud.party.PartyItemSlot::getName)
          .toList();
    } else if (player.getStats() != null && player.getStats().getEquipment() != null) {
      itemNames = player.getStats().getEquipment().values().stream()
          .map(item -> item.getName()
              + (item.getAliases().isEmpty() ? "" : "(" + item.getAliases().get(0) + ")"))
          .toList();
    } else {
      itemNames = List.of();
    }

    Map<String, Object> data = Map.of("id", player.getName(), "name",
        player.getNickname() != null ? player.getNickname() : player.getName(),
        "gender", player.getStats().getGender(), "race", player.getStats().getRace(), "description",
        lookDesc, "hpPercent", pct, "healthStatus", healthStatus, "items", itemNames);

    return MudMessage.builder().type("ENTITY_DETAIL").payload(data).build();
  }

  /**
   * 觀察Mob (Look Mob)
   */
  public static MudMessage<?> mobDetail(Mob mob) {
    return mobDetail(mob, null);
  }

  public static MudMessage<?> mobDetail(Mob mob, TemplateReader templateReader) {
    log.info("mobDetail mobId:{}", mob.getTemplate().id());
    String noun = mob.getStats().getGender().getHe();
    String lookDesc = mob.getTemplate().lookDescription().replace("$N", noun);
    if (lookDesc == null) {
      lookDesc = mob.getTemplate().description().replace("$N", noun);
    }

    double pct = (double) mob.getStats().getHp() / mob.getStats().getMaxHp();
    String healthStatus = HealthStatusUtil
        .getHealthStatus(mob.getStats().getHp(), mob.getStats().getMaxHp()).replace("$N", noun);

    // 取出裝備的 GameItem 的 name(alias)
    List<String> itemNames = (templateReader != null && mob.getTemplate().equipment() != null)
        ? mob.getTemplate().equipment().values().stream()
            .map(templateReader::findItem).flatMap(Optional::stream)
            .map(item -> item.name() + (item.aliases() != null && !item.aliases().isEmpty() ? "(" + item.aliases().get(0) + ")" : ""))
            .toList()
        : List.of();

    Map<String, Object> data =
        Map.of("id", mob.getTemplate().aliases().get(0), "name", mob.getName(), "gender",
            mob.getTemplate().gender(), "race", mob.getTemplate().race(), "description", lookDesc,
            "hpPercent", pct, "healthStatus", healthStatus, "items", itemNames);

    return MudMessage.builder().type("ENTITY_DETAIL").payload(data).build();
  }

  public static MudMessage<?> corpseDetail(GameItem item) {
    List<String> itemNames = item.getContents().stream().map(GameItem::getName).toList();
    Map<String, Object> data = Map.of("id", item.getId(), "name", item.getName(), "description",
        item.getDescription(), "items", itemNames);

    return MudMessage.builder().type("CORPSE_DETAIL").payload(data).build();
  }

  public static MudMessage<?> containerDetail(GameItem item) {
    List<String> itemNames = item.getContents().stream().map(GameItem::getName).toList();
    Map<String, Object> data = Map.of("id", item.getId(), "name", item.getName(), "description",
        item.getDescription(), "items", itemNames);

    return MudMessage.builder().type("CONTAINER_DETAIL").payload(data).build();
  }

  public static MudMessage<?> itemDetail(GameItem item) {
    Map<String, Object> data =
        Map.of("id", item.getId(), "name", item.getName(), "description", item.getDescription());

    return MudMessage.builder().type("ITEM_DETAIL").payload(data).build();
  }

  /**
   * 觀察實體 (Look Mob / Item)
   */
  public static MudMessage<?> entityDetail(String name, String desc, int hpPercent,
      List<String> actions) {
    Map<String, Object> data =
        Map.of("name", name, "description", desc, "hpPercent", hpPercent, "actions", actions);
    return MudMessage.builder().type("ENTITY_DETAIL").payload(data)
        .rawText(desc + (hpPercent >= 0 ? "\n狀態: " + hpPercent + "%" : "")).build();
  }

  /**
   * 3. 戰鬥訊息 (Combat)
   */
  public static MudMessage<?> combatLog(String attacker, String victim, int damage,
      String skillName) {
    Map<String, Object> data =
        Map.of("attacker", attacker, "victim", victim, "damage", damage, "skill", skillName);
    return MudMessage.builder().type("COMBAT_LOG").payload(data)
        .rawText(String.format("%s 使用 %s 對 %s 造成 %d 點傷害！", attacker, skillName, victim, damage))
        .build();
  }

  /**
   * 4. 簡單系統/動作反饋 (System/Action)
   */
  public static MudMessage<?> info(String text) {
    return MudMessage.builder().type("SYSTEM").rawText(text).build();
  }
}
