package com.example.htmlmud.protocol;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.example.htmlmud.domain.actor.impl.Mob;
import com.example.htmlmud.domain.actor.impl.Player;
import com.example.htmlmud.domain.model.entity.GameItem;
import com.example.htmlmud.domain.service.HealthStatusUtil;
import com.example.htmlmud.infra.persistence.repository.TemplateRepository;
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

    return null;
  }

  /**
   * 觀察Mob (Look Mob)
   */
  public static MudMessage<?> mobDetail(Mob mob) {
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
    List<String> itemNames = mob.getTemplate().equipment().values().stream()
        .map(TemplateRepository::findItem).flatMap(Optional::stream)
        .map(item -> item.name() + "(" + item.aliases().get(0) + ")").toList();

    Map<String, Object> data =
        Map.of("id", mob.getTemplate().aliases().get(0), "name", mob.getName(), "gender",
            mob.getTemplate().gender(), "race", mob.getTemplate().race(), "description", lookDesc,
            "hpPercent", pct, "healthStatus", healthStatus, "items", itemNames);

    return MudMessage.builder().type("MOB_DETAIL").payload(data).build();
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
